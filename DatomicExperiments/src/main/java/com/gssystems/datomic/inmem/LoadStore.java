package com.gssystems.datomic.inmem;

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

public class LoadStore {
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

        //Call address loads! Passing null as args will bypass create database.
        AddressLoad.main(args);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/store.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM STORE");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long store_id = rs.getLong("store_id");
            long manager_staff_id = rs.getLong("manager_staff_id");
            long address_id = rs.getLong("address_id");

            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting row into Datomic :" + store_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :store/store_id " + store_id);
            b.append(" :store/manager_staff_id " + manager_staff_id);

            //Reference to the address object...
            b.append(" :store/address_id [:address/address_id " + address_id + "]");

            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :store/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
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

        System.out.println("Printing out stores...");
        String q = "[:find ?sid ?mgrid ?lupd ?addrid :where [?e :store/store_id ?sid][?e :store/manager_staff_id ?mgrid][?e :store/last_update ?lupd][?e :store/address_id ?addrid] ]";

        getResults(db, q);

        System.out.println("Printing out store count...");
        q = "[:find (count ?aid) . :where [?aid :store/store_id ]]";
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
