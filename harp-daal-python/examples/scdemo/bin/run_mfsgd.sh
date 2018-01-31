#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

#
# current demo output performance evaluations
# TODO: add prediction on new userid 
#
#python $homedir/src/make_movielens.py
python $homedir/src/demo_mf.py
