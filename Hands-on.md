# Choose the right ML tool and use it like a pro!

K-means is a widely used clustering algorithm in machine learning community. 
It iteratively computes the distance between each training point to every centroid, 
re-assigns the training point to new cluster and re-compute the new centroid of each cluster. 

This hands-on includes two tasks

* Write a Harp-DAAL K-means program by using Java API (8 steps)
* Run and tune Harp-DAAL K-means from an image clustering application via python API (5 steps)

Users are supposed to get an access to a machine with sudo permission and pre-installed docker environment. 

## Prerequisites

### Download Docker Image and launch Container Instance

If the machine already provides a docker image (tarball file), just import the image and launch the 
container
```bash
sudo docker import /mnt/backup/harp-daal-docker-image.tar
## check the container id
sudo docker image ls
## Start a container
sudo docker run -it <container_id> /etc/bootstrap.sh -bash
```

Otherwise, download the image via network and launch the container
```bash
# Download an image
sudo docker pull lee212/harp-daal:icc_included
# Start a container
sudo docker run -it lee212/harp-daal:icc_included /etc/bootstrap.sh -bash
```

After executing the last command you will be logged on to the docker image.

The container takes up to 20GB disk space. If the machine has more than 50GB disk space, there shall be no problem to 
launch the container instance. Otherwise, users could use the following commands to clean up the docker space 

```bash
# Remove useless docker images
sudo docker image rm <useless-docker-image>
# Remove exited containers
sudo docker rm $(sudo docker ps -a -f status=exited -q)
# Clean up all dangling cache
sudo docker system prune
```
Find the docker container ID

```bash
sudo docker ps
```
and log into the docker 

```bash
sudo docker exec -it <container_id> bash
```

### Dependencies and Environment Variables

The hands-on codes have the dependencies as follows,

* Python 2.7+
* Python module Numpy 
* Hadoop 2.6.0/Hadoop 2.6.5
* DAAL 2018+ 

The following section describes where the important components of the Tutorial are

1. Harp Source Code - /harp                                                                   
2. Hadoop Installation - /usr/local/hadoop                                                        
3. K-Means tutorial code - /harp/harp-daal-app/src/main/java/edu/iu/daal_tutorial/daal_kmeans      
4. Python Code - /harp/harp-daal-python/examples/daal/                                   

The docker image already includes them and other tools, the image has the following machine learning algorithms

```bash
# List of HarpDaal applications (in harp-daal-<version>.jar)
edu.iu.daal_als.ALSDaalLauncher
edu.iu.daal_cov.COVDaalLauncher
edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher
edu.iu.daal_linreg.LinRegDaalLauncher
edu.iu.daal_mom.MOMDaalLauncher
edu.iu.daal_naive.NaiveDaalLauncher
edu.iu.daal_nn.NNDaalLauncher
edu.iu.daal_pca.PCADaalLauncher
edu.iu.daal_qr.QRDaalLauncher
edu.iu.daal_ridgereg.RidgeRegDaalLauncher
edu.iu.daal_sgd.SGDDaalLauncher
edu.iu.daal_svd.SVDDaalLauncher

# List of HarpDaal examples in Python (/harp/harp-daal-python/examples/daal/)
run_harp_daal_ALSDaal.py
run_harp_daal_COVDaal.py
run_harp_daal_KMeansDaal.py
run_harp_daal_LinRegDaal.py
run_harp_daal_MOMDaal.py
run_harp_daal_NNDaal.py
run_harp_daal_NaiveDaal.py
run_harp_daal_PCADaal.py
run_harp_daal_QRDaal.py
run_harp_daal_RidgeRegDaal.py
run_harp_daal_SGDDaal.py
run_harp_daal_SVDDaal.py

# Image clustering example
/harp/harp-daal-python/examples/scdemo/tutorial/
```

The *bootstrap* script shall launch a Hadoop Cluster and set up all the environment variables. 
To verify the Hadoop status
```bash
## To check the status of HDFS
${HADOOP_HOME}/bin/hdfs dfsadmin -report
## To check the status of Yarn 
${HADOOP_HOME}/bin/yarn node -list
```

Check out the environment variables to figure out the locations of the important files.
```bash
echo $HADOOP_HOME
/usr/local/hadoop

echo $HARP_JAR
/usr/local/hadoop/harp-app-1.0-SNAPSHOT.jar

echo $HARP_DAAL_JAR
/usr/local/hadoop/harp-daal-app-1.0-SNAPSHOT.jar

echo $DAALROOT
/harp/harp-daal-app/__release__lnx/daal

echo $PYTHONPATH
/harp/harp-daal-python
```

If the script fails to complete these steps, users could manually set them 
```bash
export HADOOP_HOME=<path to hadoop home folder>
export HARP_JAR=<path to>/harp-app-1.0-SNAPSHOT.jar
export HARP_DAAL_JAR=<path to>/harp-daal-app-1.0-SNAPSHOT.jar
export DAALROOT=<path to your compiled daal folder>
export PYTHONPATH=<path to>/harp-daal-python
```

In a situation where you would like to stop and restart Hadoop, use the following commands

```bash
## stop all services
${HADOOP_HOME}/sbin/stop-all.sh
## launch HDFS service
${HADOOP_HOME}/sbin/start-dfs.sh
## launch yarn daemons
${HADOOP_HOME}/sbin/start-yarn.sh
```

## Program K-means via Java APIs 

Harp-DAAL framework provides developers of Java API to implement inter-mapper communication patterns and invoke intra-node DAAL kernels. The K-means source files are located at 

``` bash
/harp/harp-daal-app/src/main/java/edu/iu/daal_tutorial/daal_kmeans/
```

Lets open the source file where K-Means is implemented.

``` bash
vi /harp/harp-daal-app/src/main/java/edu/iu/daal_tutorial/daal_kmeans/KMeansDaalCollectiveMapper.java
```

Users shall edit the function *runKmeans* from source file *KMeansDaalCollectiveMapper.java*. 
Currently, the function *runKmeans* is only a skeleton, and users will go through 8 steps to finish 
a complete Harp-DAAL-Kmeans application as indicated in the following code snippet.

```java
private void runKmeans(List<String> fileNames, Configuration conf, Context context) {
  long start_execution = System.currentTimeMillis();
  this.fileNames = fileNames;
  this.conf = conf;

  //************* Step 1: load training data *********************
  //************* Step 2: load centroids (model) data *********************
  //************* Step 3: convert training data from harp to daal *********************
  //************* Step 4: Setup DAAL K-means kernel and cenTable at DAAL side *********************
  // start the iteration
  for (int i = 0; i < numIterations; i++) {
    //************* Step 5: Convert Centroids data from Harp to DAAL *********************
    //************* Step 6: Local computation by DAAL to get partial result *********************
    //************* Step 7: Inter-Mapper communication *********************
  }
  //************* Step 8: Release Memory and Store Centroids *********************
}
```

The codes of missing steps are already packaged into private member function of *KMeansDaalCollectiveMapper.java* named *runKmeans_Answer* and you can use it to get it to working quickly. Please refer to member function definition for 
implementation details of each step. After adding codes at each step, re-compile the harp-daal-application 
by maven at the root harp directory (where the pom.xml resides) 

```bash
cd /harp
mvn clean package
```

and re-run the application on Hadoop cluster
```bash
cd /harp/harp-daal-app/test_scripts
./harp-daal-tutorial-kmeans.sh
```

### Solution Description

The following sections describe each step of the algorithm that is left blank. 

### Step 1: Load training data (feature vectors)

Use the following function to load training data from HDFS. 

```java
// create a pointArray
List<double[]> pointArrays = LoadTrainingData();
```

### Step 2: Load model data (centroids)

Similarly, create a harp table object *cenTable* and load centroids from HDFS. 
Because centroids are requested by all the mappers, load them at master mapper and 
broadcast them to all the other mappers. 

```java
// create a table to hold centroids data
Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());

if (this.isMaster()) {
  createCenTable(cenTable);
  loadCentroids(cenTable);
}
// Bcast centroids to other mappers
bcastCentroids(cenTable, this.getMasterID());
```

### Step 3: Convert training data from Harp side to DAAL side

The training data loaded from HDFS are stored at Java heap memory. To invoke DAAL kernel, convert them to 
DAAL *NumericTable* 

```java
// convert training data fro harp to daal
NumericTable trainingdata_daal = convertTrainData(pointArrays);
```

It allocates native memory for *NumericTable* and copy data from *pointArrays* to *trainingdata_daal*

### Step 4: Create and set up DAAL K-means kernel 

DAAL provides the following Java API to invoke their low-level native kernels written for K-means
```java
import com.intel.daal.algorithms.kmeans.*;
import com.intel.daal.algorithms.kmeans.init.*;
import com.intel.daal.services.Environment;
```

Call them by specifying the input training data object and number of centroids 

```java
// create a daal kmeans kernel object
DistributedStep1Local kmeansLocal = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense, this.numCentroids);
// set up input training data
kmeansLocal.input.set(InputId.data, trainingdata_daal);
```

As DAAL uses MKL and TBB within its implementation, specify the number of threads used by DAAL (by default a maximum available threads on a processor)

```java
// specify the threads used in DAAL kernel
Environment.setNumberOfThreads(numThreads);
```

Finally, create another *NumericTable* to store centroids at DAAL side

```java
// create cenTable at daal side
NumericTable cenTable_daal = createCenTableDAAL();
```

### Step 5: Convert centroids from Harp to DAAL

Centroids are stored in harp table *cenTable* for inter-mapper communication. Convert them 
to DAAL within each iteration of local computation. 

```java
//Convert Centroids data from Harp to DAAL
convertCenTableHarpToDAAL(cenTable, cenTable_daal);
```

### Step 6: Local computation by DAAL kernel

Call DAAL K-means kernels of local computation at each iteration.

```java
// specify centroids data to daal kernel 
kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);
// first step of local computation by using DAAL kernels to get partial result
PartialResult pres = kmeansLocal.compute();
```

### Step 7: Inter-mapper communication  

Harp-DAAL-Kmeans adopts an *allreduce* computation model, where each mapper keeps a local copy of the whole model data (centroids). 
However, it provides different communication operations to synchronize model data among mappers. 

* Regroup & Allgather (default)
* Allreduce
* Broadcast & Reduce
* Push-Pull

All of the operations will take in two arguments, 1) *cenTable* at harp side, 2) partial results obtained from DAAL; Internally, the data is retrieved from DAAL partial results and communicated by Harp.

In Regroup & Allgather operation, it first combines the same centroid from different mappers and re-distributes them 
to mappers by a specified order. After averaging the centroids, an allgather operation makes every mapper get 
a complete copy of the averaged centroids. 

```java
comm_regroup_allgather(cenTable, pres);
```

In Allreduce operation, the centroids are reduced and copied to every mapper. Then an average operation applies to them on each mapper. 

```java
comm_allreduce(cenTable, pres);
```

In Broadcast & Reduce, it first reduces centroids to a single mapper (master mapper), where the average operation applies. It then broadcasts the averaged centroids data to every other mapper. 

```java
comm_broadcastreduce(cenTable, pres);
```

In push-pull, it first pushes centroids data from *cenTable* of local mapper to a *globalTable*, which is distributed across all the mappers. It then applies the average operation on *globalTable* from each mapper, and finally, pull the results from *globalTable* to update the local *cenTable*.

```java
Table<DoubleArray> globalTable = new Table<DoubleArray>(0, new DoubleArrPlus());
comm_push_pull(cenTable, globalTable, pres);
```

After finishing each iteration, call the *printTable* to check the centroids result

```java
//for iteration i, check 10 first centroids, each 
//centroid prints out first 10 dimension
printTable(cenTable, 10, 10, i); 
```

### Step 8: Release memory and store centroids 

After all of the iterations, release the allocated memory at DAAL side and for harp table object.
The centroids as output are stored at HDFS 

```java
// free memory and record time
cenTable_daal.freeDataMemory();
trainingdata_daal.freeDataMemory();
// Write out centroids
if (this.isMaster()) {
	KMUtil.storeCentroids(this.conf, this.cenDir,
	cenTable, this.cenVecSize, "output");
}
cenTable.release();
```

## Invoke Harp-DAAL K-means via Python Interface 

Harp-DAAL currently provides Python API, which interfaces 
Harp-DAAL with other python written applications. By just adding several lines of python code, you 
are able to deploy the original python application on Hadoop Cluster and boost the performance by 
leveraging DAAL kernels. 

The python codes for image clustering is located at the path

```bash
${PYTHONPATH}/examples/scdemo/tutorial
```

### Step.1 Run image clustering on 15 scenery dataset with Python Scikit-Learn K-means 

Run the pipeline from feature extraction, training, evaluation and finally check the clusters results.

```python
cd ${PYTHONPATH}/examples/scdemo/test
../tutorial/run_kmeans.sh
```
![screen shot of results](https://raw.githubusercontent.com/DSC-SPIDAL/harp/master/harp-daal-python/examples/scdemo/tutorial/imgcluster_runlocal.png)


### Step.2 Modify to invokes Harp-DAAL

*demo_kmeans_local.py* is the original python codes of image clustering without Harp-DAAL. 
```python
# ############################################################################
# call kmeans module 
# ############################################################################
KMeans(init='random', n_clusters=n_digits, max_iter=1000, tol=0, n_init=1, n_jobs=1)
```
*demo_kmeans_daal.py* replaces the above K-means module by a Harp-DAAL invocation

```python
# ############################################################################
# call Harp-DAAL Kmeans module 
# ############################################################################
DAALKMeans(n_clusters=n_digits, max_iter=1000, n_init=1, workdir="/15scene-work")
```

View all the modifications by
```bash
diff ../tutorial/demo_kmeans_local.py ../tutorial/demo_kmeans_daal.py
```
![code diff](https://raw.githubusercontent.com/DSC-SPIDAL/harp/master/harp-daal-python/examples/scdemo/tutorial/diffcode.png)


### Step.3 Invoke Harp-DAAL

```bash
../tutorial/run_kmeans.sh daal
```

### Step.4 Check the results of clustering

Download the result files.

```bash
# Copy a file from the running container to the host server
sudo docker cp <container-id>:/harp/harp-daal-python/examples/scdemo/test/local.pdf local.pdf

# Copy a file from the server to local client
scp <username>@<server-ip>:<path-to>/local.pdf local.pdf

```

### Step.5 (Optional) Tune Harp-DAAL-Kmeans parameters

*daal_kmeans.py* contains the python API to Harp-DAAL-Kmeans Java codes.
In the *__init__* function, tune the arguments (parameters) and compare the performance.

```python
    def __init__(self, n_clusters=10, max_iter=10, init = 'random', n_init = 1,
            n_node = 1, n_thread = 8, n_mem = 10240, workdir = "/kmeans-work"
            ):
        """
        n_clusters  ; set number of clusters
        max_iter    ; set maximum iteration number
        n_node      ; set mapper number
        n_thread    ; set thread number in each mapper
        init        ; set the centroid initialization method, 'random' by default
        n_init      ; set the number of runs to select the best model, 1 by default
        """

```

* Increase iteration number *max_iter* to check the changes in results
* Increase the thread number *n_thread* to check the performance boost by multi-threading
* Increase the mapper number *n_node* to check benefits from node-level parallelism.


















