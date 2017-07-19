---
title: Harp-DAAL-Mom
---

## Overview 
Moments are basic quantitative measures of data set characteristics such as location and dispersion. We compute the following low order characteristics: minimums/maximums, sums, means, sums of squares, sums of squared differences from the means, second order raw moments, variances, standard deviations, and variations.

## Implementation of Harp-DAAL-Mom
Harp-DAAL-Mom is built upon the original DAAL distributed implementation of the algorithm using the Harp interface libraries for the collective-communication among the data nodes. 
The implementation of Mom in our Harp-DAAL consists of two levels. At the top Level, using Harp’s reduce operation to communication model data among mappers. At the bottom Level, using DAAL’s  kernels to conduct local computations.

### Brief background
* There are primarily three kinds of nodes involved with the implementation.

1. name node 
2. data node
   - master node
   - slave node 

* The Java language services provided by Intel as a wrapper to their C++ code.

* The description of the intel daal's JAVA API used can be found [here](https://software.intel.com/sites/products/documentation/doclib/daal/daal-user-and-reference-guides/hh_goto.htm?index.htm#daal_java_api/group__low__order__moments.htm#details)

### Code Walk-Through 
The actual implemented code can be found [here](https://github.com/DSC-SPIDAL/harp/tree/master/harp-daal-app/src/main/java/edu/iu/daal_mom). The MapCollective function is defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/harp-daal-app/src/main/java/edu/iu/daal_mom/MOMDaalCollectiveMapper.java).
 
#### Step 1 (on data nodes)
The first step involves reading the files from the hdfs filesystem after splitting files between each mapper. Splitting is done by MultipleFileInputFormat class defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/harp-daal-app/src/main/java/edu/iu/fileformat/MultiFileInputFormat.java). 
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
List<double[]> pointArrays = MOMUtil.loadPoints(trainingDataFiles, pointsPerFile,
        vectorSize, conf, numThreads);
      ts2 = System.currentTimeMillis();
      load_time += (ts2 - ts1);

     // converting data to Numeric Table
      ts1 = System.currentTimeMillis();
      long nFeature = vectorSize;
      long nLabel = 1;
      long totalLengthFeature = 0;

      long[] array_startP_feature = new long[pointArrays.size()];
      double[][] array_data_feature = new double[pointArrays.size()][];

      for(int k=0;k<pointArrays.size();k++)
      {
       array_data_feature[k] = pointArrays.get(k);
       array_startP_feature[k] = totalLengthFeature;
       totalLengthFeature += pointArrays.get(k).length;
     }

     long featuretableSize = totalLengthFeature/nFeature;

   //initializing Numeric Table
     NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);
```
  
#### Step 2 (on data nodes)
DistributedStep1Local is created for local computation. Computed partial results are sent to master node by **reduce** function. 
```java
DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Float.class, Method.defaultDense);
    /* Set input objects for the algorithm */
    algorithm.input.set(InputId.data, featureArray_daal);

    /* Compute partial estimates on nodes */
    partialResult = algorithm.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    partialResultTable.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(partialResult)));
    boolean reduceStatus = false;
    reduceStatus = this.reduce("mom", "sync-partialresult", partialResultTable, this.getMasterID());
```

 
#### Step 3 (only on master nodes)
Final low order characteristics are computed on master node. 
 
```java
 if(this.isMaster()){
      int[] pid = partialResultTable.getPartitionIDs().toIntArray();
    DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Float.class, Method.defaultDense);
    ts1 = System.currentTimeMillis();
    for(int j = 0; j< pid.length; j++){
      try {
        algorithm.input.add(DistributedStep2MasterInputId.partialResults,
          deserializePartialResult(partialResultTable.getPartition(pid[j]).get())); 
      } catch (Exception e) 
      {  
        System.out.println("Fail to deserilize partialResultTable" + e.toString());
        e.printStackTrace();
      }
    }
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    algorithm.compute();
    result = algorithm.finalizeCompute();

    NumericTable minimum = result.get(ResultId.minimum);
    NumericTable maximum = result.get(ResultId.maximum);
    NumericTable sum = result.get(ResultId.sum);
    NumericTable sumSquares = result.get(ResultId.sumSquares);
    NumericTable sumSquaresCentered = result.get(ResultId.sumSquaresCentered);
    NumericTable mean = result.get(ResultId.mean);
    NumericTable secondOrderRawMoment = result.get(ResultId.secondOrderRawMoment);
    NumericTable variance = result.get(ResultId.variance);
    NumericTable standardDeviation = result.get(ResultId.standardDeviation);
    NumericTable variation = result.get(ResultId.variation);
    }
```


#### Setting up Hadoop and Harp-daal
Details about setting up hadoop along with harp-daal on the cluster can be found [here](https://dsc-spidal.github.io/harp/docs/getting-started-cluster/). 

#### Running the code
Make sure that the code is placed in the `/harp/harp-daal-app` directory.
Run the `harp-daal-mom-hsw.sh` script here to run the code.
```shell
#!/bin/bash

Arch=hsw

cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

source /N/u/lc37/Lib/DAAL2018_Beta/__release_lnx/daal/bin/daalvars.sh intel64

echo "${DAALROOT}"

cd ${HADOOP_HOME}

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi
# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBBROOT}/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../omp/lib/libiomp5.so /Hadoop/Libraries/

# use the path at account lc37
logDir=/N/u/lc37/HADOOP/Test_longs/logs
export LIBJARS=${DAALROOT}/lib/daal.jar

Dataset=daal_mom_dense
Mem=110000
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=8

echo "Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd Start" 
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_mom.MOMDaalLauncher -libjars ${LIBJARS}  /Hadoop/mom-input/$Dataset /mom/work $Mem $Node $Thd 2>$logDir/Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd.log 
echo "Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd End" 
```

