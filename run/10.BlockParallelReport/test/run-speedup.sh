#!/bin/bash

bin=$1
if [ -z $tagname  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release
fi

tagname=`basename $bin`

echo "run speedup test with tagname=$tagname"

if [ ! -f $bin ]; then
	echo "Usage: run-speedup.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

../bin/xgb-speedup.sh ${bin} higgs 10 8 block 32
../bin/xgb-speedup.sh ${bin} higgs 10 12 block 32
../bin/xgb-speedup.sh ${bin} higgs 10 16 block 32


#bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-${tagname}-release
#../bin/xgb-speedup.sh ${bin} synset 10 8 block 32 
#../bin/xgb-speedup.sh ${bin} synset 10 12 block 32 
#../bin/xgb-speedup.sh ${bin} synset 10 13 block 32 
#
#bin=../bin/xgboost-g++-omp-dense-nohalftrick-short-splitonnode-${tagname}-release
#../bin/xgb-speedup.sh ${bin} synset 10 8 block 32 
#../bin/xgb-speedup.sh ${bin} synset 10 12 block 32 
#../bin/xgb-speedup.sh ${bin} synset 10 13 block 32 

echo "================================"
echo " Speedup Test Results:"
echo "================================"
# binname, runid, trainingtime
echo -e "binname\trunid\ttrainingtime"
#find . -name "SpeedUp*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' |sort
ls -tr */SpeedUp*${tagname}*.csv | xargs cat | gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' 
