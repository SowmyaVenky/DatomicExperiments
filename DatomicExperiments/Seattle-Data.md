## Explore Seattle data

* This is a little more complex example where we have the data about various communities in Seattle. This one has a more complex EDN and many enumerations. 

<pre>
## Common script to set env for maven 

set JAVA_HOME=c:\Venky\jdk-11.0.15.10-hotspot
set PATH=%PATH%;c:\Venky\spark\bin;c:\Venky\apache-maven-3.8.4\bin
set SPARK_HOME=c:\Venky\spark
SET HADOOP_HOME=C:\Venky\DP-203\AzureSynapseExperiments\SparkExamples

cd C:\Venky\DatomicExperiments\DatomicExperiments
mvn clean package

mvn exec:java -Dexec.mainClass="com.gssystems.datomic.FormatEDNFilesToRemoveId"
mvn exec:java -Dexec.mainClass="com.gssystems.datomic.SeattleDataExample"
</pre>