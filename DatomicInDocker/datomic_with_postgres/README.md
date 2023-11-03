# Datomic with a Postgres backend.

* This experiment moves on to create a network of 2 containers using docker-compose. The first one is the ubuntu container with the datomic installation mounted as an external volume. The second one is the postgres container that will act as a back-end to the datomic system. The containers are connected via the network to ensure connections are good between them. 

<pre>
C:\Venky\DatomicExperiments\DatomicInDocker\datomic_with_postgres>docker-compose up -d
[+] Building 0.0s (0/0)                                                                                                                                      docker:default
[+] Running 2/2
 ✔ Container ubuntu    Started                                                                                                                                         0.0s
 ✔ Container postgres  Started                                                                                                                                         0.0s

C:\Venky\DatomicExperiments\DatomicInDocker\datomic_with_postgres>docker ps
CONTAINER ID   IMAGE                COMMAND                  CREATED              STATUS         PORTS                              NAMES
c4d894ab321d   postgres:14-alpine   "docker-entrypoint.s…"   About a minute ago   Up 8 seconds   0.0.0.0:5432->5432/tcp             postgres
6c4ac3187ab7   ubuntu               "sleep infinity"         About a minute ago   Up 8 seconds   0.0.0.0:4334-4336->4334-4336/tcp   ubuntu

</pre>

* We need to get the java installed on the ubuntu container to make it capable of running the datomic transactor. We do this using the following commands.

<pre>
docker exec -it ubuntu bash
apt update
apt install default-jdk
apt install iputils-ping

## Do a ls -lrt and make sure the datomic mount is in place. 

## Also ping postgres to make sure the network is in place and the other container is reachable.
root@6c4ac3187ab7:/datomic# ping postgres
PING postgres (172.18.0.3) 56(84) bytes of data.
64 bytes from postgres.datomic_with_postgres_backend (172.18.0.3): icmp_seq=1 ttl=64 time=0.062 ms
64 bytes from postgres.datomic_with_postgres_backend (172.18.0.3): icmp_seq=2 ttl=64 time=0.054 ms
64 bytes from postgres.datomic_with_postgres_backend (172.18.0.3): icmp_seq=3 ttl=64 time=0.051 ms
64 bytes from postgres.datomic_with_postgres_backend (172.18.0.3): icmp_seq=4 ttl=64 time=0.050 ms
^C
--- postgres ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3101ms
rtt min/avg/max/mdev = 0.050/0.054/0.062/0.004 ms

</pre>

* Now we need to get the postgres database configured correctly to get datomic persist the data into it as a backend. The scripts required to create the database and the users are listed inside the postgres-scripts.sql file. We need to copy this file over to the postgres container, and then execute it to create the required database and roles needed. 

<pre>
docker cp postgres-scripts.sql postgres:/

C:\Venky\DatomicExperiments\DatomicInDocker\datomic_with_postgres>docker cp postgres-scripts.sql postgres:/
Successfully copied 2.56kB to postgres:/

docker exec -it postgres bash

## Execute the script to create required artifacts.

c4d894ab321d:/# psql -U postgres -f postgres-scripts.sql
CREATE DATABASE
CREATE TABLE
ALTER TABLE
GRANT
GRANT
CREATE ROLE
c4d894ab321d:/#

psql -U datomic -d datomic

datomic=# \dt
            List of relations
 Schema |    Name     | Type  |  Owner
--------+-------------+-------+----------
 public | datomic_kvs | table | postgres

</pre>

* Now we need to focus on the sql-transactor properties file that we copied from the config/samples directory back to the config folder. I have checked in a copy of the file over here for ease of use. We need to adjust the settings of the transactor to make it run on 0.0.0.0 instead of localhost similar to what we had done before. Also adjust the URL.

* Now we need to get the transactor started and see whether it can connect to the postgres backend.

<pre>
bin/transactor -Ddatomic.printConnectionInfo=true config/sql-transactor-template.properties

root@6c4ac3187ab7:/datomic# bin/transactor -Ddatomic.printConnectionInfo=true config/sql-transactor-template.properties
Launching with Java options -server -Xms1g -Xmx1g  -Ddatomic.printConnectionInfo=true
Starting datomic:sql://<DB-NAME>?jdbc:postgresql://postgres:5432/datomic?user=datomic&password=datomic, you may need to change the user and password parameters to work with your jdbc driver ...
System started datomic:sql://<DB-NAME>?jdbc:postgresql://postgres:5432/datomic?user=datomic&password=datomic, you may need to change the user and password parameters to work with your jdbc driver

</pre>

* Make sure the transactor starts and does not throw any errors. The script execution with the -f option did not create the required tables. I had to login manually and create them by executing scripts one at a time. 

* Now we need to do a maven clean package and try to execute the data loads.

<pre>
SET JAVA_HOME=C:\Venky\jdk-11.0.15.10-hotspot
Set PATH=%PATH%;C:\Venky\apache-maven-3.8.6\bin

mvn clean pacakage


mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.CreateDatabase"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.ActorsLoad"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.AddressLoad" 
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.CategoriesLoad" 
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadStore"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadStaff"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadCustomer"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadFilm"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadInventory"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadRental"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.LoadPayment"

</pre>