---
title: Linear Regression 
---

In statistics, linear regression is an approach for modelling the relationship between a scalar dependent variable y and one or more explanatory variables (or independent variables) denoted X. In linear regression, the relationships are modeled using linear predictor functions whose unknown model parameters are estimated from the data. Such models are called linear models.

Harp-DAAL currently supports distributed mode of linear regression for dense input datasets.
It has two algorithmic variants [^fn1]:

* Linear regression through normal equation
* Linear regression through QR decomposition.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-linear-regression).

[^fn1]: Trevor Hastie, Robert Tibshirani, Jerome Friedman. The Elements of Statistical Learning: Data Mining, Inference, and Prediction. Second Edition (Springer Series in Statistics), Springer, 2009. Corr. 7th printing 2013 edition (December 23, 2011).
