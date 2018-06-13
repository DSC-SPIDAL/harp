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

Figure 1 shows the architecture of Harp-DAAL within the whole HPC-Big Data software stack. 

## What is Harp-DAAL? 

Harp-DAAL is a new framework that aims to run data analytics algorithms on distributed HPC architectures. 
The framework consists of two layers: a communication layer and a computation layer. A communication layer is handled by Harp, 
a communication library plug-in into Hadoop ecosystem, and a computation layer is handled by Intel's Data Analytics Acceleration Library (DAAL), 
which is a library that provides the users of well optimized building blocks for data analytics and machine learning applications on Intel's architectures. 

Compared to contemporary distributed data processing frameworks, such as Hadoop and Spark, Harp has the following advantages:

1. MPI-like collective communication operations that are highly optimized for big data problems.
2. Efficient and innovative computation models for different machine learning problems.

However, the original Harp framework only supports development of Java applications, which is a common choice of the Hadoop ecosystem. 
The downside of the pure Java implementation is a lack of supporting high-end HPC architectures such as Intel's Xeon Phi. 
By invoking DAAL's native kernels, applications can exploit all of the hardware threads on many-core platforms, which is a great 
advantage for computation-intensive workloads.

## How to install Harp-DAAL Application ?

The libraries of Intel DAAL version 2018 has been pre-compiled and located at path *harp/third_party/daal_2018*, which provides all
of the implementations for Harp-DAAL algorithms located at path *harp/ml/daal*

The source codes of Intel DAAL version 2019 are loacted at path *harp/third_party/daal-exp*, which provides implementations for 
Harp-DAAL experimental algorithms located at path *harp/experimental*

### Compile Harp-DAAL Algorithms

To compile the Harp-DAAL algorithms, put the *ml* modules along with *core* modules in the *harp/pom.xml* file

```xml
<modules>
	<module>core</module>
    <module>ml</module>
</modules>
```

and run *maven*

```bash
mvn clean package -Phadoop-2.6.0
```

The compiled jar files are located at *harp/ml/daal/target/harp-daal-0.1.0.jar*

### Compile Harp-DAAL Applications (Experimental) 

To compile the Harp-DAAL experimental applications, first make sure that the daal source codes has been compiled locally. 

```bash
cd daal/
make daal PLAT=lnx32e
```

where directory *daal/* is the location of your daal source codes, *lnx32e* is the 64 bit linux platforms. To compile Intel DAAL
source codes on other platforms, please find detailed instructions in the README.md of Intel DAAL source code directory.
The compiled library files are located at *daal/__release_lnx/* folder. To use the compiled Intel DAAL libraries, source the 
environment setting script

```bash
source daa/__release_lnx/daal/bin/daalvars.sh intel64
```

Secondly, add the experimental module to the *harp/pom.xml* file

```xml
<modules>
	<module>core</module>
    <module>experimental</module>
</modules>
```

run the *maven*

```bash
mvn clean package -Phadoop-2.6.0
```

The compiled jar files are located at *harp/experimental/target/experimental-0.1.0.jar*

## How to run a Harp-DAAL application ?

Run harp-daal frome NameNode of the launched Hadoop daemons 

```bash
# copy harp-daal jar file to Hadoop directory
cp $HARP_ROOT_DIR/core/harp-daal-interface/target/harp-daal-interface-0.1.0.jar ${HADOOP_HOME}/share/hadoop/mapreduce
cp $HARP_ROOT_DIR/ml/daal/target/harp-daal-0.1.0.jar ${HADOOP_HOME}
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
bin/hadoop jar harp-daal-0.1.0.jar edu.iu.daal_als.ALSDaalLauncher -libjars ${LIBJARS} 
/Hadoop/sgd-input/yahoomusic-train 100 1 0.0001 10 false 2 24 110000 /Hadoop/als-work /Hadoop/sgd-input/yahoomusic-test
```
















