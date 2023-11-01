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

public class LoadCustomer {
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
        
        //Create database passing true. 
        CreateDatabase.main(new String[] {"true"});

        //Call store loads! Passing null as args will bypass create database.
        LoadStore.main(null);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/customer.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM CUSTOMER");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long customer_id = rs.getLong("customer_id");
            long store_id = rs.getLong("store_id");
            String first_name = rs.getString("first_name");
            String last_name = rs.getString("last_name");
            String email = rs.getString("email");
            long address_id = rs.getLong("address_id");
            String activebool = rs.getString("activebool");
            java.sql.Timestamp create_date = rs.getTimestamp("create_date");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");
            long active = rs.getLong("active");

            System.out.println("Inserting row into Datomic :" + customer_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :customer/customer_id " + customer_id);
            b.append(" :customer/store_id [:store/store_id " + store_id + "]");
            b.append(" :customer/first_name \"" + first_name + "\"");
            b.append(" :customer/last_name \"" + last_name + "\"");
            b.append(" :customer/email \"" + email + "\"");       
            
            //Reference to the address object...
            b.append(" :customer/address_id [:address/address_id " + address_id + "]");
            b.append(" :customer/activebool " + activebool.equalsIgnoreCase("t") );
            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :customer/create_date " + "#inst " + "\"" + sdf1.format(create_date) + "T"
                    + sdf2.format(create_date) + "\"");
            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :customer/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
            b.append(" :customer/active " + active);
            b.append("}");

            //System.out.println( b.toString());

            Object aTxn = datomic.Util.read(b.toString());
            Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
            //System.out.println(resultsFromData);
        }

        rs.close();
        pgConn.close();

        // Get the database, to get a fresh copy.
        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        System.out.println("Printing out customers...");
        String q = "[:find ?v1 ?v2 ?v3 ?v4 ?v5 ?v6 ?v7 ?v8 ?v9 ?v10 :where " + 
            "[?e :customer/customer_id ?v1]" + 
            "[?e :customer/store_id ?v2]" +
            "[?e :customer/first_name ?v3]" + 
            "[?e :customer/last_name ?v4]" + 
            "[?e :customer/email ?v5]" +
            "[?e :customer/address_id ?v6]" +
            "[?e :customer/activebool ?v7]" +
            "[?e :customer/create_date ?v8]" +
            "[?e :customer/last_update ?v9]" +
            "[?e :customer/active ?v10]" +
            "]";

        getResults(db, q);

        System.out.println("Printing out customer count...");
        q = "[:find (count ?aid) . :where [?aid :customer/customer_id ]]";
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
