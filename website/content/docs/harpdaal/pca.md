---
title: Principle Component Analysis
---

Principle Component Analysis (PCA) is a widely used statistical procedure that uses an orthogonal transformation to convert a set of observations of possibly correlated variables into a set of values of linearly uncorrelated variables called principal components (or sometimes, principal modes of variation). 
PCA can be done by two methods:

* Eigenvalue decomposition of a data covariance (or cross-correlation) 
* Singular Value Decomposition of a data 

It is usually performed after normalizing (centering by the mean) the data matrix for each attribute.

In Harp-DAAL, we have two methods for computing PCA

## SVD Based PCA

The input is a set of p-dimensional dense vectors, DAAL kernel invokes a SVD decomposition to find out $p_r$ principle directions (Eigenvectors) 
The details of SVD kernel by Intel DAAL is [here](https://software.intel.com/en-us/daal-programming-guide-singular-value-decomposition).

Harp-DAAL provides distributed mode for SVD based PCA for dense input datasets.

## Correleation Based PCA

The input is a $p\tims p$ correlation matrix, and the DAAL PCA kernel will find out the $p_r$ directions [^fn1].
Details from Intel DAAL Documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-6).

Harp-DAAL provides distributed mode for Correlation based PCA for both of dense and sparse (CSR format) input datasets.


[^fn1]: Bro, R.; Acar, E.; Kolda, T.. Resolving the sign ambiguity in the singular value decomposition. SANDIA Report, SAND2007-6422, Unlimited Release, October, 2007.





