MixMode
===============

omp thread scheduling overhead is large.

when the tree grows to a set of leaves, it's possible to schedule by node and let each node working in parallel.
In this way, we may get a version of low overhead of thread scheduling.

### depthwise

trainingtime |  growth  |   settings    | auc   | depth | tagname   |cmd
---------    |  -----   |   ---------   |   --- |   --  |   ---     | -----
4.28429 | depth |  f0,dp1,n32,async=1 |  0.787664 |   8  | unify  | ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=32 max_leaves=0  data_parallelism=1 group_parallel_cnt=32 async_mixmode=1 loadmeta=higgsmeta max_depth=8 ft_block_size=1 topk=32
6.30491 | depth |  f0,dp1,n32,async=1 |  0.809007 |   12 | unify  | ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=32 max_leaves=0  data_parallelism=1 group_parallel_cnt=32 async_mixmode=1 loadmeta=higgsmeta max_depth=12 ft_block_size=28 topk=32
13.9768 | depth |  f0,dp1,n32,async=1 |  0.809007 |   16 | unify  | ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=32 max_leaves=0  data_parallelism=1 group_parallel_cnt=32 async_mixmode=1 loadmeta=higgsmeta max_depth=16 ft_block_size=28 topk=32

### lossguide

trainingtime |  growth  |   settings    | auc   | depth | tagname   |cmd
---------    |  -----   |   ---------   |   --- |   --  |   ---     | -----
4.46673  | lossguide   |  f0,dp1,d8,async=1  |   0.794534  |    8    | unify  |   ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=8 grow_policy=lossguide max_leaves=256  data_parallelism=1 group_parallel_cnt=32 topk=8 async_mixmode=1 loadmeta=higgsmeta
7.74865  | lossguide   |  f0,dp1,d8,async=1  |   0.817459  |    12   | unify  |   ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=8 grow_policy=lossguide max_leaves=4096  data_parallelism=1 group_parallel_cnt=32 topk=32 async_mixmode=1 loadmeta=higgsmeta
39.5361  | lossguide   |  f0,dp1,d8,async=1  |   0.827554  |    16   | unify  |   ../bin/xgboost-g++-omp-dense-halftrick-byte-splitonnode-unify-release higgsmeta_eval.conf num_round=10 nthread=32 tree_method=lossguide max_depth=0 bin_block_size=0 ft_block_size=28 row_block_size=312500 node_block_size=8 grow_policy=lossguide max_leaves=65536  data_parallelism=1 group_parallel_cnt=32 topk=32 async_mixmode=1 loadmeta=higgsmeta


it is interesting to see that it achieves almost linear growth to the depth in case of depthwise.
     
