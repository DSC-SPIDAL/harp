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
datadir=${HARP_ROOT}/datasets/daal_als
Dataset=movielens-train
Testset=movielens-test

hdfs dfs -mkdir -p /Hadoop/als-input
hdfs dfs -rm -r /Hadoop/als-input/*
hdfs dfs -put ${datadir}/${Dataset} /Hadoop/als-input/ 
hdfs dfs -put ${datadir}/${Testset} /Hadoop/als-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-ALS
logDir=${HADOOP_HOME}/Harp-DAAL-ALS

## runtime parameters

Node=2
Thd=64
Tune=false

Dim=100
Mem=110000

Itr=5

Lambda=0.05
Epsilon=0.003

hdfs dfs -mkdir -p /Hadoop/als-work
hdfs dfs -rm -r /Hadoop/als-work/*

echo "Test-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune Start" 
bin/hadoop jar harp-daal-1.0-SNAPSHOT.jar edu.iu.daal_als.ALSDaalLauncher -libjars ${LIBJARS} /Hadoop/als-input/$Dataset $Dim $Lambda $Epsilon $Itr $Tune $Node $Thd $Mem /Hadoop/als-work /Hadoop/als-input/$Testset 2>$logDir/Test-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune.log 
echo "Test-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune End" 


