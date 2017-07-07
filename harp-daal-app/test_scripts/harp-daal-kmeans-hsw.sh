#!/bin/bash

# enter the directory of hadoop and copy your_harp_daal.jar file here
cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}
# set up daal environment
# source ./__release_tango_lnx/daal/bin/daalvars.sh intel64
source /N/u/lc37/Lib/DAAL2018_Beta/__release_lnx/daal/bin/daalvars.sh intel64
echo "${DAALROOT}"

cd ${HADOOP_HOME}

# check that safemode is not enabled 
hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0"  ]]; then
    hdfs dfsadmin -safemode leave
fi

# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
# hdfs dfs -put ${TBB_ROOT}/lib/intel64_lin_mic/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${TBBROOT}/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/

# daal.jar will be used in command line
export LIBJARS=${DAALROOT}/lib/daal.jar
# use the path at account lc37
logDir=/N/u/lc37/HADOOP/Test_longs/logs
Arch=hsw

# num of training data points
Pts=500000
# num of training data centroids
Ced=1000
# feature vector dimension
Dim=100
# file per mapper
File=5
# iteration times
ITR=10
# memory allocated to each mapper (MB)
# Mem=185000
Mem=110000
# generate training data or not (once generated, data file /kmeans-P$Pts-C$Ced-D$Dim-N$Node is in hdfs, you could reuse them next time)
GenData=false
# num of mappers (nodes)
Node=4
# num of threads on each mapper(node)
Thd=8

echo "Test-$Arch-daal-kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd Start" 
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher -libjars ${LIBJARS} $Pts $Ced $Dim $File $Node $Thd $ITR $Mem /kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node /tmp/kmeans $GenData 2>$logDir/Test-$Arch-daal-kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd.log  
echo "Test-$Arch-daal-kmeans-P$Pts-C$Ced-D$Dim-F$File-ITR$ITR-N$Node-Thd$Thd End" 



