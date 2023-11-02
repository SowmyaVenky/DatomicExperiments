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

public class LoadPayment {
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

        String uri = "datomic:dev://localhost:4334/dvdrental";
        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = LoadPayment.class.getClassLoader()
                .getResourceAsStream("dvdrental/payment.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery("SELECT * FROM PAYMENT");

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long payment_id = rs.getLong("payment_id");
            long customer_id = rs.getLong("customer_id");
            long staff_id = rs.getLong("staff_id");
            long rental_id = rs.getLong("rental_id");
            double amount = rs.getDouble("amount");            
            java.sql.Timestamp payment_date = rs.getTimestamp("payment_date");

            System.out.println("Inserting row into Datomic :" + payment_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :payment/payment_id " + payment_id);
            b.append(" :payment/customer_id [:customer/customer_id " + customer_id + "]");
            b.append(" :payment/staff_id [:staff/staff_id " + staff_id + "]");
            b.append(" :payment/rental_id [:rental/rental_id " + rental_id + "]");
            b.append(" :payment/amount " + amount);
            if( payment_date != null) {
                b.append(" :payment/payment_date " + "#inst " + "\"" + sdf1.format(payment_date) + "T"
                    + sdf2.format(payment_date) + "\"");
            }
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

        System.out.println("Printing payments...");
        String q = "[:find ?v1 ?v2 ?v3 ?v4 ?v5 ?v6 :where " + 
        "[?e :payment/payment_id ?v1]" + 
        "[?e :payment/customer_id ?v2]" + 
        "[?e :payment/staff_id ?v3]" + 
        "[?e :payment/rental_id ?v4] " +
        "[?e :payment/amount ?v5] " +
        "[?e :payment/payment_date ?v6] " +
        " ]";

        getResults(db, q);

        System.out.println("Printing out payments count...");
        q = "[:find (count ?aid) . :where [?aid :payment/payment_id ]]";
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
