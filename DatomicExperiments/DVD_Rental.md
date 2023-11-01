## Realize Postgres DVD Rental database in Datomic.

* This experiment will use the sample database that comes with the Postgres system called the DVD rental. This is available as a dump that can be downloaded and imported into a postgres instance. We wil use this database and realize it on Datomic. We can then run tests to compare Postgres vs Datomic. 

* Here is the ER diagram of the original database inside Postgres.
<img src="./images/dvd-rental-sample-database-diagram.png" />

* We can start the docker engine. Once the docker engine is up and running, we can start the postgres database. 

<pre>
    docker run --name some-postgres -p 5432:5432 -e POSTGRES_PASSWORD=Ganesh20022002 -d postgres

CONTAINER ID   IMAGE      COMMAND                  CREATED         STATUS         PORTS                    NAMES
b15fa5ba8490   postgres   "docker-entrypoint.s…"   3 seconds ago   Up 3 seconds   0.0.0.0:5432->5432/tcp   some-postgres

</pre>

* Now we need to download the dvdrental zip file from the Postgres support. Once we unzip the file, we will get a tar file that can be imported into the postgres instance running inside docker. 

<pre>
## Copy the unzipped tar file over to the root of the container. 

C:\Venky\datomic-experiment\datomicexperiments>docker cp C:\venky\dvdrental.tar some-postgres:/

C:\Venky\datomic-experiment\datomicexperiments>docker exec -it some-postgres bash
root@12b712abd79f:/#

root@12b712abd79f:/# psql -U postgres
psql (16.0 (Debian 16.0-1.pgdg120+1))
Type "help" for help.

postgres=#

postgres=# CREATE DATABASE dvdrental;
CREATE DATABASE
postgres=#

## Restore the database.
pg_restore -U postgres -d dvdrental /dvdrental.tar

## Relogin with postgres user pointing to the dvdrental database. 
root@12b712abd79f:/# psql -U postgres -d dvdrental
psql (16.0 (Debian 16.0-1.pgdg120+1))
Type "help" for help.

dvdrental=# \dt
             List of relations
 Schema |     Name      | Type  |  Owner
--------+---------------+-------+----------
 public | actor         | table | postgres
 public | address       | table | postgres
 public | category      | table | postgres
 public | city          | table | postgres
 public | country       | table | postgres
 public | customer      | table | postgres
 public | film          | table | postgres
 public | film_actor    | table | postgres
 public | film_category | table | postgres
 public | inventory     | table | postgres
 public | language      | table | postgres
 public | payment       | table | postgres
 public | rental        | table | postgres
 public | staff         | table | postgres
 public | store         | table | postgres
(15 rows)

</pre>

* After we load the data into the Postgres database, we can create the required schema for the same database inside Datomic. The schema is defined in a series of edn files and the program just applies the data in the edn files. There are some model differences between the relational types vs the model in datomic. 

<pre>
cd C:\Venky\datomic-experiment\datomicexperiments\DatomicExperiments
mvn clean package

mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.ActorsLoad" -Dexec.args="true"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.AddressLoad" -Dexec.args="true"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.CategoriesLoad"  -Dexec.args="true" 

## Don't pass true, otherwise it will terminate after the first dependent load...
## These programs will load more than one dependent entity and then load the main ## one.

mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadStore"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadStaff"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadCustomer"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadFilm"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadInventory"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.postgres.LoadRental"
</pre>