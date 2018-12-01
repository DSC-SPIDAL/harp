
runs=(nohalftrick-noprefetch nohalftrick-prefetch halftrick-noprefetch halftrick-prefetch)

for run in ${runs[*]}; do

conf=pmatfasthist
./xgboost-g++-omp-$run higgs_${conf}.conf nthread=28 num_round=100 2>&1 |tee runprefetch-${run}_${conf}.log

conf=hist
./xgboost-g++-omp-$run higgs_${conf}.conf nthread=28 num_round=100 2>&1 |tee runprefetch-${run}_${conf}.log

done

tail -n 4 runprefetch-*.log
