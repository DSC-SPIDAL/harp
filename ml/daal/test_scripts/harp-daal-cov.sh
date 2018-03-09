#!/bin/bash

## root path of harp  
cd ../../../
export HARP_ROOT=$(pwd)
cd ${HARP_ROOT}

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

cp ${HARP_ROOT}/ml/daal/target/harp-daal-1.0-SNAPSHOT.jar ${HADOOP_HOME}

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
datadir=${HARP_ROOT}/datasets/daal_cov
Dataset=daal_cov_dense

hdfs dfs -mkdir -p /Hadoop/cov-input
hdfs dfs -rm -r /Hadoop/cov-input/*
hdfs dfs -put ${datadir}/${Dataset} /Hadoop/cov-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-COV
logDir=${HADOOP_HOME}/Harp-DAAL-COV

## parameters
Node=2
# num of threads on each mapper(node)
Thd=16
Mem=110000

hdfs dfs -mkdir -p /Hadoop/cov-work
hdfs dfs -rm -r /Hadoop/cov-work/*

echo "Test-daal-cov-$Dataset-N$Node-T$Thd Start" 
hadoop jar harp-daal-1.0-SNAPSHOT.jar edu.iu.daal_cov.COVDaalLauncher -libjars ${LIBJARS}  /Hadoop/cov-input/$Dataset  /Hadoop/cov-work $Mem $Node $Thd 2>$logDir/Test-daal-cov-$Dataset-N$Node-T$Thd.log 
echo "Test-daal-cov-$Dataset-N$Node-T$Thd End" 
