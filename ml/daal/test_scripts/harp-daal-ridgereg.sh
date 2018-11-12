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
datadir=${HARP_ROOT}/datasets/daal_reg

hdfs dfs -mkdir -p /Hadoop/rrg-input
hdfs dfs -rm -r /Hadoop/rrg-input/*
hdfs dfs -put ${datadir}/* /Hadoop/rrg-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-RRG
logDir=${HADOOP_HOME}/Harp-DAAL-RRG

## parameters
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=16
# mapper memory allocation
Mem=110000
# iteration
nIterations=1
fileDim=12
nFeatures=10
nDependentVars=2

logName=Test-daal-ridgereg-N$Node-T$Thd-B$Batch.log

echo "Test-daal-ridgereg-N$Node-T$Thd-B$Batch Start" 
hadoop jar harp-daal-0.1.0.jar edu.iu.daal_ridgereg.RidgeRegDaalLauncher -libjars ${LIBJARS} $Node $Thd $Mem $nIterations /Hadoop/rrg-input/train /ridgereg/work $fileDim $nFeatures $nDependentVars /Hadoop/rrg-input/test /Hadoop/rrg-input/groundTruth 2>$logDir/${logName} 
echo "Test-daal-ridgereg-N$Node-T$Thd-B$Batch End" 
