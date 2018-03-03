#!/bin/bash

#
# test script for MFSGD, on movielens dataset
#

#get the startup directory
hadoopversion=2.6.0
startdir=$(dirname $0)
harproot=$(readlink -m $startdir/../../../)
datadir=$harproot/datasets/tutorial/movielens
bin=$harproot/distribution/hadoop-$hadoopversion/harp-java-1.0-SNAPSHOT.jar
hdfsroot=/harp-test
hdfsdatadir=$hdfsroot/movielens/
hdfsoutput=$hdfsroot/mfsgd/

#echo $startdir,$harproot

if [ ! -d $datadir ] ; then
    echo "rcv1 dataset not found at "$datadir
    exit -1
fi
if [ ! -f $bin ] ; then
    echo "harp java app not found at "$bin
    exit -1
fi
if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

#workdir
workdir=test_mfsgd

mkdir -p $workdir
cd $workdir

#check dataset
runlog=$(hadoop fs -ls $hdfsdatadir/train/)
if [ $? -ne 0 ]; then
    echo "initialize the dataset on hdfs......"

    hadoop fs -rm -r -f $hdfsdatadir
    hadoop fs -mkdir -p $hdfsdatadir

    rm -rf train test
    mkdir -p train
    mkdir -p test
    cp $datadir/*.bz2 .
    bzip2 -d *.bz2
    cd train
    split -l 100000 ../movielens-train.mm
    cd ../test
    split -l 100000 ../movielens-test.mm
    cd ..
    
    hadoop fs -put train test $hdfsdatadir
    echo "done!"
fi

#run test
#Usage: edu.iu.sgd.SGDLauncher <input dir> <r> <lambda> <epsilon> <num of iterations> <training ratio> <num of map tasks> <num of threads per worker> <schedule ratio><memory (MB)>  <work dir> <test dir>
#hadoop jar $bin edu.iu.sgd.SGDLauncher $hdfsdatadir/train 500 0.05 0.002 500 100 2 16 2 20000 $hdfsoutput $hdfsdatadir/test
hadoop jar $bin edu.iu.sgd.SGDLauncher $hdfsdatadir/train 40 0.05 0.002 200 100 2 16 2 20000 $hdfsoutput $hdfsdatadir/test

if [ $? -ne 0 ]; then
    echo "run mlr failure"
    exit -1
fi

#check the result
ret=$(hdfs dfs -cat $hdfsoutput/model/evaluation)
echo "TestRMSE="$ret
#0.8345
eval=$(echo "(($ret < 0.84) && ($ret > 0.80))" | bc)
if [ $eval -eq 1 ]; then
    echo "Pass!"
    exit 0
else
    echo "Fail!"
    exit -1
fi


