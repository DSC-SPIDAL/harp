#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

#demo on 20news dataset

echo "demo data format converstion with 20news"

echo "get the raw dataset first"
python $homedir/src/make_20news.py

echo "convert between svm and sparse csr"
python $homedir/src/dataconv.py --from svm --to csr 20news.svm 20news
python $homedir/src/dataconv.py --from csr --to svm --labelfile 20news_labels.csv 20news_train_csr.csv 20news-svm.svm

echo "convert to dense matrix, it takes a long time....."
python $homedir/src/dataconv.py --from svm --to csv 20news.svm 20newsx
