## DVD Rental on Docker version of Datomic.

* The previous examples took the easy route and used the in-memory version of the database to make it easier to test. Now we have started the free version of datomic as a container on docker. Once we login to the container's bash, we can start the REPL and create the database. The Peer API for some reason does not allow us to issue the command via the Peer object. It keeps throwing an error saying the URL syntax is incorrect. Using repl works as we can see...

<pre>
docker exec -it datomic-free bash
bin/repl
(require '[datomic.api :as d])
(def db-uri "datomic:free://localhost:4334/dvdrental/")
(d/create-database db-uri)
(d/get-database-names db-uri)

root@06a7441db384:/datomic# bin/repl
Clojure 1.9.0
user=> (require '[datomic.api :as d])
nil
user=> (def db-uri "datomic:free://localhost:4334/dvdrental/")
#'user/db-uri
user=> (d/create-database db-uri)
true
user=> (d/get-database-names "datomic:free://localhost:4334/*")
("dvdrental")
</pre>

* Once we create the database, we can start loading the data using the programs. 

<pre>
cd C:\Venky\datomic-experiment\datomicexperiments\DatomicExperiments
mvn clean package

mvn exec:java -Dexec.mainClass="com.gssystems.datomic.docker.ActorsLoad"

root@179c39a1924a:/datomic# bin/datomic restore-db file:///datomic/mbrainz-1968-1973/ "datomic:free://localhost:4334/mbrainz-1968-1973"
Copied 0 segments, skipped 0 segments.
Copied 1414 segments, skipped 0 segments.
:succeeded
{:event :restore, :db mbrainz-1968-1973, :basis-t 148253, :inst #inst "2017-07-20T16:07:40.880-00:00"}

</pre>