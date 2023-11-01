package com.gssystems.datomic.postgres;

import datomic.Peer;

public class CreateDatabase {
    public static void main(String[] args) {
        String uri = "datomic:mem://dvdrental";

        if (args != null && args.length == 1 && args[0].equalsIgnoreCase("true")) {
            System.out.println("Creating a new database called dvdrental...");
            Peer.createDatabase(uri);
        }
    }
}
