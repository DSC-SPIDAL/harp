#!/bin/bash

nCluster=200
nItr=10
path=/home/langshichen/Lib/__release_lnx/daal/examples/cpp/_results/intel_intel64_parallel_a

for ((nInput_row = 10000; nInput_row<50000; nInput_row += 10000));  

do

for ((nInput_col = 10000; nInput_col<50000; nInput_col += 10000));

do

	for ((i=1;i<10;i+=1));
	do

		# numactl --membind 1 $path/kmeans_dense_batch.exe $nCluster $nItr $nInput_row $nInput_col >> $path/kmeans_dense_batch-$nCluster-$nItr-$nInput_row-$nInput_col.res
		$path/kmeans_dense_batch.exe $nCluster $nItr $nInput_row $nInput_col >> $path/kmeans_dense_batch-$nCluster-$nItr-$nInput_row-$nInput_col.res

	done

done

done

