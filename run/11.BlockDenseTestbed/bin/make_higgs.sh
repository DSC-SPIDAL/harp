#bash $_gbtproject_/sub/benchm-ml/x1-data-higgs/1-getdata.txt

wget archive.ics.uci.edu/ml/machine-learning-databases/00280/HIGGS.csv.gz
gunzip HIGGS.csv.gz

head -10000000 HIGGS.csv > train.csv
tail -1000000 HIGGS.csv > test.csv
#
# outputs: higgs_xx.csv, higgs_xx.libsvm
#
python -m runner.higgs --trainfile train.csv --testfile test.csv
head -1 higgs_train.libsvm >higgs_valid.libsvm
rm -f train.csv test.csv HIGGS.csv


