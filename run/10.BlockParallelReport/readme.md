Block Based Parallelism Report
=================================

@01072019

slides and experiments results in "doc/meeting/0107-BlockParallelism"

online doc: 

    [slides](https://docs.google.com/presentation/d/1iI4aKmvN92L_Y5Tlm_7ElB_InYVWTf8ECB4Z9t8b8ms/edit?usp=sharing)
    [excel](https://docs.google.com/spreadsheets/d/1VTqkyGIVRo1wxgf-K7tbkKPywSulBRonfY0n72L_mm0/edit?usp=sharing)


## Experiments

### 1. compile

Add tag to name of the binary file to indicate different features.

```
# init
source WHEREYOURREPOROOT/gbt-test/bin/init_env.sh 

# got to directory: sub/xgb-optapprox
# this cmd will generate two release versions, with or w/o halftrick
# with the tag name "-byte-blockdense-longcube-threadinit"

cd $_gbtproject_/sub/xgb-optapprox
./makeall.sh -byte-blockdense-longcube-threadinit

```

### 2. deployment

Copy released bin files and the test scripts to a work dir.
Prepare the higgs dataset.

```
mkdir -p work
cd work
mkdir -p bin

cp $_gbtproject_/sub/xgb-optapprox/*longcube-threadinit* bin

cp -r $_gbtproject_/run/10.BlockParallelReport/* .

# test is the working dir
cd test

#$DATADIR is the dataset storage, on juliet cluster you can use:
DATADIR=/share/jproject/fg474/share/optgbt/vtune/higgs

#ln -s $DATADIR/higgs_train.libsvm higgs_train.libsvm
#ln -s $DATADIR/higgs_valid.libsvm higgs_valid.libsvm
#ln -s $DATADIR/higgs_test.libsvm higgs_test.libsvm
#ln -s $DATADIR/higgs_test.csv higgs_test.csv
cp $DATADIR/higgs_train.libsvm higgs_train.libsvm
cp $DATADIR/higgs_valid.libsvm higgs_valid.libsvm
cp $DATADIR/higgs_test.libsvm higgs_test.libsvm
cp $DATADIR/higgs_test.csv higgs_test.csv

```

### 2. validation

Validate that the release will produce the same or better result as xgboost-hist does.

Use higgs dataset, test with different tree depth is necessary. Some errors only apprears in deep trees.

```
# validate all release versions, run cmd like this
#../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 12

./run-validate-block.sh

```

Make sure they passed the test. You should see messages like "pass!".

### 3. speedup with different tree depth

Fixed with 32 threads, show the 'best' performance of each trainer.

```


```










