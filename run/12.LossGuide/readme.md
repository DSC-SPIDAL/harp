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







