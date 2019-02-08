#!/bin/bash
bin=$1
if [ -z $bin  ] ; then
    bin=../bin/lightgbm
fi

tagname=`basename $bin`

if [ ! -f $bin ]; then
	echo "Usage: run-convergence.sh <bin>"
	echo "$bin not exist, quit"
	exit -1
fi

echo "run scaling test with tagname=$tagname"

export RUNID=`date +%m%d%H%M%S`

../bin/lightgbm-convergence.sh ${bin} higgs 300 8 feature 32
#../bin/lightgbm-convergence.sh ${bin} higgs 300 12 feature 32
#../bin/lightgbm-convergence.sh ${bin} higgs 300 16 feature 32

echo "================================"
echo " Convergence Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
#find . -name "Convergence*${tagname}*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort
ls -tr */Convergence*${tagname}*${RUNID}.csv | xargs cat

