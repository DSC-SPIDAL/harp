#!/bin/bash

bin=$1
if [ -z $bin  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unity-release
fi

tagname=`basename $bin`

if [ ! -f $bin ]; then
	echo "Usage: run-scaling.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

echo "run scaling test with tagname=$tagname"

export RUNID=`date +%m%d%H%M%S`

#"Usage: xgb-strongscale.sh <bin> <dataset> <iter> <maxdepth> <tree_method> <row_blksize> <ft_blksize> <bin_blksize> <node_block_size> <growth_policy> <runid>"
../bin/xgb-strongscale.sh ${bin} criteometa 10 8 lossguide 1562500 33 0 8 lossguide data_parallelism=1 group_parallel_cnt=32 topk=1 async_mixmode=2 loadmeta=criteometa
#../bin/xgb-strongscale.sh ${bin} criteometa 10 12 lossguide 1562500 33 0 32 lossguide data_parallelism=1 group_parallel_cnt=32 topk=32 async_mixmode=1 loadmeta=criteometa
#../bin/xgb-strongscale.sh ${bin} criteometa 10 16 lossguide 1562500 33 0 32 lossguide data_parallelism=1 group_parallel_cnt=32 topk=32 async_mixmode=1 loadmeta=criteometa


echo "================================"
echo " StrongScaling Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "StrongScale*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
echo "ls -tr */StrongScale*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' "
ls -tr */StrongScale*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' 

