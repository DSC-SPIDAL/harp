#!/bin/bash

#
# test script for LDA, using the nytimes-30k dataset
#

#get the startup directory
hadoopversion=2.6.0
startdir=$(dirname $0)
harproot=$(readlink -m $startdir/../../../)
datadir=$harproot/datasets/tutorial/nytimes-30k
bin=$harproot/distribution/hadoop-$hadoopversion/harp-java-1.0-SNAPSHOT.jar
hdfsroot=/harp-test
hdfsdatadir=$hdfsroot/nytimes-30k/
hdfsoutput=$hdfsroot/lda/

#echo $startdir,$harproot

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
workdir=test_lda

mkdir -p $workdir
cd $workdir

#check dataset
runlog=$(hadoop fs -ls $hdfsdatadir/data/)
if [ $? -ne 0 ]; then
    echo "initialize the dataset on hdfs......"

    hadoop fs -rm -r -f $hdfsdatadir
    hadoop fs -mkdir -p $hdfsdatadir

    rm -rf data
    mkdir -p data
    cd data
    split -l 1000 $datadir/nytimes-30K.mrlda
    cd ..
    hadoop fs -put data $hdfsdatadir
    echo "done!"
fi

#run test
#Usage: edu.iu.lda.LDALauncher <doc dir> <num of topics> <alpha> <beta> <num of iterations> <min training percentage> <max training percentage> <num of mappers> <num of threads per worker> <schedule ratio> <m    emory (MB)> <work dir> <print model>
#hadoop jar $bin edu.iu.lda.LDALauncher $hdfsdatadir/data/ 1000 0.01 0.01 200 100 100 2 16 2 20000 $hdfsoutput true
hadoop jar $bin edu.iu.lda.LDALauncher $hdfsdatadir/data/ 1000 0.01 0.01 200 40 80 2 16 2 20000 $hdfsoutput true
if [ $? -ne 0 ]; then
    echo "run lda failure"
    exit -1
fi

#check the result
ret=$(hdfs dfs -cat $hdfsoutput/model/evaluation)
echo "LogLikelihood="$ret
eval=$(echo "($ret - -6.03E7 >0)" | bc)
if [ $eval -eq 1 ]; then
    echo "Pass!"
    exit 0
else
    echo "Fail!"
    exit -1
fi


