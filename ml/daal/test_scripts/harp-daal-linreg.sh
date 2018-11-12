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

hdfs dfs -mkdir -p /Hadoop/lrg-input
hdfs dfs -rm -r /Hadoop/lrg-input/*
hdfs dfs -put ${datadir}/* /Hadoop/lrg-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-LRG
logDir=${HADOOP_HOME}/Harp-DAAL-LRG

## parameters
Mem=110000
# iteration
nIterations=1
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=16
# fileDim
fileDim=12
nFeatures=10
nDependentVars=2

logName=Test-daal-linreg-N$Node-T$Thd.log
echo "Test-daal-linreg-N$Node-T$Thd-B$Batch Start" 
hadoop jar harp-daal-0.1.0.jar edu.iu.daal_linreg.qrdense.LinRegDaalLauncher -libjars ${LIBJARS} $Node $Thd $Mem $nIterations /Hadoop/lrg-input/train /linreg/work $fileDim $nFeatures $nDependentVars /Hadoop/lrg-input/test /Hadoop/lrg-input/groundTruth 2>$logDir/${logName} 
echo "Test-daal-linreg-N$Node-T$Thd-B$Batch End" 
