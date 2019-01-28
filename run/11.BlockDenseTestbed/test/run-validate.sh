#!/bin/bash
if [ $# -eq "0"  ] ; then
	echo "Usage: run-validate.sh <bin> <sparse|dense>"
	exit -1
fi

tagname=$1
if [ -z $tagname  ] ; then
	tagname=block
fi
dense=$2
if [ -z $dense  ] ; then
	dense=dense
fi

echo "run validate test with tagname=$tagname"
bin=../bin/xgboost-g++-omp-${dense}-halftrick-short-splitonnode-${tagname}-release


if [ ! -f $bin ]; then
	echo "$bin not exist, quit"
	exit -1
fi
export RUNID=`date +%m%d%H%M%S`

../bin/xgb-validation.sh ${bin} block 6
../bin/xgb-validation.sh ${bin} block 8
../bin/xgb-validation.sh ${bin} block 12
../bin/xgb-validation.sh ${bin} block 16

echo "====================================================="
echo "Validate Test RUNID=$RUNID:"
echo "====================================================="
find . -name "validation-auc*${tagname}*${RUNID}*.csv" -exec cat {} \; |sort

