#!/bin/bash

#
# test script for MLR, using the rcv1v2 dataset
#

#get the startup directory
hadoopversion=2.6.0
startdir=$(dirname $0)
harproot=$(readlink -m $startdir/../../)
datadir=$harproot/datasets/tutorial/rcv1
bin=$harproot/distribution/hadoop-$hadoopversion/contrib-1.0-SNAPSHOT.jar
hdfsroot=/harp-test
hdfsdatadir=$hdfsroot/rcv1/
hdfsoutput=$hdfsroot/mlr/

echo $startdir,$harproot


if [ ! -d $datadir ] ; then
    echo "rcv1 dataset not found at "$datadir
    exit -1
fi
if [ ! -f $bin ] ; then
    echo "harp tutorial app not found at "$bin
    exit -1
fi
if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

#workdir
workdir=test_mlr

mkdir -p test_mlr
cd test_mlr

#check dataset
runlog=$(hadoop fs -ls $hdfsdatadir/)
if [ $? -ne 0 ]; then
    echo "initialize the dataset on hdfs......"

    hadoop fs -rm -r -f $hdfsdatadir
    hadoop fs -mkdir -p $hdfsdatadir
    hadoop fs -put $datadir/rcv1* $hdfsdatadir

    rm -rf data
    mkdir -p data
    cd data
    split -l 1000 $datadir/lyrl2004_vectors_train.dat
    cd ..
    hadoop fs -put data $hdfsdatadir
    echo "done!"
fi

#run test
#$ hadoop jar contrib-1.0-SNAPSHOT.jar edu.iu.mlr.MLRMapCollective [alpha] [number of iteration] [number of features] [number of workers] [number of threads] [topic file path] [qrel file path] [input path in HDFS] [output path in HDFS]
hadoop jar $bin edu.iu.mlr.MLRMapCollective 1.0 5 47236 2 16 $hdfsdatadir/rcv1.topics.txt $hdfsdatadir/rcv1-v2.topics.qrels $hdfsdatadir/data $hdfsoutput
if [ $? -ne 0 ]; then
    echo "run mlr failure"
    exit -1
fi

#check the result
ret=$(hdfs dfs -cat $hdfsoutput/evaluation | grep -Po "macroF1 : (.*)" |grep -Po "0\..*")
echo "MacroF1="$ret
eval=$(echo "($ret - 0.83 < 0) && ($ret -0.80 > 0)" | bc)
if [ $eval -eq 1 ]; then
    echo "Pass!"
    exit 0
else
    echo "Fail!"
    exit -1
fi


