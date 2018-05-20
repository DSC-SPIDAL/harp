---
title: Support Vector Machine Classifier
---

Support Vector Machine (SVM) is among popular classification algorithms. 
It belongs to a family of generalized linear classification problems. 
Because SVM covers binary classification problems only in the multi-class case, SVM must be used in conjunction with multi-class classifier methods.

Harp-DAAL currently supports batch mode of multi-class SVM [^fn1] for both of dense and sparse (CSR format) input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-32).

[^fn1]: B. E. Boser, I. Guyon, and V. Vapnik. A training algorithm for optimal marginclassiﬁers.. Proceedings of the Fifth Annual Workshop on Computational Learning Theory, pp: 144–152, ACM Press, 1992.

