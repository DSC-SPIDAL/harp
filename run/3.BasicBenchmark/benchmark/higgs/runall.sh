
#./xgb higgs_exact.conf 
#cp 0300.model exact.model
#
#./xgb higgs_exact.conf task=pred model_in=0300.model
#python -m runner.runxgb --eval pred.txt --testfile higgs_test.csv
#
#./xgb higgs_hist.conf 
#cp 0300.model hist.model
#./xgb higgs_hist.conf task=pred model_in=0300.model
#python -m runner.runxgb --eval pred.txt --testfile higgs_test.csv
#
#
#./xgb higgs_approx.conf 
#cp 0300.model approx.model
#./xgb higgs_approx.conf task=pred model_in=0300.model
#python -m runner.runxgb --eval pred.txt --testfile higgs_test.csv
#
./daalgbt higgs_train.csv higgs_test.csv 300 2 28 6 1 0
cp daal-pred.txt daal-pred-10.txt
python -m runner.runxgb --eval daal-pred.txt --testfile higgs_test.csv

./daalgbt higgs_train.csv higgs_test.csv 300 2 28 6 0 0
cp daal-pred.txt daal-pred-00.txt
python -m runner.runxgb --eval daal-pred.txt --testfile higgs_test.csv

./daalgbt higgs_train.csv higgs_test.csv 300 2 28 6 1 1
cp daal-pred.txt daal-pred-11.txt
python -m runner.runxgb --eval daal-pred.txt --testfile higgs_test.csv

./daalgbt higgs_train.csv higgs_test.csv 300 2 28 6 0 1
cp daal-pred.txt daal-pred-01.txt
python -m runner.runxgb --eval daal-pred.txt --testfile higgs_test.csv

