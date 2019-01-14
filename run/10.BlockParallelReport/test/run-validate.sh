#!/bin/bash
if [ $# -eq "0"  ] ; then
	echo "Usage: run-validate.sh <bin>"
	exit -1
fi

tagname=$1
if [ -z $tagname  ] ; then
	tagname=byte-blockdense-longcube-threadinit
fi

echo "run validate test with tagname=$tagname"
bin=../bin/xgboost-g++-omp-halftrick-noprefetch-${tagname}-release
if [ ! -f $bin ]; then
	echo "$bin not exist, quit"
	exit -1
fi
bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-${tagname}-release
if [ ! -f $bin ]; then
	echo "$bin not exist, quit"
	exit -1
fi



export RUNID=`date +%m%d%H%M%S`

bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-${tagname}-release
../bin/xgb-validation.sh ${bin} blockdense 6
../bin/xgb-validation.sh ${bin} blockdense 8
../bin/xgb-validation.sh ${bin} blockdense 12
../bin/xgb-validation.sh ${bin} blockdense 16

bin=../bin/xgboost-g++-omp-halftrick-noprefetch-${tagname}-release
../bin/xgb-validation.sh ${bin} blockdense 6
../bin/xgb-validation.sh ${bin} blockdense 8
../bin/xgb-validation.sh ${bin} blockdense 12
../bin/xgb-validation.sh ${bin} blockdense 16

echo "====================================================="
echo "Validate Test RUNID=$RUNID:"
echo "====================================================="
find . -name "validation-auc*${tagname}*.csv" -exec cat {} \; |sort

