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
        echo "Usage: run_kmeans.sh [daal]"
        exit 0
    else
        runver='daal'
    fi
fi

#banner "Introduction"
#python $homedir/tutorial/demo_nb_${runver}.py --intro


#
# prepare the dataset of 20news
#
banner "Extract features from the raw dataset..."
dataset=$homedir/data/20news/20news_2k.svm
if [ ! -f $dataset ] ; then
    python $homedir/src/make_20news.py
    cp 20news_2k.svm $homedir/data/20news/
    echo "done"
else
    echo "done already"
fi

#
# Run kmeans, 'local' or 'daal' version
#
banner "Train NaiveBayes on the feature dataset..."
python $homedir/tutorial/demo_nb_${runver}.py $dataset

#
# get result
#
banner "Done! Result sample file: naivebayes_perf_${runver}.png"
