---
title: Harp-DAAL-SGD
---

## Matrix Factorization based on Stochastic Gradient Descent (MF-SGD)

Matrix Factorization based on Stochastic Gradient Descent (MF-SGD for short) is an algorithm widely used in recommender systems. 
It aims to factorize a sparse matrix into two low-rank matrices named mode W and model H as follows. 

<img src="/img/harpdaal/7-2-1.png" width="10%" height="10%"><br>

The rating Matrix V includes both training data and test data. A learning algorithm uses the training data to update matrices W and H. 
For instance, a standard SGD procedure will update the model W and H
while iterating over each training data point, i.e., an entry in matrix V in the following formula. 

<img src="/img/harpdaal/7-2-2.png" width="25%" height="25%"><br>

<img src="/img/harpdaal/7-2-3.png" width="40%" height="40%"><br>

<img src="/img/harpdaal/7-2-4.png" width="40%" height="40%"><br>

After the training process, the test data points in matrix V could be used to verify the effectiveness of the training matrix by computing the RMSE values of 
the difference 

<img src="/img/harpdaal/7-2-2.png" width="25%" height="25%"><br>

## Implementation of SGD within Harp-DAAL Framework

The main body of Harp-DAAL-SGD is the *mapCollective* function of class *SGDDaalCollectiveMapper*. 

```java
protected void mapCollective(KeyValReader reader, 
            Context context) throws IOException, InterruptedException {

            LinkedList<String> vFiles = getVFiles(reader);

            try {
                runSGD(vFiles, context.getConfiguration(), context);
            } catch (Exception e) {
                LOG.error("Fail to run SGD.", e);
            }
}

```
It first uses the function *getVFiles* to read in HDFS files, then it runs the *runSGD* to finish the iterative training process. Besides the *vFiles*, *runSGD* will also 
take in the configurations of all the parameters needed by the training and testing process. The following list includes some of the important parameters.

* r: the feature dimension of model data 
* lambda: the lambda parameter in the formula of updating model W and H 
* epsilon: the learning rate in the formula of updating model W and H 
* numIterations: the number of iterations in the training process 
* numThreads: the number of threads used in Java multi-threading programming and TBB 
* numModelSlices: the number of pipelines in model rotation 

The function *runSGD* contains several steps as follows:

### Loading Training and Testing Datasets

First invokes class *SGDUtil* to load datasets

```java

//load the train dataset
Int2ObjectOpenHashMap<VRowCol> vRowMap = SGDUtil.loadVWMap(vFilePaths, numThreads, configuration);

//load the test dataset
Int2ObjectOpenHashMap<VRowCol> testVRowMap = SGDUtil.loadTestVWMap(testFilePath, numThreads, configuration);

```

### Regrouping Datasets and Creating W matrix 

```java

//wMap is the W matrix shared by both of training and testing dataset
final Int2ObjectOpenHashMap<double[]> wMap = new Int2ObjectOpenHashMap<>();

//vWHMap contains the training points
Int2ObjectOpenHashMap<VRowCol>[] vWHMap = new Int2ObjectOpenHashMap[numRowSplits];

//testWHMap contains the testing points
Int2ObjectOpenHashMap<VRowCol>[] testWHMap = new Int2ObjectOpenHashMap[numRowSplits];

final long workerNumV = createVWHMapAndWModel(vWHMap, wMap, vRowMap, r, oneOverSqrtR, numThreads, random);
long totalNumTestV = createTestWHMap(testWHMap, wMap, testVRowMap, r, oneOverSqrtR, numThreads, random);

```
Both of training data points and testing data points are first regrouped by rows through Harp's function
between all the collectiveMappers.

```java
regroup("sgd", "regroup-vw", vSetTable, new Partitioner(this.getNumWorkers()));
```
and then they are divided into *numRowSplits* slices, and within *createVWHMapAndWModel*, 
*numThreads* of Java threads will process the data points and generate the W matrix in parallel.

### Loading Data into DAAL's Data Structure

Firstly, we load W matrix from Harp's data structure to DAAL's data structure
```java

//an index hashmap for W model data in DAAL
final Int2ObjectOpenHashMap<Integer> wMap_index = new Int2ObjectOpenHashMap<>();
//create W Model data in DAAL as a homogenNumericTable
NumericTable wMap_daal = new HomogenNumericTable(daal_Context, Double.class, r, wMap_size, NumericTable.AllocationFlag.DoAllocate);
//create the converter between Harp and DAAL
HomogenTableHarpMap<double[]> convert_wTable = new HomogenTableHarpMap<double[]>(wMap, wMap_index, wMap_daal, wMap_size, r, numThreads);
//convert wMap from Harp side to DAAL side 
convert_wTable.HarpToDaalDouble();

```
Secondly, we create the H matrix and the rotator used in the model-rotation communication paradigm.
The H matrix is divided into *numModelSlices* slices, each slice serves as a pipeline for computation and communication. 

```java

// Create H model
Table<DoubleArray>[] hTableMap = new Table[numModelSlices];
createHModel(hTableMap, numModelSlices, vWHMap, oneOverSqrtR, random);
//create the rotator
RotatorDaal<double[], DoubleArray> rotator = new RotatorDaal<>(hTableMap, r, 20, this, null, "sgd");
rotator.start();

```
Thirdly, we create an instance of computing mf_sgd within DAAL and set up the parameters

```java
//create DAAL algorithm object, using distributed version of DAAL-MF-SGD  
Distri sgdAlgorithm = new Distri(daal_Context, Double.class, Method.defaultSGD);
//first set up W Model, for all the iterations
PartialResult model_data = new PartialResult(daal_Context);
sgdAlgorithm.setPartialResult(model_data);
model_data.set(PartialResultId.presWMat, wMap_daal);

```
Finally, we pre-loading the training and testing datasets from Harp/Java side into DAAL/C++ side

```java

totalNumTestV = loadDataDaal(numWorkers, numRowSplits, rotator, vWHMap, testWHMap, hTableMap, wMap_index, taskMap_daal,train_data_wPos, train_data_hPos, train_data_val, 
                test_data_wPos, test_data_hPos, test_data_val);

```

### Starting the Iterative Training Process and the Testing Process

First, we compute the RMSE value before the training process.

```java

//computeRMSE before iteration
printRMSEbyDAAL(sgdAlgorithm, model_data,test_data_wPos,test_data_hPos,test_data_val, rotator, numWorkers, totalNumTestV, wMap_size, 0, configuration);

```

The iterative process loops over all the iterations, rotated workers, and the pipelines. 

```java
for (int i = 1; i <= numIterations; i++) {

    for (int j = 0; j < numWorkers; j++) {

        for (int k = 0; k < numModelSlices; k++) {

            //get the h matrix from the rotator
            NumericTable hTableMap_daal = rotator.getDaal_Table(k);
            model_data.set(PartialResultId.presHMat, hTableMap_daal);

            //get the pre-loaded training dataset
            NumericTable daal_task_wPos = train_data_wPos.get(slice_index); 
            NumericTable daal_task_hPos = train_data_hPos.get(slice_index); 
            NumericTable daal_task_val = train_data_val.get(slice_index); 

            //set up the datasets within the DAAL's instance
            sgdAlgorithm.input.set(InputId.dataWPos, daal_task_wPos);
            sgdAlgorithm.input.set(InputId.dataHPos, daal_task_hPos);
            sgdAlgorithm.input.set(InputId.dataVal, daal_task_val);

            //set up the parameters for MF-DAAL-SGD
            sgdAlgorithm.parameter.set(epsilon,lambda, r, wMap_size, hPartitionMapSize, 1, numThreads, 0, 1);

            //computation 
            sgdAlgorithm.compute();

            //trigger the rotator after one time of computation
            rotator.rotate(k);

        }
    }
}

```

## Task Parallel Programming

We chose a taskflow based programming model in Harp-DAAL-SGD application. Within this model, a model matrix W is initially 
released into the DAAL's data structures, which is a *HomogenNumericTable*, and stay within DAAL till the end of program's life cycle. 
The other model data, matrix H, is loaded into DAAL's data structure after every occurrence of the model rotation. 
The training model data, V, is stored in Harp's side. 
Each training point is represented by a Task object. Each $Task$ object consists of three fields:

* Position of the associated row in matrix W
* Position of the associated column in matrix H
* The value of the training data V

The taskflow is organized on the Harp side, which then delivers the tasks into DAAL's computation kernels. The DAAL sgd kernel will get the corresponding row and column from the W and H matrices stored in its 
data structure, and complete the computation and updating work. 

## TBB versus Java Multithreading

In the original Bingjing's SGD implementation, It uses raw Java threads to accomplish the tasks in parallel. 
Accordingly, it implements its own scheduler and the policy, the timer and the pipeline. In contrast, our Harp-DAAL-SGD uses Intel's Threading Building Block
(TBB) to compute the SGD tasks in parallel. TBB provides the users of many parallel algorithm templates, and we use the *parallel_for* template to achieve the computation within SGD. 

Compared to the raw Java threads based parallel computing, the use of TBB has the following benefits:

* The users only take care of the parallel tasks instead of the raw threads.
* For each TBB thread, the C++ codes have more parallelism from the optimization of compilers. 
* TBB's scheduler will probably enjoy a better load balance than that of user's own scheduler. 

Although DAAL with TBB could give us faster computation of operations such as vector inner product and matrix-vector multiplication, 
it still has some additional overhead within the Harp environment. DAAL has a different
Data structure than Harp, so we need to convert data from Harp to DAAL, from Java side to C++ side. 
If the computation time is not dominant in the total execution time, which is related to the dimension of W and H, then the 
additional overhead of using DAAL will make the codes less competitive than the original pure Java based implementation. 
Once the dimension rises, the computation becomes more intensive, and the advantages of DAAL and TBB will
appear and outperform the original implementation on Java threads. 


