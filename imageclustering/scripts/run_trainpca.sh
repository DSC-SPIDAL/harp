#!/bin/bash

#set the dataset root dir
YFCCDATA_DIR=/share/jproject/fg474/dataset/YFCC100M/

#get the vgg feature files list
find $YFCCDATA_DIR -name ".txt" > yfcc-batchfiles.lst

#
#train a pca model by samples
#the whole dataset is about 100Mx4Kx8 = 3.2T Bytes in double precision
#you need a large enough cluster to run a distributed version of PCA
#otherwise, train a pca model on a subset of samples is just ok
#
python ../scripts/pca2yfcc.py -train yfcc-batchfiles.lst pca128-50samples.pkl


