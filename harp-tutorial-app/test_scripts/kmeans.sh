#!/bin/bash

## export the HARP_DAAL_ROOT
cd ../
export HARP_TUTORIAL_ROOT=$(pwd)

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

cp ${HARP_TUTORIAL_ROOT}/target/harp-tutorial-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

cd $HADOOP_HOME

   # <numOfDataPoints>: the number of data points you want to generate randomly
   # <num of centriods>: the number of centroids you want to clustering the data to
   # <size of vector>: the number of dimension of the data
   # <number of map tasks>: number of map tasks
   # <number of iteration>: the number of iterations to run
   # <work dir>: the root directory for this running in HDFS
   # <local dir>: the harp kmeans will firstly generate files which contain data points to local directory. Set this argument to determine the local directory.
   # <communication operation> includes:
	# 	[allreduce]: use allreduce operation to synchronize centroids
	# 	[regroup-allgather]: use regroup and allgather operation to synchronize centroids 
	# 	[broadcast-reduce]: use broadcast and reduce operation to synchronize centroids
	# 	[push-pull]: use push and pull operation to synchronize centroids

hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 /kmeans /tmp/kmeans allreduce

hdfs dfs -ls /
# hdfs dfs -cat /kmeans/centroids/output/*
hdfs dfs -cat /kmeans/out/kmeans_out_0/part-m-00000
