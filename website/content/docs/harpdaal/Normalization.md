---
title: Normalization 
---

In statistics and applications of statistics, normalization can have a range of meanings. 
In the simplest cases, normalization of ratings means adjusting values measured on different scales to a notionally common scale, often prior to averaging. 
In more complicated cases, normalization may refer to more sophisticated adjustments where the intention is to bring the entire probability distributions of adjusted values into alignment.

Harp-DAAL currently supports batch mode of normalization for dense input datasets, and it 
provides two algorithm kernels.

## Min-max

Min-max normalization is an algorithm to linearly scale the observations by each feature (column) into the range [a, b].

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-min-max).

## Z-score

Z-score normalization is an algorithm to normalize the observations by each feature (column).

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-z-score).

