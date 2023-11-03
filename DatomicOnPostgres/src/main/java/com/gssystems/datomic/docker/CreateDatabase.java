package com.gssystems.datomic.docker;

import datomic.Peer;

/*
Note this class. Now we are not using the in-mem version of the database.
*/
public class CreateDatabase {
    public static void main(String[] args) {
        String uri = "datomic:sql://dvdrental?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic";
        System.out.println("Creating a new database called dvdrental...");
        boolean dbCreateStatus = Peer.createDatabase(uri);
        System.out.println(dbCreateStatus);
        System.exit(0);     
    }
}