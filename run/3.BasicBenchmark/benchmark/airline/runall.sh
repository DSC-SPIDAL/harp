
../bin/xgboost-orig airline_hist.conf
../bin/xgboost-orig airline_eact.conf


#inexac
../bin/daalgbt-icc-orig encoded_train.csv encoded_test.csv 300 2 690 6 1 0
#exact
../bin/daalgbt-icc-orig encoded_train.csv encoded_test.csv 300 2 690 6 0 0
