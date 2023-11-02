# Docker run for Datomic 

* Make sure docker desktop is installed and running fine on the computer before we can go ahead. 

<pre>
## Go to the data directory before launching the docker instance. 
cd C:\venky\DatomicExperiments\data

docker run -d -e ADMIN_PASSWORD="admin" -e DATOMIC_PASSWORD="datomic" -p 4334-4336:4334-4336 --name datomic-free -v %cd%:/data akiel/datomic-free 

C:\Venky\DatomicExperiments\data>docker ps
CONTAINER ID   IMAGE                COMMAND        CREATED         STATUS         PORTS                              NAMES
af72d4a453c3   akiel/datomic-free   "./start.sh"   7 seconds ago   Up 6 seconds   0.0.0.0:4334-4336->4334-4336/tcp   datomic-free

</pre>

* This ensures that the data directory is mounted correctly to the container via the -v flag to point to /datomic/data from inside the container. We can put our data files at this location, and use the datomic exec commands from inside the container to import the data into a datomic instance.
