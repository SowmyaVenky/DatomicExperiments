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

* Unzip the dvdrental.zip to create the dvdrental.tar file. They docker cp that to the postgres container, and import that into the database dvdrental. Once this is done, we have the source database and the datomic postgres backing storage on the same container. 

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

</pre>

* If we check the backing storage for datomic we can see the data was inserted.

<pre>
c4d894ab321d:/# psql -U postgres -d datomic
psql (14.9)
Type "help" for help.

datomic=# select count(*) from datomic_kvs;
 count
-------
 38483
(1 row)

datomic=#
</pre>

* Time to shut down everything 

<pre>
C:\Venky\DatomicExperiments\DatomicInDocker\datomic_with_postgres>docker ps
CONTAINER ID   IMAGE                COMMAND                  CREATED       STATUS       PORTS                              NAMES
c4d894ab321d   postgres:14-alpine   "docker-entrypoint.s…"   5 hours ago   Up 5 hours   0.0.0.0:5432->5432/tcp             postgres
6c4ac3187ab7   ubuntu               "sleep infinity"         5 hours ago   Up 5 hours   0.0.0.0:4334-4336->4334-4336/tcp   ubuntu

C:\Venky\DatomicExperiments\DatomicInDocker\datomic_with_postgres>docker-compose down
[+] Running 3/3
 ✔ Container postgres                     Removed                                                                                                                      0.7s
 ✔ Container ubuntu                       Removed                                                                                                                     10.5s
 ✔ Network datomic_with_postgres_backend  Removed                                                                                                                      0.3s
</pre>