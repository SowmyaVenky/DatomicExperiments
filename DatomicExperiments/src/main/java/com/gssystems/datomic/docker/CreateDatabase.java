package com.gssystems.datomic.docker;
import datomic.Peer;

/*
Note this class. Now we are not using the in-mem version of the database.
That is the reason we need to use the Client API not the Peer API to 
create the database 
*/
public class CreateDatabase {
    public static void main(String[] args) {
        String uri = "datomic:free://localhost:4334/dvdrental";
        System.out.println("Creating a new database called dvdrental...");
        boolean res = Peer.createDatabase(uri);   
        System.out.println("Create database result: " + res);
        System.exit(0);     
    }
}

/* 
Here are the steps to login to docker container and create the database.
For some reason we can't execute the create database via the Peer API. It keeps
throwing errors saying that the URL is not correct. 
bin/repl
(require '[datomic.api :as d])
(def db-uri "datomic:free://localhost:4334/dvdrental/")
(d/create-database db-uri)
(d/get-database-names "datomic:free://localhost:4334/*")
 */