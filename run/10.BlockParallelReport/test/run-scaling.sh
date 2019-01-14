#!/bin/bash

if [ $# -eq "0"  ] ; then
	echo "Usage: run-scaling.sh <bin>"
	exit -1
fi

tagname=$1
if [ -z $tagname  ] ; then
	tagname=byte-blockdense-longcube-threadinit
fi

echo "run scaling test with tagname=$tagname"

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


bin=../bin/xgboost-g++-omp-halftrick-noprefetch-${tagname}-release
../bin/xgb-strongscale.sh ${bin} higgs 10 8 blockdense 
../bin/xgb-strongscale.sh ${bin} synset 10 8 blockdense 

bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-${tagname}-release
../bin/xgb-strongscale.sh ${bin} higgs 10 8 blockdense 
../bin/xgb-strongscale.sh ${bin} synset 10 8 blockdense 

echo "================================"
echo " StrongScaling Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
find . -name "StrongScale*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort


