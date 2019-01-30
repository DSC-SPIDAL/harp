#!/bin/bash

bin=$1
if [ -z $tagname  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release
fi

tagname=`basename $bin`

if [ ! -f $bin ]; then
	echo "Usage: run-scaling.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

echo "run scaling test with tagname=$tagname"

export RUNID=`date +%m%d%H%M%S`

../bin/xgb-strongscale.sh ${bin} higgs 10 8 block 
../bin/xgb-strongscale.sh ${bin} higgs 10 12 block 
../bin/xgb-strongscale.sh ${bin} higgs 10 16 block 
#../bin/xgb-strongscale.sh ${bin} synset 10 8 block 

#bin=../bin/xgboost-g++-omp-dense-nohalftrick-short-splitonnode-${tagname}-release
#../bin/xgb-strongscale.sh ${bin} higgs 10 8 block 
#../bin/xgb-strongscale.sh ${bin} synset 10 8 block 

echo "================================"
echo " StrongScaling Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "StrongScale*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
ls -tr */StrongScale*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' 

