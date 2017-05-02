---
title: Harp-DAAL-ALS
---

Alternating least squares (ALS) is another frequently used algorithm to decompose rating matrices in recommender systems. 
The algorithm has a computation complexity for each iteration as 

<img src="/img/als-complexity-1.png" width="30%" height="30%"><br>

Omega is the set of training samples, K is the feature dimension, m is the row number of the rating
matrix, and n is the column number of the rating matrix. Unlike MF-SGD, ALS alternatively 
computes model $W$ and $H$ independently of each other. The implementation of ALS in our Harp-DAAL 
framework chooses the regroup-allgather operation. 

