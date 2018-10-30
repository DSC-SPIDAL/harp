#!/bin/bash

#
# comparing the performance of original approx and optimized approx
#

confs=(approx binidcache pmatcompact)

#runs=(xgboost-orig-vtune xgboost-optapprox-binid)
#runs=(xgboost-optapprox-binid xgboost-orig-vtune)
bin=xgboost-optapprox-binid

#for bin in ${runs[*]}; do
for conf in ${confs[*]}; do

runid=higgs_${conf}_optapprox_$bin
echo "==============================================================="
echo $runid

echo "./$bin higgs_${conf}.conf $1 $2"
./$bin higgs_${conf}.conf $1 $2

model=`ls 0*.model`
echo "output model is $model"
mv $model ${runid}.model

./$bin higgs_${conf}.conf task=pred model_in=${runid}.model
mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile higgs_test.csv



done





