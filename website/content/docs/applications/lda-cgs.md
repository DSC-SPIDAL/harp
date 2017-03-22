---
title: Latent Dirichlet Allocation (CGS)
---

<img src="/img/lda-cgs-illustration.png" width="30%"  >

# Understanding CGS LDA algorithm

<img src="/img/lda-description.png" width="100%" >

### Pseudo code for SGD and CCD algorithm:

<img src="/img/cgs-algorithm.png" width="80%" >




# Using Model Rotation to parallel LDA algorithm
Model rotation is expressed as a collective communication. The operation takes the model part owned by a process and performs the rotation.
By default, the operation sends the model partitions to the next neighbor and receives the model partitions from the last neighbor in a predefined ring
topology of workers. An advanced option is that the ring topology can be dynamically defined before performing the model rotation. Thus programming model rotation requires just one API.
For local computations inside each worker, they are simply programmed through an interface of “schedule-update”. A scheduler employs a user-defined function to maintain a dynamic order of
model parameter updates and to avoid the update conflict. Since the local computation only needs to process the model obtained during the rotation without considering the parallel model updates from
other workers, the code of a parallel machine learning algorithm can be modularized as a process of performing computation and rotating model partitions.

We adopt `Model Rotation` to parallel CGS LDA algorithm. The data flow and algorithm are show as follows:

  <img src="/img/model-rotation.png" width="100%" >

# Performance

<img src=/harp-test/img/2-1-5.png width="50%">

![Overview-3](/img/2-1-3.png)

Experiments are conducted on a 128-node Intel Haswell cluster at Indiana University. Among them, 32 nodes each have two 18-core Xeon E5-2699 v3 processors (36 cores in total), and 96 nodes each have two 12-core Xeon E5- 2670 v3 processors (24 cores in total). All the nodes have 128 GB memory and are connected by QDR InfiniBand. For our tests, JVM memory is set to "-Xmx120000m -Xms120000m", and IPoIB is used for communication.

We use one big dataset which is generated from "ClueWeb09" to test LDA both on Harp and Petuum.

Through examining the model likelihood achieved by the training time, the results show that Harp consistently outperforms Petuum. We test Harp CGS and Petuum on "clueweb1" with 30 nodes × 30 threads and 60 nodes × 20 threads two configurations. Both results show that Harp CGS converges faster than Petuum. Concerning the convergence speed on the same dataset with different configurations, we observe that the fewer the number of cores used and the more computation per core, the faster Harp runs compared to Petuum. When the scale goes up, the difference of the convergence speed reduces. With 30 nodes × 30 threads Xeon E5-2699 v3 nodes, Harp is 45% faster than Petuum while with 60 nodes × 20 threads Xeon E5-2670 v3 nodes, Harp is 18% faster than Petuum when the model likelihood converges to −1.37 × 1011.

# Run LDA example

## Data
   You can download public data and transfer to the data format required by sgd and ccd application. Here are some [datasets](https://github.iu.edu/IU-Big-Data-Lab/ml-bench/blob/master/dataset/dataset-mf.md) for your reference.
### Data Format
The format for the data should be a list of ``doc-name  wordid1 wordid2 ...>``. For example,
```bash
doc1   63 264 374 4695 5441  1200 1200 25529  1707
doc2   545 1939 206863 773279 773279
...
```

## Usage
```bash
hadoop jar harp-app-1.0-SNAPSHOT.jar  edu.iu.lda.LDALauncher <doc dir> <num of topics> <alpha> <beta> <num of iterations> <num of map tasks> <num of threads per worker> <timer> <num of model slices> <enable timer tuning> <work dir> <print model>
```
