
if [ $# -eq 0 ]; then
	echo "run-distlightgbm.sh <nodenum> <dataset> <eval>"
	exit 0
fi


num_machines=$1
dataset=$2
eval=$3

if [ -z $dataset ]; then
dataset=higgs
fi

iter=10
thread=32
tree_method=data


bin=./lightgbm-mpi
appname=Speedup
if [ -z $eval ]; then
	conf=distlightgbm_${dataset}.conf
else
	conf=distlightgbm_${dataset}_eval.conf
fi


prefix=distlightgbm-d${dataset}-n${num_machines}-i${iter}-t${thread}-m${tree_method}

RUNID=`date +%m%d%H%M%S`

logfile=${prefix}-$RUNID

echo "mpirun --hostfile hostfile -np ${num_machines} $bin config=${conf} num_trees=${iter} nthread=${thread} " 2>&1 | tee $logfile
mpirun --hostfile hostfile -np ${num_machines} $bin config=${conf} num_trees=${iter} nthread=${thread} 2>&1 |tee -a $logfile

