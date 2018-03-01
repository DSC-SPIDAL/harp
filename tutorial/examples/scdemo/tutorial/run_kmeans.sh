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

#
# Extract features from the raw jpg dataset of 15scene
#

banner "Extract features from the raw images..."
if [ ! -f $homedir/data/15scene/15scene.sbow.mat ] ; then
    pushd $homedir/tool/devkit-1.0/feature
    #./extract_sbow.sh 15scene/15scene.imglst 15scene/15scene.idlst 15scene
    #cp 15scene.sbow.mat $homedir/data/15scene/
    popd
    echo "done"
else
    echo "done already"
fi

#
# convert .mat format into numpy format
#
python $homedir/src/make_15scene.py --input $homedir/data/15scene/ --output 15scene


#
# Run kmeans, 'local' or 'daal' version
#
banner "Train KMeans on the feature dataset..."
python $homedir/tutorial/demo_kmeans_${runver}.py 15scene.npz 10   

#
# get result
#
banner "Evaluate and Check the clustering result..."
python $homedir/tutorial/show_kmeans.py $homedir/data/15scene/ ${runver}.cluster 10

banner "Done! Result sample file: ${runver}.pdf"
