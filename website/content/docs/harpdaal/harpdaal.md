---
title: Harp-DAAL Framework
---

<img src="/img/6-1-2/6-1-2-1-1.png" width="7.1775%"><img src="/img/6-1-2/6-1-2-1-2.png" width="51.3975%"><img src="/img/6-1-2/6-1-2-1-3.png" width="7.425%">
<br />
<img src="/img/6-1-2/6-1-2-2-1.png" width="7.1775%"><a href="/docs/programming/computation-models/"><img src="/img/6-1-2/6-1-2-2-2.png" width="51.3975%"></a><img src="/img/6-1-2/6-1-2-2-3.png" width="7.425%">
<br />
<img src="/img/6-1-2/6-1-2-3-1.png" width="7.1775%"><img src="/img/6-1-2/6-1-2-3-2.png" width="51.3975%"><img src="/img/6-1-2/6-1-2-3-3.png" width="7.425%">
<br />
<img src="/img/6-1-2/6-1-2-4-1.png" width="7.1775%"><a href="https://software.intel.com/en-us/intel-daal"><img src="/img/6-1-2/6-1-2-4-2.png" width="51.3975%"></a><img src="/img/6-1-2/6-1-2-4-3.png" width="7.425%">
<br />
<img src="/img/6-1-2/6-1-2-5-1.png" width="7.1775%"><img src="/img/6-1-2/6-1-2-5-2.png" width="51.3975%"><img src="/img/6-1-2/6-1-2-5-3.png" width="7.425%">

Figure 1 shows the position of Harp-DAAL within the whole HPC-Big Data software stack. 

## What is Harp-DAAL? 

Harp-DAAL is a new framework that aims to run data analytics algorithms on distributed HPC architectures. The framework consists of two layers: a communication layer and a computation layer. A communication layer is handled by Harp, 
a communication library plug-in into Hadoop ecosystem, and a computation layer is handled by Intel's Data Analytics Acceleration Library (DAAL), which is a library that provides 
the users of well optimized building blocks for data analytics and machine learning applications on Intel's architectures. 


Compared to contemporary communication libraries, such as Hadoop and Spark, Harp has the following advantages:

* MPI-like collective communication operations that are highly optimized for big data problems.
* efficient and innovative computation models for different machine learning problems.

However, the original Harp framework only supports development of Java applications, which is a common choice within the Hadoop ecosystem. 
The downside of the pure Java implementation is the lack of support for emerging new hardware architectures such as Intel's Xeon Phi. 
By invoking DAAL's native kernels, applications can leverage the huge number of threads on many-core platforms, which is a great 
advantage for computation-intensive data analytics algorithms. This is also the tendency of merging HPC and Big Data domain. 

## How to build a Harp-DAAL Application ?

If you already have a legacy Harp application codes, you need only identify the local computation module, and replace it by invoking correspondent 
DAAL kernels. Although DAAL's kernels are written in C/C++, it does provide users of a Java API. The API is highly packaged, and the users only need
a few lines of codes to finish the invocation of kernels. For instance, the main function of a PCA application in DAAL is shown as below: 

```java
public static void main(String[] args) throws java.io.FileNotFoundException, java.io.IOException {

     /* Read a data set from a file and create a numeric table for storing the input data */
     CSRNumericTable data = Service.createSparseTable(context, datasetFileName);

     /* Create an algorithm to compute PCA decomposition using the correlation method */
     Batch pcaAlgorithm = new Batch(context, Double.class, Method.correlationDense);

     com.intel.daal.algorithms.covariance.Batch covarianceSparse
         = new com.intel.daal.algorithms.covariance.Batch(context, Double.class, com.intel.daal.algorithms.covariance.Method.fastCSR);
     pcaAlgorithm.parameter.setCovariance(covarianceSparse);

     /* Set the input data */
     pcaAlgorithm.input.set(InputId.data, data);

     /* Compute PCA decomposition */
     Result res = pcaAlgorithm.compute();

     NumericTable eigenValues = res.get(ResultId.eigenValues);
     NumericTable eigenVectors = res.get(ResultId.eigenVectors);
     Service.printNumericTable("Eigenvalues:", eigenValues);
     Service.printNumericTable("Eigenvectors:", eigenVectors);

     context.dispose();
}
```

DAAL's Java API is usually contains the following objects:

* Data: user's data packed in DAAL's data structure, e.g., NumericTable, DataCollection 
* Algorithm:  the engine of machine learning, each has three modes: Batch, Distri, Online 
* Input: the user's input data to Algorithm 
* Parameter: the parameters provided by users during the running of algorithms
* Result: the feedback of Algorithm after running, retrieved by users

Before invoking your DAAL kernels,the most suitable data structure for the problem should be chosen. For many NumericTable types, 
the Java API provides two ways of storing data. One is to store data on the JVM heap side, and whenever the native computation kernels require 
the dataset, it will automatically copy the data from JVM heap to the off-heap memory space. The other way is to store data on Java's direct byte buffer, and 
native computation kernels can access them directly without any data copy. Therefore, you should evaluate the overhead of loading and writing data from memory 
in your application. For many data-intensive applications, it is wise to store the data on the direct byte buffer. 

If you build the Harp-DAAL application from scratch, you should also carefully choose the data structure on the Harp side. The thumb rule is to allocate data in 
contiguous primitive Java array, because most of DAAL's Java API only accepts primitive array as input arguments. If Harp's own Table structure is used and the 
contained data is distributed into different partitions, then you may use the Harp-DAAL data conversion API to transfer the data between a Harp table and a DAAL
table. 

### Harp-DAAL Data Conversion API

Harp-DAAL now provides a group of classes under the path *Harp/harp-daal-app/src/edu/iu/daal*, which manipulates the data transfer
between Harp's data structure and that of DAAL.

* RotatorDaal: a rotator which internally converts the H matrix from Harp table to DAAL's NumericTable
* RotateTaskDaal: the tasks executed by RotatorDaal in the model rotation paradigm.
* HomogenTableHarpMap: convert data between DAAL's HomogenNumericTable and Harp's map
* HomogenTableHarpTable: convert data between DAAL's HomogenNumericTable and Harp's table

Within the *RotatorDaal*, the data transfers between Harp and DAAL is also overlapped by the computation work in another pipeline. Thus, if there is enough computation workload, the 
overhead of data conversion could be significantly reduced. It is also very straightforward to invoke these conversion tools. 

```java

//create a conversion class between harp map and daal's table
HomogenTableHarpMap<double[]> converter = new HomogenTableHarpMap<double[]>(wMap, wMap_index, wMap_daal, wMap_size, r, numThreads);
convert_wTable.HarpToDaalDouble();

//create a conversion class between a harp table and a daal table
converter = new HomogenTableHarpTable<I, P, Table<P> >(table, this.daal_table, table.getNumPartitions(), this.rdim, this.numThreads);
converter.HarpToDaalDouble();

```

## How to Compile and Run Harp-DAAL Application ?

### Install Intel's DAAL 

You can either download Intel's DAAL product with licence from their website https://software.intel.com/en-us/blogs/daal, or build it from source code. DAAL's source code 
is open-sourced and available on the GitHub. 

```bash

git clone https://github.com/01org/daal.git

```
After installation, you can run the bin/daalvars.sh script to set up all the DAAL related environment variables. 

```bash

source /path-to-daal/bin/daalvars.sh intel64

```
The important environment variable is the *DAALROOT*, which points to the path of DAAL's source code. You can run the examples of each algorithm within DAAL to test 
the installation of your DAAL library. 

```bash

cd $DAALROOT/../__release_lnx/daal/examples/cpp
make {libia32|soia32|libintel64|sointel64|help} [example=name] [compiler=compiler_name] [mode=mode_name] [threading=threading_name]

```

### Setup DAAL within Harp

To use DAAL within Harp, you need first add DAAL Java API to your Java source code

```java

// packages from Daal
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.*;
import com.intel.daal.data_management.data.NumericTable;

```

To compile your Harp-DAAL codes by ant, add the following lines to your *build.xml*

```xml

<fileset dir="${env.DAALROOT}">
<include name="**/*.jar" />
<include name="**/lib/*.jar" />
</fileset>

```

Since DAAL's Java API will invoke the native DAAL kernels, we need to load the native lib files into Harp's environment in two steps

1.Load these required native libraries in HDFS

```bash

hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/

```

2.Load native libraries from HDFS to distributed cache of Harp's program. 

```java

/* Put shared libraries into the distributed cache */
Configuration conf = this.getConf();
DistributedCache.createSymlink(conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libJavaAPI.so#libJavaAPI.so"), conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so.2#libtbb.so.2"), conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so#libtbb.so"), conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so.2#libtbbmalloc.so.2"), conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so#libtbbmalloc.so"), conf);
DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libiomp5.so#libiomp5.so"), conf);

```

















