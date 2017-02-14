---
title: k-means
---

k-means clustering is a method of vector quantization, originally from signal processing, that is popular for cluster analysis in data mining. k-means clustering aims to partition `N` observations into `K` clusters in which each observation belongs to the cluster with the nearest mean, serving as a prototype of the cluster.

The problem is an NP-hard question. However, there are efficient heuristic algorithms that are commonly employed and converge quickly to a local optimum. These are usually similar to the expectation-maximization algorithm for mixtures of Gaussian distributions via an iterative refinement approach employed by both algorithms. Additionally, they both use cluster centers to model the data; however, k-means clustering tends to find clusters of comparable spatial extent, while the expectation-maximization mechanism allows clusters to have different shapes.

The K-Means algorithm simply repeats the following set of steps until there is no change in the partition:

1. Choose `K` points as the initial set of centroids.

2. Assign each data point in the data set to the closest centroid (this is done by calculating the distance between the data point and each centroid).

3. Calculate the new centroids based on the clusters that were generated in step 2. Normally this is done by calculating the mean of each cluster.

4. Repeat steps 2 and 3 until data points do not change cluster assignments, meaning their centroids are set.

Harp provides several methods in order to communicates among machines. In the following links, you can find the details in using these methods such as `allreduce` and so on.

* [`allreduce` k-means](/docs/examples/allreducekmeans/)

* [`broadcast` & `reduce` k-means](/docs/examples/bcastreducekmeans/)

* [`push` & `pull` k-means](/docs/examples/pushpullkmeans/)

* [`regroup` & `allgather` k-means](/docs/examples/regroupallgatherkmeans/)