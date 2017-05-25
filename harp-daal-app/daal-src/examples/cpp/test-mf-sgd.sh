#!/bin/bash

#choose MCDRAM in flat mode
mbind=1 

#choose DDR4 in flat mode
# mbind=0 

#setup the result file name
bin_path=${DAALROOT}/examples/cpp/_results/intel_intel64_parallel_a
result_file=$bin_path/mf_sgd_yahoomusic_thds_mem$mbind.res

for ((nItr = 256; nItr>=1; nItr /= 2));
do

  printf "Execution with $nItr threads\n"
  printf "Execution with $nItr threads\n\n" >> $result_file
  numactl --membind $mbind $bin_path/mf_sgd_batch.exe $nItr >> $result_file

done
