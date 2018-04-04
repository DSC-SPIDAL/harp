# HARP

@Copyright 2013-2017 Inidana University
Apache License 2.0

## WHAT IS HARP?

Harp is a HPC-ABDS (High Performance Computing Enhanced Apache Big Data Stack) framework aiming to provide distributed 
machine learning and other data intensive applications. 

## Highlights

1. Plug into Hadoop ecosystem.
2. Rich computation models for different machine learning/data intensive applications
2. MPI-like Collective Communication operations 
4. High performance native kernels supporting many-core processors (e.g., Intel Xeon and Xeon Phi) 

## Online Documentation

Please find the full documentation of Harp at https://dsc-spidal.github.io/harp/ including quick start, programming guide, and 
examples.

## Installation of Harp 

### Install from Source Code

1. Install Maven by following the [maven official instruction](http://maven.apache.org/install.html)
2. Compile harp by Maven with different hadoop versions
```bash
## x.x.x could be 2.6.0, 2.7.5, and 2.9.0 
mvn clean package -Phadoop-x.x.x
```

3. Copy compiled modules jar files to $HADOOP_HOME 
```bash
cd harp/
## the core modules 
cp core/harp-hadoop/target/harp-hadoop-0.1.0.jar $HADOOP_HOME/share/hadoop/mapreduce/
cp core/harp-collective/target/harp-collective-0.1.0.jar $HADOOP_HOME/share/hadoop/mapreduce/
cp core/harp-daal-interface/target/harp-daal-interface-0.1.0.jar $HADOOP_HOME/share/hadoop/mapreduce/
## the applications modules 
cp ml/java/target/harp-java-0.1.0.jar $HADOOP_HOME/
cp ml/daal/target/harp-daal-0.1.0.jar $HADOOP_HOME/
cp contrib/target/contrib-0.1.0.jar $HADOOP_HOME/ 
cp experimental/target/experimental-0.1.0.jar $HADOOP_HOME/
```

Here the *experimental-0.1.0.jar* could only be installed from our source codes.

## Enable third party dependencies

Harp depends on a group of third party libraries. Make sure to install them before launching the applications

```bash
cd third_party/
## JAR files
cp *.jar $HADOOP_HOME/share/hadoop/mapreduce/
## DAAL 2018
## copy daal java API lib
cp daal-2018/lib/daal.jar $HADOOP_HOME/share/hadoop/mapreduce/
## copy native libs to HDFS
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -put daal-2018/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries
hdfs dfs -put tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries
```

If you would like to test experimental applications, please install DAAL from source codes kept at daal_2018 branch 
```bash
git clone -b daal_2018 git@github.com:DSC-SPIDAL/harp.git
mv harp harp-daal-exp
cd harp-daal-exp
```

or git pull the submodule from third_party/daal-exp/
```bash
cd harp/
git submodule update --init --recursive
cd third_party/daal-exp/
```

compile the native library either by icc or gnu
```bash
## use COMPILER=gun if icc is not available
make daal PLAT=lnx32e COMPILER=icc
## copy native libs to HDFS 
cp ../__release_lnx/daal/lib/daal.jar $HADOOP_HOME/share/hadoop/mapreduce/
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -put ../__release_lnx/daal/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries
hdfs dfs -put ../__release_lnx/tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries
hdfs dfs -put harp/third_party/omp/libiomp5.so /Hadoop/Libraries/
hdfs dfs -put harp/third_party/hdfs/libhdfs.so* /Hadoop/Libraries/
```

The experimental codes have only been tested on Linux 64 bit platforme with Intel icc compiler and GNU compiler.

## Run example of K-means

Make sure that harp-java-0.1.0.jar has been copied to $HADOOP_HOME.
Start the Hadoop service
```bash
cd $HADOOP_HOME
sbin/start-dfs.sh
sbin/start-yarn.sh
```

The usage of K-means is 
```bash
hadoop jar harp-java-0.1.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher
  <num of points> <num of centroids> <vector size> <num of point files per worker>
  <number of map tasks> <num threads> <number of iteration> <work dir> <local points dir>
```

For example:
```bash
hadoop jar harp-app-0.1.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans
```

