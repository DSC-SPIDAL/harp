#!/bin/bash

## export the HARP_DAAL_ROOT
cd ../
export HARP_DAAL_ROOT=$(pwd)

if [ -z ${HADOOP_HOME+x} ];then
	echo "HADOOP not setup"
	exit
fi

cp ${HARP_DAAL_ROOT}/target/harp-daal-app-1.0-SNAPSHOT.jar ${HADOOP_HOME}

## check if DAAL env is setup 
if [ -z ${DAALROOT+x} ];then
	echo "DAAL not installed, please setup DAALROOT"
	exit
else
	echo "${DAALROOT}"
fi

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
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/
hdfs dfs -put ${HARP_DAAL_ROOT}/external/omp/libiomp5.so /Hadoop/Libraries/
hdfs dfs -put ${HARP_DAAL_ROOT}/external/hdfs/libhdfs.so* /Hadoop/Libraries/

## log directory
mkdir -p ${HADOOP_HOME}/Harp-DAAL-LOG
logDir=${HADOOP_HOME}/Harp-DAAL-LOG

export LIBJARS=${DAALROOT}/lib/daal.jar

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
# ompschedule=static
# ompschedule=dynamic
ompschedule=guided
ROTATION_PIP=true
# run exp multiple times
round=1
# input graph data
graph=sc-miami
# template file
template=u3-1.fascia
template_name=$(echo "$template" | cut -d'.' -f1 )

if [ "$ROTATION_PIP" == "false" ];then
	memjavaratio=0.6
else
	memjavaratio=0.2
fi

logName=Test-subgraph-N$Node-$template_name-$graph-Thd-$Thd-Core-$Core-$Affinity-Omp-$ompschedule-SendLimit-$SENDLIMIT-TaskLen-$NBRTASKLEN-Rotation-${ROTATION_PIP}-Itr-$ITR.log

echo "Start Test harp-daal-subgraph Node: $Node template: $template_name graph: $graph Thd: $Thd Core: $Core Affinity: $Affinity TPC: $tpc SENDLIMIT: $SENDLIMIT NBRTASKLEN: $NBRTASKLEN Rotation $ROTATION_PIP Itr: $ITR Round: $round"
hadoop jar harp-daal-app-1.0-SNAPSHOT.jar edu.iu.daal_subgraph.SCDaalLauncher -libjars ${LIBJARS} $Node $multiThd /Hadoop/sc-input/$template /Hadoop/sc-input/$graph /tmp/subgraph $Thd $Core $Affinity $ompschedule $tpc $Mem $memjavaratio $SENDLIMIT $NBRTASKLEN $ROTATION_PIP $ITR 2>&1 | tee $logDir/${logName}
echo "Finish Test harp-daal-subgraph Node: $Node template: $template_name graph: $graph Thd: $Thd Core: $Core Affinity: $Affinity TPC: $tpc SENDLIMIT: $SENDLIMIT NBRTASKLEN: $NBRTASKLEN Rotation $ROTATION_PIP Itr: $ITR Round: $round"



