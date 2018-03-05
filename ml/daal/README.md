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
Edit the harp/ml/daal/pom.xml and harp/core/pom.xml files and comment out the installation of harp-daal related 
modules, we will install that later.

harp/core/pom.xml

```xml
<modules>
        <module>harp-collective</module>
        <module>harp-hadoop</module>
        <!-- <module>harp-daal-interface</module> -->
</modules>
```

harp/ml/daal/pom.xml

```xml
<modules>
        <module>java</module>
        <!--module>daal</module-->
</modules>
```

Compile harp (Please install Apache Maven https://maven.apache.org before compiling harp source code)

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 
and 2.9.0
 
```bash
mvn clean package -Phadoop-2.6.0
```
Add compiled harp JAR files to Hadoop directory.
```bash
cp distribution/hadoop-2.6.0/harp-collective-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
cp distribution/hadoop-2.6.0/harp-hadoop-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
cp third_party/fastutil-7.0.13.jar $HADOOP_HOME/share/hadoop/mapreduce/
```
harp-collective-1.0-SNAPSHOT.jar and harp-hadoop-1.0-SNAPSHOT.jar are the core modules that implement harp's 
collective communication for Hadoop mappers. 
fastutil-7.0.13.jar has data structures used by harp-java. 

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
# pull the daal src (as a submodule) for the first time
git submodule update --init --recursive
```
Compile and make the lib files
```bash
# enter daal src directory
cd ml/daal/daal-src
# compile and install
make daal PLAT=lnx32e
# setup the DAALROOT environment variables
source ../__release_lnx/daal/bin/daalvars.sh intel64
```
The DAAL source code within DSC-SPIDAL/harp has some modifications upon a certain version of Intel DAAL source code. 
The current source code is based on Intel DAAL version 2018 beta update1. Installation from Intel DAAL latest version 
may accelerate the performance of harp-daal, however, it may also cause compilation errors if Intel 
change some of the DAAL Java APIs. Therefore, we recommend users to use the tested DAAL stable version provided by our 
repository. Some harp-daal codes like MF-SGD contains DAAL native implementation codes that are not yet 
included to Intel DAAL repository, and users can only run them with installation of DAAL codes from DSC-SPIDAL/harp.
In addition, our DAAL codes provide users of exclusive optimized data structures for machine learning algorithms 
with big model. 

3. Update daal-src from harp-daal repository
If users choose second option and install the submodule daal-src of DSC-SPIDAL/harp. The daal-src points to a 
certain commit of our DAAL code version. If users would like to explore the latest updates of our DAAL code
please make branch daal_2018_beta of repository https://github.com/DSC-SPIDAL/harp.git as a remote upstream and git pull daal_2018_beta 
branch
```bash
cd harp/ml/daal/daal-src
git checkout daal_2018_beta
git remote -v 
git remote rename origin upstream 
git pull upstream daal_2018_beta:daal_2018_beta
```
Another way to update daal-src by git submodule command 
```bash
cd harp
git submodule update --recursive --remote
```
## Compile and Run Harp-DAAL Applications
1. Add harp-daal-interface module back to harp/core/pom.xml file
```xml
<modules>
        <module>harp-collective</module>
        <module>harp-hadoop</module>
        <module>harp-daal-interface</module>
</modules>
```
2. Add daal module back to harp/ml/pom.xml file
```xml
<modules>
        <module>java</module>
        <module>daal</module>
</modules>
```
3. Add external daal lib dependency to harp/core/harp-daal-interface/pom.xml and harp/ml/daal/pom.xml file
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
4. Re-compile harp to generate harp-daal application targets.

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 
and 2.9.0

```bash
cd harp/
mvn clean package -Phadoop-2.6.0
```
The generated harp-daal lib is at distribution/hadoop-2.6.0/harp-daal-1.0-SNAPSHOT.jar

5. Run harp-daal frome NameNode of the launched Hadoop daemons 

```bash
## setup additional env vars needed by DAAL native
export HARP_DAAL_HOME=harp/ml/daal/
export TBBROOT=${HARP_DAAL_HOME}/daal-src/externals/tbb
# copy harp-daal jar file to Hadoop directory
cp ${HARP_ROOT_DIR}/distribution/hadoop-2.6.0/harp-daal-1.0-SNAPSHOT.jar ${HADOOP_HOME}
# enter hadoop home directory
cd ${HADOOP_HOME}
# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache of 
# running harp mappers
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBBROOT}/lnx/lib/intel64/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${HARP_DAAL_HOME}/external/omp/libiomp5.so /Hadoop/Libraries/
# set up path to the DAAL Java APIs lib
export LIBJARS=${DAALROOT}/lib/daal.jar
# launch mappers, e.g., harp-daal-als 
bin/hadoop jar harp-daal-1.0-SNAPSHOT.jar edu.iu.daal_als.ALSDaalLauncher -libjars ${LIBJARS} 
/Hadoop/sgd-input/yahoomusic-train 100 1 0.0001 10 false 2 24 110000 /Hadoop/als-work /Hadoop/sgd-input/yahoomusic-test
```
command line arguments vary from app to app, please refer to the src of harp-daal
there is also a test_scripts directory under /ml/daal, which contains example scripts to run each harp-daal application







