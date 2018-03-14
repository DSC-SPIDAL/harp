---
title: Harp-DAAL-NN
---

<img src="/img/nn.png" width="60%"  >

## Overview 
Neural Networks are a beautiful biologically-inspired programming paradigm which enable a computer to learn from observational data.
The motivation for the development of neural network technology stemmed from the desire to develop an artificial system that could perform "intelligent" tasks similar to those performed by the human brain.
Neural networks, with their remarkable ability to derive meaning from complicated or imprecise data, can be used to extract patterns and detect trends that are too complex to be noticed by either humans or other computer techniques.

## Implementation of Harp-DAAL-NN
Harp-DAAL-NN is built upon the original DAAL distributed implementation of the algorithm using the Harp interface libraries for the collective communication among the data nodes. The two-step implementation is similar to the Map-Reduce Model of Hadoop.
Here are the steps to implement and run the Neural Network code. 

### Brief background
* There are primarily three kinds of nodes involved with the implementation.
  1. name node 
  1. data node (divided into master and slave nodes, where slave nodes compute in parallel and communicate results to the master node)
  1. login node
  
* The data files (csv [sample](http://https://github.com/01org/daal/tree/daal_2018_beta_update1/examples/data/distributed)) which is data tagged with `neural_network_train` can be tested the following ways:
  1. On the shared location
  1. On each datanode individually
  1. Hadoop Filesystem Format (HDFS) [Tutorial](https://www.tutorialspoint.com/hadoop/hadoop_hdfs_overview.htm "hdfs tutorial")

* Note: The CSV files of training data each are of dimension 1500*21. Specifically, each contains 1500 training samples, and the vector size of each training sample is 20. Finally, the last column contains label (0/1) of each data sample.
   
* The description of the intel daal's algorithm kernel used can be found [here](https://software.intel.com/sites/products/documentation/doclib/daal/daal-user-and-reference-guides/daal_prog_guide/GUID-E8F6E40F-60EC-422B-8D46-492E110BB0BD.htm)

* The Java language services provided by Intel as a wrapper to their C++ code.

### Code Walk-Through 
The MapCollective function is defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/core/harp-hadoop/src/main/java/org/apache/hadoop/mapred/CollectiveMapper.java).
 
#### Step 1 (on slave nodes)
The first step involves reading the files from the HDFS filesystem after splitting files between each mapper. Splitting is done by MultiFileInputFormat class defined [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/src/main/java/edu/iu/fileformat/MultiFileInputFormat.java). 
Data is converted into an array which is eventually converted into the DAAL [_Numeric Table_](https://software.intel.com/en-us/node/564579) data structure.  In this example the files have been stored on HDFS. The files have to be read in the DAAL table format as the local computations are performed by the DAAL libraries.</br>
Getting Array From CSV File (below)
-----------------------------
```java
public static List<double[]> loadPoints(String file, int pointsPerFile, int cenVecSize, Configuration conf) throws Exception {
  List<double[]> points = new LinkedList<double[]>();
  double[] trainingData = new double[pointsPerFile * cenVecSize];
  double[] labelData = new double[pointsPerFile * 1]; 
  Path pointFilePath = new Path(file);
  FileSystem fs =pointFilePath.getFileSystem(conf);
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
  } finally {
    in.close();
    points.add(trainingData);
    points.add(labelData);
  }
  return points;
} 
```
Converting Array to Numeric Table (below)
-----------------------------------
```java
//initializing Numeric Table
NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);
NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Double.class, nLabel, labeltableSize, NumericTable.AllocationFlag.DoAllocate);

int row_idx_feature = 0;
int row_len_feature = 0;

for (int k=0; k<featurePoints.size(); k++) {
  row_len_feature =(array_data_feature[k].length)/(int)nFeature;
  //release data from Java side to native side
  ((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, Doub  leBuffer.wrap(array_data_feature[k]));
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
  
#### Step 2 (on slave node)
Basic input parameters such as optimization solver(sgd) training model are obtained for the initialization of the algorithm on local nodes. The Neural Network algorithm is created and its input is set as the Numeric Table created in the previous step. 
```java
double[] learningRateArray = new double[1];
learningRateArray[0] = 0.001;
com.intel.daal.algorithms.optimization_solver.sgd.Batch sgdAlgorithm = new com.intel.daal.algorithms.optimization_solver.sgd.Batch(daal_Context, Double.class, com.intel.daal.algorithms.optimization_solver.sgd.Method.defaultDense);
sgdAlgorithm.parameter.setBatchSize(batchSizeLocal);
sgdAlgorithm.parameter.setLearningRateSequence(new HomogenNumericTable(daal_Context, learningRateArray, 1, 1));

net = new DistributedStep2Master(daal_Context, sgdAlgorithm);
TrainingTopology topology = NeuralNetConfiguratorDistr.configureNet(daal_Context);

net.parameter.setOptimizationSolver(sgdAlgorithm);
net.initialize(featureTensorInit.getDimensions(), topology);

TrainingModel trainingModel = net.getResult().get(TrainingResultId.model);

```
Computation is done iteratively for each batch, and the input data is obtained from Numeric Table using the Service class function.
Results are computed on slave nodes (as below)
----------------------------------------------
```java
DistributedStep1Local netLocal = new DistributedStep1Local(daal_Context, sgdAlgorithm);
netLocal.input.set(DistributedStep1LocalInputId.inputModel, trainingModel);
netLocal.input.set(TrainingInputId.data, Service.getNextSubtensor(daal_Context, featureTensorInit, i, batchSizeLocal));
netLocal.input.set(TrainingInputId.groundTruth, Service.getNextSubtensor(daal_Context, labelTensorInit, i, batchSizeLocal));
netLocal.parameter.setOptimizationSolver(sgdAlgorithm);
PartialResult partialResult = netLocal.compute();	
```

 
#### Step 3 (communication)
The partial results are sent to the master node for final computations using [reduce](https://dsc-spidal.github.io/harp/docs/communications/reduce/ "harp reduce").
 
```java
/*Do an reduce to send all the data to the master node*/
Table<ByteArray> partialResultTable = this.reduce("nn", "sync-partialresult", partialResultTable, this.getMasterID());
```
 
#### Step 4 (on master node)
The master node obtains the final result aggregated from the slave nodes (below)
```java
//adding partial result in master algorithm
for(int j = 0; j< pid.length; j++){
  net.input.add(DistributedStep2MasterInputId.partialResults, 0, deserializePartialResult(partialResultTable.getPartition(pid[j]).get()));
}
DistributedPartialResult result = net.compute();
wbHarpTable.addPartition(new Partition<(this.getMasterID(),  serializeNumericTable(result.get(DistributedPartialResultId.resultFromMaster).get(TrainingResultId.model).getWeightsAndBiases())));
NumericTable wbMaster = result.get(DistributedPartialResultId.resultFromMaster).get(TrainingResultId.model).getWeightsAndBiases();

``` 

#### Step 5 (Results)
TrainingResult  can be printed using the Service class function.
```java
TrainingResult result = net.finalizeCompute();
TrainingModel finalTrainingModel = result.get(TrainingResultId.model);
PredictionModel predictionModel = trainingModel.getPredictionModel(Float.class);
Tensor predictionData = getTensor(daal_Context, conf, testFilePath, vectorSize, 2000);
Tensor predictionGroundTruth = getTensor(daal_Context, conf, "/nn/test/neural_network_test_ground_truth.csv", 1, 2000);
PredictionBatch net = new PredictionBatch(daal_Context);
long[] predictionDimensions = predictionData.getDimensions();
net.parameter.setBatchSize(predictionDimensions[0]);
// setting the input data from test file 
net.input.set(PredictionTensorInputId.data, predictionData);
//setting the prediction model
net.input.set(PredictionModelInputId.model, predictionModel);
predictionResult = net.compute();
//printing the results
Service.printTensors("Ground truth", "Neural network predictions: each class probability",
"Neural network classification results (first 50 observations):",predictionGroundTruth, predictionResult.get(PredictionResultId.prediction), 50);
```

### Executing the code 

#### Setting up Hadoop and Harp-daal
------------------------------------
Details about setting up Hadoop along with Harp-DAAL on the cluster can be found [here](https://dsc-spidal.github.io/harp/docs/getting-started-cluster/ "Harp Cluster Installation") and [here](https://dsc-spidal.github.io/harp/docs/harpdaal/harpdaal/ "DAAL Installation"). 

#### Running the code
----------------------
Make sure that the code is placed in the `/harp/ml/daal` directory.
Run the `harp-daal-nn-run.sh` script here to run the code.
```shell
cd $HARP_ROOT/ml/daal
./harp-daal-nn.sh  
```
Details of the script can be seen below: 
```shell
#!/bin/bash

# enter the directory of hadoop and copy your_harp_daal.jar file here
cp $HARP_ROOT_DIR/ml/daal/target/harp-daal-0.1.0.jar ${HADOOP_HOME}
# set up daal environment
#source ./__release_tango_lnx/daal/bin/daalvars.sh intel64
echo "${DAALROOT}"

cd ${HADOOP_HOME}

# check that safemode is not enabled 
hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0"  ]]; then
    hdfs dfsadmin -safemode leave
fi

# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBB_ROOT}/lib/intel64_lin_mic/libtbb* /Hadoop/Libraries/
hdfs dfs -put /N/u/mayank/daal/omp/lib/libiomp5.so /Hadoop/Libraries/

# daal.jar will be used in command line
export LIBJARS=${DAALROOT}/lib/daal.jar

# num of training data points
#Pts=50000
# num of training data centroids
#Ced=1000
# feature vector dimension
#Dim=100
# file per mapper
#File=5
# iteration times
#ITR=10
# memory allocated to each mapper (MB)
#Mem=185000
# generate training data or not (once generated, data file /kmeans-P$Pts-C$Ced-D$Dim-N$Node is in hdfs, you could reuse them next time)
#GenData=false
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=64

hadoop jar harp-daal-0.1.0.jar edu.iu.daal_nn.NNDaalLauncher -libjars ${LIBJARS}  /nn/input /nn/work $Node $Thd 
```
