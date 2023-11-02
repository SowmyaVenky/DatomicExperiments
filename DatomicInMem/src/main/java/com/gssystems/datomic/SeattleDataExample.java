package com.gssystems.datomic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;

/**
 * This program takes the seattle data edn from
 * https://github.com/Datomic/datomic-java-examples/tree/master/src/resources/datomic-java-examples
 * and explores the data.
 *
 */
public class SeattleDataExample {
    public static void main(String[] args) throws Exception {
        Gson gs = new GsonBuilder().setPrettyPrinting().create();

        String uri = "datomic:mem://seattle";

        System.out.println("Creating a new database called seattle...");
        Peer.createDatabase(uri);

        Connection conn = Peer.connect(uri);
        System.out.println("Applying the schema to the database we created...");

        InputStream in = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("seattle/seattle-schema.edn");

        InputStreamReader isr = new InputStreamReader(in);
        List<?> schemaList = datomic.Util.readAll(isr);

        // Note we are using connection to transact not peer.
        Map<?, ?> resultsFromSchema = conn.transact(schemaList).get();
        isr.close();
        // String resJson = gs.toJson(resultsFromSchema);
        // System.out.println("After schema create transaction answer is : " + resJson);

        InputStream in1 = SeattleDataExample.class.getClassLoader()
                .getResourceAsStream("seattle/seattle_mod.edn");

        InputStreamReader isr1 = new InputStreamReader(in1);
        BufferedReader br = new BufferedReader(isr1);
        String aLine = null;
        while ((aLine = br.readLine()) != null) {
            if (aLine.trim().length() > 0) {
                Object aTxn = datomic.Util.read(aLine);
                //System.out.println(aTxn);
                Map<?, ?> resultsFromData = conn.transact(datomic.Util.list(aTxn)).get();
                //System.out.println("After data create transaction answer is : " + resultsFromData);
            }
        }

        System.out.println("Seattle data insert complete...");
        br.close();

        Database db = conn.db();
        System.out.println("Peer connected to the datbase : " + db);
               
        System.out.println("Printing out names of communities...");
        String q = "[:find ?cname ?ccat  " + 
        " :where [?e :community/name ?cname] " +
        " [?e :community/category ?ccat] " +
        "]";
        getResults(db,q);

        System.out.println("Printing out names of neighborhood...");
        q = "[:find ?nhood  " + 
        " :where [?e :neighborhood/name ?nhood] " +
        "]";
        getResults(db,q);

        System.exit(0);
    }

    
    private static void getResults(Database db, String q) {
        Collection<?> results = Peer.query(q, db);
        System.out.println("Total number of records : " + results.size());
        System.out.println("===================");
        for( Object o: results) {
            System.out.println(o);
        }
        System.out.println("===================");
    }
}
