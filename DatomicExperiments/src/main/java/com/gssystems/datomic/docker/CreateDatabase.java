package com.gssystems.datomic.docker;

import datomic.Peer;

public class CreateDatabase {
    public static void main(String[] args) {
        String uri = "datomic:free://localhost:4334/";
        System.out.println("Creating a new database called dvdrental...");
        Peer.createDatabase(uri);
    }
}
