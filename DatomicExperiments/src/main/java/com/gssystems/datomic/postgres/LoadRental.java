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

public class LoadRental {
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

        CreateDatabase.main(args);
        
        //Load films - pass null to not recreate the db
        LoadCustomer.main(null);
        LoadInventory.main(null);
        LoadStaff.main(null);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/rental.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM RENTAL");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long rental_id = rs.getLong("rental_id");
            java.sql.Timestamp rental_date = rs.getTimestamp("rental_date");
            long inventory_id = rs.getLong("inventory_id");
            long customer_id = rs.getLong("customer_id");
            java.sql.Timestamp return_date = rs.getTimestamp("return_date");
            long staff_id = rs.getLong("staff_id");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting row into Datomic :" + rental_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :rental/rental_id " + rental_id);
            b.append(" :rental/rental_date " + "#inst " + "\"" + sdf1.format(rental_date) + "T"
                    + sdf2.format(rental_date) + "\"");
            b.append(" :rental/inventory_id [:inventory/inventory_id " + inventory_id + "]");
            b.append(" :rental/customer_id [:customer/customer_id " + customer_id + "]");
            if( return_date != null) {
                b.append(" :rental/return_date " + "#inst " + "\"" + sdf1.format(return_date) + "T"
                    + sdf2.format(return_date) + "\"");
            }

            b.append(" :rental/staff_id [:staff/staff_id " + staff_id + "]");
            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :rental/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
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

        System.out.println("Printing rentals...");
        String q = "[:find ?v1 ?v2 ?v3 ?v4 ?v5 ?v6 ?v7 :where " + 
        "[?e :rental/rental_id ?v1]" + 
        "[?e :rental/rental_date ?v2]" + 
        "[?e :rental/inventory_id ?v3]" + 
        "[?e :rental/customer_id ?v4] " +
        "[?e :rental/return_date ?v5] " +
        "[?e :rental/last_update ?v6] " +
        "[?e :rental/staff_id ?v7] " +
        " ]";

        getResults(db, q);

        System.out.println("Printing out rentals count...");
        q = "[:find (count ?aid) . :where [?aid :rental/rental_id ]]";
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
