#!/bin/bash

if [ $# -eq "0" ] ; then
    echo "Usage: runfourclass.sh <bin> [params]"
    exit -1
fi

bin=$1
param=$2
param2=$3
conf=approx
runid=$bin-$conf

echo "./$bin fourclass_${conf}.conf $param $param2"
./$bin fourclass_${conf}.conf $param $param2

#get model name 00$num_round.model

model=`ls 0*.model`
echo "output model is $model"
mv $model ${runid}.model
./$bin fourclass_${conf}.conf task=pred model_in=${runid}.model nthread=24
mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile fourclass_test.csv

