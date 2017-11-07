# Choose the right ML tool and use it like a pro!

K-means is a widely used clustering algorithm in machine learning community. 
It iteratively computes the distance between each training point to every centroids, 
re-assigns the training point to new cluster and re-compute the new centroid of each cluster. 

This hands-on includes two tasks

* Write a Harp-DAAL K-means program by using Java API
* Run and tune Harp-DAAL K-means from an image clustering application via python API 

Users are supposed to get an access to a machine with sudo permission and pre-installed docker environment. 

## Environment Setups

Execute the two commands to load in docker image and launch a container instance.
```bash
# Download an image
sudo docker pull lee212/harp-daal:icc_included
# Start a container
sudo docker run -it lee212/harp-daal:icc_included /etc/bootstrap.sh -bash
```

The container takes up to 20GB disk space, if the machine has more than 50GB disk space, there shall be no problem to 
launch the container instance. Otherwise, users could use the following commands to clean up the Docker space 
```bash
# Remove useless docker images
sudo docker image rm <useless-docker-image>
# Remove exited containers
sudo docker rm $(sudo docker ps -a -f status=exited -q)
# Clean up all dangling cache
sudo docker system prune
```

If network is not available, a docker image in tar file is provided with the instructions to load. 
https://docs.docker.com/engine/reference/commandline/load/

The *bootstrap* script shall launch a Hadoop Cluster and setup all the environment variables. 
To verify the Hadoop status
```bash
## To check the status of HDFS
bin/hdfs dfsadmin -report
## To check the status of Yarn 
bin/yarn node -list
```

and the env vars
```bash
echo $HADOOP_HOME
echo $HARP_JAR
echo $HARP_DAAL_JAR
echo $DAALROOT
echo $PYTHONPATH
```

If the script fails to complete these steps, users could manually set them 
```bash
export HADOOP_HOME=<path to hadoop home folder>
export HARP_JAR=<path to>/harp-app-1.0-SNAPSHOT.jar
export HARP_DAAL_JAR=<path to>/harp-daal-app-1.0-SNAPSHOT.jar
export DAALROOT=<path to your compiled daal folder>
export PYTHONPATH=<path to>/harp-daal-python
```

and launch the hadoop daemons
```bash
## launch HDFS service
${HADOOP_HOME}/sbin/start-dfs.sh
## launch yarn daemons
${HADOOP_HOME}/sbin/start-yarn.sh
```

## Program K-means via Java APIs 

Harp-DAAL framework provides developers of Java API to implement inter-mapper communication patterns and invoke intra-node DAAL kernels. 
The K-means source files are located at 
**harp-daal-app/src/main/java/edu/iu/daal_tutorial/daal_kmeans**

Users shall edit the function *runKmeans* from source file **KMeansDaalCollectiveMapper.java**. 
Currently, the function *runKmeans*is only a skeleton, and users will go through 8 steps to finish 
a complete Harp-DAAL-Kmeans application as indicated in the following code snippet. Besides, the answers to 
*runKmeans* are located at function *runKmeans_Answer* of the same file. 

```java
private void runKmeans(List<String> fileNames, Configuration conf, Context context)
{
	long start_execution = System.currentTimeMillis();
	this.fileNames = fileNames;
	this.conf = conf;

	//************* Step 1: load in training data *********************
	//************* Step 2: load in centroids (model) data *********************
	//************* Step 3: convert training data fro harp to daal *********************
	//************* Step 4: Setup DAAL K-means kernel and cenTable at DAAL side *********************
	// start the iteration
	for (int i = 0; i < numIterations; i++) 
	{
		//************* Step 5: Convert Centroids data from Harp to DAAL *********************
		//************* Step 6: Local computation by DAAL to get partial result *********************
		//************* Step 7: Inter-Mapper communication *********************
	}
	//************* Step 8: Release Memory and Record time *********************
}
```

The codes of missing steps are already packaged into private member functions of *KMeansDaalCollectiveMapper.java*. 
Please refer to function definitions for all the implementation details.
After adding codes at each step, re-compile the harp-daal-application by maven
at the root harp directory (where the pom.xml resides) 
```bash
mvn clean package
```

and re-run the application on Hadoop cluster
```bash
./harp-daal-app/test_scripts/harp-daal-tutorial-kmeans.sh
```

### Step 1: Load Training Data 

Use the following function to load in training data (vectors) from HDFS in parallel
```java
// create a pointArray
List<double[]> pointArrays = LoadTrainingData();
```

### Step 2: Load in Model Data (Centroids)

Similarly, create a harp table object *cenTable* and load in centroid data from HDFS. 
Because centroid data are requested by all the mappers, load them at master mapper and 
broadcast them to all the other mappers. 
```java
// create a table to hold centroids data
Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());
//
if (this.isMaster()) 
{
createCenTable(cenTable);
loadCentroids(cenTable);
}
// Bcast centroids to other mappers
bcastCentroids(cenTable, this.getMasterID());
```

### Step 3: Convert Training Data from Harp side to DAAL side

The training data loaded from HDFS are stored at Java heap memory. To invoke DAAL kernel, convert them into 
the DAAL *NumericTable* 

```java
// convert training data fro harp to daal
NumericTable trainingdata_daal = convertTrainData(pointArrays);
```

It allocates native memory for *NumericTable* and copy data from *pointArrays* to *trainingdata_daal*

### Step 4: Create and setup DAAL K-means kernel 

DAAL provides the following Java API for invoking their low-level native kernels written for K-means
```java
import com.intel.daal.algorithms.kmeans.*;
import com.intel.daal.algorithms.kmeans.init.*;
import com.intel.daal.services.Environment;
```

Call them by specifying the input training data object and centroids number 

```java
// create a daal kmeans kernel object
DistributedStep1Local kmeansLocal = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense, this.numCentroids);
// set up input training data
kmeansLocal.input.set(InputId.data, trainingdata_daal);
```
As DAAL uses MKL and TBB at its implementation, specify the number of threads used by DAAL (by default a maximal available threads on processor)

```java
// specify the threads used in DAAL kernel
Environment.setNumberOfThreads(numThreads);
```

Finally, create another *NumericTable* to store centroids (model) data at DAAL side

```java
// create cenTable at daal side
NumericTable cenTable_daal = createCenTableDAAL();
```

### Step 5: Convert Centroids data from Harp to DAAL

Centroids are stored in harp table *cenTable* for inter-mapper communication. Convert them 
to DAAL within each iteration of local computation. 
```java
//Convert Centroids data from Harp to DAAL
convertCenTableHarpToDAAL(cenTable, cenTable_daal);
```

### Step 6: Local Computation by DAAL kernel

Call DAAL K-means kernels of local computation at each iteration.

```java
// specify centroids data to daal kernel 
kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);
// first step of local computation by using DAAL kernels to get partial result
PartialResult pres = kmeansLocal.compute();
```

### Step 7: Inter-Mapper Communication  

Harp-DAAL-Kmeans adopts an *allreduce* computation model, where each mapper keeps a local copy of the whole model data (centroids). 
However, it provides different communication operations to synchronize model data among mappers. 

* Regroup & Allgather (default)
* Allreduce
* Broadcast & Reduce
* Push-Pull

All of the operations will take in two arguments, 1) *cenTable* at harp side, 2) partial results computed from DAAL; Internally, the 
data is retrieved from DAAL partial results and communicated by Harp.

In Regroup & Allgather operation, it first combines the same centroid from different mappers and re-distribute them 
to mappers with a specified order. After average operation on the centroids, an allgather operation lets every mapper get 
a complete copy of the averaged centroids data. 

```java
comm_regroup_allgather(cenTable, pres);
```

In Allreduce operation, the centroids are reduced and copied to every mapper. Then an average operation apply to them on each mapper to 
get the results. 
```java
comm_allreduce(cenTable, pres);
```

In Broadcast & Reduce, it first reduces centroids to a single mapper (master mapper), where the average operation applied. It then broadcasts
the averaged centroids data to every other mapper. 

```java
comm_broadcastreduce(cenTable, pres);
```

In push-pull operation, it first pushes centroids data from *cenTable* of local mapper to a *globalTable*, which is consistent across all the mappers. 
It then applies the average operation on *globalTable* from each mapper, and finally, pull the results from *globalTable* to update the local *cenTable*.
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

### Step 8: Release Memory and Record execution time

After all of the iterations, release the allocated memory at DAAL side and for harp table object.
Log the execution time for all the iterations
```java
// free memory and record time
cenTable_daal.freeDataMemory();
trainingdata_daal.freeDataMemory();
cenTable.release();
LOG.info("Execution Time: " + (System.currentTimeMillis() - start_execution));
```

## Invoke Harp-DAAL K-means via Python Interface 

Besides, the Java programming API, Harp-DAAL currently provides another Python API, which interfaces 
Harp-DAAL with other python written applications. By just adding several lines of python code, you 
are able to deploy the original python application on Hadoop Cluster and boost the performance by 
leveraging DAAL kernels. 

The python codes for image clustering is located at the path

```bash
${PYTHONPATH}/examples/scdemo/tutorial
```

### Step.1 Run Imageclustering on 15Scenery Dataset with Python Scikit-Learn K-means 

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


### Step.3 Invokes Harp-DAAL

```bash
../tutorial/run_kmeans.sh daal
```

### Step.4 Check the results of clustering

Download the result files.

```bash

TODO: need BoFeng's input here.

```

### Step.5 (Optional)Tune Harp-DAAL-Kmeans Parameters

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


















