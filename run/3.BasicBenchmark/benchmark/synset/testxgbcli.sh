mkdir 1m50
cd 1m50
ln -s ../train-1000000m-50.csv train.csv
ln -s ../test-1000000m-50.csv test.csv

python -m runner.savelibsvm

#output the .libsvm files
head -1 train.libsvm >valid.libsvm

#run xgb
../../bin/xgboost-orig exact.conf


