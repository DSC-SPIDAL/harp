#!/bin/bash

save()
{
ret=`tail -13 $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`

echo $1,$ret >> $output

#echo >> $output
#echo >> $output
}

saveauc()
{
ret=`grep "test-auc" $1.log | grep "test-auc" | grep -Po "\[[0-9]*\].*[0-9]*\.[0-9]*" | sed 's/\[//g' | sed 's/\]//' |sed 's/test-auc:/,/'`

echo $1 > $output
#echo $1,$ret >> $output
grep "test-auc" $1.log | grep "test-auc" | grep -Po "\[[0-9]*\].*[0-9]*\.[0-9]*" | sed 's/\[//g' | sed 's/\]//' |sed 's/test-auc:/,/' | gawk '{print $1+1, $2}' >> $output

#echo >> $output
#echo >> $output
}


check_init()
{
	if [ -z "$_gbtproject_" ]; then
		echo "_gbtproject_ env var not set, init the project first please, quit."
		exit -1
	fi
}


if [ $# -eq "0" ] ; then
	echo "Usage: xgb-convergence.sh <bin> <dataset> <iter> <maxdepth> <tree_method> <row_blksize> <ft_blksize> <bin_blksize> <node_block_size> <growth_policy> <runids>"
	echo "	$RUNID	; evn variable to set the runid for a group of testing"
    exit -1
fi

check_init

appname=Convergence

bin=$1
num_round=10
max_depth=6
dataset=higgs
tree_method=block
thread=32
bin_blksize=0
ft_blksize=1
#row_blksize=500000
row_blksize=0
node_blksize=65536
grow_policy=depth

runtag=20190101
if [ -z "$RUNID"  ] ; then
	runtag=`date +%m%d%H%M%S`
else
	runtag=$RUNID
fi
runids=($runtag)


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

shift 6
if [ ! -z $1 ]; then
	row_blksize=$1
fi
if [ ! -z $2 ]; then
	ft_blksize=$2
fi
if [ ! -z $3 ]; then
	bin_blksize=$3
fi
if [ ! -z $4 ]; then
	node_blksize=$4
fi
if [ ! -z $5 ]; then
	grow_policy=$5
fi

if [ ! -z $6 ]; then
	shift 5
	runids=( $@ )
fi


echo "row_blksize=${row_blksize},ft=${ft_blksize},bin=${bin_blksize},node=${node_blksize}"
echo "runids=${runids[*]}"
#exit 0

#bin=../bin/xgb-fnb
binname=`basename $bin`

conf=${dataset}_eval.conf
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-t${thread}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}-x${node_blksize}-g${grow_policy}-${runtag}

if [ ! -f $conf ]; then
	echo "$conf not found, quit"
	exit -1
fi

echo "start test: $prefix"


for runid in ${runids[*]}; do


	logfile=${prefix},${runid}

	if [ "$grow_policy" = "depth" ] ; then
	# traing with depthwise
	echo "$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} node_block_size=${node_blksize}" | tee ${logfile}.log
	$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} node_block_size=${node_blksize} 2>&1 | tee -a ${logfile}.log
    else

	# traing with lossguide

	num_leaves=$(echo "2^$max_depth" | bc -l)

	echo "$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=0 bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} node_block_size=${node_blksize} grow_policy=lossguide max_leaves=${num_leaves}" | tee ${logfile}.log
 
	$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=0 bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} node_block_size=${node_blksize} grow_policy=lossguide max_leaves=${num_leaves} 2>&1 | tee -a ${logfile}.log


    fi
	
	# save model
    #mv "00${num_round}.model" ${logfile}.model

	# save timing results
	output=${appname}-time-${prefix}-${runid}.csv
	save $logfile

    # save auc results
	output=${appname}-auc-${prefix}-${runid}.csv
	saveauc $logfile



done

mkdir -p ${appname}-${prefix}
mv ${prefix}* ${appname}-${prefix}
mv ${appname}-*${prefix}*.csv ${appname}-${prefix}


echo "result dir: ${appname}-${prefix}"
