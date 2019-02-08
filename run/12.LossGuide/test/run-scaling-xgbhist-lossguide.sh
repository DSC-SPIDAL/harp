#!/bin/bash

bin=$1
if [ -z $bin  ] ; then
    bin=../bin/xgb-latest
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
../bin/xgb-strongscale.sh ${bin} higgs 10 8 hist 500000 1 0 8 lossguide
../bin/xgb-strongscale.sh ${bin} higgs 10 12 hist 500000 1 0 64 lossguide
../bin/xgb-strongscale.sh ${bin} higgs 10 16 hist 500000 1 0 64 lossguide

echo "================================"
echo " StrongScaling Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "StrongScale*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
ls -tr */StrongScale*${tagname}*${RUNID}.csv | xargs cat |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' 

