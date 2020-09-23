#!/bin/bash

# Setting slurm job requirements
# Number of nodes to use
#SBATCH -N 4
#SBATCH --ntasks-per-node=1
# submit to the juliet partition on FutureSystems
#SBATCH --partition=juliet
#SBATCH --exclusive
#SBATCH --time=12:00:00

# Prepare Intel library for mkl and mpi
source /opt/intel/compilers_and_libraries/linux/bin/compilervars.sh -arch intel64 -platform linux
source /opt/intel/impi/*/intel64/bin/mpivars.sh

# Actual mpirun command. Configure the path to the executable and graph/template data to fit your settings
mpirun -genv OMP_NUM_THREADS=24 -genv I_MPI_PIN_DOMAIN=omp $HOME/harp/sc-hsw-icc-mpiicc.bin /share/project/FG474/TrainingData/subgraph/Edge-Graph/miami.graph /share/project/FG474/TrainingData/subgraph/template/u10-1.fascia 1 24 0 0 1 1 1

# parameters are explained below:
# Itr=1 ## the number of iteration 
# tpproc=24 ## the number of threads per process
# read_binary=0 ## equals 1 if graph_file is in binary format
# writeBinary=0 ## set this value to 1 and the input text graph_file is output in a binary file 
# prune=1 ## trigger the complexity reduction (default)
# useSPMM=1 ## 0 for SpMV kernel and 1 for SpMM kernel (by default) 
# useCSC=1  ## choose the adjacency matrix format, 1 for CSC-Split and 0 for CSR
