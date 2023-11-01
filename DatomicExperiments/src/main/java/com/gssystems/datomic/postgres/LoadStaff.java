package com.gssystems.datomic.postgres;

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

public class LoadStaff {
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


        String uri = "datomic:mem://dvdrental";
        
        //Call store loads!
        LoadStore.main(args);

        //System.out.println("Creating a new database called dvdrental...");
        //Peer.createDatabase(uri);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/staff.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM STAFF");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long staff_id = rs.getLong("staff_id");
            String first_name = rs.getString("first_name");
            String last_name = rs.getString("last_name");
            long address_id = rs.getLong("address_id");
            String email = rs.getString("email");
            long store_id = rs.getLong("store_id");
            String active = rs.getString("active");
            String username = rs.getString("username");
            String pw = rs.getString("password");
            String picture = rs.getString("picture");

            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting row into Datomic :" + staff_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :staff/staff_id " + staff_id);
            b.append(" :staff/first_name \"" + first_name + "\"");
            b.append(" :staff/last_name \"" + last_name + "\"");

            //Reference to the address object...
            b.append(" :staff/address_id [:address/address_id " + address_id + "]");

            b.append(" :staff/email \"" + email + "\"");            
            b.append(" :staff/store_id [:store/store_id " + store_id + "]");
            b.append(" :staff/active \"" + active + "\"");
            b.append(" :staff/username \"" + username + "\"");  
            b.append(" :staff/password \"" + pw + "\"");

            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :staff/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
            b.append("}");

            //System.out.println( b.toString());

            Object aTxn = datomic.Util.read(b.toString());
            Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
            //System.out.println(resultsFromData);
        }

        // Get the database, to get a fresh copy.
        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        System.out.println("Printing out staff...");
        String q = "[:find ?v1 ?v2 ?v3 ?v4 ?v5 ?v6 ?v7 ?v8 ?v9 ?v10 :where " + 
            "[?e :staff/store_id ?v1]" + 
            "[?e :staff/first_name ?v2]" +
            "[?e :staff/last_name ?v3]" + 
            "[?e :staff/address_id ?v4]" +
            "[?e :staff/email ?v5]" +
            "[?e :staff/store_id ?v6]" +
            "[?e :staff/active ?v7]" +
            "[?e :staff/username ?v8]" +
            "[?e :staff/password ?v9]" +
            "[?e :staff/last_update ?v10]" +
            "]";

        getResults(db, q);

        System.out.println("Printing out staff count...");
        q = "[:find (count ?aid) . :where [?aid :staff/staff_id ]]";
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
