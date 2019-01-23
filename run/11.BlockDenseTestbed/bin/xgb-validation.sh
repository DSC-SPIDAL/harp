#!/bin/bash

validate()
{
#truth="0.821590"
ret=`tail -1 $1.log |grep -Po "[0-9]*\.[0-9]*" | gawk '{printf("%s",$1)}'`
echo "AUC=$ret"

pass="Fail"
if (( $(echo  "$ret >= $truth" |bc -l)  )) ; then
	pass="Pass"
	echo "pass!"
else
	echo "!!!!!!!!!!!Failed, AUC=$ret, not $truth"
fi

# save result
echo "$1,AUC:,$ret,Truth:,$truth,$pass" >> $output

}

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
	echo "Usage: xgb-validation.sh <bin> <tree_method> <depth>"
	echo "	$RUNID	; evn variable to set the runid for a group of testing"
    exit -1
fi

check_init

appname=validation

bin=$1
num_round=10
max_depth=16
dataset=higgs
tree_method=blockdense
thread=32
bin_blksize=0
ft_blksize=1
#row_blksize=500000
row_blksize=0

if [ ! -z "$2"  ] ; then
	tree_method=$2
fi
if [ ! -z "$3"  ] ; then
	max_depth=$3
fi

#get truth
truth=0
case $max_depth in
	6)
		truth="0.771113"
		;;
	8)
		truth="0.787664"
		;;
	12)
		truth="0.809008"
		;;
	16)
		truth="0.821590"
		;;
	*)
		echo "max_depth=$max_depth not support, quit"
		exit 0
		;;
esac

	
runid=20190101
if [ -z "$RUNID"  ] ; then
	runid=`date +%m%d%H%M%S`
else
	runid=$RUNID
fi


binname=`basename $bin`
conf=${dataset}.conf
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-t${thread}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}

if [ ! -f $conf ]; then
	echo "$conf not found, quit"
	exit -1
fi

echo "start test: $prefix"

logfile=${prefix},${runid}

# run trainer
echo "$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize}" | tee ${logfile}.log
$bin $conf num_round=${num_round} nthread=${thread} tree_method=${tree_method} max_depth=${max_depth} bin_block_size=${bin_blksize} ft_block_size=${ft_blksize} row_block_size=${row_blksize} 2>&1 | tee -a ${logfile}.log

if [ "$?" -ne "0" ]; then
	echo "run trainer fails, quit"
	exit -1
fi

# save timeinfo
output=${appname}-time-${prefix}-${runid}.csv
save $logfile

# save model
mv "00${num_round}.model" ${logfile}.model

# evaluation on the model
$bin $conf task=pred model_in=${logfile}.model
python -m runner.runxgb --eval pred.txt --testfile higgs_test.csv 2>&1 |tee ${logfile}-eval.log
rm pred.txt

# validate and save result
output=${appname}-auc-${prefix}-${runid}.csv
validate ${logfile}-eval

mkdir -p ${appname}-${prefix}
mv ${prefix}* ${appname}-${prefix}
mv ${appname}-*${prefix}*.csv ${appname}-${prefix}

echo "result dir: ${appname}-${prefix}"
