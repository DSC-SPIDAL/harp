#!/bin/bash
#if [ $# -eq "0"  ] ; then
#	echo "Usage: run-validate.sh <bin>"
#	exit -1
#fi

bin=$1
if [ -z $tagname  ] ; then
    bin=../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release
fi

tagname=`basename $bin`
echo "run validate test with tagname=$tagname"
if [ ! -f $bin ]; then
	echo "Usage: run-validate.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

export RUNID=`date +%m%d%H%M%S`

#../bin/xgb-validation.sh ${bin} block 6
../bin/xgb-validation.sh ${bin} block 8
../bin/xgb-validation.sh ${bin} block 12
../bin/xgb-validation.sh ${bin} block 16

echo "====================================================="
echo "Validate Test RUNID=$RUNID:"
echo "====================================================="
find . -name "validation-auc*${RUNID}.csv" -exec cat {} \; |sort

