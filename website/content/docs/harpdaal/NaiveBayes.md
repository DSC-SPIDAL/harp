---
title: Naive Bayes Classifier 
---

Naïve Bayes is a set of simple and powerful classification methods often used for text classification, 
medical diagnosis, and other classification problems. In spite of their main assumption about independence between features, 
Naïve Bayes classifiers often work well when this assumption does not hold. 
An advantage of this method is that it requires only a small amount of training data to estimate model parameters.

Harp-DAAL currently supports distributed mode of Multinomial Naive Bayes [^fn1] for both of dense and sparse (CSR format) input datasets.

More details from Intel DAAL Documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-28)

[^fn1]: Jason D.M. Rennie, Lawrence, Shih, Jaime Teevan, David R. Karget. Tackling the Poor Assumptions of Naïve Bayes Text classifiers. Proceedings of the Twentieth International Conference on Machine Learning (ICML-2003), Washington DC, 2003. 
