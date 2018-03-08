#!/bin/bash

#set the dataset root dir
YFCCDATA_DIR=/share/jproject/fg474/dataset/YFCC100M/

#get the vgg feature files list
find $YFCCDATA_DIR -name ".txt" > yfcc-batchfiles.lst

#
#transform the dataset by appling a trained pca model
#you can parallell this process by split the lstfile
#and run this scripts with multiple instances
#
lstfile=yfcc-batchfiles.lst
outdir=pca128
mkdir -p $outdir
python ../scripts/pca2yfcc.py -predict $lstfile pca128-50samples.pkl


