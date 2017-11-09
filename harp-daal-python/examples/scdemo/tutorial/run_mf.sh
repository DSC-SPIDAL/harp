#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

banner()
{
echo ""
echo "===================================================================="
echo $1
echo "===================================================================="
}

runver='local'
if [ $# -eq "1" ] ; then
    if [ $1 == "-h" ]; then
        echo "Usage: run_mf.sh [daal]"
        exit 0
    else
        runver='daal'
    fi
fi


#
# prepare the dataset of Moivelens
#
banner "Get rating dataset..."
dataset=$HOME/.surprise_data/ml-1m/ml-1m
if [ ! -d $dataset ] ; then
    mkdir -p $dataset
    cp $homedir/data/movielens/surprise_data/* $dataset
    echo "load moivelens-1M dataset done!"
else
    echo "done already"
fi

#
# Run kmeans, 'local' or 'daal' version
#
banner "Train Recommender System by MatrixFactorization..."
python $homedir/tutorial/demo_mf_${runver}.py $dataset

#
# get result
#
banner "Done!"
