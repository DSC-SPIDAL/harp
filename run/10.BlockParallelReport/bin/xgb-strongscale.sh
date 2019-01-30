#!/bin/bash
save()
{
ret=`tail -13 $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s,",$1)}'`

echo $1,$ret >> $output

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
	echo "Usage: xgb-strongscale.sh <bin> <dataset> <iter> <maxdepth> <tree_method> <row_blksize> <ft_blksize> <bin_blksize> <runid>"
	echo "	$RUNID	; evn variable to set the runid for a group of testing"
    exit -1
fi


check_init

appname=StrongScale

bin=$1
num_round=10
max_depth=8
dataset=higgs
tree_method=blockdense
bin_blksize=0
ft_blksize=1
#row_blksize=500000
row_blksize=0

runid=20190101
if [ -z "$RUNID"  ] ; then
	runid=`date +%m%d%H%M%S`
else
	runid=$RUNID
fi


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
	row_blksize=$6
fi
if [ ! -z $7 ]; then
	ft_blksize=$7
fi
if [ ! -z $8 ]; then
	bin_blksize=$8
fi

if [ ! -z $9 ]; then
	runid=$9
fi

#bin=../bin/xgb-fnb
binname=`basename $bin`

conf=${dataset}.conf
threads=(1 8 16 24 32 40 48)
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}

if [ ! -f $conf ]; then
	echo "$conf not found, quit"
	exit -1
fi

echo "start test: $prefix"


for thread in ${threads[*]}; do

	#training
	logfile=${prefix}-t${thread},${thread},${runid}
	echo "$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize}" | tee ${logfile}.log
	$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} 2>&1 | tee -a ${logfile}.log

	# save timing results
	output=${appname}-time-${prefix}-${runid}.csv
	save $logfile

done

mkdir -p ${appname}-${prefix}
mv ${prefix}* ${appname}-${prefix}
mv ${appname}-*${prefix}*.csv ${appname}-${prefix}


echo "result dir: StrongScale-${prefix}"
