---
title: I/O Module API 
---

Harp-DAAL has a dedicated I/O module to load data from HDFS files to 
memory space accessible by native kernels of DAAL. It currently supports
loading dense data (HomogenNumericTable) and CSR data (CSRNumericTable).
For dense matrix, we support parallel I/O by using multiple java threads.

To use I/O module:

```java
import edu.iu.datasource.*;
```

To create a *HarpDAALDataSource* data loader for dense data:

```java
HarpDAALDataSource datasource = new HarpDAALDataSource(dataFiles, fileDim, harpThreads, conf);
```

where four parameters are required

* List<String> dataFiles: the list of HDFS file names of input data
* int fileDim: the dimension of each file row (column number of dense matrix)
* int harpThreads: the multi-thread number used in parallel I/O, default value is the maximal available java threads 
* Configuration conf: the Configuration handle of Harp mapper. 

To create a *HarpDAALDataSource* for CSR sparse data, the second argument *fileDim* is omitted.

```java
HarpDAALDataSource datasource = new HarpDAALDataSource(dataFiles, harpThreads, conf);
```

## Load Input Data (Training Data)

The file path of input data is already initialized during the creation of *HarpDAALDataSource* object.
To create a dense training dataset from HDFS

```java
NumericTable inputTable = datasource.createDenseNumericTableInput(daal_Context);
```

To create a CSR training dataset from HDFS

```java
NumericTable inputTable = datasource.createCSRNumericTableInput(daal_Context);
```

## Load Additional Data (ground truth data, label data, and test data)

These datasets are relatively much smaller than the training data, and their HDFS paths are 
not bind to the Harp *KeyValReader*. To load these files, we obtain their HDFS paths from 
command line arguments and explicitly invoke *HarpDAALDataSource* to load them.

To create a dense additional data from filenames and file dimension

```java
NumericTable table = datasource.createDenseNumericTable(filenames, fileDim, daal_Context);
```

To create a CSR additional data from filenames

```java
NumericTable table = datasource.createCSRNumericTable(filenames, daal_Context);
```






