---
title: Harp-DAAL-Kmeans
---

## Overview 

K-means is a widely used clustering algorithm in machine learning community. It iteratively computes the distance between each 
training point to every centroids, re-assigns the training point to new cluster and re-compute the new centroid of each cluster. 

## Implementation of Harp-DAAL-Kmeans

Harp-DAAL-Kmeans is built upon Harp's original K-means parallel implementation, where it uses a regroup-allgather pattern to 
synchronize model data, i.e. centroids, among each mapper. The difference between original Harp-Kmeans and Harp-DAAL-Kmeans 
is their local computation kernel. Harp-DAAL-Kmeans uses DAAL's K-means kernel, where the computation of point-centroid distance
is transformed into BLAS-level 3 matrix-matrix operations. This replacement significantly improves the computation intensity of 
the local computation, and leading to a much higher vectorized codes than the original Harp-Kmeans. 

## A Code Walk through of Harp-DAAL-Kmeans


