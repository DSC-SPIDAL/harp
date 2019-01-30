Block Based Parallelism Report
=================================

@01072019

slides and experiments results in "doc/meeting/0107-BlockParallelism"

online doc: [slides](https://docs.google.com/presentation/d/1iI4aKmvN92L_Y5Tlm_7ElB_InYVWTf8ECB4Z9t8b8ms/edit?usp=sharing),[excel](https://docs.google.com/spreadsheets/d/1VTqkyGIVRo1wxgf-K7tbkKPywSulBRonfY0n72L_mm0/edit?usp=sharing)


## Experiments

### 1. compile

Skip this section and go to section 2 if you want to use the binary pre-compiled.

Add tag to name of the binary file to indicate different features.

```
# init
source WHEREYOURREPOROOT/gbt-test/bin/init_env.sh 

# got to directory: sub/xgb-optapprox
# this cmd will generate differ release versions, 
#   with or w/o halftrick
#   dense or sparse input support
#   byte or short blkaddress(byte only for block size<256)
# with the tag name "block"

cd $_gbtproject_/sub/xgb-optapprox
./makeall.sh block

# 
# for xgb-latest
#
cd $_gbtproject_/sub/xgb-latest
make -j 24

#
# for lightgbm
#
cd $_gbtproject_/sub/lightgbm
mkdir -p build
cd build
cmake ..
make -j 24 

```


### 2. deployment

Copy released bin files and the test scripts to a work dir.
Prepare the higgs dataset.

```
mkdir -p work
cd work

cp -r $_gbtproject_/run/10.BlockParallelReport/* .
#
# update bin if you have new release compiled
#
#cp $_gbtproject_/sub/xgb-optapprox/*block-release bin
#cp $_gbtproject_/sub/xgb-latest/xgboost bin/xgb-latest
#cp $_gbtproject_/sub/xgb-optapprox/lightgbm bin


# test is the working dir
cd test

#$DATADIR is the dataset storage, on juliet cluster you can use:
DATADIR=/share/jproject/fg474/dataset/
cp $DATADIR/higgs/higgs_train.libsvm higgs_train.libsvm
cp $DATADIR/higgs/higgs_test.libsvm higgs_test.libsvm
cp $DATADIR/higgs/higgs_test.csv higgs_test.csv
head -1 higgs_train.libsvm > higgs_valid.libsvm

cp $DATADIR/synset/synset_train_1000000x1024.libsvm synset_train.libsvm
head -1 synset_train.libsvm > synset_valid.libsvm

#
# if you can not access to juliet, create the datasetset
#
#../bin/make_higgs.sh
#../bin/make_synset.sh


```

### 2. validation

Validate that the release will produce the same or better result as xgboost-hist does.

Use higgs dataset, test with different tree depth is necessary. Some errors only apprears in deep trees.

```
# validate all release versions, run cmd like this
#../bin/xgb-validation.sh ../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release block 12
#./run-validate.sh ../bin/xgboost-g++-omp-dense-halftrick-short-splitonnode-block-release 

./run-validate.sh 

```

Make sure they passed the test. You should see messages like "pass!".

### 3. speedup with different tree depth

Fixed with 32 threads, show the 'best' performance of each trainer.

```
#../bin/xgb-speedup.sh <bin> <dataset> <num_round> <depth> <tree_method> <thread>
#../bin/xgb-speedup.sh ../bin/xgb-latest higgs 10 8 hist 32
#
./run-speedup.sh

```

### 4. strong scaling test 

Run strong scaling test

```
#../bin/xgb-strongscale.sh <bin> <dataset> <num_round> <depth> <tree_method>
#../bin/xgb-strongscale.sh ../bin/xgb-latest higgs 10 8 block 
#
./run-scaling.sh 

```

### 5. convergence test 

Run convergence test, output auc every 10 iteration

```
#../bin/xgb-convergence.sh <bin> <dataset> <num_round> <depth> <tree_method> <thread>
#../bin/xgb-convergence.sh ../bin/xgb-latest higgs 10 8 hist 32
#
./run-convergence.sh 

```











