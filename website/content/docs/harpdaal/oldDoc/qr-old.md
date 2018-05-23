---
title: Harp-DAAL-QR
---

<img src="/img/harpdaal/QR.png" width="80%" >

## Overview 
The QR decomposition or QR factorization of a matrix is a decomposition of the matrix into an orthogonal matrix and a triangular matrix. A QR decomposition of a real square matrix A is a decomposition of A as
A = QR,
where Q is an orthogonal matrix (its columns are orthogonal unit vectors meaning Q<sup>T</sup>Q = I) and R is an upper triangular matrix (also called right triangular matrix).

## Implementation of Harp-DAAL-QR
Harp-DAAL-QR is built upon the original DAAL distributed implementation of the algorithm using the Harp interface libraries for the collective-communication among the data nodes. 
The implementation of QR in our Harp-DAAL consists of two levels. At the top Level, using Harp’s broadcast and reduce operation to communication model data among mappers. At the bottom Level, using DAAL’s ALS kernels to conduct local computations.

### Brief background
* There are primarily three kinds of nodes involved with the implementation.

    1. name node 
    2. data node
   - master node
   - slave node 

* The Java language services provided by Intel as a wrapper to their C++ code.

* The description of the intel daal's JAVA API used can be found [here](https://software.intel.com/sites/products/documentation/doclib/daal/daal-user-and-reference-guides/daal_java_api/group__qr__without__pivoting.htm)

### Code Walk-Through 
The actual implemented code can be found [here](https://github.com/DSC-SPIDAL/harp/tree/master/ml/daal/src/main/java/edu/iu/daal_qr). The MapCollective function is defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/src/main/java/edu/iu/daal_qr/QRDaalCollectiveMapper.java).

#### Step 1 (on data nodes)
The first step involves reading the files from the hdfs filesystem after splitting files between each mapper. Splitting is done by MultipleFileInputFormat class defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/src/main/java/edu/iu/fileformat/MultiFileInputFormat.java). 
Data is converted into array which is eventually converted into the daal [_Numeric Table_](https://software.intel.com/en-us/node/564579) datastructure.  In this example the files have been stored on the hdfs. The files have to be read in the daal table format as the local computations are performed by the daal libraries. 
Getting array from csv file - 

```java
public static double[] loadPoints(String file,
int pointsPerFile, int cenVecSize,
Configuration conf) throws Exception {
double[] points = new double[pointsPerFile * cenVecSize];
Path pointFilePath = new Path(file);
FileSystem fs =
pointFilePath.getFileSystem(conf);
FSDataInputStream in = fs.open(pointFilePath);
int k =0;
try{
for(int i = 0; i < pointsPerFile;i++){
String[] line = in.readLine().split(",");
for(int j = 0; j < cenVecSize; j++){
points[k] = Double.parseDouble(line[j]);
k++;
}
}
} finally{
in.close();
}
return points;
}
```

Converting array to Numeric Table - 

```java
NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);

int row_idx_feature = 0;
int row_len_feature = 0;

for (int k=0; k<pointArrays.size(); k++) 
{
row_len_feature = (array_data_feature[k].length)/(int)nFeature;
//release data from Java side to native side
((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, DoubleBuffer.wrap(array_data_feature[k]));
row_idx_feature += row_len_feature;
}
```

#### Step 2 (on data nodes)

DistributedStep1Local is created for local computation. Computed partial results are sent to master node by **reduce** function. 

```java
qrStep1Local = new DistributedStep1Local(daal_Context, Float.class, Method.defaultDense);
qrStep1Local.input.set(InputId.data, featureArray_daal);
DistributedStep1LocalPartialResult pres = qrStep1Local.compute();
dataFromStep1ForStep2 = pres.get(PartialResultId.outputOfStep1ForStep2);
dataFromStep1ForStep3 = pres.get(PartialResultId.outputOfStep1ForStep3);

ts1 = System.currentTimeMillis();

Table<ByteArray> partialStep12 = new Table<>(0, new ByteArrPlus());
partialStep12.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(dataFromStep1ForStep2)));
System.out.println("number of partition in partialresult before reduce :" + partialStep12.getNumPartitions());
boolean reduceStatus = false;
reduceStatus = this.reduce("nn", "sync-partialresult", partialStep12, this.getMasterID()); 	
```


#### Step 3 (only on master nodes)
The partial results are computed on master node. R matrix is obtained and computed partial results are sent to master node for computation of Q matrix by **broadcast** function.

```java
if(this.isMaster())
{
qrStep2Master = new DistributedStep2Master(daal_Context, Float.class, Method.defaultDense);

System.out.println("this is a master node");
int[] pid = partialStep12.getPartitionIDs().toIntArray();

ts1 = System.currentTimeMillis();
for(int j = 0; j< pid.length; j++){
try {
System.out.println("pid : "+pid[j]);
qrStep2Master.input.add(DistributedStep2MasterInputId.inputOfStep2FromStep1, pid[j],
deserializePartialResult(partialStep12.getPartition(pid[j]).get())); 
} catch (Exception e) 
{  
System.out.println("Fail to deserilize partialResultTable" + e.toString());
e.printStackTrace();
}
}

ts2 = System.currentTimeMillis();
comm_time += (ts2 - ts1);

ts1 = System.currentTimeMillis();
DistributedStep2MasterPartialResult presStep2 = qrStep2Master.compute();

inputForStep3FromStep2 = presStep2.get(DistributedPartialResultCollectionId.outputOfStep2ForStep3);

for(int j = 0; j< pid.length; j++){
partialStep32.addPartition(new Partition<>(j,serializePartialResult
((DataCollection)inputForStep3FromStep2.get(j))));
}

Result result = qrStep2Master.finalizeCompute();
R = result.get(ResultId.matrixR);

ts2 = System.currentTimeMillis();
compute_time += (ts2 - ts1);
}
boolean isSuccess = broadcast("main","broadcast-partialStep32", partialStep32, 0,false);
```

#### Step 4 (on data node)

Q matrix is obtained on data nodes.

```java
qrStep3Local = new DistributedStep3Local(daal_Context, Float.class, Method.defaultDense);
qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep1,dataFromStep1ForStep3); 

ts1 = System.currentTimeMillis();

try{
qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep2, 
deserializePartialResult(partialStep32.getPartition(this.getSelfID()).get()));
} catch (Exception e) 
{  
System.out.println("Fail to deserilize partialResultTable" + e.toString());
e.printStackTrace();
}

ts2 = System.currentTimeMillis();
comm_time += (ts2 - ts1);

ts1 = System.currentTimeMillis();
qrStep3Local.compute();
Result result = qrStep3Local.finalizeCompute();

ts2 = System.currentTimeMillis();
compute_time += (ts2 - ts1);

Qi = result.get(ResultId.matrixQ);
System.out.println("number of rows" + Qi.getNumberOfRows());
System.out.println("number of columns" + Qi.getNumberOfColumns());

Table<ByteArray> resultNT = new Table<>(0, new ByteArrPlus());

Service.printNumericTable("Orthogonal matrix Q (10 first vectors):", Qi, 10);
if(this.isMaster()){
Service.printNumericTable("Triangular matrix R:", R);
}
```

## Running the codes

Make sure that the code is placed in the `/harp/ml/daal` directory.
Run the `harp-daal-qr.sh` script here to run the code.

```bash
cd $HARP_ROOT/ml/daal
./test_scripts/harp-daal-qr.sh
```

The details of script is [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/test_scripts/harp-daal-qr.sh)

