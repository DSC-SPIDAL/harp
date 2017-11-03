#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

python $homedir/src/make_15scene.py --input $homedir/data/15scene/ --output 15scene


# run with local kmeans call
python $homedir/tutorial/demo_kmeans_local.py 15scene.npz 10   
python $homedir/tutorial/show_kmeans.py $homedir/data/15scene/ local.cluster 10

# run with harp dall kmeans call
python $homedir/tutorial/demo_kmeans_daal.py 15scene.npz 10
python $homedir/tutorial/show_kmeans.py $homedir/data/15scene/ daal.cluster 10

echo "now, please check the result sample file: local.pdf, daal.pdf"
