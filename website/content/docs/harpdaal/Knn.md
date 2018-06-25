---
title: K-Nearest Neighbors Classifier
---

k-Nearest Neighbors (kNN) classification is a non-parametric classification algorithm. The model of the kNN classifier is based on feature vectors and class labels from the training data set. This classifier induces the class of the query vector from the labels of the feature vectors in the training data set to which the query vector is similar. A similarity between feature vectors is determined by the type of distance (for example, Euclidian) in a multidimensional feature space.

Harp-DAAL currently supports batch mode of K-NN [^fn1][^fn2] for dense input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-34).

[^fn1]: Gareth James, Daniela Witten, Trevor Hastie, and Rob Tibshirani. An Introduction to Statistical Learning with Applications in R. Springer Series in Statistics, Springer, 2013 (Corrected at 6th printing 2015).

[^fn2]: Md. Mostofa Ali Patwary, Nadathur Rajagopalan Satish, Narayanan Sundaram, Jialin Liu, Peter Sadowski, Evan Racah, Suren Byna, Craig Tull, Wahid Bhimji, Prabhat, Pradeep Dubey. PANDA: Extreme Scale Parallel K-Nearest Neighbor on Distributed Architectures, 2016. Available from https://arxiv.org/abs/1607.08220.

