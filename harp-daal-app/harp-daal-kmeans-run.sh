#!/bin/bash

# enter the directory of hadoop and copy your_harp_daal.jar file here
cp ./target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}
cd ${HADOOP_HOME}
source /N/u/fg474admin/lc37/SPIDAL/harp/harp-daal-app/__release_tango_lnx/daal/bin/daalvars.sh intel64
echo "${DAALROOT}"

# check that safemode is not enabled 
hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0"  ]]; then
    hdfs dfsadmin -safemode leave
fi

# put daal and tbb, omp libs to hdfs, they will be loaded into the distributed cache
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${TBB_ROOT}/lib/intel64_lin_mic/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../omp/lib/libiomp5.so /Hadoop/Libraries/

# daal.jar will be used in command line
export LIBJARS=${DAALROOT}/lib/daal.jar

# num of training data points
Pts=50000
# num of training data centroids
Ced=1000
# feature vector dimension
Dim=100
# file per mapper
File=5
# iteration times
ITR=10
# memory allocated to each mapper (MB)
Mem=185000
# generate training data or not (once generated, data file /kmeans-P$Pts-C$Ced-D$Dim-N$Node is in hdfs, you could reuse them next time)
GenData=true
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=64

hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher -libjars ${LIBJARS} $Pts $Ced $Dim $File $Node $Thd $ITR $Mem /kmeans-P$Pts-C$Ced-D$Dim-N$Node /tmp/kmeans $GenData 
