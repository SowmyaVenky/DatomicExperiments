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

</pre>

