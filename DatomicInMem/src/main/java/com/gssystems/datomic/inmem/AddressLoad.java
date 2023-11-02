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

public class AddressLoad {
    private static final  int MAX_RECORDS_TO_SHOW = 10;
    private static final String QUERY = "select address_id, address, address2, district, city, country, postal_code, phone, address.last_update from " 
    + " address left outer join city on address.city_id = city.city_id "
    + " left outer join country on city.country_id = country.country_id ";

    public static void main(String[] args) throws Exception {
        System.out.println("Executing query: " + QUERY);
        
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

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/address.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        java.sql.Statement st = pgConn.createStatement();
        java.sql.ResultSet rs = st.executeQuery(QUERY);

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

        while (rs.next()) {
            long address_id = rs.getLong("address_id");
            String address = rs.getString("address");
            String address2 = rs.getString("address2");
            String district = rs.getString("district");
            String city = rs.getString("city");
            String country = rs.getString("country");
            String postal_code = rs.getString("postal_code");
            String phone = rs.getString("phone");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");

            System.out.println("Inserting address row into Datomic :" + address_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :address/address_id " + address_id);
            b.append(" :address/address \"" + address + "\"");
            b.append(" :address/address2 \"" + address2 + "\"");
            b.append(" :address/district \"" + district + "\"");
            b.append(" :address/city \"" + city + "\"");
            b.append(" :address/country \"" + country + "\"");
            b.append(" :address/postal_code \"" + postal_code + "\"");
            b.append(" :address/phone \"" + phone + "\"");

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

        System.out.println("Printing out names of addresses...");
        String q = "[:find ?aid ?address ?city ?cntry :where [?e :address/address_id ?aid][?e :address/address ?address][?e :address/city ?city][?e :address/country ?cntry] ]";

        getResults(db, q);

        System.out.println("Printing out addresses count...");
        q = "[:find (count ?aid) . :where [?aid :address/address_id ]]";
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
