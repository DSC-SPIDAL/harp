# SubGraph2Vec: Highly-Vectorized Tree-like Subgraph Counting   

SubGraph2Vec provides a higly-vectorized shared-memory multi-threading implementation upon a new 
algorithm of color coding in counting tree-like templates from massive network G(V,E). The new algorithm 
reduces the complexity of traversing vertex neighbours within the original algorithm of color coding, and 
the dynamic programming procedure is decoupled into two linear algebra kernels

1. Sparse Matrix Dense Vector Multiplication (SpMV)
2. Element-wised Dense Vector Multiplication and Addition (eMA)  

The eMA kernel is, by default, implemented by using OpenMP and AVX intrinsics operations of FMA (fusion of multiplication and addition).   
For SpMV kernel, we provide two options on CPU.

- Utilizing SpMV kernel from Intel MKL library with the adjacency matrix stored in CSR format.
- Utilizing our customized CSC-Split adjacency matrix format and a Sparse matrix dense matrix Multiplication (SpMM) kernel. 

## To compile the codes

## To run the codes






