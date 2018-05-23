---
title: Pivoted QR Decomposition 
---

<img src="/img/harpdaal/QR.png" width="80%" >

QR decomposition with column pivoting introduces a permutation matrix P and convert the original 
*A=QR* to *AP=QR*. Column pivoting is useful when A is (nearly) rank deficient, or is suspected of being so. It can also improve numerical accuracy. 

Harp-DAAL currently supports distributed mode of Pivoted QR for dense input datasets.

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-details-12).
