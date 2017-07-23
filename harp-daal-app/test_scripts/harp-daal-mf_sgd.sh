#!/bin/bash

## export the HARP_DAAL_ROOT
cd ../
export HARP_DAAL_ROOT=$(pwd)

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

cp ${HARP_DAAL_ROOT}/target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

## check if DAAL env is setup 
if [ -z ${DAALROOT+x} ];then
    echo "DAAL not installed, please setup DAALROOT"
    exit
else
    echo "${DAALROOT}"
fi

cd ${HADOOP_HOME}

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi

# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${HARP_DAAL_ROOT}/external/omp/libiomp5.so /Hadoop/Libraries/

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-LOG
logDir=${HADOOP_HOME}/Harp-DAAL-LOG

export LIBJARS=${DAALROOT}/lib/daal.jar


# ---------------------- test Harp-DAAL ----------------------

# Scaling Test from 10 nodes to 30 nodes on yahoomusic 
# Dataset=yahoomusic-train
# Testset=yahoomusic-test
Dataset=netflix-train
Testset=netflix-test
# Dataset=movielens-train
# Testset=movielens-test

Dim=500

## pars for yahoomusic
# Lambda=1
# Epsilon=0.0001

## pars for netflix
Lambda=0.05
Epsilon=0.002

Itr=5
Mem=110000

## on 6 nodes 
Thd=24
Tune=false

Node=2
hdfs dfs -mkdir -p /Hadoop/sgd-work
hdfs dfs -rm -r /Hadoop/sgd-work/*                                                        

echo "Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune Start" 
bin/hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/$Dataset $Dim $Lambda $Epsilon $Itr $Tune $Node $Thd $Mem /Hadoop/sgd-work /Hadoop/sgd-input/$Testset 2>$logDir/Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune.log
echo "Test-daal-sgd-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune End" 


