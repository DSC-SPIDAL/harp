#!/bin/bash

#
# comparing the performance of original approx and optimized approx
#

conf=approx

#runs=(xgboost-orig-vtune xgboost-optapprox-binid)
runs=(xgboost-optapprox-binid xgboost-orig-vtune)

for bin in ${runs[*]}; do

runid=higgs_${conf}_optapprox_$bin
echo "==============================================================="
echo $runid

./$bin higgs_${conf}.conf 
mv 0050.model ${runid}.model
./$bin higgs_${conf}.conf task=pred model_in=${runid}.model
mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile higgs_test.csv



done





