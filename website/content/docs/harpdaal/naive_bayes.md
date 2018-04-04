---
title: Harp-DAAL-Naive
---

## Overview 
Naïve Bayes is a set of simple and powerful classification methods often used for text classification, medical diagnosis, and other classification problems. In spite of their main assumption about independence between features, Naïve Bayes classifiers often work well when this assumption does not hold. An advantage of this method is that it requires only a small amount of training data to estimate model parameters.

## Implementation of Harp-DAAL-Naive
Harp-DAAL-Naive is built upon the original DAAL distributed implementation of the algorithm using the Harp interface libraries for the collective-communication among the data nodes. 
The implementation of Naive in our Harp-DAAL consists of two levels. At the top Level, using Harp’s reduce operation to communication model data among mappers. At the bottom Level, using DAAL’s  kernels to conduct local computations.

### Brief background
* There are primarily three kinds of nodes involved with the implementation.

1. name node 
2. data node
   - master node
   - slave node 

* The Java language services provided by Intel as a wrapper to their C++ code.

* The description of the intel daal's JAVA API used can be found [here](https://software.intel.com/sites/products/documentation/doclib/daal/daal-user-and-reference-guides/hh_goto.htm?index.htm#daal_java_api/group__multinomial__naive__bayes.htm)

### Code Walk-Through 
The actual implemented code can be found [here](https://github.com/DSC-SPIDAL/harp/tree/master/ml/daal/src/main/java/edu/iu/daal_naive). The MapCollective function is defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/src/main/java/edu/iu/daal_naive/NaiveDaalCollectiveMapper.java).
 
#### Step 1 (on data nodes)
The first step involves reading the files from the hdfs filesystem after splitting files between each mapper. Splitting is done by MultipleFileInputFormat class defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/src/main/java/edu/iu/fileformat/MultiFileInputFormat.java). 
Data is converted into array which is eventually converted into the daal [_Numeric Table_](https://software.intel.com/en-us/node/564579) datastructure.  In this example the files have been stored on the hdfs. The files have to be read in the daal table format as the local computations are performed by the daal libraries. 
Getting array from csv file - 
```java
public static List<double[]> loadPoints(String file,
int pointsPerFile, int cenVecSize,
Configuration conf) throws Exception {
  System.out.println("filename: "+file );
  List<double[]> points = new LinkedList<double[]>();
  double[] trainingData =
    new double[pointsPerFile * cenVecSize];
    double[] labelData =
    new double[pointsPerFile * 1]; 
  Path pointFilePath = new Path(file);
  FileSystem fs =
    pointFilePath.getFileSystem(conf);
  FSDataInputStream in = fs.open(pointFilePath);
  int k =0;
  try{
    for(int i = 0; i < pointsPerFile;i++){
      String[] line = in.readLine().split(",");
      for(int j = 0; j < cenVecSize; j++){
        trainingData[k] = Double.parseDouble(line[j]);
        k++;
      }
      labelData[i] = Double.parseDouble(line[cenVecSize]);
    }
  } finally{
    in.close();
    points.add(trainingData);
    points.add(labelData);
  }
return points;
}
```
Converting array to Numeric Table - 
```java
List<List<double[]>> pointArrays = NaiveUtil.loadPoints(trainingDataFiles, pointsPerFile,
                    vectorSize, conf, numThreads);
      List<double[]> featurePoints = new LinkedList<>();
      for(int i = 0; i<pointArrays.size(); i++){
          featurePoints.add(pointArrays.get(i).get(0));
      }
      List<double[]> labelPoints = new LinkedList<>();
      for(int i = 0; i<pointArrays.size(); i++){
          labelPoints.add(pointArrays.get(i).get(1));
      }

      ts2 = System.currentTimeMillis();
      load_time += (ts2 - ts1);



    // converting data to Numeric Table
      ts1 = System.currentTimeMillis();

      long nFeature = vectorSize;
      long nLabel = 1;
      long totalLengthFeature = 0;
      long totalLengthLabel = 0;

      long[] array_startP_feature = new long[pointArrays.size()];
      double[][] array_data_feature = new double[pointArrays.size()][];
      long[] array_startP_label = new long[labelPoints.size()];
      double[][] array_data_label = new double[labelPoints.size()][];

      for(int k=0;k<featurePoints.size();k++)
            {
                array_data_feature[k] = featurePoints.get(k);
                array_startP_feature[k] = totalLengthFeature;
                totalLengthFeature += featurePoints.get(k).length;
            }

      for(int k=0;k<labelPoints.size();k++)
      {
          array_data_label[k] = labelPoints.get(k);
          array_startP_label[k] = totalLengthLabel;
          totalLengthLabel += labelPoints.get(k).length;
      }

    long featuretableSize = totalLengthFeature/nFeature;
    long labeltableSize = totalLengthLabel/nLabel;

   //initializing Numeric Table


    NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);
    NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Double.class, nLabel, labeltableSize, NumericTable.AllocationFlag.DoAllocate);


    int row_idx_feature = 0;
    int row_len_feature = 0;

    for (int k=0; k<featurePoints.size(); k++) 
      {
          row_len_feature = (array_data_feature[k].length)/(int)nFeature;
          //release data from Java side to native side
          ((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, DoubleBuffer.wrap(array_data_feature[k]));
          row_idx_feature += row_len_feature;
      }

    int row_idx_label = 0;
    int row_len_label = 0;

    for (int k=0; k<labelPoints.size(); k++) 
    {
        row_len_label = (array_data_label[k].length)/(int)nLabel;
        //release data from Java side to native side
        ((HomogenNumericTable)labelArray_daal).releaseBlockOfRows(row_idx_label, row_len_label, DoubleBuffer.wrap(array_data_label[k]));
        row_idx_label += row_len_label;
    }
```
  
#### Step 2 (on data nodes)
TrainingDistributedStep1Local is created for local computation. Computed partial results are sent to master node by **reduce** function. 
```java
TrainingDistributedStep1Local algorithm = new TrainingDistributedStep1Local(localContext, Float.class,
                    TrainingMethod.defaultDense, nClasses);
    algorithm.input.set(InputId.data, featureArray_daal);
    algorithm.input.set(InputId.labels, labelArray_daal);

    TrainingPartialResult pres = algorithm.compute();
    pres.changeContext(daal_Context);
    localContext.dispose();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    partialResultTable.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(pres)));
    boolean reduceStatus = false;
    reduceStatus = this.reduce("naive", "sync-partialresult", partialResultTable, this.getMasterID());
```

 
#### Step 3 (only on master nodes)
Final result is computed on master node. 
 
```java
 if(this.isMaster()){
     TrainingDistributedStep2Master masterAlgorithm = new TrainingDistributedStep2Master(daal_Context, Float.class,
                TrainingMethod.defaultDense, nClasses);
      ts1 = System.currentTimeMillis();
      int[] pid = partialResultTable.getPartitionIDs().toIntArray();
      for(int j = 0; j< pid.length; j++){
        try {
          System.out.println("pid : "+pid[j]);
          masterAlgorithm.input.add(TrainingDistributedInputId.partialModels,
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
      masterAlgorithm.compute();
      trainingResult = masterAlgorithm.finalizeCompute();
    }
```

## Running the codes

Make sure that the code is placed in the `/harp/ml/daal` directory.
Run the `harp-daal-naive.sh` script here to run the code.

```bash
cd $HARP_ROOT/ml/daal
./test_scripts/harp-daal-naive.sh
```

The details of script is [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/test_scripts/harp-daal-naive.sh)

