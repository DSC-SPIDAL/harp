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
HarpDAALDataSource datasource = new HarpDAALDataSource(harpThreads, conf);
```

where two parameters are required

* int harpThreads: the multi-thread number used in parallel I/O, default value is the maximal available java threads 
* Configuration conf: the Configuration handle of Harp mapper. 

## Load Dense CSV Data

To load a dense training dataset stored in CSV files from HDFS

```java
NumericTable inputTable = datasource.createDenseNumericTableInput(inputFiles, nFeatures, sep,  daal_Context);
```

where parameters are

* inputFiles: the path to the CSV files stored in HDFS
* nFeatures: the dimension of each feature vector (column number of CSV file)
* sep: the seperator of CSV files (e.g., comma, or space)
* daal_Context: the DAALContext handle to create instance of DAAL NumericTable.

In some cases, the CSV files shall be splitted into two dense matrix (e.g, training data and label data)

```java
NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(inputFiles, nFeatures, 1, sep, daal_Context);
NumericTable trainData = load_table[0];
NumericTable trainLabels = load_table[1];
```

where *nFeatures* is the feature dimension of the first NumericTable, and the label table takes 1 dimension. 

## Load Sparse CSR Data

To load a CSR training dataset from HDFS

```java
NumericTable inputTable = datasource.loadCSRNumericTable(inputFiles, sep, context);
```

where the *nFeatures* parameter is not required for CSR files. 

## Load Sparse Distributed COO Data

As Intel DAAL has no NumericTable type dedicated to COO sparse format, 
the loading of distributed COO files takes following steps: 1) Loading distributed COO files from HDFS to harp mapper , 2) 
grouping COO entry either by their row ID or column ID, 3) regrouping COO entries across all mappers according to their row ID (or column ID), 
4) convert COO format to CSR format.

```java
// load COO files
List<COO> coo_data = this.datasource.loadCOOFiles(this.inputFiles,",");
// group by row ID 
HashMap<Long, COOGroup> coo_group = this.datasource.groupCOOByIDs(coo_data, true);
// create compact global ID mapping
HashMap<Long, Integer> gid_remap = this.datasource.remapCOOIDs(coo_group, this.getSelfID(), this.getNumWorkers(), this);
// regrouping COO entries
Table<COOGroup> regrouped_table = this.datasource.regroupCOOList(coo_group, gid_remap, this, maxCompactID);
// convert COO to CSRNumericTable
CSRNumericTable trainDaalTable = this.datasource.COOToCSR(regrouped_table, gid_remap_tran, daal_Context);
```







