#!/bin/bash

if [ $# -lt 2 ]; then
	echo "usage: run-prefetch.sh <dataset> <nthread> [tagname]"
	exit 0
fi

dataset=$1
nthread=$2
tagname=$3

output=runprefetch_result.csv


runs=(nohalftrick-noprefetch nohalftrick-prefetch halftrick-noprefetch halftrick-prefetch)

for runid in ${runs[*]}; do

run=$runid$tagname

if [ ! -f ../bin/xgboost-g++-omp-$run ]; then
	echo "../bin/xgboost-g++-omp-$run not exist, quit"
	exit -1
fi

conf=pmatfasthist
if [ ! -f runprefetch-${run}_${dataset}_${conf}.log ]; then
../bin/xgboost-g++-omp-$run $dataset.conf tree_method=${conf} nthread=$nthread num_round=100 2>&1 |tee runprefetch-${run}_${dataset}_${conf}.log
fi


ret=`tail -7 runprefetch-${run}_${dataset}_${conf}.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`
echo $run,$conf,$ret >> $output

conf=hist
if [ ! -f runprefetch-${run}_${dataset}_${conf}.log ]; then
../bin/xgboost-g++-omp-$run $dataset.conf tree_method=${conf} nthread=$nthread num_round=100 2>&1 |tee runprefetch-${run}_${dataset}_${conf}.log
fi

ret=`tail -7 runprefetch-${run}_${dataset}_${conf}.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`
echo $run,$conf,$ret >> $output


done

echo >> $output
echo >> $output

#tail -n 4 runprefetch-*.log

