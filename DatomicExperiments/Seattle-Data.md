## Explore Seattle data

* This is a little more complex example where we have the data about various communities in Seattle. This one has a more complex EDN and many enumerations. Please refer to the file <a href="./src/main/resources/seattle/seattle-schema.edn">schema</a> to understand the schema we are defining in Datomic. Please refer to this file <a href="./src/main/resources/seattle/seattle_mod.edn">seattle data</a> for the data loaded. Note the raw data we got had some reference IDs but this is no longer supported by datomic. That is the reason we need to transform the data and remove these reference IDs and then load the data. Also note that the data type of the neighborhood was changed from a reference type to string to make it easier to load. 

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

The run log for these programs can be seen <a href="./seattle_run.txt">here</a>