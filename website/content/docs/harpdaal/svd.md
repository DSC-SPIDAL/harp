# Harp - DAAL - Singular Value Decomposition

## Overview

Singular Value Decomposition is a method which seeks to reduce the rank of a data matrix, thus finding the unique vectors, features, or characteristics of the data matrix at hand. This algorithm has been used in, but is not limited to signal processing, weather prediction, hotspot detection, and recommender systems.

The basic idea is to decompose the data matrix M into the following components:

<img src="/img/svd/svdpng.png" width="25%" height="25%"><br>

Where

<img src="/img/svd/sigma.png" width="3%" height="3%"><br>

Is a diagonal matrix holding the singular values of M.

While,

<img src="/img/svd/u.png" width="4%" height="4%"><br>

and

<img src="/img/svd/vt.png" width="5%" height="5%"><br>

are orthogonal matrices which diagonalize the matrix M in some way.

This package supports the implementation of Singular Value Decomposition under the Harp environment. It leverages
Intel's DAAL computation routines to compute decomposition in a highly efficient manner and provides a wrapper
for Harp integration which uses collective communication to further optimize the algorithm.

## Getting Started

### Setting up Hadoop and Harp-daal
Details about setting up Hadoop along with Harp on the cluster can be found [here](https://dsc-spidal.github.io/harp/docs/getting-started-cluster/ "Installation"). 
Furthermore DAAL installation and usage can be found [here](https://dsc-spidal.github.io/harp/docs/harpdaal/harpdaal/ "Daal usage").

The following commands and information are useful in understanding and writing Harp-DAAL-SVD examples. 

### How to run Harp - DAAL - SVD

Easiest way to run the Harp-DAAL-SVD example is through the command line input shown below as an example (all together as one command)

`hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_svd.SVDDaalLauncher -libjars ${LIBJARS} 10000 20 1 2 64 185000 /kmeans-P$Pts-C$Ced-D$Dim-N$Node /tmp/kmeans true`

### Explanation of the arguments required by Harp-DAAL in the example input

* 10000 --- Number of training data points
* 20 --- Dimension of feature vector
* 1 --- Files per mapper
* 2 --- Number of nodes (mappers)
* 64 --- Number of threads on each mapper (node)
* 185000 --- Memory allocated to each mapper (in MB)
* /kmeans-P$Pts-C$Ced-D$Dim-N$Node --- workDir
* /tmp/kmeans --- outDir
* true --- Boolean specifying to generate data or not

## Overview of the code for adding routines and debugging 

A step by step introduction of main code fragments to help understand the data flow and 
code structure. 

### Main functions in SVDDaalLauncher.java
-------------------------------------------

```java
public int run(String[] args);
```
Takes and checks input given from the commandline/shell script. Initializes values like number of data points,
number of mappers, threads per mapper, memory allocated to each mapper, etcetera.

```java
private void launch(int numOfDataPoints, int vectorSize, int numPointFiles, int numMapTasks, int numThreads, int mem, String workDir, String localPointFilesDir, boolean generateData);
```
Configures and launches Harp-DAAL-SVD jobs, generating data if required. 

### Main functions in SVDDaalCollectiveMapper.java
--------------------------------------------------

```java
protected void mapCollective(KeyValReader reader, Context context);
```
Assigns the reader to different nodes.

```java
private void runSVD(List<String> fileNames, Configuration conf, Context context);
```

This function should be visualized in three parts. In the first part it receives data and converts it into a DAAL table. It then calculuates svdStep1Local on each slave node which is the step 1 of distributed SVD algorithm.

It then uses allgather to communicate data from step 1 for step 2 to be done on the master node.

```java
this.allgather("svd", "sync-partial-res", step1LocalResultForStep2_table);
```

If this function is run on the master node, it receives data from each of the local nodes and computes inputForStep3FromStep2.
This is communicated to each of the local nodes using allgather.

Finally, the function calculates svdStep3Local on each of the local nodes which completes the calculation of the two orthogonal matrices and extrapolates singular values as part of Singular Value Decomposition

The results are printed to standard output

## Step 1, 2, and 3 references

Step 1 on local nodes

<img src="/img/svd/step1.png" width="100%" height="100%"><br>

Step 2 on master node

<img src="/img/svd/step2.png" width="100%" height="100%"><br>

Step 3 on local nodes

<img src="/img/svd/step3.png" width="100%" height="100%"><br>

### Some nuances of the code.
Serialisation of DAAL tables into HARP tables for collective communication and the deserialization 
back to DAAL tables is done with the helper functions written in SVDDaalCollectiveMapper.java

* S --- NumericTable containing singular values
* U --- NumericTable containing left orthogonal matrix
* V --- NumericTable containing right orthogonal matrix  

### Debugging and testing

Code is to be run on namenode and the output is generated on datanodes. 
To check the std output, stderr and syslog, go to a datanode and browse to the log files 
from the appropriate folder specified in core-site.xml specified during the Harp configuration.

Log files are contained in latest application-xxxxx-xxx folder for the latest run and in 
container-xx-xx folder inside it. The log files are distributed across different datanodes, to check each of them you have to visit different datanodes and repeat the process described
above.

