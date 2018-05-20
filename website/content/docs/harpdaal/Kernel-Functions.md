---
title: Kernel Functions 
---

Kernel functions form a class of algorithms for pattern analysis. The main characteristic of kernel functions is a distinct approach to this problem. 
Instead of reducing the dimension of the original data, kernel functions map the data into higher-dimensional spaces 
in order to make the data more easily separable there.

Harp-DAAL currently supports batch mode of kernel functions for bothh of dense and sparse (CSR format) input datasets, which includes two sub-types

## Linear Kernel

A linear kernel is the simplest kernel function.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-linear-kernel).

## Radial Basis Function Kernel

The Radial Basis Function (RBF) kernel is a popular kernel function used in kernelized learning algorithms.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-radial-basis-function-kernel).

