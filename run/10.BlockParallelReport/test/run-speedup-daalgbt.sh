#!/bin/bash

if [ $# -eq "0"  ] ; then
	echo "Usage: run-speedup.sh <bin>"
	exit -1
fi

bin=$1
binname=$(basename $bin)
if [ ! -f $bin ]; then
	echo "$bin not exist, quit"
	exit -1
fi

depths=(8 12 16)
thread=32

prefix=${binname}-t${thread}
output=SpeedUp-time-${prefix}.csv

for depth in ${depths[*]}; do
logfile=${prefix}-d${depth}
echo "$bin higgs_train.csv higgs_valid.csv 10 2 28 ${depth} 1 0 0.1 x 32" |tee ${logfile}.log
$bin higgs_train.csv higgs_valid.csv 10 2 28 ${depth} 1 0 0.1 x 32 2>&1 |tee -a ${logfile}.log

ret=`grep "Kernel execution time" ${logfile}.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`
echo ${logfile}, $ret >> $output

done


echo "================================"
echo " Speedup Test Results:"
echo "================================"
# binname, runid, trainingtime
echo -e "binname\trunid\ttrainingtime"
find . -name "SpeedUp*${binname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' |sort

