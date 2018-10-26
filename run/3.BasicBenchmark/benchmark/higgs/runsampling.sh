#!/bin/bash


conf=hist

rows=(0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 1.0)
cols=(0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 1.0)

for rowsample in ${rows[*]}; do
for colsample in ${cols[*]}; do


runid=higgs_${conf}_r${rowsample}_c${colsample}
echo "==============================================================="
echo $runid

./xgb higgs_hist.conf subsample=$rowsample colsample_bytree=$colsample
mv 0300.model ${runid}.model
./xgb higgs_hist.conf task=pred model_in=${runid}.model
mv pred.txt ${runid}.pred.txt
python -m runner.runxgb --eval ${runid}.pred.txt --testfile higgs_test.csv


done
done






