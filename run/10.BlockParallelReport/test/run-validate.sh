#!/bin/bash

export RUNID=`date +%m%d%H%M%S`
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 6
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 8
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 12
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 16

../bin/xgb-validation.sh ../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 6
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 8
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 12
../bin/xgb-validation.sh ../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release blockdense 16

echo "====================================================="
echo "Validate Test RUNID=$RUNID:"
echo "====================================================="
find . -name "validation-auc*.csv" -exec cat {} \; |sort

