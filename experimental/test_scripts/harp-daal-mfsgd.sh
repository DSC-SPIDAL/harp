#!/bin/bash

## root path of harp  
cd ../../
export HARP_ROOT=$(pwd)
cd ${HARP_ROOT}

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

## replace the path with your DAAL compiled lib path
source ~/Lib/DAAL2018_Beta/__release_hsw_lnx/daal/bin/daalvars.sh intel64

## check if DAAL env is setup 
if [ -z ${DAALROOT+x} ];then
	echo "DAAL not installed, please setup DAALROOT"
	exit
else
	echo "${DAALROOT}"
fi

cp ${HARP_ROOT}/experimental/target/experimental-1.0-SNAPSHOT.jar ${HADOOP_HOME}
cd ${HADOOP_HOME}

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi

## copy required third_party native libs to HDFS
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${HARP_ROOT}/third_party/omp/libiomp5.so /Hadoop/Libraries/
hdfs dfs -put ${HARP_ROOT}/third_party/hdfs/libhdfs.so* /Hadoop/Libraries/

export LIBJARS=${DAALROOT}/lib/daal.jar

## load training and test data
datadir=${HARP_ROOT}/datasets/daal_als
Dataset=movielens-train
Testset=movielens-test

hdfs dfs -mkdir -p /Hadoop/mfsgd-input
hdfs dfs -rm -r /Hadoop/mfsgd-input/*
hdfs dfs -put ${datadir}/${Dataset} /Hadoop/mfsgd-input/ 
hdfs dfs -put ${datadir}/${Testset} /Hadoop/mfsgd-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-MFSGD
logDir=${HADOOP_HOME}/Harp-DAAL-MFSGD

## parameters
Node=2
Thd=24
Tune=false

Dim=500
Mem=110000

Itr=5

Lambda=0.05
Epsilon=0.003

hdfs dfs -mkdir -p /Hadoop/mfsgd-work
hdfs dfs -rm -r /Hadoop/mfsgd-work/*                                                        

echo "Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune Start" 
hadoop jar experimental-1.0-SNAPSHOT.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/mfsgd-input/$Dataset $Dim $Lambda $Epsilon $Itr $Tune $Node $Thd $Mem /Hadoop/mfsgd-work /Hadoop/mfsgd-input/$Testset 2>$logDir/Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune.log
echo "Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune End" 


