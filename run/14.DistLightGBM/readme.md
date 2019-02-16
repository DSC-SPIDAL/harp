distributed lightgbm
======================

This is a weak scaling test to test the performance of the communication layer.
All nodes contains the same higgs dataset, therefore, the final auc should be the same.

### 1. TCP/Socket Version
Run the following scripts in current directory on each of the nodes (ip list in mlist)

```
./run-distlightgbm.sh 1 higgsall true
./run-distlightgbm.sh 2 higgsall true
./run-distlightgbm.sh 4 higgsall true
./run-distlightgbm.sh 8 higgsall true
```

use C3 tool, if you are familiar with it, as:

```
#on head node
./run-distlightgbm.sh 4 higgsall true
# then run
cexec mycluster4: "cd `pwd`; ./run-distlightgbm.sh 4 higgsall true"
```


test results on juliet cluster:

	higgs-weakscaling		
	nodenum	trainingtime	auc
	1	10.846337	0.79474
	2	12.825699	0.79474
	4	17.65029	0.79474
	8	25.434788	0.79474

### 2. MPI version

There is a pre-compiled binary included in test directory, linked to MPICH2. (OPENMPI and INTELMPI failed by reporting link error)
Jump to next step, or you can compile from source to build the mpi version following the commands:

```
cd sub/lightgbm
mkdir -p build
cd build
cmake -DUSE_MPI=on ..
make -j8
mv ../lightgbm ../lightgbm-mpi
```

Run mpi version is easier than the TCP/Socket version.

```
# run with one machine will report error
#./run-distlightgbm-mpi.sh 1 higgsall true
./run-distlightgbm-mpi.sh 2 higgsall true
./run-distlightgbm-mpi.sh 4 higgsall true
./run-distlightgbm-mpi.sh 8 higgsall true
```

test resuls on juliet cluster:

    higgs-weak scaling      
    nodenum trainingtime    auc
    1       
    2   14.526147   0.79474
    4   18.238468   0.79474
    8   23.134626   0.79474


