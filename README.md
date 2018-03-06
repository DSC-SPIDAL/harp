# HARP

@Copyright 2013-2017 Inidana University
Apache License 2.0

@Author: Bingjing Zhang, Langshi Chen, Bo Peng, Sabra Ossen

## WHAT IS HARP?

Harp is a HPC-ABDS (High Performance Computing Enhanced Apache Big Data Stack) framework aiming to provide distributed 
machine learning and other data intensive applications. 

## Highlights

1. Plug into Hadoop ecosystem.
2. Rich computation models for different machine learning/data intensive applications
2. MPI-like Collective Communication operations 
4. High performance native kernels supporting many-core processors (e.g., Intel Xeon Phi) 

## Overview

Harp provides users of a distributed machine learning/data analytics framework to develop new algorithms as well as out-of-box highly optimized 
applications. It consists of the following modules in the repository.

### harp/core

The core module of harp includes:

* Collective and event-driven communication operations
* Hadoop MapReduce engines
* Harp-DAAL Java API to transfer data between Harp and Intel DAAL

### harp/ml

Folder *harp/ml* contains all the released Harp/Harp-DAAL applications. 

* harp/ml/java includes applications with local computation kernels written in Java and implemented by Java threads. 
* harp/ml/daal includes applications with local computation kernels from Intel DAAL. 

### harp/experimental

The experimental folder contains all the ongoing application developments (beta version). Once the application codes have
been fully optimized and tested, they will be moved to harp/ml folder

### harp/contrib

The contrib folder contains all the applications contributed by users. Similar to harp/experimental, these codes shall 
be accepted by harp/ml after testing. 

### harp/third_party

The *third_party* folder has all the third party dependencies of harp project. For instance, harp/third_party/daal-2018 folder
contains pre-compiled Java and native libraries from Intel DAAL 2018 version. harp/third_party/daal-exp keeps a forked version of Intel DAAL 
source codes with our experimental codes for applications in harp/experimental

### harp/tutorial

The tutorial contains examples to run and program harp/harp-daal applications either by Java API or by Python API.

### harp/datasets

Some example datasets used by harp/harp-daal applications

### harp/distribution

Pre-compiled jar files of harp project for different Hadoop versions.

## COMPILATION & INSTALLATION

If users would like to compile the harp project on their own machines, please take the following steps

### 1. Install Maven 

Please follow the [maven official instruction](http://maven.apache.org/install.html)

### 2. Compile harp

Use maven to compile the harp jars

```bash
	cd harp/
    mvn clean package
```

### 3. Install harp Core Module 

Copy the compiled core module jars to $HADOOP_HOME folders

```bash
    cp core/harp-collective/target/harp-collective-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
    cp core/harp-hadoop/target/harp-hadoop-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
	cp core/harp-daal-interface/harp-daal-interface-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
```

### 4. Install third party dependencies  


```bash 
    cp third_party/*.jar $HADOOP_HOME/share/hadoop/mapreduce/
	cp third_party/daal-2018/daal.jar $HADOOP_HOME/share/hadoop/mapreduce/
```
