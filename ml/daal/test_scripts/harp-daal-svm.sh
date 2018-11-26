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
datadir=${HARP_ROOT}/datasets/daal_svm/multidense
hdfs dfs -mkdir -p /Hadoop/svm-input
hdfs dfs -rm -r /Hadoop/svm-input/*
hdfs dfs -put ${datadir}/* /Hadoop/svm-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-SVM
logDir=${HADOOP_HOME}/Harp-DAAL-SVM

## parameters
# feature vector dimension
nFeature=20
# file length 
nFile=21
# num class
nClass=5
# num of mappers (nodes)
Node=1
# num of threads on each mapper(node)
Thd=48
# memory allocated to each mapper (MB)
Mem=110000
# iteration
nIterations=1
# input training data 
trainData=train
testData=test

logName=Test-daal-svm-P$Pts-D$Dim-F$File-N$Node-T$Thd.log
echo "$logName Start" 
hadoop jar harp-daal-0.1.0.jar edu.iu.daal_svm.MultiClassDenseBatch.SVMDaalLauncher -libjars ${LIBJARS} $Node $Thd $Mem $nIterations /Hadoop/svm-input/${trainData} /tmp/svm $nFile $nFeature $nClass /Hadoop/svm-input/${testData} 2>$logDir/$logName
echo "$logName End" 
