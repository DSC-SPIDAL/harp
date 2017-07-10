# Harp-DAAL-2018 

Harp-DAAL-2018 is a distributed machine learning framework built upon Harp, a Hadoop plug-in for collective-communication on Big Data problems, and Intel Data Analytics Acceleration Library (DAAL). 

## Harp and Collective-Communication  

Harp is a Hadoop plug-in, which rewrites the Hadoop MapReduce framework to achieve in-memory communication among nodes of a distributed environment. It has the following two features 

* Harp has MPI-like collective communication operations that are highly optimized for big data problems.
* Harp has efficient and innovative computation models for different machine learning problems.

## Machine Learning Kernels on HPC platforms

The original Harp project is written in Java, which compromises its exploiting of hardware resources on many-core HPC platforms. 
This motives us to bring in Intel® Data Analytics Acceleration Library (DAAL), a library aims to provide the users of highly optimized 
building blocks for data analytics and machine learning applications. 

The algorithm kernels in DAAL are implemented in C++ while taking advantage of Intel's math library (MKL) and multi-threading building blocks (TBB). Also, DAAL provides users of language interfaces for
Java and Python. Through DAAL's Java APIs, we are able to invoke DAAL's native language kernels from the Java codes of Harp applications. Therefore, we observe more than ten folds of performance 
acceleration of using Harp-DAAL framework in machine learning applications, such as collaborative filtering (ALS) and K-means clustering. 

## Install Harp-DAAL Framework 

Harp-DAAL framework has dependencies on two modules

1. Harp framework
2. Intel DAAL library

### Install Harp Framework

Download the source code of Harp framework from Github page of DSC-SPIDAL/harp 
```bash
git clone git@github.com:DSC-SPIDAL/harp.git
```
Edit the harp/pom.xml file and comment out the installation of harp-daal-app module, we will install that later.
```xml
<modules>
        <module>harp-project</module>
        <module>harp-tutorial-app</module>
        <module>harp-app</module>
        <!-- <module>harp-daal-app</module> -->
</modules>
```
Compile harp (Please install Apache Maven https://maven.apache.org before compiling harp source code) 
```bash
mvn clean package
```
Add compiled harp JAR files to Hadoop directory.
```bash
cp harp-project/target/harp-project-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
cp third_party/fastutil-7.0.13.jar $HADOOP_HOME/share/hadoop/mapreduce/
```
harp-project-1.0-SNAPSHOT.jar is the core module that implements harp's collective communication for Hadoop mappers. 
fastutil-7.0.13.jar has data structures used by harp-app. 

### Install DAAL Framework

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
source __release_lnx/daal/bin/daalvars.sh intel64
```
2. Installation from optimized DAAL source code within DSC-SPIDAL/harp (Recommended)
If you already use option "--recursive" (supported by git version 1.9 and later) when you git clone 
harp repository, the daal-src as a submodule has already been downloaded. Otherwise, you shall manually
fetch the submodule source code of daal-src as follows.
```bash
# enter the harp root directory
cd harp
# pull the daal src (as a submodule)
git submodule update --init --recursive
```
Compile and make the lib files
```bash
# enter daal src directory
cd harp-daal-app/daal-src
# compile and install
make daal PLAT=lnx32e
# setup the DAALROOT environment variables
source ../__release_lnx/daal/bin/daalvars.sh intel64
```
The DAAL source code within DSC-SPIDAL/harp has some modifications upon a certain version of Intel DAAL source code. 
The current source code is based on Intel DAAL version 2018 beta update1. Installation from Intel DAAL latest version 
may accelerate the performance of harp-daal-app, however, it may also cause compilation errors if Intel 
change some of the DAAL Java APIs. Therefore, we recommend users to use the tested DAAL stable version provided by our 
repository. Some harp-daal-app codes like MF-SGD contains DAAL native implementation codes that are not yet 
included to Intel DAAL repository, and users can only run them with installation of DAAL codes from DSC-SPIDAL/harp.
In addition, our DAAL codes provide users of exclusive optimized data structures for machine learning algorithms 
with big model. 

3. Update daal-src submodule
If users choose second option and install the submodule daal-src of DSC-SPIDAL/harp. The daal-src points to a 
certain commit of our DAAL code version. If users would like to explore the latest updates of our DAAL code
please make branch daal_2018_beta of repository https://github.com/DSC-SPIDAL/harp.git as a remote upstream and git pull daal_2018_beta 
branch
```bash
cd harp/harp-daal-app/daal-src
git checkout daal_2018_beta
git remote -v 
git remote rename origin upstream 
git pull upstream daal_2018_beta:daal_2018_beta
```

## Compile and Run Harp-DAAL Applications
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







