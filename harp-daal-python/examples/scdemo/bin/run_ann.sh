#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

#
# current demo output performance evaluations
#
cp $homedir/data/mnist8m/test-mnist8m.scale.bz2 .
bzip2 -d *.bz2
python $homedir/src/demo_ann.py
