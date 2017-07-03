---
title: Harp-DAAL Framework
---

<img src="/img/6-1-2/6-1-2-1-1.png" width="7.08%"><img src="/img/6-1-2/6-1-2-1-2.png" width="55.54%"><img src="/img/6-1-2/6-1-2-1-3.png" width="7.38%">
<br />
<img src="/img/6-1-2/6-1-2-2-1.png" width="7.08%"><a href="/docs/programming/computation-models/"><img src="/img/6-1-2/6-1-2-2-2.png" width="55.54%"></a><img src="/img/6-1-2/6-1-2-2-3.png" width="7.38%">
<br />
<img src="/img/6-1-2/6-1-2-3-1.png" width="7.08%"><img src="/img/6-1-2/6-1-2-3-2.png" width="55.54%"><img src="/img/6-1-2/6-1-2-3-3.png" width="7.38%">
<br />
<img src="/img/6-1-2/6-1-2-4-1.png" width="7.08%"><a href="https://software.intel.com/en-us/intel-daal"><img src="/img/6-1-2/6-1-2-4-2.png" width="55.54%"></a><img src="/img/6-1-2/6-1-2-4-3.png" width="7.38%">
<br />
<img src="/img/6-1-2/6-1-2-5-1.png" width="7.08%"><img src="/img/6-1-2/6-1-2-5-2.png" width="55.54%"><img src="/img/6-1-2/6-1-2-5-3.png" width="7.38%">

Figure 1 shows the position of Harp-DAAL within the whole HPC-Big Data software stack. 

## What is Harp-DAAL? 

Harp-DAAL is a new framework that aims to run data analytics algorithms on distributed HPC architectures. 
The framework consists of two layers: a communication layer and a computation layer. A communication layer is handled by Harp, 
a communication library plug-in into Hadoop ecosystem, and a computation layer is handled by Intel's Data Analytics Acceleration Library (DAAL), 
which is a library that provides the users of well optimized building blocks for data analytics and machine learning applications on Intel's architectures. 

Compared to contemporary communication libraries, such as Hadoop and Spark, Harp has the following advantages:

1. MPI-like collective communication operations that are highly optimized for big data problems.
2. Efficient and innovative computation models for different machine learning problems.

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

+ Data: user's data packed in DAAL's data structure, e.g., NumericTable, DataCollection 
+ Algorithm:  the engine of machine learning, each has three modes: Batch, Distri, Online 
+ Input: the user's input data to Algorithm 
+ Parameter: the parameters provided by users during the running of algorithms
+ Result: the feedback of Algorithm after running, retrieved by users

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

+ RotatorDaal: a rotator which internally converts the H matrix from Harp table to DAAL's NumericTable
+ RotateTaskDaal: the tasks executed by RotatorDaal in the model rotation paradigm.
+ HomogenTableHarpMap: convert data between DAAL's HomogenNumericTable and Harp's map
+ HomogenTableHarpTable: convert data between DAAL's HomogenNumericTable and Harp's table

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

### Installation of DAAL framework

There are two options to install DAAL library for Harp-DAAL 

1. Installation from latest Intel DAAL source code (https://github.com/01org/daal)
```bash
# clone from Intel Github repository
git clone git@github.com:01org/daal.git
# enter the src directory
cd daal
# compile and install
make daal PLAT=lnx32e
# setup the DAALROOT environment variables
source __release_lnx/daal/bin/daalvars.sh 
```
2. Installation from optimized DAAL source code within DSC-SPIDAL/harp (Recommended)
```bash
# enter the harp root directory
cd harp
# pull the daal src (as a submodule)
git submodule update --init --recursive
# enter daal src directory
cd harp-daal-app/daal-src
# compile and install
make daal PLAT=lnx32e
# setup the DAALROOT environment variables
source ../__release_lnx/daal/bin/daalvars.sh 
```
The DAAL source code within DSC-SPIDAL/harp has some modifications upon a certain version of Intel DAAL source code. 
The current source code is based on Intel DAAL version 2018 beta update1. Installation from Intel DAAL latest version 
may accelerate the performance of harp-daal-app, however, it may also cause compilation errors if Intel 
change some of the DAAL Java APIs. Therefore, we recommend users to use the tested DAAL stable version provided by our 
repository. Some harp-daal-app codes like MF-SGD contains DAAL native implementation codes that are not yet included to Intel DAAL repository, 
and users can only run them with installation of DAAL codes from DSC-SPIDAL/harp.
In addition, our DAAL codes provide users of exclusive optimized data structures for machine learning algorithms 
with big model. 

3. Update daal-src submodule
If users choose second option and install the submodule daal-src of DSC-SPIDAL/harp. The daal-src points to a 
certain commit of our DAAL code version. If users would like to explore the latest updates of our DAAL code
please make https://github.com/francktcheng/Harp-DAAL-Local.git as a remote upstream repository and git pull daal_2018_beta_update1 
branch
```bash
cd harp/harp-daal-app/daal-src
git remote -v 
git remote rename origin upstream 
git pull upstream daal_2018_beta_update1:daal_2018_beta_update1
git checkout daal_2018_beta_update1
```

### Compile and Run Harp-DAAL Applications
1. add harp-daal-app module back to harp/pom.xml file
```xml
<modules>
        <module>harp-project</module>
        <module>harp-tutorial-app</module>
        <module>harp-app</module>
        <module>harp-daal-app</module>
</modules>
```

2. Add external daal lib dependency to harp/harp-daal-app/pom.xml file
The daal.jar file contains the Java APIs provided by DAAL to its native kernels
```xml
<dependency>
<groupId>daal</groupId>
<artifactId>daal</artifactId>
<scope>system</scope>
<version>1.0</version>
<systemPath>${DAALROOT}/lib/daal.jar</systemPath>
</dependency>
```

3. Re-compile harp to generate harp-daal-app targets
```bash
cd harp/
mvn clean package 
```
The generated harp-daal-app lib is at harp/harp-daal-app/target/harp-daal-app-1.0-SNAPSHOT.jar

4. Run harp-daal-app frome NameNode of the launched Hadoop daemons 
```bash
# copy harp-daal-app jar file to Hadoop directory
cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}
# enter hadoop home directory
cd ${HADOOP_HOME}
# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache of 
# running harp mappers
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBBROOT}/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/
# set up path to the DAAL Java APIs lib
export LIBJARS=${DAALROOT}/lib/daal.jar
# launch mappers, e.g., harp-daal-als 
bin/hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_als.ALSDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/yahoomusic-train 100 1 0.0001 10 false 2 24 110000 /Hadoop/als-work /Hadoop/sgd-input/yahoomusic-test
```
comand line arguments vary from app to app, please refer to the src of harp-daal-app
there is also a test_scripts directory under /harp-daal-app, which contains example scripts to run each harp-daal-app



















