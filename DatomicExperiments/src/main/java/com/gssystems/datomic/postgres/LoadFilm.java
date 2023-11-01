package com.gssystems.datomic.postgres;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.activemq.artemis.utils.StringEscapeUtils;

import com.gssystems.datomic.SeattleDataExample;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;

public class LoadFilm {
    private static final  int MAX_RECORDS_TO_SHOW = 10;
    private static final String QUERY = "SELECT  " + 
    "film.film_id,  " +
    "film.title,  " +
    "film.description,  " +
    "film.release_year,  " +
    "language.name as film_language,  " +
    "film.rental_duration,  " +
    "film.rental_rate,  " +
    "film.length,  " +
    "film.replacement_cost,  " +
    "film.rating,  " +
    "film.last_update,  " +
    "film.special_features,  " +
    "film.fulltext,  " +
    "film_category.category_id  " +
    " from film left outer join film_category on film.film_id = film_category.film_id left outer join language on film.language_id = language.language_id";

    public static void main(String[] args) throws Exception {

        System.out.println(QUERY);

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
        
        //Call address loads!
        CategoriesLoad.main(args);

        //System.out.println("Creating a new database called dvdrental...");
        //Peer.createDatabase(uri);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("dvdrental/film.edn");

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
            long film_id = rs.getLong("film_id");
            String title = rs.getString("title");
            String description = rs.getString("description");
            long release_year = rs.getLong("release_year");
            String language = rs.getString("film_language").trim();
            double rental_duration = rs.getDouble("rental_duration");
            double rental_rate = rs.getDouble("rental_rate");
            double length = rs.getLong("length");
            double replacement_cost = rs.getDouble("replacement_cost");
            String rating = rs.getString("rating");
            java.sql.Timestamp last_update = rs.getTimestamp("last_update");
            String special_features = rs.getString("special_features");
            String fulltext = rs.getString("fulltext");
            long category_id = rs.getLong("category_id");

            System.out.println("Inserting row into Datomic :" + film_id);
            StringBuffer b = new StringBuffer();
            b.append("{");
            b.append(" :film/film_id " + film_id);
            b.append(" :film/title \"" + title + "\"");
            b.append(" :film/description \"" + StringEscapeUtils.escapeString(description) + "\"");
            b.append(" :film/release_year " + release_year );
            b.append(" :film/language \"" + language + "\"");
            b.append(" :film/rental_duration " + rental_duration);
            b.append(" :film/rental_rate " + rental_rate);
            b.append(" :film/length " + length);
            b.append(" :film/replacement_cost " + replacement_cost);
            b.append(" :film/rating \"" + rating + "\"");

            // Note the usage of #inst to convert into the datetime needed.
            b.append(" :film/last_update " + "#inst " + "\"" + sdf1.format(last_update) + "T"
                    + sdf2.format(last_update) + "\"");
            b.append(" :film/special_features \"" + StringEscapeUtils.escapeString(special_features) + "\"");
            b.append(" :film/fulltext \"" + fulltext + "\"");

            //Reference to the category object...
            b.append(" :film/category_id [:category/category_id " + category_id + "]");
            b.append("}");

            //System.out.println( b.toString());

            Object aTxn = datomic.Util.read(b.toString());
            Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
            //System.out.println(resultsFromData);
        }

        // Get the database, to get a fresh copy.
        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        System.out.println("Printing out fimns...");
        String q = "[:find ?v1 ?v2 ?v3 ?v4 ?v5 ?v6 ?v7 ?v8 ?v9 ?v10 ?v11 ?v12 ?v13 ?v14 :where " + 
        "[?e :film/film_id ?v1]" + 
        "[?e :film/title ?v2]" +
        "[?e :film/description ?v3]" + 
        "[?e :film/release_year ?v4]" +
        "[?e :film/language ?v5]" +
        "[?e :film/rental_duration ?v6]" +
        "[?e :film/rental_rate ?v7]" +
        "[?e :film/length ?v8]" +
        "[?e :film/replacement_cost ?v9]" +
        "[?e :film/rating ?v10]" +
        "[?e :film/last_update ?v11]" +
        "[?e :film/special_features ?v12]" +
        "[?e :film/fulltext ?v13]" +
        "[?e :film/category_id ?v14]" +
        "]";

        getResults(db, q);

        System.out.println("Printing out film count...");
        q = "[:find (count ?aid) . :where [?aid :film/film_id ]]";
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
