# Datomic with a Cassandra backend.

* This experiment moves on to create a network of 3 containers using docker-compose. The first one is the ubuntu container with the datomic installation mounted as an external volume. The second one is the Cassandra container that will act as a back-end to the datomic system. The third one is the postgres database that has the database with the dvdrental system on it. The containers are connected via the network to ensure connections are good between them. 

<pre>
C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker ps
CONTAINER ID   IMAGE                COMMAND                  CREATED          STATUS          PORTS                                                                               NAMES
e65209f53c25   postgres:14-alpine   "docker-entrypoint.s…"   5 seconds ago    Up 4 seconds    0.0.0.0:5432->5432/tcp                                                              postgres
7e56a81813fd   cassandra            "docker-entrypoint.s…"   37 minutes ago   Up 37 minutes   7000-7001/tcp, 7199/tcp, 0.0.0.0:9042->9042/tcp, 9160/tcp, 0.0.0.0:9842->9842/tcp   cassandra
adc75bf79321   ubuntu               "sleep infinity"         37 minutes ago   Up 37 minutes   0.0.0.0:4334-4336->4334-4336/tcp                                                    ubuntu

</pre>

* We need to get the java installed on the ubuntu container to make it capable of running the datomic transactor. We do this using the following commands.

<pre>
docker exec -it ubuntu bash
apt update
apt install -y default-jdk iputils-ping

## Do a ls -lrt and make sure the datomic mount is in place. 

## Also ping cassandra to make sure the network is in place and the other container is reachable.
root@af1067bfcf85:/# ping cassandra
PING cassandra (172.23.0.3) 56(84) bytes of data.
64 bytes from cassandra.datomic_with_cassandra_backend (172.23.0.3): icmp_seq=1 ttl=64 time=0.065 ms
64 bytes from cassandra.datomic_with_cassandra_backend (172.23.0.3): icmp_seq=2 ttl=64 time=0.055 ms
64 bytes from cassandra.datomic_with_cassandra_backend (172.23.0.3): icmp_seq=3 ttl=64 time=0.082 ms
64 bytes from cassandra.datomic_with_cassandra_backend (172.23.0.3): icmp_seq=4 ttl=64 time=0.079 ms
64 bytes from cassandra.datomic_with_cassandra_backend (172.23.0.3): icmp_seq=5 ttl=64 time=0.049 ms
^C
--- cassandra ping statistics ---
5 packets transmitted, 5 received, 0% packet loss, time 4121ms
rtt min/avg/max/mdev = 0.049/0.066/0.082/0.012 ms

</pre>

* Now we need to get the cassandra database configured correctly to get datomic persist the data into it as a backend. The scripts required to create the database and the users are listed inside the datomic install directory under the /datomic/bin/cql/. We need to execute it to create the required database and roles needed. 

<pre>
## Check to see the folders where the cassandra scripts are located. This is coming from the mounted folder.

root@adc75bf79321:/datomic/bin/cql# ls -lrt
total 0
-rwxrwxrwx 1 root root 126 Oct 17 22:48 cassandra2-user.cql
-rwxrwxrwx 1 root root 169 Oct 17 22:48 cassandra2-table.cql
-rwxrwxrwx 1 root root 151 Oct 17 22:48 cassandra2-keyspace.cql
-rwxrwxrwx 1 root root 122 Oct 17 22:48 cassandra-user.cql
-rwxrwxrwx 1 root root 150 Oct 17 22:48 cassandra-table.cql
-rwxrwxrwx 1 root root 150 Oct 17 22:48 cassandra-keyspace.cql

We need to push these scripts to the docker container cassandra and run them. Note we are using Cassandra not Cassandra2 scripts.

C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker cp C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra\cql\cassandra-keyspace.cql cassandra:/
Successfully copied 2.05kB to cassandra:/

C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker cp C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra\cql\cassandra-user.cql cassandra:/
Successfully copied 2.05kB to cassandra:/

C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker cp C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra\cql\cassandra-table.cql cassandra:/
Successfully copied 2.05kB to cassandra:/

docker exec -it cassandra bash

root@7e56a81813fd:/# ls -lrt *.cql
-rwxr-xr-x 1 root root 122 Oct 17 22:48 cassandra-user.cql
-rwxr-xr-x 1 root root 150 Oct 17 22:48 cassandra-table.cql
-rwxr-xr-x 1 root root 150 Oct 17 22:48 cassandra-keyspace.cql
root@7e56a81813fd:/# cqlsh -f cassandra-keyspace.cql -u cassandra -p cassandra

Warning: Using a password on the command line interface can be insecure.
Recommendation: use the credentials file to securely provide the password.


Warnings :
Your replication factor 3 for keyspace datomic is higher than the number of nodes 1

root@7e56a81813fd:/# cqlsh -f cassandra-table.cql -u cassandra -p cassandra

Warning: Using a password on the command line interface can be insecure.
Recommendation: use the credentials file to securely provide the password.

## We can validate that the tables were created fine...

root@7e56a81813fd:/# cqlsh
Connected to Test Cluster at 127.0.0.1:9042
[cqlsh 6.1.0 | Cassandra 4.1.3 | CQL spec 3.4.6 | Native protocol v5]
Use HELP for help.
cqlsh> desc keyspaces;

datomic  system_auth         system_schema  system_views
system   system_distributed  system_traces  system_virtual_schema

cqlsh> use datomic
   ... ;
cqlsh:datomic> desc tables;

datomic

cqlsh:datomic> select * from datomic;

 id | map | rev | val
----+-----+-----+-----

(0 rows)
cqlsh:datomic>

</pre>

* Now we need to focus on the cassandra-transactor properties file that we copied from the config/samples directory back to the config folder. I have checked in a copy of the file over here for ease of use. We need to adjust the settings of the transactor to make it run on 0.0.0.0 instead of localhost similar to what we had done before. 

* Now we need to get the transactor started and see whether it can connect to the cassandra backend.

* I RAN INTO AN ISSUE because the create keyspace has a replication factor set to 3. Since i have just one node in the cluster, it failed saying that the replication factor of 3 is more than the node count of 1. I had to drop the keyspace, table and recreated it with the replication factor of 1 to get things to start with no errors.

<pre>
bin/transactor -Ddatomic.printConnectionInfo=true config/cassandra-transactor-template.properties

root@adc75bf79321:/datomic# bin/transactor -Ddatomic.printConnectionInfo=true config/cassandra-transactor-template.properties
Launching with Java options -server -Xms1g -Xmx1g  -Ddatomic.printConnectionInfo=true
Starting datomic:cass://cassandra:9042/datomic.datomic/<DB-NAME>?user=cassandra&password=cassandra&ssl= ...
System started datomic:cass://cassandra:9042/datomic.datomic/<DB-NAME>?user=cassandra&password=cassandra&ssl=

</pre>

* Make sure the transactor starts and does not throw any errors. The script execution with the -f option did not create the required tables. I had to login manually and create them by executing scripts one at a time. 

* Now we need to do a maven clean package and try to execute the data loads.

* NOTE THAT IF WE HAVE DONE THE POSTGRES backend example before this one, the mount folders are exactly the same for postgres and we will already have the dvd rental database and data in there. If that is not the case, we need to follow this extra step to load that data.

* Unzip the dvdrental.zip to create the dvdrental.tar file. They docker cp that to the cassandra container, and import that into the database dvdrental. Once this is done, we have the source database and the datomic cassandra backing storage on the same container. 

* Login to the postgres container to make sure the data is in there. 

<pre>
C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker exec -it postgres bash
e65209f53c25:/# psql -U postgres
psql (14.9)
Type "help" for help.

postgres=# \q
e65209f53c25:/# psql -U postgres -d dvdrental
psql (14.9)
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

* Make sure that we change the Constants.java to point to the cassandra instance. 
<pre>
package com.gssystems.datomic.docker;

public class Constants {
    public static final String DATOMIC_URL =  "datomic:cass://localhost:9042/datomic.datomic/dvdrental?user=cassandra&password=cassandra&ssl=";
}

</pre>

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


Printing out names of actors...
Total number of records : 200
Showing top 10
===================
[182 "Debbie" "Akroyd" #inst "2013-05-26T14:47:57.000-00:00"]
[143 "River" "Dean" #inst "2013-05-26T14:47:57.000-00:00"]
[75 "Burt" "Posey" #inst "2013-05-26T14:47:57.000-00:00"]
[14 "Vivien" "Bergen" #inst "2013-05-26T14:47:57.000-00:00"]
[88 "Kenneth" "Pesci" #inst "2013-05-26T14:47:57.000-00:00"]
[31 "Sissy" "Sobieski" #inst "2013-05-26T14:47:57.000-00:00"]
[131 "Jane" "Jackman" #inst "2013-05-26T14:47:57.000-00:00"]
[82 "Woody" "Jolie" #inst "2013-05-26T14:47:57.000-00:00"]
[160 "Chris" "Depp" #inst "2013-05-26T14:47:57.000-00:00"]
===================
Printing out actor count...
Result is : 200

Printing out names of addresses...
Total number of records : 603
Showing top 10
===================
[114 "804 Elista Drive" "Enshi" "China"]
[245 "1103 Bilbays Parkway" "Xiangfan" "China"]
[117 "1079 Tel Aviv-Jaffa Boulevard" "Cuman" "Venezuela"]
[431 "1596 Acua Parkway" "Purnea (Purnia)" "India"]
[27 "1780 Hino Boulevard" "Liepaja" "Latvia"]
[515 "886 Tonghae Place" "Kamyin" "Russian Federation"]
[128 "848 Tafuna Manor" "Ktahya" "Turkey"]
[442 "1245 Ibirit Way" "La Romana" "Dominican Republic"]
[136 "898 Belm Manor" "Botshabelo" "South Africa"]
===================
Printing out addresses count...
Result is : 603

Printing out names of categories...
Total number of records : 16
Showing top 10
===================
[7 "Drama" #inst "2006-02-15T09:46:27.000-00:00"]
[1 "Action" #inst "2006-02-15T09:46:27.000-00:00"]
[5 "Comedy" #inst "2006-02-15T09:46:27.000-00:00"]
[14 "Sci-Fi" #inst "2006-02-15T09:46:27.000-00:00"]
[2 "Animation" #inst "2006-02-15T09:46:27.000-00:00"]
[9 "Foreign" #inst "2006-02-15T09:46:27.000-00:00"]
[11 "Horror" #inst "2006-02-15T09:46:27.000-00:00"]
[15 "Sports" #inst "2006-02-15T09:46:27.000-00:00"]
[4 "Classics" #inst "2006-02-15T09:46:27.000-00:00"]
===================
Printing out category count...
Result is : 16

Printing out stores...
Total number of records : 2
Showing top 10
===================
[1 1 #inst "2006-02-15T09:57:12.000-00:00" 17592186045820]
[2 2 #inst "2006-02-15T09:57:12.000-00:00" 17592186045822]
===================
Printing out store count...
Result is : 2

Printing out staff...
Total number of records : 2
Showing top 10
===================
[17592186047060 "Mike" "Hillyer" 17592186045824 "Mike.Hillyer@sakilastaff.com" 17592186047060 "t" "Mike" "8cb2237d0679ca88db6464eac60da96345513964" #inst "2006-05-16T16:13:11.000-00:00"]
[17592186047062 "Jon" "Stephens" 17592186045826 "Jon.Stephens@sakilastaff.com" 17592186047062 "t" "Jon" "8cb2237d0679ca88db6464eac60da96345513964" #inst "2006-05-16T16:13:11.000-00:00"]
===================
Printing out staff count...
Result is : 2

Printing out customers...
Total number of records : 599
Showing top 10
===================
[318 17592186047060 "Brian" "Wyman" "brian.wyman@sakilacustomer.org" 17592186046462 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[46 17592186047062 "Catherine" "Campbell" "catherine.campbell@sakilacustomer.org" 17592186045918 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[300 17592186047060 "John" "Farnsworth" "john.farnsworth@sakilacustomer.org" 17592186046426 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[311 17592186047062 "Paul" "Trout" "paul.trout@sakilacustomer.org" 17592186046448 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[11 17592186047062 "Lisa" "Anderson" "lisa.anderson@sakilacustomer.org" 17592186045848 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[149 17592186047060 "Valerie" "Black" "valerie.black@sakilacustomer.org" 17592186046124 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[502 17592186047060 "Brett" "Cornwell" "brett.cornwell@sakilacustomer.org" 17592186046830 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[409 17592186047062 "Rodney" "Moeller" "rodney.moeller@sakilacustomer.org" 17592186046644 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[135 17592186047062 "Juanita" "Mason" "juanita.mason@sakilacustomer.org" 17592186046096 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
===================
Printing out customer count...
Result is : 599

Printing out fimns...
Total number of records : 5462
Showing top 10
===================
[609 "Muscle Bright" "A Stunning Panorama of a Sumo Wrestler And a Husband who must Redeem a Madman in Ancient India" 2006 "English" 7.0 2.99 185.0 23.99 "G" #inst "2013-05-26T14:50:58.000-00:00" "{\"Deleted Scenes\"}" "'ancient':19 'bright':2 'husband':12 'india':20 'madman':17 'muscl':1 'must':14 'panorama':5 'redeem':15 'stun':4 'sumo':8 'wrestler':9" 17592186047057 17592186045761]
[443 "Hurricane Affair" "A Lacklusture Epistle of a Database Administrator And a Woman who must Meet a Hunter in An Abandoned Mine Shaft" 2006 "English" 6.0 2.99 49.0 11.99 "PG" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,Commentaries,\"Behind the Scenes\"}" "'abandon':20 'administr':9 'affair':2 'databas':8 'epistl':5 'hunter':17 'hurrican':1 'lacklustur':4 'meet':15 'mine':21 'must':14 'shaft':22 'woman':12" 17592186047035 17592186045711]
[652 "Pajama Jawbreaker" "A Emotional Drama of a Boy And a Technical Writer who must Redeem a Sumo Wrestler in California" 2006 "English" 3.0 0.99 126.0 14.99 "R" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,\"Deleted Scenes\"}" "'boy':8 'california':20 'drama':5 'emot':4 'jawbreak':2 'must':14 'pajama':1 'redeem':15 'sumo':17 'technic':11 'wrestler':18 'writer':12" 17592186047033 17592186045753]
[393 "Halloween Nuts" "A Amazing Panorama of a Forensic Psychologist And a Technical Writer who must Fight a Dentist in A U-Boat" 2006 "English" 6.0 2.99 47.0 19.99 "PG-13" #inst "2013-05-26T14:50:58.000-00:00" "{\"Deleted Scenes\"}" "'amaz':4 'boat':23 'dentist':18 'fight':16 'forens':8 'halloween':1 'must':15 'nut':2 'panorama':5 'psychologist':9 'technic':12 'u':22 'u-boat':21 'writer':13" 17592186047037 17592186045573]
[718 "Rebel Airport" "A Intrepid Yarn of a Database Administrator And a Boat who must Outrace a Husband in Ancient India" 2006 "English" 7.0 0.99 73.0 24.99 "G" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,\"Behind the Scenes\"}" "'administr':9 'airport':2 'ancient':19 'boat':12 'databas':8 'husband':17 'india':20 'intrepid':4 'must':14 'outrac':15 'rebel':1 'yarn':5" 17592186047049 17592186045443]
[491 "Jumping Wrath" "A Touching Epistle of a Monkey And a Feminist who must Discover a Boat in Berlin" 2006 "English" 4.0 0.99 74.0 18.99 "NC-17" #inst "2013-05-26T14:50:58.000-00:00" "{Commentaries,\"Behind the Scenes\"}" "'berlin':18 'boat':16 'discov':14 'epistl':5 'feminist':11 'jump':1 'monkey':8 'must':13 'touch':4 'wrath':2" 17592186047031 17592186045663]
[170 "Command Darling" "A Awe-Inspiring Tale of a Forensic Psychologist And a Woman who must Challenge a Database Administrator in Ancient Japan" 2006 "English" 5.0 4.99 120.0 28.99 "PG-13" #inst "2013-05-26T14:50:58.000-00:00" "{\"Behind the Scenes\"}" "'administr':20 'ancient':22 'awe':5 'awe-inspir':4 'challeng':17 'command':1 'darl':2 'databas':19 'forens':10 'inspir':6 'japan':23 'must':16 'psychologist':11 'tale':7 'woman':14" 17592186047043 17592186045567]
[398 "Hanover Galaxy" "A Stunning Reflection of a Girl And a Secret Agent who must Succumb a Boy in A MySQL Convention" 2006 "English" 5.0 4.99 47.0 21.99 "NC-17" #inst "2013-05-26T14:50:58.000-00:00" "{Commentaries,\"Deleted Scenes\",\"Behind the Scenes\"}" "'agent':12 'boy':17 'convent':21 'galaxi':2 'girl':8 'hanov':1 'must':14 'mysql':20 'reflect':5 'secret':11 'stun':4 'succumb':15" 17592186047049 17592186045547]
[398 "Hanover Galaxy" "A Stunning Reflection of a Girl And a Secret Agent who must Succumb a Boy in A MySQL Convention" 2006 "English" 5.0 4.99 47.0 21.99 "NC-17" #inst "2013-05-26T14:50:58.000-00:00" "{Commentaries,\"Deleted Scenes\",\"Behind the Scenes\"}" "'agent':12 'boy':17 'convent':21 'galaxi':2 'girl':8 'hanov':1 'must':14 'mysql':20 'reflect':5 'secret':11 'stun':4 'succumb':15" 17592186047049 17592186045541]
===================
Printing out film count...
Result is : 1000

Total number of records : 4581
Showing top 10
===================
[748 17592186048595 17592186047060 #inst "2006-02-15T10:09:17.000-00:00"]
[870 17592186048649 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[1349 17592186048863 17592186047060 #inst "2006-02-15T10:09:17.000-00:00"]
[3440 17592186049775 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[1118 17592186048761 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[2552 17592186049385 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[1951 17592186049117 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[622 17592186048537 17592186047062 #inst "2006-02-15T10:09:17.000-00:00"]
[493 17592186048487 17592186047060 #inst "2006-02-15T10:09:17.000-00:00"]
===================
Printing out inventory count...
Result is : 4581

Printing rentals...
Total number of records : 15861
Showing top 10
===================
[14988 #inst "2005-08-22T07:46:05.000-00:00" 17592186054496 17592186047442 #inst "2005-08-29T06:43:05.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
[6925 #inst "2005-07-26T22:52:32.000-00:00" 17592186056106 17592186047154 #inst "2005-07-29T21:22:32.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
[3677 #inst "2005-07-06T09:11:58.000-00:00" 17592186051468 17592186047080 #inst "2005-07-08T10:50:58.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047067]
[8667 #inst "2005-07-29T15:40:57.000-00:00" 17592186057146 17592186047120 #inst "2005-07-31T20:59:57.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
[2260 #inst "2005-06-18T05:38:36.000-00:00" 17592186055814 17592186047664 #inst "2005-06-20T08:08:36.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
[2269 #inst "2005-06-18T06:20:54.000-00:00" 17592186056444 17592186047932 #inst "2005-06-25T04:51:54.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047067]
[6283 #inst "2005-07-11T16:47:32.000-00:00" 17592186056906 17592186047874 #inst "2005-07-17T21:46:32.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
[12375 #inst "2005-08-18T08:20:08.000-00:00" 17592186057284 17592186047800 #inst "2005-08-21T08:50:08.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047067]
[9941 #inst "2005-07-31T15:31:25.000-00:00" 17592186053780 17592186047420 #inst "2005-08-05T17:23:25.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047065]
===================
Printing out rentals count...
Result is : 16044

Printing payments...
Total number of records : 14596
Showing top 10
===================
[30697 17592186047368 17592186047065 17592186068957 3.99 #inst "2007-04-08T13:44:30.000-00:00"]
[30468 17592186047330 17592186047065 17592186078325 0.99 #inst "2007-04-30T20:47:42.000-00:00"]
[26875 17592186047906 17592186047065 17592186069155 2.99 #inst "2007-04-08T17:37:30.000-00:00"]
[23327 17592186047204 17592186047065 17592186086727 2.99 #inst "2007-03-20T06:21:00.000-00:00"]
[18953 17592186047302 17592186047067 17592186062493 0.99 #inst "2007-02-15T23:14:28.000-00:00"]
[21875 17592186048100 17592186047065 17592186085201 5.99 #inst "2007-03-19T02:09:57.000-00:00"]
[29917 17592186047236 17592186047065 17592186079155 1.99 #inst "2007-04-30T11:45:30.000-00:00"]
[26231 17592186047790 17592186047067 17592186076145 2.99 #inst "2007-04-29T03:37:23.000-00:00"]
[29570 17592186047172 17592186047065 17592186070371 7.99 #inst "2007-04-09T21:47:37.000-00:00"]
===================
Printing out payments count...
Result is : 14596

The counts for payments, films and rental are all matching as we can see

dvdrental=# select count(*) from film;
 count
-------
  1000
(1 row)

dvdrental=# select count(*) from rental;
 count
-------
 16044
(1 row)

dvdrental=# select count(*) from payment;
 count
-------
 14596
(1 row)

dvdrental=# select count(*) from customer
dvdrental-# ;
 count
-------
   599
(1 row)

dvdrental=# select count(*) from actor;
 count
-------
   200
(1 row)

dvdrental=# select count(*) from category;
 count
-------
    16
(1 row)

## Checking the total number of rows in Cassandra

C:\Venky\DatomicExperiments\DatomicOnCassandra>mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.TestCassandraConnection"

Datomic table contains row count : 38482

cassandra@cqlsh:datomic> select count(*) from datomic;

 count
-------
 38482

(1 rows)

</pre>

* If we check the backing storage for datomic we can see the data was inserted.

<pre>
cassandra@cqlsh:datomic> select * from datomic limit 10;

 id                                   | map                                            | rev  | val
--------------------------------------+------------------------------------------------+------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 654ad51f-844d-4e58-b299-34ee6b9d49d4 | {:prev "654ad51f-6349-4844-b58b-49e17f2eeea0"} | null | 0xfec0eacaf7cddc6964c3d910654ad51f1c4b433598e74a3f1bb2d2ffcaf7cddb745533caf7cdde64617461e9efdf646174756d06f503553332c87b8bac5083125533a0f5045534504850a65533a0f50455345049de4e69636b5533a0f5045534504ae309446567656e657265735533a0f5045534504bc87b3ee14f65485533
 654ad51b-50c1-4536-8204-c428405eb190 | {:prev "654ad51b-2417-4e6c-b74d-25692eaabb7c"} | null |         0xfec0eacaf7cddc6964c3d910654ad51b231d497e9a3700ee891022b2caf7cddb745405caf7cdde64617461e9efdf646174756d06f503540532c87b8bac5074ee5405a0f504540650480f5405a0f50454065049de437562615405a0f5045406504ae14f6c69766965725405a0f5045406504bc87b3ee14f65485405
 654ad51e-d972-4d4c-bd38-35ea49479eb4 | {:prev "654ad51e-a055-472b-addd-caa0bcc76813"} | null |         0xfec0eacaf7cddc6964c3d910654ad51e00f4416a955cbf1a36474821caf7cddb7454ddcaf7cdde64617461e9efdf646174756d06f50354dd32c87b8bac507f5854dda0f50454de5048507a54dda0f50454de5049df53616c6d6154dda0f50454de504adf4e6f6c746554dda0f50454de504bc87b3ee14f654854dd
 654ad51f-6349-4844-b58b-49e17f2eeea0 | {:prev "654ad51f-76ff-4446-be95-d54b3790e052"} | null |           0xfec0eacaf7cddc6964c3d910654ad51f672546879b82a19698d427eacaf7cddb745531caf7cdde64617461e9efdf646174756d06f503553132c87b8bac5082fc5531a0f5045532504850a55531a0f50455325049dc416c5531a0f5045532504ae14761726c616e645531a0f5045532504bc87b3ee14f65485531
 654ad51e-7017-4f0e-a23f-5e706c2a52ad | {:prev "654ad51e-6583-4ff7-a553-9490bc685ca5"} | null |   0xfec0eacaf7cddc6964c3d910654ad51ea411453ca2da7a92f35e6b5fcaf7cddb7454cfcaf7cdde64617461e9efdf646174756d06f50354cf32c87b8bac507eb954cfa0f50454d05048507354cfa0f50454d05049e3084861727269736f6e54cfa0f50454d0504ade42616c6554cfa0f50454d0504bc87b3ee14f654854cf
 654ad51b-bff9-41b8-bac5-b5eb47ba7f51 | {:prev "654ad51b-3d67-41f4-a483-a3dd7d9dce98"} | null |         0xfec0eacaf7cddc6964c3d910654ad51bbe6944ff888ba4a8bb904f86caf7cddb745409caf7cdde64617461e9efdf646174756d06f503540932c87b8bac50752c5409a0f504540a5048115409a0f504540a5049df48656c656e5409a0f504540a504ae0566f696768745409a0f504540a504bc87b3ee14f65485409
                          pod-catalog | {:tail "654ad4eb-7840-482d-94b8-28b358c29b78"} |    0 |                                                                                                                                                                                                                                                             null
 654ad51c-acbe-4d8c-ad72-e5aca6f98798 | {:prev "654ad51c-edfe-45e9-b510-9d2f4d7c8eb3"} | null |     0xfec0eacaf7cddc6964c3d910654ad51c01a34d2fa4e6bf0276a7f1d7caf7cddb74542bcaf7cdde64617461e9efdf646174756d06f503542b32c87b8bac507719542ba0f504542c504822542ba0f504542c5049e0417564726579542ba0f504542c504ae14f6c6976696572542ba0f504542c504bc87b3ee14f6548542b
 654ad4eb-7840-482d-94b8-28b358c29b78 |                                             {} | null |                                                                     0x7b2264766472656e74616c22207b3a64622d6964202264766472656e74616c2d66373938613539342d306162322d343338302d623532392d343732663866326164333339227d2c203a6461746f6d69632f64656c6574656420237b7d7d
 654ad51c-3e8f-4891-8eda-d96f6a09fdd9 | {:prev "654ad51c-ec7d-4c38-b1b9-3c2b4cc892e6"} | null |             0xfec0eacaf7cddc6964c3d910654ad51ccd8343589163838149e3cec9caf7cddb745421caf7cdde64617461e9efdf646174756d06f503542132c87b8bac5076975421a0f504542250481d5421a0f50454225049de416c65635421a0f5045422504adf5761796e655421a0f5045422504bc87b3ee14f65485421

(10 rows)
</pre>

* Now we can try some queries to see how we can use the bin/repl

## Important NOTES 
* I had a lot of problems getting this to work with abscure errors. Everything failed with some kind of SSL auth error or Tag mismatch error. I found that we needed to have these 2 maven imports on here to get it to work. For using the datastax oss library, I could do that with the native java program, but not via datomic. 

<pre>    
    <dependency>
      <groupId>com.datastax.cassandra</groupId>
      <artifactId>cassandra-driver-core</artifactId>
      <version>3.1.0</version>
    </dependency>
    
    <!-- https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core -->
    <dependency>
        <groupId>com.codahale.metrics</groupId>
        <artifactId>metrics-core</artifactId>
        <version>3.0.2</version>
    </dependency>
<pre>

<pre>
bin/repl

(require '[datomic.api :as d])
(def db-uri "datomic:cass://cassandra:9042/datomic.datomic/dvdrental?user=datomic&password=datomic&ssl=")
(def conn (d/connect db-uri))
(def db (d/db conn))

(def all-actor-q '[:find (count ?aid) . :where [?aid :actor/actor_id ]])
(def all-address-q '[:find (count ?aid) . :where [?aid :address/address_id ]])
(def all-category-q '[:find (count ?aid) . :where [?aid :category/category_id ]])
(def all-customer-q '[:find (count ?aid) . :where [?aid :customer/customer_id ]])
(def all-film-q '[:find (count ?aid) . :where [?aid :film/film_id ]])
(def all-inventory-q '[:find (count ?aid) . :where [?aid :inventory/inventory_id ]])
(def all-payment-q '[:find (count ?aid) . :where [?aid :payment/payment_id ]])
(def all-rental-q '[:find (count ?aid) . :where [?aid :rental/rental_id ]])
(def all-staff-q '[:find (count ?aid) . :where [?aid :staff/staff_id ]])
(def all-store-q '[:find (count ?aid) . :where [?aid :store/store_id ]])

(d/q all-actor-q db)
(d/q all-address-q db)
(d/q all-category-q db)
(d/q all-customer-q db)
(d/q all-film-q db)
(d/q all-inventory-q db)
(d/q all-payment-q db)
(d/q all-rental-q db)
(d/q all-staff-q db)
(d/q all-store-q db)

</pre>

* The output comes out like this

<pre>
user=>
(def all-actor-q '[:find (count ?aid) . :where [?aid :actor/actor_id ]])
(def all-address-q '[:find (count ?aid) . :where [?aid :address/address_id ]])
(def all-category-q '[:find (count ?aid) . :where [?aid :category/category_id ]])
(def all-customer-q '[:find (count ?aid) . :where [?aid :customer/customer_id ]])
(def all-film-q '[:find (count ?aid) . :where [?aid :film/film_id ]])
(def all-inventory-q '[:find (count ?aid) . :where [?aid :inventory/inventory_id ]])
(def all-payment-q '[:find (count ?aid) . :where [?aid :payment/payment_id ]])
(def all-rental-q '[:find (count ?aid) . :where [?aid :rental/rental_id ]])
(def all-staff-q '[:find (count ?aid) . :where [?aid :staff/staff_id ]])
(def all-store-q '[:find (count ?aid) . :where [?aid :store/store_id ]])

(d/q all-actor-q db)
(d/q all-address-q db)
(d/q all-category-q db)
(d/q all-customer-q db)
(d/q all-film-q db)
(d/q all-inventory-q db)
(d/q all-payment-q db)
(d/q all-rental-q db)
(d/q all-staff-q db)
(d/q all-store-q db)
user=> #'user/all-actor-q
user=> #'user/all-address-q
user=> #'user/all-category-q
user=> #'user/all-customer-q
user=> #'user/all-film-q
user=> #'user/all-inventory-q
user=> #'user/all-payment-q
user=> #'user/all-rental-q
user=> #'user/all-staff-q
user=> #'user/all-store-q
user=> user=> 200
user=> 603
user=> 16
user=> 599
user=> 1000
user=> 4581
user=> 14596
user=> 16044
user=> 2
user=> 2
user=>
</pre>


* Time to shut down everything 

<pre>
C:\Venky\DatomicExperiments\DatomicOnCassandra>docker ps
CONTAINER ID   IMAGE                      COMMAND                  CREATED          STATUS          PORTS                                            NAMES
ae75d9ed4947   bitnami/cassandra:latest   "/opt/bitnami/script…"   16 minutes ago   Up 16 minutes   0.0.0.0:7000->7000/tcp, 0.0.0.0:9042->9042/tcp   cassandra
e65209f53c25   postgres:14-alpine         "docker-entrypoint.s…"   10 hours ago     Up 10 hours     0.0.0.0:5432->5432/tcp                           postgres
adc75bf79321   ubuntu                     "sleep infinity"         11 hours ago     Up 11 hours     0.0.0.0:4334-4336->4334-4336/tcp                 ubuntu

## docker-compose down

C:\Venky\DatomicExperiments\DatomicOnCassandra\datomic_with_cassandra>docker-compose down
[+] Running 4/4
 ✔ Container ubuntu                        Removed                                                                                                                    11.0s
 ✔ Container postgres                      Removed                                                                                                                     0.8s
 ✔ Container cassandra                     Removed                                                                                                                    10.9s
 ✔ Network datomic_with_cassandra_backend  Removed
</pre>
