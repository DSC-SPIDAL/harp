#!/bin/bash

bin=$1
if [ -z $tagname  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release
fi

tagname=`basename $bin`

if [ ! -f $bin ]; then
	echo "Usage: run-convergence.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

echo "run scaling test with tagname=$tagname"

export RUNID=`date +%m%d%H%M%S`

../bin/xgb-convergence.sh ${bin} higgs 300 8 block 32
#../bin/xgb-convergence.sh ${bin} higgs 300 12 block 32
#../bin/xgb-convergence.sh ${bin} higgs 300 16 block 32


echo "================================"
echo " Convergence Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "Convergence*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
ls -tr */Convergence*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' 

