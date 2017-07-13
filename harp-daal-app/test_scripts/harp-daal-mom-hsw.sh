#!/bin/bash

Arch=hsw

cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

source /N/u/mayank/daal/__release_lnx/daal/bin/daalvars.sh intel64
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
hdfs dfs -put ${DAALROOT}/../../omp/lib/libiomp5.so /Hadoop/Libraries/

# use the path at account lc37
logDir=/N/u/mayank/HADOOP/Test_longs/logs
export LIBJARS=${DAALROOT}/lib/daal.jar

Dataset=daal_mom
Mem=110000
Batch=50
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=24

echo "Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd-B$Batch Start" 
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_mom.MOMDaalLauncher -libjars ${LIBJARS}  /Hadoop/mom-input/$Dataset/train /Hadoop/mom-input/$Dataset/test /Hadoop/mom-input/$Dataset/groundTruth /mom/work $Mem $Batch $Node $Thd 2>$logDir/Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd-B$Batch.log 
echo "Test-$Arch-daal-mom-$Dataset-N$Node-T$Thd-B$Batch End" 
