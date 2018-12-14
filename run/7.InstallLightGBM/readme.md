Install LightGBM
======================


### Building from source

```
#
#original code from github
#
#git clone --recursive https://github.com/Microsoft/LightGBM ; cd LightGBM
#mkdir build ; cd build
#cmake ..
#make -j4

#
# we have a clone at sub/lightgbm, the current modifications goes to this directory
#
cd sub/lightgbm
mkdir -p build
cd build
cmakd ..
make -j 16


```

Current version [2.1.2](https://github.com/Microsoft/LightGBM/commit/7282533ab03fa3e1c961888de522d3c7bcada43f)


### benchmark

Test on j-030, 24 core Intel(R) Xeon(R) CPU E5-2670 v3

```
./lightgbm config=higgs_lightgbm.conf num_trees=100 tree_learner=feature num_threads=16

# test by lightgbm itself or the python script
./lightgbm config=higgs_lightgbm.conf num_trees=100 tree_learner=feature num_threads=16 metric_freq=100 valid_data=higgs_test.libsvm
#or
./lightgbm config=higgs_lgbm_predict.conf
python -m runner.runxgb --eval LightGBM_predict_result.txt --testfile ../higgs/higgs_test.csv

```

Results at iteration 100

```
#tree_learner = feature
auc = 0.826093
Train Time: 60.054349

#tree_learner = data
auc = 0.826093
Train Time: 60.447312

```

### References

1. [core parameters](https://github.com/Microsoft/LightGBM/blob/master/docs/Parameters.rst#core-parameters)

    # frequence for metric output
    metric_freq = 1000
    # true if need output metric for training data, alias: tranining_metric, train_metric
    is_training_metric = false
    
    data = higgs_train.libsvm
    valid_data = higgs_valid.libsvm
    
    # number of trees(iterations), alias: num_tree, num_iteration, num_iterations, num_round, num_rounds
    num_trees = 100
    
    # shrinkage rate , alias: shrinkage_rate
    learning_rate = 0.1
    
    # number of leaves for one tree, alias: num_leaf
    num_leaves = 128
    # type of tree learner, support following types:
    # serial , single machine version
    # feature , use feature parallel to train
    # data , use data parallel to train
    # voting , use voting based parallel to train
    # alias: tree
    tree_learner = feature
    # number of threads for multi-threading. One thread will use one CPU, defalut is setted to #cpu. 
    # num_threads = 8

2. [Experiments Results](https://github.com/Microsoft/LightGBM/blob/master/docs/Experiments.rst#comparison-experiment)
