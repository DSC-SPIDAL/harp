---
title: Decision Tree
---

Decision trees partition the feature space into a set of hypercubes, and then fit a simple model in each hypercube. The simple model can be a prediction model, which ignores all predictors and predicts the majority (most frequent) class (or the mean of a dependent variable for regression), also known as 0-R or constant classifier.

Harp-DAAL currently supports batch mode of Decision Tree for dense input datasets, which includes two sub-types

* Classification
* Regression

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-22).

