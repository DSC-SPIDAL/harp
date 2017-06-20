---
title: Harp-DAAL-PCA
---

## Overview 
Principle Component Analysis (PCA) is a widely used statistical procedure that uses an orthogonal transformation to convert a set of observations of possibly correlated variables into a set of values of linearly uncorrelated variables called principal components (or sometimes, principal modes of variation). 
PCA can be done by two methods:

* Eigenvalue decomposition of a data covariance (or cross-correlation) 
* Singular Value Decomposition of a data 

It is usually performed after normalizing (centering by the mean) the data matrix for each attribute.

## Implementation of Harp-DAAL-PCA
Harp-DAAL-PCA is built upon the original DAAL distributed implementation of the algorithm using the Harp interface libraries for the collective-communication among the data nodes. The two step implementation is similar to the Map-Reduce Model of the Hadoop.
Here are the steps to implement and run the PCA code. 

### Brief background
* There are primarily three kind of nodes involved with the implementation.
  1. name node
  1. data node (divided into master and slave nodes, where slave nodes compute in parallel and communicate results to the master node)
  1. login node
  
* The data files (csv [sample](https://github.com/DSC-SPIDAL/harp/tree/master/harp-daal-app/daal-src/examples/data/distributed "sample data")) which is data tagged with `pca` can be tested the following ways:
  1. On the shared location
  1. On each datanode individually
  1. Hadoop Filesystem Format (HDFS) [Tutorial](https://www.tutorialspoint.com/hadoop/hadoop_hdfs_overview.htm "hdfs tutorial")   

* The description of the Intel DAAL algorithm used can be found [here](https://software.intel.com/en-us/daal-programming-guide "Intel implementation")

* Harp's collective communication has been described [here](https://dsc-spidal.github.io/harp/docs/programming/overview/ "Collective Communication")

* The [Java language](https://github.com/DSC-SPIDAL/harp/tree/master/harp-daal-app/daal-src/lang_interface/java/com/intel/daal "language interface") services provided by intel as a wrapper to their C++ code.

### Code Walk-Through 
Only the MapCollective function is explained [here](https://github.com/DSC-SPIDAL/harp/blob/master/harp-project/src/main/java/org/apache/hadoop/mapred/CollectiveMapper.java "Collective Mapper") as the rest of the code follows the same Harp-DAAL style.

#### Step 1 (on slave nodes)
The first step involves reading the files from the filesystem into the DAAL [_NumericTable_](https://software.intel.com/en-us/node/564579 "Numeric Table") data structure.  The files have to be read in the DAAL table format as the local computations are performed by the DAAL libraries. Note that for the first step, all the nodes act as slave nodes and the master node comes into the picture only for the second step. 

Reading the files could be done in following two ways:

------------------------------------------------------------
1) The files have been stored on the shared memory system

```java
FileDataSource dataSource = new FileDataSource(daal_Context, "/pca_"+this.getSelfID()+".csv",DataSource.DictionaryCreationFlag.DoDictionaryFromContext,DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);

/* Retrieve the data from the input */ dataSource.loadDataBlock();

/* Set the input data on local nodes */
NumericTable pointsArray_daal = dataSource.getNumericTable();
```
-----------------------------------------------------------------
2) The files have been randomly generated and stored on HDFS, which are then read by the `PCAUtil` as a list of double arrays afterwards being converted to the DAAL [_NumericTable_](https://software.intel.com/en-us/node/564579 "Numeric Table") data structure.
```java
List<double[]> pointArrays = PCAUtil.loadPoints(fileNames, pointsPerFile, vectorSize, conf, numThreads);

//create the daal table for pointsArrays
long nFeature = vectorSize;
long totalLength = 0;

long[] array_startP = new long[pointArrays.size()];
double[][] array_data = new double[pointArrays.size()][];

for(int k=0;k<pointArrays.size();k++)
{
  array_data[k] = pointArrays.get(k);
  array_startP[k] = totalLength;
  totalLength += pointArrays.get(k).length;
}

long tableSize = totalLength/nFeature;
NumericTable pointsArray_daal = new HomogenBMNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);

int row_idx = 0;
int row_len = 0;
for (int k=0; k<pointArrays.size(); k++)
{
  row_len = (array_data[k].length)/(int)nFeature;
  //release data from Java side to native side
  ((HomogenBMNumericTable)pointsArray_daal).releaseBlockOfRowsByte(row_idx, row_len, array_data[k]);
  row_idx += row_len;
}
```
  
#### Step 2 (on slave node)
The PCA algorithm is created and its input is set as the Numeric Table created in the previous step.  The computation gives the partial results on each local node.
```java 
/* Create an algorithm to compute PCA using the correlation method on local nodes */
DistributedStep1Local pcaLocal = new DistributedStep1Local(daal_Context, Double.class, Method.correlationDense);

/* Set the input data on local nodes */
pcaLocal.input.set(InputId.data, pointsArray_daal);

/*Compute the partial results on the local data nodes*/
pres =(PartialCorrelationResult)pcaLocal.compute();

```
 
#### Step 3 (communication)
The partial results on each local node are sent to the master node for final computations using [allreduce](https://dsc-spidal.github.io/harp/docs/communications/allreduce/ "harp all reduce").
 
```java
/*Do an reduce to send all the data to the master node*/
Table<ByteArray> step1LocalResult_table = communicate(pres);  
```
 
#### Step 4 (on master node)
The master node collects the partial results from each slave into the `step1LocalResult_table` table which has as many partitions as the number of slaves. Each partition id also denotes the slave id.  The corresponding partition's  `PartialCorrelationResult` data is extracted and added to the list of inputs to the final `DistributedStep2Master` algorithm which is to be executed on the master node. The `compute()` followed by `finalizeCompute()` methods will calculate the eigenvectors and eigenvalues. 
```java
DistributedStep2Master pcaMaster = new DistributedStep2Master(daal_Context, Double.class, Method.correlationDense);
for (int i = 0; i < this.getNumWorkers(); i++)
{
   /*get the partial results from the local nodes and deserialize*/
   PartialCorrelationResult step1LocalResultNew = deserializeStep1Result(step1LocalResult_table.getPartition(i).get().get());

   /*add the partial results from the loacl nodes to the master node input*/
   pcaMaster.input.add(MasterInputId.partialResults, step1LocalResultNew);
}
/*compute the results on the master node*/
pcaMaster.compute();
/*get the results from master node*/
Result res = pcaMaster.finalizeCompute();

``` 

#### Step 5 (Results)
The Results calculated in the previous steps can be printed using the Service class function.
```java
NumericTable eigenValues = res.get(ResultId.eigenValues);
NumericTable eigenVectors = res.get(ResultId.eigenVectors);

/*printing the results*/
Service.printNumericTable("Eigenvalues:", eigenValues);
Service.printNumericTable("Eigenvectors:", eigenVectors);

```

### Executing the code 

#### Setting up Hadoop and Harp-DAAL
Details about setting up Hadoop along with Harp on the cluster can be found [here](https://dsc-spidal.github.io/harp/docs/getting-started-cluster/ "Installation"). 
Furthermore DAAL installation and usage can be found [here](https://dsc-spidal.github.io/harp/docs/harpdaal/harpdaal/ "Daal usage").

#### Running the code
Run the `harp-daal-pca.sh` script to run the code.
```shell
cd $HARP_ROOT_DIR/harp-daal-app
./harp-daal-pca.sh  
```
To edit the location of the data set file location edit the following line in the  shell script file

```shell
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_pca.PCADaalLauncher -libjars ${LIBJARS} $Pts $Dim $File $Node $Thd $Mem /pca-P$Pts-D$Dim-N$Node /tmp/pca $GenData
```
Where each variable is defined as below

* Pts: Number of training data points (i.e. 1000)
* Dim: Feature vector dimension (i.e. 10)
* File: File per mapper (i.e. 1)
* Mem: Memory allocated to each mapper in MB (i.e. 185000)
* GenData: Generate training data or not(once generated, data file /PCA-P\$Pts-C\$Dim-N\$Node is in hdfs, you could reuse the training data here next time) (i.e. false)
* Node: Number of mappers or nodes (i.e. 2)
* Thd: Number of threads on each mapper or node (i.e. 64)
* Where /pca-P$Pts-D$Dim-N$Node is the location holding the input files
* Where /tmp/pca is the working directory