#!/bin/bash

save()
{
#[LightGBM] [Info] 50.857357 seconds elapsed, finished iteration 100
	#ret=`grep "finished iteration 10" $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`
	ret=`grep "finished iteration $num_round" $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`

	echo $1,$ret >> $output
}

if [ $# -eq "0" ] ; then
	echo "Usage: lightgbm-scaling.sh <bin> <dataset> <iter> <maxdepth> <tree_method> <runid>"
    exit -1
fi

appname=StrongScale

bin=$1
num_round=10
max_depth=6
dataset=higgs
tree_method=feature
thread=32
runid=0
#echo "runids=${runids[*]}"

if [ ! -z $2 ]; then
	dataset=$2
fi
if [ ! -z $3 ]; then
	num_round=$3
fi
if [ ! -z $4 ]; then
	max_depth=$4
fi
if [ ! -z $5 ]; then
	tree_method=$5
fi
if [ ! -z $6 ]; then
	thread=$6
fi
if [ ! -z $7 ]; then
	shift 6
	runids=( $@ )
fi

binname=`basename $bin`

conf=lightgbm_${dataset}.conf
threads=(1 8 16 24 32 40 48)
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}

if [ ! -f $conf ]; then
	echo "$conf not found, quit"
	exit -1
fi

echo "start test: $prefix"


for thread in ${threads[*]}; do


	logfile=${prefix}-t${thread},${thread},${runid}

	# traing
	num_leaves=$(echo "2^$max_depth" | bc -l)
	echo "$bin config=$conf num_trees=${num_round} nthread=${thread} tree_learner=${tree_method} num_leaves=${num_leaves}" | tee ${logfile}.log
	$bin config=$conf num_trees=${num_round} nthread=${thread} tree_learner=${tree_method} num_leaves=${num_leaves} 2>&1 | tee -a ${logfile}.log
	
	# save model
	mv LightGBM_model.txt ${logfile}.model

	# save timing results
	output=${appname}-time-${prefix}-${runid}.csv
	save $logfile

done

mkdir -p ${appname}-${prefix}
mv ${prefix}* ${appname}-${prefix}
mv ${appname}-*${prefix}*.csv ${appname}-${prefix}


echo "result dir: ${appname}-${prefix}"


