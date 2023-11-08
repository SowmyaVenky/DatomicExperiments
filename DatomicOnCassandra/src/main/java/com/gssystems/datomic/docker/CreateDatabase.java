package com.gssystems.datomic.docker;

import datomic.Peer;

/*
Note this class. Now we are not using the in-mem version of the database.
*/
public class CreateDatabase {
    public static void main(String[] args) {
        String uri = Constants.DATOMIC_URL;
        System.out.println(uri);
        System.out.println("Creating a new database called dvdrental...");
        boolean dbCreateStatus = Peer.createDatabase(uri);
        System.out.println(dbCreateStatus);
        System.exit(0);     
    }
}