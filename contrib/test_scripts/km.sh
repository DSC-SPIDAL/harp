#!/bin/bash

#
# test script for KMeans, using random generated dataset
#

#get the startup directory
startdir=$(dirname $0)
harproot=$(readlink -m $startdir/../../)
bin=$harproot/harp-tutorial-app/target/harp-tutorial-app-1.0-SNAPSHOT.jar
hdfsroot=/harp-test
hdfsdatadir=$hdfsroot/km-syn/
hdfsoutput=$hdfsroot/km/

if [ ! -f $bin ] ; then
    echo "harp tutorial app not found at "$bin
    exit -1
fi
if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

#workdir
workdir=test_km

mkdir -p $workdir
cd $workdir

#
# runtest <outputdir> <comm>
#
runtest()
{
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
    hadoop jar $bin edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 $1 /tmp/kmeans $2
    
    if [ $? -ne 0 ]; then
        echo "run km failure"
        exit -1
    fi
    
    #check the result
    echo "checking result of :"$2
    ret=$(hdfs dfs -cat $1/evaluation | grep -Po "MSE : (.*)" |grep -Po "[0-9]\..*")
    echo "MSE="$ret
    eval=$(echo "($ret < 7.8) && ($ret >7.0)" | bc)
    if [ $eval -eq 1 ]; then
        echo "Pass!"
        #exit 0
    else
        echo "Fail!"
        exit -1
    fi
    
}

#run test
#hadoop jar $bin edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 $hdfsoutput/allreduce /tmp/kmeans allreduce
#hadoop jar $bin edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 $hdfsoutput/regroup /tmp/kmeans regroup-allgather
#hadoop jar $bin edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 $hdfsoutput/broadcast /tmp/kmeans broadcast-reduce
#hadoop jar $bin edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 100 $hdfsoutput/pushpull /tmp/kmeans push-pull
runtest $hdfsoutput/allreduce allreduce
runtest $hdfsoutput/regroup regroup-allgather
runtest $hdfsoutput/broadcast broadcast-reduce
runtest $hdfsoutput/pushpull push-pull
