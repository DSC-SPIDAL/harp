#!/bin/bash

# enter the directory of hadoop and copy your_harp_daal.jar file here
cp ../target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}
# set up daal environment
source /N/u/lc37/Lib/DAAL2018_Beta/__release_lnx/daal/bin/daalvars.sh intel64
echo "${DAALROOT}"

curDir=$(pwd )

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
hdfs dfs -put ${TBBROOT}/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/

# daal.jar will be used in command line
logDir=/N/u/lc37/HADOOP/Test_longs/logs
export LIBJARS=${DAALROOT}/lib/daal.jar
Arch=hsw

# num of training data points
Pts=100000
# feature vector dimension
Dim=100
# file per mapper
File=5
# num of mappers (nodes)
Node=2
# num of threads on each mapper(node)
Thd=8
# memory allocated to each mapper (MB)
Mem=110000
# generate training data or not (once generated, data file /kmeans-P$Pts-C$Ced-D$Dim-N$Node is in hdfs, you could reuse them next time)
GenData=true

echo "Test-$Arch-daal-svd-P$Pts-D$Dim-F$File-N$Node-T$Thd Start" 
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_svd.SVDDaalLauncher -libjars ${LIBJARS} $Pts $Dim $File $Node $Thd $Mem /Svd-P$Pts-D$Dim-F$File-N$Node /tmp/SVD $GenData 2>$logDir/Test-$Arch-daal-svd-P$Pts-D$Dim-F$File-N$Node-T$Thd.log
echo "Test-$Arch-daal-svd-P$Pts-D$Dim-F$File-N$Node-T$Thd End" 
