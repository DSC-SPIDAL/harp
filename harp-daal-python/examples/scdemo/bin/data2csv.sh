#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

cp $homedir/data/mnist8m/test-mnist8m.scale.bz2 .
bzip2 -d test-mnist8m.scale.bz2 
python $homedir/src/data2csv.py --input test-mnist8m.scale --output mnist8m
python $homedir/src/data2csv.py --input $homedir/data/lfw/lfw_people.npz --output lfw_people --type npz --split 0
python $homedir/src/data2csv.py --input $homedir/data/15scene/15scene.npz --output 15scene --type npz --split 1
