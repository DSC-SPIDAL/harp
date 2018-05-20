---
title: Ridge Regression
---

Ridge Regression is a technique for analyzing multiple regression data that suffer from multicollinearity. When
multicollinearity occurs, least squares estimates are unbiased, but their variances are large so they may be far from
the true value. By adding a degree of bias to the regression estimates, ridge regression reduces the standard errors.
It is hoped that the net effect will be to give estimates that are more reliable.

Harp-DAAL currently supports distributed mode of Ridge regression [^fn1] for dense input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-ridge-regression).

[^fn1]: Arthur E. Hoerl and Robert W. Kennard. Ridge Regression: Biased Estimation for Nonorthogonal Problems. Technometrics, Vol. 12, No. 1 (Feb., 1970), pp. 55-67.
