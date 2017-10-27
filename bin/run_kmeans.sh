#!/bin/bash

trap "echo 'signal.....quit'; exit" SIGHUP SIGINT SIGTERM
homedir=`dirname $0`
homedir=`dirname $homedir`

echo $homedir

python $homedir/src/make_15scene.py --input $homedir/data/15scene/ --output 15scene


# run with local kmeans call
python $homedir/src/demo_kmeans.py 15scene.npz 10   

# run with harp dall kmeans call
# remove the line below when daal-kmeans call ready
# now there is just a mock call inside the demo_kmenas.py
cp $homedir/hadoop/hdfs/15scene/centroids/out/output .
python $homedir/src/demo_kmeans.py 15scene.npz 10 daal  


python $homedir/src/show_kmeans.py $homedir/data/15scene/ random.cluster 10

echo "now, please check the result sample file: random.pdf"
