#!/bin/bash

## root path of harp  
cd ../../../
export HARP_ROOT=$(pwd)
cd ${HARP_ROOT}

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

cp ${HARP_ROOT}/ml/daal/target/harp-daal-0.1.0.jar ${HADOOP_HOME}
cd ${HADOOP_HOME}

# check that safemode is not enabled 
hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0"  ]]; then
    hdfs dfsadmin -safemode leave
fi

## copy required third_party native libs to HDFS
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${HARP_ROOT}/third_party/daal-2018/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${HARP_ROOT}/third_party/tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/

export LIBJARS=${HARP_ROOT}/third_party/daal-2018/lib/daal.jar

## load training and test data
hdfs dfs -mkdir -p /Hadoop/pca-input

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-PCA
logDir=${HADOOP_HOME}/Harp-DAAL-PCA

# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=16
# memory allocated to each mapper (MB)
Mem=110000
# iteration
nIterations=1
# feature vector dimension
Dim=10
GenData=true
# num of training data points
Pts=1000
# file num 
File=10

logName=Test-daal-pca_svd-P$Pts-D$Dim-F$File-N$Node-T$Thd.log

echo "Test-daal-pca-P$Pts-D$Dim-F$File-N$Node-T$Thd Start" 
hadoop jar harp-daal-0.1.0.jar edu.iu.daal_pca.svddensedistr.PCADaalLauncher -libjars ${LIBJARS} $Node $Thd $Mem $nIterations /Hadoop/pca-input/Pca-P$Pts-D$Dim-F$File-N$Node /Hadoop/pca-work $Dim $Dim $GenData $Pts $File /tmp/PCA 2>$logDir/${logName}
echo "Test-daal-pca-P$Pts-D$Dim-F$File-N$Node-T$Thd End" 
