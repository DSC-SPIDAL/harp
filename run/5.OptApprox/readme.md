OptApprox
=============

Goal: optimize approx method in xgboost

Approx in xgboost is column-wise implementation with layer-by-layer parallelism.

Known performance issues include binid cache and load imblance in case of small numbers of features.

version with different optimizations:

+ approx        ; standard approx
+ binidcache    ; add binid into DMatrix as cahce, reducing the binid search time but increasing the memory footprint
+ nocompact ; using binid but init only once rather than for each tree
+ compact   ; replace histogram with HistCutMatrix as same as the fasthist and init only once rather than for each tree
+ compactbinid   ; replace DMatrix with DMatrixCompact during training, pack index:binid as one integer


### evaluation

num_round=10 , warm run(the results of the second run)

tainer          |     training time(s)     | AUC          |     CPU     | MEM
----              |     ----------               |    ----------     | ------         | -----------
hist               |      22.9458          |     0.697216                   |  1300            | 10.1g
approx-histmaker       |     73.044              | 0.698075      |  2000           | 6.1g
binidcache     |    64.7914              | 0.699271       |  1800               | 6.1g
pmatfasthist-nocompact     |     55.7953     | 0.697216     |  1700-1800      |  6.1g
pmatfasthist-compact         |     65.6896     | 0.697216     |  1700-1800      |  8.0g 
pmatfasthist-compactbinid |     64.8387     | 0.697216     |  1800-1900      |  7.1g 

pmatfasthist-nocompact =  make -j 24

pmatfasthist-compact =  make -j 24 USE_COMPACT=1

pmatfasthist-compactbinid = make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1
