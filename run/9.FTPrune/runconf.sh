#!/bin/bash

if [ $# -eq "0" ] ; then
    echo "Usage: runconf.sh <bin> <conf> [params]"
    exit -1
fi

bin=$1
conf=$2
param1=$3
param2=$4
param3=$5
param4=$6
runid=$bin-$conf-$param1-$param2-$param3-$param4

#init env
. ~/hpda/gbt-test/bin/init_env.sh
mkdir -p tmp
mv 0*.model tmp 2>/dev/null
logfile=${runid}.log

#run training
echo "./$bin ${conf}.conf $param1 $param2 $param3 $param4" 2>&1 |tee $logfile
./$bin ${conf}.conf $param1 $param2 $param3 $param4 2>&1 |tee -a $logfile

#predict
model=`ls 0*.model`
echo "output model is $model" 2>&1 |tee -a $logfile

mv $model ${runid}.model
./$bin ${conf}.conf task=pred model_in=${runid}.model nthread=24 2>&1 |tee -a $logfile

mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile higgs_test.csv 2>&1 |tee -a $logfile

