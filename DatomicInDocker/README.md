# Java API Experiments with Datomic 

* First we need to setup the docker instance of datomic and make sure it is running. Once we have this on, we can develop the code to interact with local in memory database, or with the docker version. 

<pre>
## Common script to set env for maven 

set JAVA_HOME=c:\Venky\jdk-11.0.15.10-hotspot
set PATH=%PATH%;c:\Venky\spark\bin;c:\Venky\apache-maven-3.8.4\bin
set SPARK_HOME=c:\Venky\spark
SET HADOOP_HOME=C:\Venky\DP-203\AzureSynapseExperiments\SparkExamples

cd C:\Venky\DatomicExperiments\DatomicExperiments
mvn clean package

mvn exec:java -Dexec.mainClass="com.gssystems.datomic.MoviesExample"

[INFO] Scanning for projects...
[INFO]
[INFO] --------------< com.gssystems.datomic:DatomicExperiments >--------------
[INFO] Building DatomicExperiments 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- exec-maven-plugin:3.1.0:java (default-cli) @ DatomicExperiments ---
Creating a new database called movies...
[com.gssystems.datomic.MoviesExample.main()] INFO datomic.process-monitor - {:event :metrics/initializing, :metricsCallback clojure.core/identity, :phase :begin, :pid 20688, :tid 28}
[com.gssystems.datomic.MoviesExample.main()] INFO datomic.process-monitor - {:event :metrics/initializing, :metricsCallback clojure.core/identity, :msec 1.43, :phase :end, :pid 20688, :tid 28}
[com.gssystems.datomic.MoviesExample.main()] INFO datomic.process-monitor - {:metrics/started clojure.core/identity, :pid 20688, :tid 28}
[clojure-agent-send-off-pool-0] INFO datomic.domain - {:event :cache/create, :cache-bytes 2069889024, :pid 20688, :tid 29}
Applying the schema to the database we created...
[clojure-agent-send-off-pool-0] INFO datomic.process-monitor - {:DbAddFulltextMsec {:lo 0, :hi 76, :sum 97, :count 15}, :AvailableMB 3830.0, :ObjectCacheCount 0, :event :metrics, :pid 20688, :tid 29}
After schema create transaction answer is : {:db-before datomic.db.Db@2716fa51, :db-after datomic.db.Db@7a6c7f71, :tx-data [#datom[13194139534312 50 #inst "2023-10-28T02:16:46.120-00:00" 13194139534312 true] #datom[72 10 :movie/title 13194139534312 true] #datom[72 42 38 13194139534312 true] #datom[72 40 23 13194139534312 true] #datom[72 41 35 13194139534312 true] #datom[72 62 "The title of the movie" 13194139534312 true] #datom[73 10 :movie/genre 13194139534312 true] #datom[73 40 23 13194139534312 true] #datom[73 41 35 13194139534312 true] #datom[73 62 "The genre of the movie" 13194139534312 true] #datom[74 10 :movie/release-year 13194139534312 true] #datom[74 40 22 13194139534312 true] #datom[74 41 35 13194139534312 true] #datom[74 62 "The year the movie was released in theaters" 13194139534312 true] #datom[0 13 72 13194139534312 true] #datom[0 13 73 13194139534312 true] #datom[0 13 74 13194139534312 true]], :tempids {-9223301668109598134 72, -9223301668109598133 73, -9223301668109598132 74}}
Inserting new movies to the database after reading from file...
After movies insert transaction answer is : {:db-before datomic.db.Db@7a6c7f71, :db-after datomic.db.Db@4368ca69, :tx-data [#datom[13194139534313 50 #inst "2023-10-28T02:16:46.151-00:00" 13194139534313 true] #datom[17592186045418 72 "The Goonies" 13194139534313 true] #datom[17592186045418 73 "action/adventure" 13194139534313 true] #datom[17592186045418 74 1985 13194139534313 true] #datom[17592186045419 72 "Commando" 13194139534313 true] #datom[17592186045419 73 "action/adventure" 13194139534313 true] #datom[17592186045419 74 1985 13194139534313 true] #datom[17592186045420 72 "Repo Man" 13194139534313 true] #datom[17592186045420 73 "punk dystopia" 13194139534313 true] #datom[17592186045420 74 1984 13194139534313 true] #datom[17592186045421 72 "Dil Chahata Hai" 13194139534313 true] #datom[17592186045421 73 "Indian" 13194139534313 true] #datom[17592186045421 74 1984 13194139534313 true] #datom[17592186045422 72 "Hum Aap Ke Hai Kaun" 13194139534313 true] #datom[17592186045422 73 "Indian" 13194139534313 true] #datom[17592186045422 74 1984 13194139534313 true]], :tempids {-9223301668109598131 17592186045418, -9223301668109598130 17592186045419, -9223301668109598129 17592186045420, -9223301668109598128 17592186045421, -9223301668109598127 17592186045422}}
Peer connected to the datbase : datomic.db.Db@4368ca69
===================
[17592186045418]
[17592186045419]
[17592186045420]
[17592186045421]
[17592186045422]
===================
Printing out the movie titles instead of the IDs
===================
["Dil Chahata Hai"]
["Commando"]
["The Goonies"]
["Repo Man"]
["Hum Aap Ke Hai Kaun"]
===================
Printing out the movie titles for year = 1985
===================
["Commando"]
["The Goonies"]
===================
Printing out the movie DETAILS for year = 1984
===================
["Dil Chahata Hai" "Indian" 1984]
["Repo Man" "punk dystopia" 1984]
["Hum Aap Ke Hai Kaun" "Indian" 1984]
===================
</pre>

