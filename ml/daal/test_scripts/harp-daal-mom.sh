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
datadir=${HARP_ROOT}/datasets/daal_mom
Dataset=daal_mom_dense

hdfs dfs -mkdir -p /Hadoop/mom-input
hdfs dfs -rm -r /Hadoop/mom-input/*
hdfs dfs -put ${datadir}/$Dataset /Hadoop/mom-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-MOM
logDir=${HADOOP_HOME}/Harp-DAAL-MOM

Mem=110000
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=16

echo "Test-daal-mom-$Dataset-N$Node-T$Thd Start" 
hadoop jar harp-daal-1.0-SNAPSHOT.jar edu.iu.daal_mom.MOMDaalLauncher -libjars ${LIBJARS}  /Hadoop/mom-input/$Dataset /mom/work $Mem $Node $Thd 2>$logDir/Test-daal-mom-$Dataset-N$Node-T$Thd.log 
echo "Test-daal-mom-$Dataset-N$Node-T$Thd End" 
