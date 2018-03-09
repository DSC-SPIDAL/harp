#!/bin/bash

## root path of harp  
cd ../../
export HARP_ROOT=$(pwd)
cd ${HARP_ROOT}

if [ -z ${HADOOP_HOME+x} ];then
    echo "HADOOP not setup"
    exit
fi

## replace the path with your DAAL compiled lib path
source ~/Lib/DAAL2018_Beta/__release_hsw_lnx/daal/bin/daalvars.sh intel64

## check if DAAL env is setup 
if [ -z ${DAALROOT+x} ];then
	echo "DAAL not installed, please setup DAALROOT"
	exit
else
	echo "${DAALROOT}"
fi

cp ${HARP_ROOT}/experimental/target/experimental-1.0-SNAPSHOT.jar ${HADOOP_HOME}
cd ${HADOOP_HOME}

# check that safemode is not enabled 
hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0"  ]]; then
	hdfs dfsadmin -safemode leave
fi

## copy required third_party native libs to HDFS
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${HARP_ROOT}/third_party/omp/libiomp5.so /Hadoop/Libraries/
hdfs dfs -put ${HARP_ROOT}/third_party/hdfs/libhdfs.so* /Hadoop/Libraries/

export LIBJARS=${DAALROOT}/lib/daal.jar

## load training and test data
datadir=${HARP_ROOT}/datasets/daal_subgraph
graph=web-Google
template=u3-1.template
template_name=$(echo "$template" | cut -d'.' -f1 )

hdfs dfs -mkdir -p /Hadoop/subgraph-input
hdfs dfs -rm -r /Hadoop/subgraph-input/*
hdfs dfs -put ${datadir}/graphs/${graph} /Hadoop/subgraph-input/ 
hdfs dfs -put ${datadir}/templates/${template} /Hadoop/subgraph-input/ 

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-SC
logDir=${HADOOP_HOME}/Harp-DAAL-SC

# ------------------------ default parameters ------------------------
multiThd=true
# total cores per node 
Core=24
# physical threads per core
tpc=2
# total threads per node
Thd=48
# set up openmp thread affinity
# Affinity=compact
Affinity=scatter
# memory allocated to each mapper (MB)
Mem=110000
# iteration times
ITR=1
# num of distributed nodes
Node=2
# memory (MB) limit of each regroup array
SENDLIMIT=250 #default
NBRTASKLEN=50
ompschedule=guided
ROTATION_PIP=true
# run exp multiple times
round=1

if [ "$ROTATION_PIP" == "false" ];then
	memjavaratio=0.6
else
	memjavaratio=0.2
fi

logName=Test-subgraph-N$Node-$template_name-$graph-Thd-$Thd-Core-$Core-$Affinity-Omp-$ompschedule-SendLimit-$SENDLIMIT-TaskLen-$NBRTASKLEN-Rotation-${ROTATION_PIP}-Itr-$ITR.log
echo "Start Test harp-daal-subgraph Node: $Node template: $template_name graph: $graph Thd: $Thd Core: $Core Affinity: $Affinity TPC: $tpc SENDLIMIT: $SENDLIMIT NBRTASKLEN: $NBRTASKLEN Rotation $ROTATION_PIP Itr: $ITR Round: $round"
hadoop jar experimental-1.0-SNAPSHOT.jar edu.iu.daal_subgraph.SCDaalLauncher -libjars ${LIBJARS} $Node $multiThd /Hadoop/subgraph-input/$template /Hadoop/subgraph-input/$graph /tmp/subgraph $Thd $Core $Affinity $ompschedule $tpc $Mem $memjavaratio $SENDLIMIT $NBRTASKLEN $ROTATION_PIP $ITR 2>$logDir/${logName}
echo "Finish Test harp-daal-subgraph Node: $Node template: $template_name graph: $graph Thd: $Thd Core: $Core Affinity: $Affinity TPC: $tpc SENDLIMIT: $SENDLIMIT NBRTASKLEN: $NBRTASKLEN Rotation $ROTATION_PIP Itr: $ITR Round: $round"

