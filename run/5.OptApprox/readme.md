OptApprox
=============

Goal: optimize approx method in xgboost

Approx in xgboost is column-wise implementation with layer-by-layer parallelism.

Known performance issues include binid cache and load imblance in case of small numbers of features.

version with different optimizations:

+ approx        ; standard approx
+ binidcache    ; add binid into DMatrix as cahce, reducing the binid search time but increasing the memory footprint
+ pmatcompact   ; replace DMatrix with DMatrixCompact during training, pack index:binid as one integer


### evaluation

parameters: higgs, num_round=10

tainer          |     training time(s)     | AUC
----              |     ----------               |    ----------
histmaker       |     73.044              | 0.698075 
binidcahce     |    75.3414              | 0.699271
pmatcompact|     61.7751               |  x 0.582335
