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
hdfs dfs -put ${HARP_DAAL_ROOT}/external/hdfs/libhdfs.so* /Hadoop/Libraries/

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-LOG
logDir=${HADOOP_HOME}/Harp-DAAL-LOG

export LIBJARS=${DAALROOT}/lib/daal.jar

Dataset=daal_mom_dense
Mem=110000
# num of mappers (nodes)
Node=1
# num of threads on each mapper(node)
Thd=16

echo "Test-daal-mom-$Dataset-N$Node-T$Thd Start" 
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_mom.MOMDaalLauncher -libjars ${LIBJARS}  /Hadoop/mom-input/$Dataset /mom/work $Mem $Node $Thd 2>$logDir/Test-daal-mom-$Dataset-N$Node-T$Thd.log 
echo "Test-daal-mom-$Dataset-N$Node-T$Thd End" 
