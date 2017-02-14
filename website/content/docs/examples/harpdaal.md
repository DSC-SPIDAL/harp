---
title: Harp-DAAL-APP
---

[harp3-daal-app](https://github.iu.edu/IU-Big-Data-Lab/Harp/tree/master/harp3-daal-app) includes the application implemented within the Harp-DAAL framework. 

## Introduction of Harp-DAAL 

Intel® Data Analytics Acceleration Library (DAAL) is a library from Intel that aims to provide the users of some highly optimized building blocks for data analytics and machine learning applications. 
For each of its kernel, DAAL has three modes:

* A Batch Processing mode is the default mode that works on an entire dataset that fits into the memory space of a single node.
* A Online Processing mode works on the blocked dataset that is streamed into the memory space of a single node.
* A Distributed Processing mode works on datasets that are stored in distributed systems like multiple nodes of a cluster.

Within DAAL's framework, the communication layer of the Distributed Processing mode is left to the users, which could be any of the user-defined middleware for communication. 
The goal of Harp-DAAL project is thus to fit Harp, a plug-in into Hadoop ecosystem, into the Distributed Processing mode of DAAL. Compared to contemporary communication libraries, 
Harp has the advantages as follows:

* Harp has MPI-like collective communication operations that are highly optimized for big data problems.
* Harp has efficient and innovative computation models for different machine learning problems.

The original Harp project has all of its codes written in Java, which is a common choice within the Hadoop ecosystem. 
The downside of the pure Java implementation is the slow speed of the computation kernels that are limited by Java's data management. 
Since manycore architectures devices are becoming a mainstream choice for both server and personal computer market, 
the computation kernels should also fully take advantage of the architecture's new features, which are also beyond the capability of the Java language. 
Thus, a reasonable solution for Harp is to accomplish the computation tasks by invoking C++ based kernels from libraries such as DAAL. 

# Compile and Run Harp-DAAL 

To compile Harp-DAAL, users shall first install Intel's DAAL repository. The source code is available in their github page
https://github.com/01org/daal
After installation, please follow the procedure as below:

1.setup the DAALROOT environment in the .bashrc file
```bash
export DAALROOT=/path-to-daal-src/
```
2.Enter the harp3-daal-app directory, and build the apps
```bash
cd harp3-daal-app
ant
```
3.Create scripts to run the examples within harp3-daal-app

