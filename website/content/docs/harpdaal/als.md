---
title: Harp-DAAL-ALS
---

## Alternating least squares in Collaborative Filtering

Alternating least squares (ALS) is another frequently used algorithm to decompose rating matrices in recommender systems. 
Like MF-SGD, ALS is training the model data X and Y to minimize the cost function as below

<img src="/img/als-training-costfunction.png" width="50%" height="50%"><br>
<img src="/img/als-training-costfunction-2.png" width="30%" height="30%"><br>

where 

* c_ui measures the confidence in observing p_ui
* alpha is the rate of confidence
* r_ui is the element of the matrix R
* labmda is the parameter of the regularization
* n_xu, m_yi denote the number of ratings of user u and item i respectively.

Unlike MF-SGD, ALS alternatively computes model x and y independently of each other in the following formula:

<img src="/img/als-x-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-x-compute-2.png" width="50%" height="50%"><br>
<img src="/img/als-y-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-y-compute-2.png" width="50%" height="50%"><br>

The algorithm has a computation complexity for each iteration as 

<img src="/img/als-complexity-1.png" width="30%" height="30%"><br>

Omega is the set of training samples, K is the feature dimension, m is the row number of the rating
matrix, and n is the column number of the rating matrix. 

## Implementation 

The implementation of ALS in our Harp-DAAL consists of two levels.
At the top Level, using Harp's *regroup* and *allgather* operation to communication model data among mappers
At the bottom Level, using DAAL's ALS kernels to conduct local computations. 

## A Code Walk Through of Harp-DAAL-ALS

### Load Training Data

Harp-DAAL-ALS uses both of the original training matrix and the transposed matrix, therefore we load the training matrix data twice. In the 
second load process, we exchanged the row and column indices of each training point to make a transposed matrix.

```java

Int2ObjectOpenHashMap<VRowCol> trainDataMap = SGDUtil.loadVMapRow(vFilePaths, numThreads_harp, configuration);
Int2ObjectOpenHashMap<VRowCol> trainDataMapTran = SGDUtil.loadVMapTran(vFilePaths, numThreads_harp, configuration);

```

### Format Conversion 

The default Sparse Matrix format of Harp is Coordinate Format (COO), which represents a training point in triple values (RowID, ColID, val), whereas, the 
sparse matrix format for DAAL ALS kernel is Compressed Sparse Row Format (CSR). Therefore, the first step is to convert incoming training data from COO to 
CSR. Before converting sparse data format, a remapping process of row/column indices are required to make sure that no empty row or column will occur
in the CSR format.

```java

//remapping row ids
ReMapRowColID remapper_row = new ReMapRowColID(rowIds, this.getSelfID(), this.getNumWorkers(), this);
int[] row_mapping = remapper_row.getRemapping();

//remapping col ids
ReMapRowColID remapper_col = new ReMapRowColID(colIds, this.getSelfID(), this.getNumWorkers(), this);
int[] col_mapping = remapper_col.getRemapping();

```

We then do a data format conversion by using a conversion class named *COOToCSR*

```java

COOToCSR converter = new COOToCSR(trainDataTable, col_mapping);
CSRNumericTable trainDaalTable = converter.convert();

COOToCSR converter_tran = new COOToCSR(trainDataTableTran, row_mapping);
CSRNumericTable trainDaalTableTran = converter_tran.convert();

```

### Initialize DAAL Variables 

The DAAL ALS kernel has the following important variables. Some of them are local to each mapper (node), while others require 
a global synchronization among different mapper (nodes).

```java

//store the offset of rows/columns i 
long[] usersPartition = new long[workerNum + 1];
long[] itemsPartition = new long[workerNum + 1];

//local var
KeyValueDataCollection usersOutBlocks;
//local var
KeyValueDataCollection itemsOutBlocks;

//local var, sync on master node
DistributedPartialResultStep1 step1LocalResult;
//global var
NumericTable step2MasterResult = null;
//local var
KeyValueDataCollection step3LocalResult;
//global vars
KeyValueDataCollection step4LocalInput;
//local vars
DistributedPartialResultStep4 itemsPartialResultLocal = null;
//local vars
DistributedPartialResultStep4 usersPartialResultLocal = null;

```

*usersPartition* (*itemsPartition*) record the start position of rows of distributed CSR formatted training matrix (transposed training matrix) on each mapper. 
*usersOutBlocks* (*itemsOutBlocks*) store the row/column indices that would be required by other mappers.
*step1LocalResult*, *step2MasterResult*, *step3LocalResult*, and *step4LocalInput* are intermediate results of each training step. 
*itemsPartialResultLocal* and *usersPartialResultLocal* store the final model data (low rank matrix x and y).

The following codes initialize these variables

```java

InitDistributed initAlgorithm = new InitDistributed(daal_Context, Double.class, InitMethod.fastCSR);
initAlgorithm.parameter.setFullNUsers(this.maxRowID + 1);
initAlgorithm.parameter.setNFactors(r);
initAlgorithm.parameter.setNumThreads(numThreads);
initAlgorithm.parameter.setSeed(initAlgorithm.parameter.getSeed() + this.getSelfID());
initAlgorithm.input.set(InitInputId.data, trainDaalTableTran);

// Initialize the implicit ALS model
InitPartialResult initPartialResult = initAlgorithm.compute();

// partialModel is local on each node
PartialModel partialModel = initPartialResult.get(InitPartialResultId.partialModel);
itemsPartialResultLocal = new DistributedPartialResultStep4(daal_Context);

//store the partialModel on local slave node
itemsPartialResultLocal.set(DistributedPartialResultStep4Id.outputOfStep4ForStep1, partialModel);
step4LocalInput = new KeyValueDataCollection(daal_Context);

long dataTableRows = trainDaalTable.getNumberOfRows();
long dataTableTranRows = trainDaalTableTran.getNumberOfRows();

//allreduce to get the users and items partition table
Table<LongArray> dataTable_partition = new Table<>(0, new LongArrPlus());

LongArray partition_array = LongArray.create(2, false);
partition_array.get()[0] = dataTableRows;
partition_array.get()[1] = dataTableTranRows;

dataTable_partition.addPartition(new Partition<>(this.getSelfID(), partition_array));

this.allgather("als", "get-partition-info", dataTable_partition);

usersPartition[0] = 0;
itemsPartition[0] = 0;

for (int j=0;j<workerNum;j++)
{
usersPartition[j+1] = usersPartition[j] + dataTable_partition.getPartition(j).get().get()[0];
itemsPartition[j+1] = itemsPartition[j] + dataTable_partition.getPartition(j).get().get()[1];

}

//compute out blocks
usersOutBlocks = computeOutBlocks(daal_Context, workerNum, trainDaalTable, itemsPartition);
itemsOutBlocks = computeOutBlocks(daal_Context, workerNum, trainDaalTableTran, usersPartition);

```

The initialization of *usersPartition* and *itemsPartition* demand a synchronization among mappers, where we use harp's *allgather* operation.
The training process consists of four steps. Firstly, the four steps will compute model data x (*usersPartialResultLocal*) while fixing the *itemsPartialResultLocal* as a constant.
Secondly, the four steps will repeat and compute the *itemsPartialResultLocal* while fixing *usersPartialResultLocal* as a constant.

### Training Process Step 1

```java

ALSTrainStep1 algo_step1 = new ALSTrainStep1(r, numThreads, itemsPartialResultLocal, this);

//compute step 1
step1LocalResult = algo_step1.compute();

//communication step 1
Table<ByteArray> step1LocalResult_table = algo_step1.communicate(step1LocalResult);

```

In step 1, we create a *ALSTrainStep1* class, which takes *itemsPartialResultLocal* as an input argument. We first invoke the *compute()* method and store the result at 
*step1LocalResult*, which is then communicated among all the mappers by invoking the *communicate()* function of *ALSTrainStep1* 

### Training Process Step 2

The computation work of Step 2 happens on the master node, which is the mapper with its id equals zero. The result *step2MasterResult* is then broadcast to other 
slaves nodes by invoking the *communicate* function.

```java

//step 2 on master node
ALSTrainStep2 algo_step2 = new ALSTrainStep2(r, numThreads, step1LocalResult_table, this);

if (this.getSelfID() == 0)
{
step2MasterResult = algo_step2.compute();
}

//free up memory
step1LocalResult_table = null;
step1LocalResult = null;

//broadcast step2MasterResult to other slave nodes step2MasterResult is HomogenNumericTable
step2MasterResult = algo_step2.communicate(step2MasterResult);

```

### Training Process Step 3

Step 3 also contains the computation part and a communication part. Each node computes its local *step3LocalResult* and uses harp table to 
accomplish a *allgather* operation inside the *communicate()* function of class *ALSTrainStep3*. 

```java
ALSTrainStep3 algo_step3 = new ALSTrainStep3(r, numThreads, itemsPartition, itemsOutBlocks, itemsPartialResultLocal, this);
//compute step 3
DistributedPartialResultStep3 partialResult_step3 = algo_step3.compute();

// Prepare input objects for the fourth step of the distributed algorithm
step3LocalResult = partialResult_step3.get(DistributedPartialResultStep3Id.outputOfStep3ForStep4);
Table<ByteArray> step3LocalResult_table = new Table<>(0, new ByteArrPlus());

step4LocalInput = algo_step3.communicate(step3LocalResult, step4LocalInput, step3LocalResult_table);

```

### Training Process Step 4

Finally, the fourth step computes out the *usersPartialResultLocal* which is local on each node. 

```java

ALSTrainStep4 algo_step4 = new ALSTrainStep4(r, numThreads, alpha, lambda_als, step4LocalInput, trainDaalTable,
step2MasterResult, this);

usersPartialResultLocal = algo_step4.compute();

```

### Test Process

After several rounds of training process, we could examine the effectiveness of Harp-DAAL-ALS by computing the root of mean square errors (RMSE) of 
a test dataset. Like the training dataset, the test dataset is first loaded into Harp-DAAL-ALS 

```java

Int2ObjectOpenHashMap<VRowCol> testDataMap = SGDUtil.loadTMapRow(testFilePath, numThreads_harp, configuration);

```

Unlike the training dataset that is distributed on all the nodes, each node loads the whole test dataset into its memory space. We could compute
an initial RMSE value before the start of training process. Here, we also use the *row_mapping* and *col_mapping* values that records the new row/column 
id for the points of test dataset.

```java

// ------------------------------ compute initial RMSE from test dataset ------------------------------
testModelInitRMSE(usersPartition, itemsPartition, dataTableRows, testDataMap, row_mapping, col_mapping);

```

After training process, we compute again the RMSE value of test dataset

```java

//test model after this iteration
testModel(iteration, usersPartition, itemsPartition, usersPartialResultLocal, itemsPartialResultLocal, testDataMap, row_mapping, col_mapping);

```

Comparing the RMSE value before and after the training process, we could evaluate the correctness and efficiency of our Harp-DAAL-ALS algorithm.

## Running the codes

Make sure that the code is placed in the `/harp/ml/daal` directory.
Run the `harp-daal-als.sh` script here to run the code.
```bash
cd $HARP_ROOT/ml/daal
./test_scripts/harp-daal-als.sh
```

The details of script is [here](https://github.com/DSC-SPIDAL/harp/blob/master/ml/daal/test_scripts/harp-daal-als.sh)




