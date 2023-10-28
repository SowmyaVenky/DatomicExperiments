package com.gssystems.datomic;

import java.io.InputStream;
import java.io.InputStreamReader;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MoviesExample {
    public static void main(String[] args) throws Exception {
        String uri = "datomic:mem://movies";

        System.out.println("Creating a new database called movies...");
        Peer.createDatabase(uri);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = MoviesExample.class.getClassLoader()
                .getResourceAsStream("movies/movies.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();

        System.out.println("After schema create transaction answer is : " + resultsFromSchema);

        System.out.println("Inserting new movies to the database after reading from file...");
        InputStream in1 = MoviesExample.class.getClassLoader()
                .getResourceAsStream("movies/movieslist.edn");

        InputStreamReader isr1 = new InputStreamReader(in1);
        List<?> moviesList = datomic.Util.readAll(isr1);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromMoviesInsert = conn.transact(moviesList).get();
        System.out.println("After movies insert transaction answer is : " + resultsFromMoviesInsert);
        isr1.close();

        // Now it is time to query the movies...
        // First get a copy of the database into the peer, then issue queries...
        // VERY IMPORTANT. If we do not get a new database back from the connection, the
        // mutate results will not be visible.

        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);

        String q = "[:find ?e :where [?e :movie/title]]";
        getResults(db,q);

        System.out.println("Printing out the movie titles instead of the IDs");
        q = "[:find ?movie-title :where [_ :movie/title ?movie-title]]";
        getResults(db,q);
        
        System.out.println("Printing out the movie titles for year = 1985");
        q = "[:find ?title :where " + 
            " [?e :movie/title ?title] " + 
            "[?e :movie/release-year 1985]]";
        getResults(db,q);
        
        System.out.println("Printing out the movie DETAILS for year = 1984");
        q = "[:find ?title ?genre ?release-year " + 
        " :where [?e :movie/title ?title] " +
        " [?e :movie/release-year ?release-year] " +
        " [?e :movie/genre ?genre] " + 
        " [?e :movie/release-year 1984] " +
        "]";
        getResults(db,q);
        
        System.exit(0);
    }

    private static void getResults(Database db, String q) {
        Collection<?> results = Peer.query(q, db);
        System.out.println("===================");
        for( Object o: results) {
            System.out.println(o);
        }
        System.out.println("===================");
    }
}
