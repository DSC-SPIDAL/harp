LossGuide
===================

The tree growth strategy of lightdbm by default uses lossguide rather than depthguide.
And xgboost.hist also support it by set the parameter grow_policy = lossguide.
In this policy, the node with the largest loss_gain will be selected to split. It will 
create a unbalanced tree and get better performance than depthguide can get.

Basic experiments shows:  higgs, num_round=10

trainer         |   tree_depth  |   num_leaves  |   auc
---             |   -------     |   -------     |   -----------
xgb.hist        |   8           |       0       |  0.787664
xgb.hist.lossguide  |   0       |   255         |  0.794680 
lightgbm        |   0           |   255         |  0.79474

As lossguide expand the tree one node by another, there is no parallelism at the node level.
A large overhead of synchronizations of threads will be observed for deep trees.

To minic the overhead of OMP scheduling on the node, experiment on xgb-block shows:
higgs, depth=16, num_leaves=65536

group_parallel_cnt  |   omp for |   training time
--------------      |   ---------------         |   ----------------
1                   |   gorup         |   41.1
32768               |   group         |   56.5
32768               |   one node      |   69.2

Overhead of omp threads synchronization is very large here. 

### use node_block_size to control node level parallelism


    higgs   TrainingTime       higgs   TrainingTime               Speedup-hist            speedup-lightgbm
    Tree_depth  xgboost block-depth Tree_depth  xgboost-lossguide   lightgbm    block-lossguide Tree_depth  block-depth block-lossguide lightgbm    block-lossguide
    8   7.78    4.27    8   9.33    7.11    6.27    8   1.82    1.49    1.31    1.13
    12  32.35   7.86    12  35.74   17.83   13.47   12  4.12    2.65    2.00    1.32
    16  245.83  17.24   16  403.58  126.51  44.36   16  14.26   9.10    3.19    2.85


### TODO: remove OMP scheduling and thread synchronization overhead 

it can be an mix mode that using task scheduling for each node when node number is big enough, 
and using normal fid/bid level parallelism in the first few levels.







