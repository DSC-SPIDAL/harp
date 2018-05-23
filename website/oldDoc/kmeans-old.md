---
title: Harp-DAAL-Kmeans
---

## Overview 

K-means is a widely used clustering algorithm in machine learning community. It iteratively computes the distance between each 
training point to every centroids, re-assigns the training point to new cluster and re-compute the new centroid of each cluster. 

## Implementation of Harp-DAAL-Kmeans

Harp-DAAL-Kmeans is built upon Harp's original K-means parallel implementation, where it uses a [regroup](https://dsc-spidal.github.io/harp/docs/communications/regroup/)
and another [allgather](https://dsc-spidal.github.io/harp/docs/communications/allgather/) to synchronize model data, i.e. centroids, among each mapper. 
The difference between original Harp-Kmeans and Harp-DAAL-Kmeans 
is their local computation kernel. Harp-DAAL-Kmeans uses DAAL's K-means kernel, where the computation of point-centroid distance
is transformed into BLAS-level 3 matrix-matrix operations. This replacement significantly improves the computation intensity of 
the local computation, generating highly vectorized codes when compared to original Harp-Kmeans. 

## A Code Walk through of Harp-DAAL-Kmeans

At the inter-node level, Harp-DAAL-Kmeans launches a group of harp-mappers, 
The master mapper (node) will load the centroids data and broadcast it to all the each mappers. 
The mappers will then take a portion of the partitioned training data points.

```java

// Load centroids
Table<DoubleArray> cenTable =
new Table<>(0, new DoubleArrPlus());
if (this.isMaster()) {
createCenTable(cenTable, numCentroids,
numCenPars, cenVecSize);
loadCentroids(cenTable, cenVecSize, cenDir + File.separator + Constants.CENTROID_FILE_NAME, conf);
}

// Bcast centroids
bcastCentroids(cenTable, this.getMasterID());

//pointArrays are used in daal table with feature dimension to be
//vectorSize instead of cenVecSize
List<double[]> pointArrays =
KMUtil.loadPoints(fileNames, pointsPerFile,
vectorSize, conf, numThreads);

```

At next step, each mapper will create a *NumericTable* of DAAL's Java API and load Harp's data into it. 

```java

//create a daal NumericTable to hold training point data at native side
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

At the loop for iteration, each mapper will first load data from harp side to DAAL's table container. After 
the local computation by DAAL's K-means kernel, the centroids will be copied back into harp side for 
collective regroup and allgather operations. Thus, each mapper will get its local model data synchronized 
to be latest. 

```java

for (int i = 0; i < numIterations; i++) {

long t1 = System.currentTimeMillis();

/**  the codes to prepare the data for daal **/

//create the daal numeric table at native side
long tableSize_cen = totalLength_cen/nFeature_cen;
NumericTable cenTable_daal = new HomogenBMNumericTable(daal_Context, Double.class, nFeature_cen, tableSize_cen, NumericTable.AllocationFlag.DoAllocate);
double[] buffer_array_cen = new double[(int)totalLength_cen];

/** convert centroids from harp to daal table **/

//release the array into daal side cenTable
((HomogenBMNumericTable)cenTable_daal).releaseBlockOfRowsByte(0, tableSize_cen, buffer_array_cen);
kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);

//invoke the compute kernel to do the local computation
PartialResult pres = kmeansLocal.compute();

//retrieve the partial sum for centroids
double[] partialSum = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.partialSums)).getDoubleArray(); 
//retrieve the sum of counts of data points for each centroid
double[] nObservations = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.nObservations)).getDoubleArray();

/** convert centroids from daal table to list **/

// Allreduce
regroup("main", "regroup-" + i, cenTable,
new Partitioner(this.getNumWorkers()));

/**  calculate the average value in multi-threading **/

allgather("main", "allgather-" + i, cenTable);

}

```
## Running the codes

Make sure that the code is placed in the `/harp/ml/daal` directory.
Run the `harp-daal-kmeans.sh` script here to run the code.
```bash
cd $HARP_ROOT/ml/daal
./test_scripts/harp-daal-kmeans.sh
```

The details of script is [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/test_scripts/harp-daal-kmeans.sh)


