distributed lightgbm
======================

This is a weak scaling test to test the performance of the communication layer.
All nodes contains the same higgs dataset, therefore, the final auc should be the same.

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
