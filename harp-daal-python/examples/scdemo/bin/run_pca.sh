#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

#python $homedir/src/make_lfw.py
python $homedir/src/demo_pca.py $homedir/data/lfw/lfw_people.npz


