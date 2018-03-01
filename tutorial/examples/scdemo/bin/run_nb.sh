#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

python $homedir/src/demo_classification.py $homedir/data/20news/20news.svm

#python $homedir/src/make_rcv1.py
#python $homedir/src/demo_classification.py rcv1.svm
