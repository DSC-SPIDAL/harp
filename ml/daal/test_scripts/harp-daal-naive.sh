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

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
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
datadir=${HARP_ROOT}/datasets/daal_naive

hdfs dfs -mkdir -p /Hadoop/naive-input
hdfs dfs -rm -r /Hadoop/naive-input/*
hdfs dfs -put ${datadir}/* /Hadoop/naive-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-NB
logDir=${HADOOP_HOME}/Harp-DAAL-NB

## parameters
Node=2
Thd=24
Mem=110000
nIterations=1
Dim=20
NClass=20
genData=true
numTrain=10000 ## used when genData == true
numfiles=10 ## used when genData == true
NTest=200

logName=Test-daal-naive-N$Node-T$Thd.log
echo "Test-daal-naive-$Dataset-N$Node-T$Thd Start" 
hadoop jar harp-daal-0.1.0.jar edu.iu.daal_naive.densedistri.NaiveDaalLauncher -libjars ${LIBJARS} $Node $Thd $Mem $nIterations /Hadoop/naive-input/train /Hadoop/naive-work $Dim $Dim $NClass /Hadoop/naive-input/test /Hadoop/naive-input/groundTruth $genData $numTrain $numfiles $NTest /tmp/naive 2>$logDir/${logName} 
echo "Test-daal-naive-$Dataset-N$Node-T$Thd End" 
