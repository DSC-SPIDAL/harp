---
title: Implicit Alternating Least Squares
---

Alternating least squares (ALS) is another frequently used algorithm to decompose rating matrices in recommender systems. 
Like MF-SGD, ALS is training the model data X and Y to minimize the cost function as below

<img src="/img/als-training-costfunction.png" width="50%" height="50%"><br>
<img src="/img/als-training-costfunction-2.png" width="30%" height="30%"><br>

where 

* c_ui measures the confidence in observing p_ui
* alpha is the rate of confidence
* r_ui is the element of the matrix R
* labmda is the parameter of the regularization
* n_xu, m_yi denote the number of ratings of user u and item i respectively.

Unlike MF-SGD, ALS alternatively computes model x and y independently of each other in the following formula:

<img src="/img/als-x-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-x-compute-2.png" width="50%" height="50%"><br>
<img src="/img/als-y-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-y-compute-2.png" width="50%" height="50%"><br>

The algorithm has a computation complexity for each iteration as 

<img src="/img/als-complexity-1.png" width="30%" height="30%"><br>

Omega is the set of training samples, K is the feature dimension, m is the row number of the rating
matrix, and n is the column number of the rating matrix. 

Harp-DAAL currently supports distributed mode of ALS [^fn1] for dense and sparse (CSR format) input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-38).

[^fn1]: Yifan Hu, Yehuda Koren, Chris Volinsky. Collaborative Filtering for Implicit Feedback Datasets. ICDM'08. Eighth IEEE International Conference, 2008.

