# DAAL-MF-SGD 

DAAL-MF-SGD is an algorithm that we implemented based on the DAAL2017 version released by Intel. 
at their github repository: https://github.com/01org/daal
DAAL2017 is licensed under Apache License 2.0.

This solver could be used to factorize a sparse matrix $V$ into two dense matrices $W$ and $H$, which is widely

$V = WH$

used in the recommender systems, such as the recommended movies provided to users by Netflix. This Matrix Factorization 
(MF for short) uses a machine learning algorithm, Stochastic Gradient Descent (SGD), to find the two object matrices $W$ and 
$H$. In this machine learning scenario, matrix $V$ contains two datasets, one is the training set and the other is the 
test set. Both of matrices $W$ and $H$ are considered to be model data, whose values are updated by the training process of 
SGD. After the training, we evaluate the result by calculating the difference between the true value of test points and the multiplication
of that value by model $W$ and $H$. The procedure could be expressed as two stages:

1.Training Stage

* $E^{t-1}_{ij} = V^{t-1}_{train,ij} - \sum_{k=0}^r W^{t-1}_{ik} H^{t-1}_{kj}$
* $W^t_{i*} = W^{t-1}_{i*} - \eta (E^{t-1}_{ij}\cdot H^{t-1}_{*j} - \lambda \cdot W^{t-1}_{i*})$
* $H^t_{*j} = H^{t-1}_{*j} - \eta (E^{t-1}_{ij}\cdot W^{t-1}_{i*} - \lambda \cdot H^{t-1}_{*j})$

2.Test Stage

* $RMSE = V_{test, ij} - \sum_{k=0}^{r}W_{i,k}H_{k,j}$

The training process uses an iterative Standard SGD algorithm, which contains one vector inner product and two AXPY updates. In order to 
improve the performance of these linear algebra computation, we implement these kernels within DAAL's framework by using highly optimized 
libraries and tools from Intel. 

Our delivered package includes a core part of C++ native codes under the following paths of DAAL2017,

* daal/include/algorithms/mf_sgd
* daal/algorithms/mf_sgd

and it also has a Java interface under the paths 

* daal/lang_interface/java/com/intel/daal/algorithms/mf_sgd
* daal/lang_service/java/com/intel/daal/algorithms/mf_sgd

There are examples of SGD under the paths

* daal/examples/cpp/source/mf_sgd
* daal/examples/java/com/intel/daal/examples/mf_sgd

To Run the examples, unzip the movielens train and test dataset under *daal/examples/data/batch*.
Using tar to combine the three split files of movielens-train into one movielens-train.mm file.

Copy the two files:

* movielens-train.mm
* movielens-test.mm

into *daal/examples/data/distributed* if you would like to test the distributed examples. 

A detailed online documentation could be found at https://github.iu.edu/pages/IU-Big-Data-Lab/DAAL-2017-MF-SGD/


