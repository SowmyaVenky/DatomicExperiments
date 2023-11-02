package com.gssystems.datomic.docker;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gssystems.datomic.SeattleDataExample;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;

public class ActorsLoad {
    private static final  int MAX_RECORDS_TO_SHOW = 10;
    public static void main(String[] args) throws Exception {
        // Connect to JDBC and pull from Postgres
        String url = "jdbc:postgresql://localhost:5432/dvdrental";
        String user = "postgres";
        String password = "Ganesh20022002";

        java.sql.Connection pgConn = null;
        try {
            pgConn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String uri = "datomic:free://localhost:4334/dvdrental?password=datomic";
        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/actor.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM ACTOR");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long actor_id = rs.getLong("actor_id");
            String first_name = rs.getString("first_name");
            String last_name = rs.getString("last_name");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting row into Datomic :" + actor_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :actor/actor_id " + actor_id);
            b.append(" :actor/first_name \"" + first_name + "\"");
            b.append(" :actor/last_name \"" + last_name + "\"");

            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :actor/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
            b.append("}");

            // System.out.println( b.toString());

            Object aTxn = datomic.Util.read(b.toString());
            Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
            //System.out.println(resultsFromData);
        }

        rs.close();
        pgConn.close();

        // Get the database, to get a fresh copy.
        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        System.out.println("Printing out names of actors...");
        String q = "[:find ?aid ?fname ?lname ?lupd :where [?e :actor/actor_id ?aid][?e :actor/first_name ?fname][?e :actor/last_name ?lname][?e :actor/last_update ?lupd] ]";

        getResults(db, q);

        System.out.println("Printing out actor count...");
        q = "[:find (count ?aid) . :where [?aid :actor/actor_id ]]";
        getResults(db, q);

        System.exit(0);
    }

    private static void getResults(Database db, String q) {
        Object res = Peer.query(q, db);
        if (res instanceof Collection) {
            Collection<?> results = (Collection<?>) res;
            int x = 0;
            System.out.println("Total number of records : " + results.size());
            System.out.println("Showing top 10 ");
            System.out.println("===================");
            for (Object o : results) {
                x++;
                if( x >= MAX_RECORDS_TO_SHOW) {
                    break;
                }
                System.out.println(o);
            }
            System.out.println("===================");
        } else if( res instanceof Integer) {
            System.out.println( "Result is : " + res );
        }
    }
}
