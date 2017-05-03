---
title: Harp-DAAL-ALS
---

## Alternating least squares in Collaborative Filtering

Alternating least squares (ALS) is another frequently used algorithm to decompose rating matrices in recommender systems. 
Like MF-SGD, ALS is training the model data X and Y to minimize the cost function as below

<img src="/img/als-training-costfunction.png" width="50%" height="50%"><br>
<img src="/img/als-training-costfunction-2.png" width="50%" height="50%"><br>

where 
* c_ui measures the confidence in observing p_ui
* alpha is the rate of confidence
* r_ui is the element of the matrix R
* labmda is the parameter of the regularization
* n_xu, m_yi denote the number of ratings of user u and item i respectively.

Unlike MF-SGD, ALS alternatively computes model x and y independently of each other in the following formula:

<img src="/img/als-x-compute-1.png" width="30%" height="30%"><br>
<img src="/img/als-x-compute-2.png" width="50%" height="50%"><br>
<img src="/img/als-y-compute-1.png" width="30%" height="30%"><br>
<img src="/img/als-y-compute-2.png" width="50%" height="50%"><br>

The algorithm has a computation complexity for each iteration as 
<img src="/img/als-complexity-1.png" width="30%" height="30%"><br>
Omega is the set of training samples, K is the feature dimension, m is the row number of the rating
matrix, and n is the column number of the rating matrix. 

## Implementation 

The implementation of ALS in our Harp-DAAL consists of two levels.
* Top Level, using Harp's *regroup* and *allgather* operation to communication model data among mappers
* Bottom Level, using DAAL's ALS kernels to conduct local computations. 

## A Code Walk Through of Harp-DAAL-ALS

### Data Conversion From COO to CSR


