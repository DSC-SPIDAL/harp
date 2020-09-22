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

Choose the platform in `ARCH` and compiler in `COMPILER` 

```bash
## to compile on Haswell architecture and use Intel icc compiler
make ARCH=hsw COMPILER=icc
```

Currenlty, we have tested the codes on Haswell and Skylake architectures with icc and g++ compiler. You can customize the 
`Makefile` to use other compilers and test more hardware platforms.

## To run the codes

A minimial script to run SubGraph2Vec is shown below:

```bash
app=sc-skl-icc.bin ## run SubGraph2Vec on a skylake node compiled by Intel icc
graph_file=gnp.graph ## specify the input network
template_file=u3-1.fascia ## specify the tree template
Itr=1 ## the number of iteration 
tpproc=24 ## the number of threads per process
read_binary=0 ## equals 1 if graph_file is in binary format
writeBinary=0 ## set this value to 1 and the input text graph_file is output in a binary file 
prune=1 ## trigger the complexity reduction (default)
useSPMM=1 ## 0 for SpMV kernel and 1 for SpMM kernel (by default) 
useCSC=1  ## choose the adjacency matrix format, 1 for CSC-Split and 0 for CSR

$app ${graph_file} ${template_file} ${Itr} ${tpproc} ${read_binary} ${writeBinary} ${prune} ${useSPMM}
```


## SubGraph2Vec on MPI

Make sure that `intel-mpi` and `icc` variables are sourced:

```bash
source <path>/mpi/intel64/bin/mpivars.sh
source <path>/bin/compilervars.sh  --arch intel64
```

### To compile the codes

Run `compile-hsw-mpiicc.sh` to compile the code:

```bash
./compile-hsw-mpiicc.sh
```

This code will generate `sc-hsw-icc-mpiicc.bin` file.

### To run the codes

Modify the following variables in `testscripts/hsw-run-sg2vec-mpi.sh` to run the codes.

```bash
app_dir=<path of sc-hsw-icc-mpiicc.bin>

# mpi location
mpiexe=<path of mpirun>

# host file path
mpihost=<path of mpi host file>

# graph and template location
graph_loc=<path of graphs>
template_loc=<path of templates>
```

Example:
```bash
# location of `sc-hsw-icc-mpiicc.bin` file
app_dir=/N/u/lc37/WorkSpace/cpuPGBSC

# mpi location
mpiexe=/opt/intel/compilers_and_libraries_2018/linux/mpi/intel64/bin/mpirun

# host file path
mpihost=/N/u/lc37/WorkSpace/SG2VECTest/machinehosts

# graph and template location
graph_loc=/scratch_hdd/lc37/sc-vec/graphs
template_loc=/scratch_hdd/lc37/sc-vec/templates
```

Run the script:

```bash
./testscripts/hsw-run-sg2vec-mpi.sh
```




