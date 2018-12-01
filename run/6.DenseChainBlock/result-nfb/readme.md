DenseChainBlock
===================

@11292018

Single column block and dense compact binid input matrix.

updater version:

    commit: 
    commit d1c7bdb65b6e8e48a3f3a1329d6921071e0a884d
    blockScheduler:: sync the cut and findsplit with fast_hist, now add min_val for leftmost split point in case of missing values

### experiment setting

j-127

nthread = 28  ;  make it load balancing

higgs   ; dense version

### conclusion

1. buildPostset is stable, row_set maintenance costs more time
2. halftrick is critical to hist, row based parallelism, cut more than half of buildhist time 
3. halftrick brings much fewer improvements for densechainblock, as single column based strategy is not cache friendly
4. densechainblock cost 2x times in buildhist than hist can achieve, due to ineffectiveness of utilizing cache
5. unroll is not effective, especially in densechainblock

### todo

+ extending the single column block to multi-column is the next step to test.
+ utilize larger dataset and care about the load imbalance problem


