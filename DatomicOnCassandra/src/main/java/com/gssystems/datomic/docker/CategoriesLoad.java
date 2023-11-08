package com.gssystems.datomic.docker;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;

public class CategoriesLoad {
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

        String uri = Constants.DATOMIC_URL;

        System.out.println("Creating a new database called dvdrental...");

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = CategoriesLoad.class.getClassLoader()
                .getResourceAsStream("dvdrental/category.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM CATEGORY");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long category_id = rs.getLong("category_id");
            String cat_name = rs.getString("name");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting row into Datomic :" + category_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :category/category_id " + category_id);
            b.append(" :category/name \"" + cat_name + "\"");

            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :category/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
            b.append("}");

            // System.out.println( b.toString());

            Object aTxn = datomic.Util.read(b.toString());
            Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
            System.out.println(resultsFromData);
        }

        rs.close();
        pgConn.close();
        
        // Get the database, to get a fresh copy.
        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        System.out.println("Printing out names of categories...");
        String q = "[:find ?cid ?cname ?lupd :where [?e :category/category_id ?cid][?e :category/name ?cname][?e :category/last_update ?lupd] ]";

        getResults(db, q);

        System.out.println("Printing out category count...");
        q = "[:find (count ?aid) . :where [?aid :category/category_id ]]";
        getResults(db, q);
        if (args != null && args.length == 1 && args[0].equalsIgnoreCase("true")) {
            //Stand-alone run, can kill session to allow maven to terminate.
            System.exit(0);
        }
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
