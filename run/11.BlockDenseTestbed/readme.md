BlockDense TestBed 
=================================

@01072019

refer to 10.BlockParallelism for more details.

## Experiments

### 1. compile

Add tag to name of the binary file to indicate different features.

```
# init
source WHEREYOURREPOROOT/gbt-test/bin/init_env.sh 

# got to directory: sub/xgb-optapprox
# this cmd will generate two release versions, with or w/o halftrick
# with the tag name "blockdense"

cd $_gbtproject_/sub/xgb-optapprox
./makeall.sh blockdense

```

### 2. deployment

Copy released bin files and the test scripts to a work dir.
Prepare the higgs dataset.

```
mkdir -p work
cd work
mkdir -p bin

cp -r $_gbtproject_/run/11.BlockDenseTestbed/* .
cp $_gbtproject_/sub/xgb-optapprox/*blockdense* bin


# test is the working dir
cd test

# build the dataset, time consuming....
../bin/make_higgs.sh

../bin/make_synset.sh

```

### 2. validation

Validate that the release will produce the same as xgboost-hist does, without approximation introduced.

Use higgs dataset, test with different tree depth is necessary. Some errors only apprears in deep trees.

```
# validate all release versions, run cmd like this
#../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-blockdense-release blockdense 12

./run-validate.sh blockdense

```

Make sure they passed the test. You should see messages like "pass!".

### 3. run with different block settings

```

#
# this is pure feature parallelism
#
../bin/xgboost-g++-omp-halftrick-noprefetch-splitonnode-blockdense-release synset.conf num_round=10 nthread=32 tree_method=blockdense max_depth=14 bin_block_size=0 ft_block_size=1 row_block_size=0 

#
# add node parallelism
#
../bin/xgboost-g++-omp-halftrick-noprefetch-splitonnode-blockdense-release synset.conf num_round=10 nthread=32 tree_method=blockdense max_depth=14 bin_block_size=0 ft_block_size=1 row_block_size=0 node_block_size=32 group_parallel_cnt=2

#
# add multi-feature as a block
#
../bin/xgboost-g++-omp-halftrick-noprefetch-splitonnode-blockdense-release synset.conf num_round=10 nthread=32 tree_method=blockdense max_depth=14 bin_block_size=0 ft_block_size=8 row_block_size=0 node_block_size=32 group_parallel_cnt=1


```








