---
title: K-Means Clustering 
---

K-means is a widely used clustering algorithm in machine learning community. It iteratively computes the distance between each 
training point to every centroids, re-assigns the training point to new cluster and re-compute the new centroid of each cluster. 
In other words, the clustering methods enable reducing the problem of analysis of the entire data set to the analysis of clusters.

Harp-DAAL currently supports distributed mode of K-means clustering for both of dense and sparse (CSR format)
input datasets.

More algorithmic details from Intel DAAL Documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-5).
