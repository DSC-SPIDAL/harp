---
title: Implicit Alternating Least Squares
---

Alternating least squares (ALS) is an algorithm used in recommender systems, 
which trains the model data X and Y to minimize the cost function as below

<img src="/img/als-training-costfunction.png" width="50%" height="50%"><br>
<img src="/img/als-training-costfunction-2.png" width="30%" height="30%"><br>

where 

* c_ui measures the confidence in observing p_ui
* alpha is the rate of confidence
* r_ui is the element of the matrix R
* labmda is the parameter of the regularization
* n_xu, m_yi denote the number of ratings of user u and item i respectively.

ALS alternatively computes model x and y independently of each other in the following formula:

<img src="/img/als-x-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-x-compute-2.png" width="50%" height="50%"><br>
<img src="/img/als-y-compute-1.png" width="20%" height="20%"><br>
<img src="/img/als-y-compute-2.png" width="50%" height="50%"><br>

Harp-DAAL currently supports distributed mode of ALS [^fn1][^fn2] for dense and sparse (CSR format) input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-38).

[^fn1]: Rudolf Fleischer, Jinhui Xu. Algorithmic Aspects in Information and Management. 4th International conference, AAIM 2008, Shanghai, China, June 23-25, 2008. Proceedings, Springer. 
[^fn2]: Yifan Hu, Yehuda Koren, Chris Volinsky. Collaborative Filtering for Implicit Feedback Datasets. ICDM'08. Eighth IEEE International Conference, 2008.

