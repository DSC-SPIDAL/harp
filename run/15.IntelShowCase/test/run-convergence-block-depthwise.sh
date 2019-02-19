#!/bin/bash

bin=$1
if [ -z $bin  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-criteo-release
fi

tagname=`basename $bin`

if [ ! -f $bin ]; then
	echo "Usage: run-convergence.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

echo "run scaling test with tagname=$tagname"

export RUNID=`date +%m%d%H%M%S`
../bin/xgb-convergence.sh ${bin} criteometa 300 8 lossguide 32 1562500 33 0 8 depth data_parallelism=1 group_parallel_cnt=32 topk=0 async_mixmode=2 loadmeta=criteometa
#../bin/xgb-convergence.sh ${bin} criteometa 300 12 lossguide 32 1562500 33 0 8 depth data_parallelism=1 group_parallel_cnt=32 topk=0 async_mixmode=2 loadmeta=criteometa
#../bin/xgb-convergence.sh ${bin} criteometa 300 16 lossguide 32 1562500 33 0 32 depth data_parallelism=1 group_parallel_cnt=32 topk=0 async_mixmode=2 loadmeta=criteometa

echo "================================"
echo " Convergence Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "Convergence*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
echo "ls -tr */Convergence*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}'" 
ls -tr */Convergence*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' 

