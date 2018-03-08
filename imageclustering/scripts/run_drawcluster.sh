#!/bin/bash

#set the dataset root dir
YFCCDATA_DIR=/share/jproject/fg474/dataset/YFCC100M/
hdfsroot=/yfccfull/

#
#get the train model
#
hadoop fs -get $hdfsroot/centroids/out/output
mv output yfccfull.kmeans.model


#
#get a small subset of the raw feature files
#yfcc100m_dataset-0_batch_[0-9].txt, 10 files about 100K images
#
cp $YFCCDATA_DIR/features/vgg/yfcc100m_dataset-0/yfcc100m_dataset-0_batch_?.txt .

#
#feature transform
#
mkdir -p pca128
ls yfcc100m_dataset-0_batch_?.txt >sub10.lstfile
python ../scripts/pca2yfcc.py -predict sub10.lstfile pca128-50samples.pkl
cd pca128/
split -l 10000 sub10.lstfile-0000 -d lstfile-0000
for f in `ls lstfile-00000?`; 
    do python ../../scripts/kmpredict.py $f ../yfccfull.kmeans.model
done
cd ..

#
#get a subset of the clusters
#
#.cluster is the cluster id for each image
rm -f top10.cluster
for f in `ls pca128/*.cluster`; do 
    cat $f >> top10.cluster; 
done
sort -n top10.cluster |uniq -c |sort -n -k 1 >cluster.uniq
#get the last few of them
tail -11 cluster.uniq | head -10 >samples.cluster

#get hash id for each image
for f in `ls yfcc100m_dataset-0_batch_?.txt`; do gawk 'BEGIN{id=0}{printf("%d/%s\n",id%10,$1);id+=1;}' $f ; done >top10_hash.txt

#draw the clusters by download the corresponding images ondemand
mkdir -p image
for d in `seq 0 9`; do mkdir -p image/$d; done
python ../scripts/makefig.py image/ top10.cluster samples.cluster 7
#get image urls
python ../scripts/dumpbyret.py yfcc100m_dataset hash.ids
sh downloadbyret.sh
python ../scripts/makefig.py image/ top10.cluster samples.cluster 7

#top10.pdf is the final result
echo 'please read the final cluster samples in top10.pdf'



