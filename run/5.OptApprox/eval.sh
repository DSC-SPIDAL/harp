#!/bin/bash

if [ $# -eq "0" ] ; then
    echo "Usage: runconf.sh <bin> <conf> [params]"
    exit -1
fi

bin=$1
conf=$2
param=$3
param2=$4
runid=$bin-$conf

echo "./$bin higgs_${conf}.conf $param $param2"
#./$bin higgs_${conf}.conf $param $param2

#get model name 00$num_round.model

model=`ls 0*.model`
echo "output model is $model"
mv $model ${runid}.model
./$bin higgs_${conf}.conf task=pred model_in=${runid}.model nthread=24
mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile higgs_test.csv
