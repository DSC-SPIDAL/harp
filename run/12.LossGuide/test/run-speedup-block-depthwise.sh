#!/bin/bash

bin=$1
if [ -z $tagname  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-lossguide-release
fi

tagname=`basename $bin`

echo "run speedup test with tagname=$tagname"

if [ ! -f $bin ]; then
	echo "Usage: run-speedup.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

export RUNID=`date +%m%d%H%M%S`
#"Usage: xgb-strongscale.sh <bin> <dataset> <iter> <maxdepth> <tree_method> <row_blksize> <ft_blksize> <bin_blksize> <node_block_size> <growth_policy> <runid>"
../bin/xgb-speedup.sh ${bin} higgs 10 8 lossguide 32 500000 1 0 65536
../bin/xgb-speedup.sh ${bin} higgs 10 12 lossguide 32 500000 1 0 65536
../bin/xgb-speedup.sh ${bin} higgs 10 16 lossguide 32 500000 1 0 65536


echo "================================"
echo " Speedup Test Results:"
echo "================================"
# binname, runid, trainingtime
echo -e "binname\trunid\ttrainingtime"
#find . -name "SpeedUp*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' |sort
echo "ls -tr */SpeedUp*${tagname}*${RUNID}.csv | xargs cat | gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' "
ls -tr */SpeedUp*${tagname}*${RUNID}*.csv | xargs cat | gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' 
