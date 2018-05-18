#!/bin/bash

#
# test script for LDA, using a sample dataset
#

#get dataset name
if [ $# -eq "1" ]; then
	dataset=$1
else
	#dataset=10k
	dataset=sample
fi

#get the startup directory
if [ ! -z ${HARP_ROOT_DIR+x} ];then
	harproot=$HARP_ROOT_DIR
else
	startdir=$(dirname $0)
	harproot=$(readlink -m $startdir/../../)
fi

datadir=$harproot/datasets/tutorial/lda-cvb/
bin=$harproot/contrib/target/contrib-0.1.0.jar
hdfsroot=/harp-test
hdfsdatadir=$hdfsroot/ldacvb/$dataset/
hdfsoutput=$hdfsroot/ldacvb/output/
metafile="$dataset".meta

echo $startdir,$harproot


if [ ! -d $datadir ] ; then
    echo "dataset not found at "$datadir
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

mkdir -p test_lda
cd test_lda

#check dataset
runlog=$(hadoop fs -ls $hdfsdatadir)
if [ $? -ne 0 ]; then
    echo "initialize the dataset on hdfs......"

    hadoop fs -rm -f $metafile
    hadoop fs -rm -r -f $hdfsdatadir
    hadoop fs -mkdir -p $hdfsdatadir 

	if [ $dataset == "sample" ]; then
		#sample
		hadoop fs -put $datadir/sample-sparse-data/*.txt $hdfsdatadir
		hadoop fs -put $datadir/sample-sparse-data/*metadata $metafile
	elif [ $dataset == "10k" ]; then
		#10k
		hadoop fs -put $datadir/sample-sparse-10k/x* $hdfsdatadir
		hadoop fs -put $datadir/sample-sparse-10k/*metadata $metafile
	fi
    echo "done!"
fi

#run test
#hadoop jar contrib-1.0.SNAPSHOT.jar  edu.iu.lda.LdaMapCollective <input dir>  <metafile>  <output dir> <number of terms> <number of topics> <number of docs> <number of MapTasks> <number of iterations> <number of threads> <mode, 1=multithreading>

if [ $dataset == "sample" ]; then
	#sample-sparse-data
	echo "hadoop jar $bin  edu.iu.lda.LdaMapCollective $hdfsdatadir $metafile $hdfsoutput 11 2 12 2 5 4 1"
	hadoop jar $bin  edu.iu.lda.LdaMapCollective $hdfsdatadir $metafile $hdfsoutput 11 2 12 2 5 4 1

elif [ $dataset == "10k" ]; then
	# sample-sparse-10k
	echo "hadoop jar $bin  edu.iu.lda.LdaMapCollective $hdfsdatadir $metafile $hdfsoutput 435840 50 7698 2 5 16 1"
	hadoop jar $bin  edu.iu.lda.LdaMapCollective $hdfsdatadir $metafile $hdfsoutput 435840 50 7698 2 5 16 1
fi


if [ $? -ne 0 ]; then
    echo "run lda failure"
    exit -1
fi

#check the result

