#!/bin/bash

log()
{
	echo $1 |tee -a $resultfile
}

if [ $# -eq "0" ] ; then
	echo "Usage: run-benchmark.sh <bin> <max_depth>"
    exit -1
fi

bin=$1


num_round=10
max_depth=8
dataset=higgs
tree_method=blockdense
thread=32
bin_blksize=0
ft_blksize=1
row_blksize=500000

if [ ! -z $2 ]; then
	max_depth=$2
fi

runid=BENCHMARK
binname=`basename $bin`
mkdir -p Benchmark
resultfile=Benchmark/Benchmark-${binname}-d${max_depth}.log

if [ -f $resultfile ]; then
	echo "benchmark result exists already, quit"
	cat $resultfile
	exit 0
fi


#
# run validation
#
appname=validation
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-t${thread}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}
output=${appname}-auc-${prefix}-${runid}.csv
outdir=${appname}-${prefix}
log "====================================="
if [ -f ${outdir}/${output} ]; then
	log " Validation Result Exists already, skip test:"
else
	export RUNID=$runid
	log " Run Validation ..."
	../bin/xgb-validation.sh ${bin} ${tree_method} ${max_depth} 1>/dev/null 2>/dev/null


	log " Validation Result:"
fi
log "====================================="
cat ${outdir}/${output} |tee -a $resultfile


#
# run speedup
#
appname=SpeedUp
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-t${thread}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}
output=${appname}-time-${prefix}-${runid}.csv
outdir=${appname}-${prefix}
log "====================================="
if [ -f ${outdir}/${output} ]; then
	log " Speedup Result Exists already, skip test:"
else
	export RUNID=$runid
	log " Run Speedup ..."
	../bin/xgb-speedup.sh ${bin} ${dataset} ${num_round} ${max_depth} ${tree_method} ${thread} ${row_blksize} ${ft_blksize} ${bin_blksize} ${runid} 1>/dev/null 2>/dev/null

	log " Speedup Result:"
fi
log "====================================="
cat ${outdir}/${output} |gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' |sort|tee -a $resultfile



#
# run scaling
#
appname=StrongScale
prefix=$binname-${dataset}-n${num_round}-d${max_depth}-m${tree_method}-b${bin_blksize}-f${ft_blksize}-r${row_blksize}
output=${appname}-time-${prefix}-${runid}.csv
outdir=${appname}-${prefix}
log "====================================="
if [ -f ${outdir}/${output} ]; then
	log " StrongScaling Result Exists already, skip test:"
else
	export RUNID=$runid
	log " Run Strong Scaling ..."
	../bin/xgb-strongscale.sh ${bin} ${dataset} ${num_round} ${max_depth} ${tree_method} ${row_blksize} ${ft_blksize} ${bin_blksize} ${runid} 1>/dev/null 2>/dev/null 
	log " StrongScaling Result:"
fi
log "====================================="
cat ${outdir}/${output} |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort -k 2 -n |tee -a $resultfile



