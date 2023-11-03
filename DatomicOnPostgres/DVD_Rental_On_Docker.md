## DVD Rental on Docker version of Datomic.

* The previous examples took the easy route and used the in-memory version of the database to make it easier to test. Now we have started the free version of datomic as a container on docker. 

* I had a lot of problems getting the code to communicate with the datomic instance running inside docker. There are some docker containers available, but for some reason the datomic:free URLs are not allowed by the Peer library. I had to actually download the version of datomic from the website and try to run it on Windows. It has some classpath issues when it runs on windows and had to abort that plan.

* I setup docker and get ubuntu container running on it. I pointed the directory of the datomic install as /datomic. We need to modify the transator properties file to make the IP point from localhost to 0.0.0.0. THIS IS A VERY CRITICAL STEP OTHERWISE THE Peer will not be able to connect to datomic. 

<pre>
## Download the datomic pro version from the website. This will download as a zip file and we need to unzip it. 

I have unzipped this in the folder C:\Venky\datomic-pro-1.0.7021

We now need to start the docker container with the ubuntu base. Since ubuntu's default entry point is /bin/bash, it will stop immediately, so we need to change the entry point call into ubuntu. Note the /dev/null tail command to make sure the container does not stop. Also note that the project directory and the datomic install directories are mounted into the container as 2 separate mount points. MAKE SURE TO RUN THE CMD FROM INSIDE THE datomic install. That makes the %CD% work. 

docker run -d -p 4334-4336:4334-4336 --name datomic-pro -v %cd%:/datomic/ -v C:\Venky\DatomicExperiments:/project/ ubuntu tail -f /dev/null

Now we need to get into the directory of datomic install and copy the contents of the config/samples/dev-transactor-template.properties to config/dev-transactor-template.properties.

NOTE VERY IMPORTANT - CHANGE THE localhost to 0.0.0.0 to make sure it can listen on all addresses. I have checked in a copy of the properties file if needed to copy and use. Note also that I have put in the storage password to datomic by uncommenting a few lines below.

The base ubuntu container does not contain java, git or maven. Install them 
apt update
apt install java git maven

Once java is installed, we can start our datomic instance from inside the docker container. 

bin/transactor -Ddatomic.printConnectionInfo=true config/dev-transactor-template.properties

We should see something like this 

root@31bc28314f21:/datomic# bin/transactor -Ddatomic.printConnectionInfo=true config/dev-transactor-template.properties
Launching with Java options -server -Xms1g -Xmx1g  -Ddatomic.printConnectionInfo=true
Starting datomic:dev://0.0.0.0:4334/<DB-NAME>, storing data in: ./data ...
System started datomic:dev://0.0.0.0:4334/<DB-NAME>, storing data in: ./data

</pre>

* Note that we need to start the postgres container and load the data into it like we have seen before in the memory based datomic example. 

After we run both containers, we should see something like this.

<pre>
C:\Venky\DatomicExperiments>docker ps
CONTAINER ID   IMAGE      COMMAND                  CREATED       STATUS       PORTS                              NAMES
c9548fc070cc   postgres   "docker-entrypoint.s…"   2 hours ago   Up 2 hours   0.0.0.0:5432->5432/tcp             some-postgres
31bc28314f21   ubuntu     "tail -f /dev/null"      2 hours ago   Up 2 hours   0.0.0.0:4334-4336->4334-4336/tcp   datomic-pro

</pre>

* Once we login to the container's bash, we can start the REPL and create the database. 

<pre>
docker exec -it datomic-pro bash

cd /datomic

bin/repl
(require '[datomic.api :as d])
(def db-uri "datomic:dev://localhost:4334/dvdrental/")
(d/create-database db-uri)
(d/get-database-names "datomic:dev://localhost:4334/*")

root@31bc28314f21:/# cd datomic/
root@31bc28314f21:/datomic# bin/repl
Clojure 1.11.1
user=> (require '[datomic.api :as d])
(def db-uri "datomic:dev://localhost:4334/dvdrental/")
nil
user=> #'user/db-uri
user=> (def db-uri "datomic:dev://localhost:4334/dvdrental/")
#'user/db-uri
user=> (d/create-database db-uri)
true
user=> (d/get-database-names "datomic:dev://localhost:4334/*")
("dvdrental")
</pre>

* Once we create the database, we can start loading the data using the programs. 

<pre>
cd C:\Venky\datomic-experiment\datomicexperiments\DatomicExperiments
mvn clean package

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

* Here is the run log.
<pre>
CreateDatabase
Creating a new database called dvdrental...
true

Actors Load
Peer connected to the datbase : datomic.db.Db@a41da3bd
Printing out names of actors...
Total number of records : 200
Showing top 10
===================
[143 "River" "Dean" #inst "2013-05-26T14:47:57.000-00:00"]
[182 "Debbie" "Akroyd" #inst "2013-05-26T14:47:57.000-00:00"]
[75 "Burt" "Posey" #inst "2013-05-26T14:47:57.000-00:00"]
[14 "Vivien" "Bergen" #inst "2013-05-26T14:47:57.000-00:00"]
[131 "Jane" "Jackman" #inst "2013-05-26T14:47:57.000-00:00"]
[31 "Sissy" "Sobieski" #inst "2013-05-26T14:47:57.000-00:00"]
[88 "Kenneth" "Pesci" #inst "2013-05-26T14:47:57.000-00:00"]
[82 "Woody" "Jolie" #inst "2013-05-26T14:47:57.000-00:00"]
[160 "Chris" "Depp" #inst "2013-05-26T14:47:57.000-00:00"]
===================
Printing out actor count...
Result is : 200

Address Load
Peer connected to the datbase : datomic.db.Db@5d6af5b4
Printing out names of addresses...
Total number of records : 603
Showing top 10
===================
[114 "804 Elista Drive" "Enshi" "China"]
[117 "1079 Tel Aviv-Jaffa Boulevard" "Cuman" "Venezuela"]
[245 "1103 Bilbays Parkway" "Xiangfan" "China"]
[27 "1780 Hino Boulevard" "Liepaja" "Latvia"]
[431 "1596 Acua Parkway" "Purnea (Purnia)" "India"]
[515 "886 Tonghae Place" "Kamyin" "Russian Federation"]
[128 "848 Tafuna Manor" "Ktahya" "Turkey"]
[442 "1245 Ibirit Way" "La Romana" "Dominican Republic"]
[136 "898 Belm Manor" "Botshabelo" "South Africa"]
===================
Printing out addresses count...
Result is : 603

Categories Load
Peer connected to the datbase : datomic.db.Db@c8407b34
Printing out names of categories...
Total number of records : 16
Showing top 10
===================
[1 "Action" #inst "2006-02-15T09:46:27.000-00:00"]
[7 "Drama" #inst "2006-02-15T09:46:27.000-00:00"]
[5 "Comedy" #inst "2006-02-15T09:46:27.000-00:00"]
[14 "Sci-Fi" #inst "2006-02-15T09:46:27.000-00:00"]
[2 "Animation" #inst "2006-02-15T09:46:27.000-00:00"]
[9 "Foreign" #inst "2006-02-15T09:46:27.000-00:00"]
[15 "Sports" #inst "2006-02-15T09:46:27.000-00:00"]
[11 "Horror" #inst "2006-02-15T09:46:27.000-00:00"]
[4 "Classics" #inst "2006-02-15T09:46:27.000-00:00"]
===================
Printing out category count...
Result is : 16

Stores
Printing out stores...
Total number of records : 2
Showing top 10
===================
[2 2 #inst "2006-02-15T09:57:12.000-00:00" 17592186045821]
[1 1 #inst "2006-02-15T09:57:12.000-00:00" 17592186045819]
===================
Printing out store count...
Result is : 2

Staff
Printing out staff...
Total number of records : 2
Showing top 10
===================
[17592186047061 "Jon" "Stephens" 17592186045825 "Jon.Stephens@sakilastaff.com" 17592186047061 "t" "Jon" "8cb2237d0679ca88db6464eac60da96345513964" #inst "2006-05-16T16:13:11.000-00:00"]
[17592186047059 "Mike" "Hillyer" 17592186045823 "Mike.Hillyer@sakilastaff.com" 17592186047059 "t" "Mike" "8cb2237d0679ca88db6464eac60da96345513964" #inst "2006-05-16T16:13:11.000-00:00"]
===================
Printing out staff count...
Result is : 2

Customer Load
Printing out customers...
Total number of records : 599
Showing top 10
===================
[383 17592186047059 "Martin" "Bales" "martin.bales@sakilacustomer.org" 17592186046591 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[232 17592186047061 "Constance" "Reid" "constance.reid@sakilacustomer.org" 17592186046289 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[582 17592186047061 "Andy" "Vanhorn" "andy.vanhorn@sakilacustomer.org" 17592186046989 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[246 17592186047059 "Marian" "Mendoza" "marian.mendoza@sakilacustomer.org" 17592186046317 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[264 17592186047059 "Gwendolyn" "May" "gwendolyn.may@sakilacustomer.org" 17592186046353 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[332 17592186047059 "Stephen" "Qualls" "stephen.qualls@sakilacustomer.org" 17592186046489 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[207 17592186047059 "Gertrude" "Castillo" "gertrude.castillo@sakilacustomer.org" 17592186046239 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[350 17592186047059 "Juan" "Fraley" "juan.fraley@sakilacustomer.org" 17592186046525 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
[316 17592186047059 "Steven" "Curley" "steven.curley@sakilacustomer.org" 17592186046457 true #inst "2006-02-14T00:00:00.000-00:00" #inst "2013-05-26T14:49:45.000-00:00" 1]
===================
Printing out customer count...
Result is : 599

Film Load
Printing out fimns...
Total number of records : 5462
Showing top 10
===================
[335 "Freedom Cleopatra" "A Emotional Reflection of a Dentist And a Mad Cow who must Face a Squirrel in A Baloon" 2006 "English" 5.0 0.99 133.0 23.99 "PG-13" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,Commentaries,\"Behind the Scenes\"}" "'baloon':20 'cleopatra':2 'cow':12 'dentist':8 'emot':4 'face':15 'freedom':1 'mad':11 'must':14 'reflect':5 'squirrel':17" 17592186047034 17592186045544]
[755 "Sabrina Midnight" "A Emotional Story of a Squirrel And a Crocodile who must Succumb a Husband in The Sahara Desert" 2006 "English" 5.0 4.99 99.0 11.99 "PG" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,\"Behind the Scenes\"}" "'crocodil':11 'desert':20 'emot':4 'husband':16 'midnight':2 'must':13 'sabrina':1 'sahara':19 'squirrel':8 'stori':5 'succumb':14" 17592186047030 17592186045736]
[604 "Mulan Moon" "A Emotional Saga of a Womanizer And a Pioneer who must Overcome a Dentist in A Baloon" 2006 "English" 4.0 0.99 160.0 10.99 "G" #inst "2013-05-26T14:50:58.000-00:00" "{\"Behind the Scenes\"}" "'baloon':19 'dentist':16 'emot':4 'moon':2 'mulan':1 'must':13 'overcom':14 'pioneer':11 'saga':5 'woman':8" 17592186047034 17592186045504]
[118 "Canyon Stock" "A Thoughtful Reflection of a Waitress And a Feminist who must Escape a Squirrel in A Manhattan Penthouse" 2006 "English" 7.0 0.99 85.0 26.99 "R" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,\"Deleted Scenes\"}" "'canyon':1 'escap':14 'feminist':11 'manhattan':19 'must':13 'penthous':20 'reflect':5 'squirrel':16 'stock':2 'thought':4 'waitress':8" 17592186047028 17592186045490]
[252 "Dream Pickup" "A Epic Display of a Car And a Composer who must Overcome a Forensic Psychologist in The Gulf of Mexico" 2006 "English" 6.0 2.99 135.0 18.99 "PG" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,Commentaries,\"Behind the Scenes\"}" "'car':8 'compos':11 'display':5 'dream':1 'epic':4 'forens':16 'gulf':20 'mexico':22 'must':13 'overcom':14 'pickup':2 'psychologist':17" 17592186047026 17592186045482]
[42 "Artist Coldblooded" "A Stunning Reflection of a Robot And a Moose who must Challenge a Woman in California" 2006 "English" 5.0 2.99 170.0 10.99 "NC-17" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,\"Behind the Scenes\"}" "'artist':1 'california':18 'challeng':14 'coldblood':2 'moos':11 'must':13 'reflect':5 'robot':8 'stun':4 'woman':16" 17592186047054 17592186045628]
[398 "Hanover Galaxy" "A Stunning Reflection of a Girl And a Secret Agent who must Succumb a Boy in A MySQL Convention" 2006 "English" 5.0 4.99 47.0 21.99 "NC-17" #inst "2013-05-26T14:50:58.000-00:00" "{Commentaries,\"Deleted Scenes\",\"Behind the Scenes\"}" "'agent':12 'boy':17 'convent':21 'galaxi':2 'girl':8 'hanov':1 'must':14 'mysql':20 'reflect':5 'secret':11 'stun':4 'succumb':15" 17592186047048 17592186045578]
[882 "Tenenbaums Command" "A Taut Display of a Pioneer And a Man who must Reach a Girl in The Gulf of Mexico" 2006 "English" 4.0 0.99 99.0 24.99 "PG-13" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,Commentaries}" "'command':2 'display':5 'girl':16 'gulf':19 'man':11 'mexico':21 'must':13 'pioneer':8 'reach':14 'taut':4 'tenenbaum':1" 17592186047038 17592186045750]
[833 "Splendor Patton" "A Taut Story of a Dog And a Explorer who must Find a Astronaut in Berlin" 2006 "English" 5.0 0.99 134.0 20.99 "R" #inst "2013-05-26T14:50:58.000-00:00" "{Trailers,Commentaries,\"Deleted Scenes\",\"Behind the Scenes\"}" "'astronaut':16 'berlin':18 'dog':8 'explor':11 'find':14 'must':13 'patton':2 'splendor':1 'stori':5 'taut':4" 17592186047030 17592186045502]
===================
Printing out film count...
Result is : 1000

Total number of records : 4581
Showing top 10
===================
[1995 17592186049134 17592186047061 #inst "2006-02-15T10:09:17.000-00:00"]
[1288 17592186048836 17592186047059 #inst "2006-02-15T10:09:17.000-00:00"]
[383 17592186048436 17592186047061 #inst "2006-02-15T10:09:17.000-00:00"]
[3197 17592186049670 17592186047061 #inst "2006-02-15T10:09:17.000-00:00"]
[1631 17592186048978 17592186047061 #inst "2006-02-15T10:09:17.000-00:00"]
[3544 17592186049816 17592186047059 #inst "2006-02-15T10:09:17.000-00:00"]
[1449 17592186048900 17592186047061 #inst "2006-02-15T10:09:17.000-00:00"]
[1575 17592186048958 17592186047059 #inst "2006-02-15T10:09:17.000-00:00"]
[366 17592186048426 17592186047059 #inst "2006-02-15T10:09:17.000-00:00"]
===================
Printing out inventory count...
Result is : 4581

Rental 
Printing rentals...
Total number of records : 15861
Showing top 10
===================
[10737 #inst "2005-08-01T19:31:24.000-00:00" 17592186053813 17592186047367 #inst "2005-08-10T19:17:24.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047064]
[4088 #inst "2005-07-07T05:31:55.000-00:00" 17592186050735 17592186047201 #inst "2005-07-15T07:35:55.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047064]
[7598 #inst "2005-07-27T23:36:01.000-00:00" 17592186059357 17592186047945 #inst "2005-07-29T23:35:01.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047066]
[5432 #inst "2005-07-09T21:21:25.000-00:00" 17592186054183 17592186047197 #inst "2005-07-14T21:34:25.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047066]
[726 #inst "2005-05-29T06:05:29.000-00:00" 17592186052133 17592186047973 #inst "2005-06-05T04:40:29.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047066]
[14702 #inst "2005-08-21T21:00:03.000-00:00" 17592186058459 17592186047759 #inst "2005-08-30T16:59:03.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047064]
[13197 #inst "2005-08-19T14:44:03.000-00:00" 17592186056647 17592186047785 #inst "2005-08-22T10:11:03.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047064]
[89 #inst "2005-05-25T14:28:29.000-00:00" 17592186054629 17592186048067 #inst "2005-05-29T14:33:29.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047064]
[1152 #inst "2005-05-31T21:32:17.000-00:00" 17592186050687 17592186047451 #inst "2005-06-04T21:07:17.000-00:00" #inst "2006-02-16T02:30:53.000-00:00" 17592186047066]
===================
Printing out rentals count...
Result is : 16044

Payment
Printing payments...
Total number of records : 14596
Showing top 10
===================
[30803 17592186047387 17592186047066 17592186073193 4.99 #inst "2007-04-12T19:24:30.000-00:00"]
[19762 17592186047651 17592186047066 17592186090745 3.99 #inst "2007-03-23T07:22:52.000-00:00"]
[27665 17592186048045 17592186047066 17592186075551 2.99 #inst "2007-04-28T15:44:04.000-00:00"]
[28416 17592186048183 17592186047064 17592186077139 4.99 #inst "2007-04-29T22:13:01.000-00:00"]
[18810 17592186047231 17592186047064 17592186065133 5.99 #inst "2007-02-19T21:40:14.000-00:00"]
[21580 17592186048033 17592186047066 17592186088885 0.99 #inst "2007-03-21T20:50:55.000-00:00"]
[19320 17592186047501 17592186047064 17592186062755 0.99 #inst "2007-02-16T08:43:46.000-00:00"]
[31306 17592186047491 17592186047064 17592186075373 2.99 #inst "2007-04-28T12:41:13.000-00:00"]
[19016 17592186047333 17592186047064 17592186065667 4.99 #inst "2007-02-20T16:51:56.000-00:00"]
===================
Printing out payments count...
Result is : 14596
</pre>

* We can also restore the mbrianz dataset from the Datomic examples and restore it

<pre>
wget https://s3.amazonaws.com/mbrainz/datomic-mbrainz-1968-1973-backup-2017-07-20.tar -O mbrainz.tar
tar -xvf mbrainz.tar

root@31bc28314f21:/datomic# bin/datomic restore-db file:///datomic/mbrainz-1968-1973/ "datomic:dev://localhost:4334/mbrainz-1968-1973"
Copied 0 segments, skipped 0 segments.
Copied 1226 segments, skipped 0 segments.
Copied 1414 segments, skipped 0 segments.
:succeeded
{:event :restore, :db mbrainz-1968-1973, :basis-t 148253, :inst #inst "2017-07-20T16:07:40.880-00:00"}
root@31bc28314f21:/datomic#
</pre>
