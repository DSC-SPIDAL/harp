#!/bin/bash

Arch=hsw

cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

source /N/u/lc37/Lib/DAAL2018_Beta/__release_lnx/daal/bin/daalvars.sh intel64
echo "${DAALROOT}"

cd ${HADOOP_HOME}

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi

# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBBROOT}/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/

# use the path at account lc37
logDir=/N/u/lc37/HADOOP/Test_longs/logs
export LIBJARS=${DAALROOT}/lib/daal.jar
Arch=hsw

# ---------------------- test Harp-DAAL ----------------------

# Scaling Test from 10 nodes to 30 nodes on yahoomusic 
Dataset=yahoomusic-train
Testset=yahoomusic-test
# Dataset=netflix-train
# Testset=netflix-test
# Dataset=movielens-train
# Testset=movielens-test

Dim=100

## pars for yahoomusic
Lambda=1
Epsilon=0.0001

## pars for netflix
# Lambda=0.05
# Epsilon=0.002
Itr=5
Mem=110000

## on 6 nodes 
Thd=24
Tune=false

Node=2
hdfs dfs -mkdir -p /Hadoop/als-work
hdfs dfs -rm -r /Hadoop/als-work/*                                                        

echo "Test-$Arch-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune Start" 
bin/hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_als.ALSDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/$Dataset $Dim $Lambda $Epsilon $Itr $Tune $Node $Thd $Mem /Hadoop/als-work /Hadoop/sgd-input/$Testset 2>$logDir/Test-$Arch-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune.log 
echo "Test-$Arch-daal-als-$Dataset-D$Dim-N$Node-T$Thd-ITR$Itr-Timer-$Tune End" 


