#!/bin/bash

## export the HARP_DAAL_ROOT
export HARP_DAAL_ROOT=/scratch/hpda/imagecluster/harpdall/harp-daal-app/

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

## check if DAAL env is setup 
if [ -z ${DAALROOT+x} ];then
    echo "DAAL not installed, please setup DAALROOT"
    exit
else
    echo "${DAALROOT}"
fi

## check that safemode is not enabled 
#hdfs dfsadmin -safemode get | grep -q "ON"
#if [[ "$?" = "0"  ]]; then
#    hdfs dfsadmin -safemode leave
#fi
#
## put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
#hdfs dfs -mkdir -p /Hadoop/Libraries
#hdfs dfs -rm /Hadoop/Libraries/*
#hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
#hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
#hdfs dfs -put ${HARP_DAAL_ROOT}/external/omp/libiomp5.so /Hadoop/Libraries/

### log directory
mkdir -p  logs
logDir=logs
export LIBJARS=${DAALROOT}/lib/daal.jar

# num of training data points
Pts=35000
# num of training data centroids
#Ced=100
Ced=100000
# feature vector dimension
Dim=128
# file per mapper
File=1
# iteration times
ITR=1
# memory allocated to each mapper (MB)
Mem=185000
#Mem=110000
# generate training data or not (once generated, data file /kmeans-P$Pts-C$Ced-D$Dim-N$Node is in hdfs, you could reuse them next time)
GenData=False
# num of mappers (nodes)
Node=30
# num of threads on each mapper(node)
#Thd=48
Thd=64

dataroot=/yfccfull

#
#initialize the data directory
#this is a very simple initialization solution
#you can take random initialization or more elaborated methods such as kmeans++
#
mkdir -p centroids
inputdatafile=`ls pca128/*.txt |head -1`
head -${Ced} pca128/$inputdatafile | sed 's/,/ /g' >centroids/init_centroids
hadoop fs -put centroids $dataroot

#run kmeans
echo "Test-daal-kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd Start" 
echo hadoop jar $HARP_DAAL_JAR edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher -libjars ${LIBJARS} $Pts $Ced $Dim $File $Node $Thd $ITR $Mem $dataroot /tmp/yfcc $GenData |tee -a  $logDir/Test-daal-kmeans-P
$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd.log  
hadoop jar $HARP_DAAL_JAR edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher -libjars ${LIBJARS} $Pts $Ced $Dim $File $Node $Thd $ITR $Mem $dataroot /tmp/yfcc $GenData |tee -a  $logDir/Test-daal-kmeans-P$Pts-
C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd.log  
echo "Test-daal-kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd End" 
