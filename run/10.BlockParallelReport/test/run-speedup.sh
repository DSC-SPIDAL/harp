
#../bin/xgb-speedup.sh ../bin/xgb-latest higgs 10 8 hist 32
#../bin/xgb-speedup.sh ../bin/xgb-latest higgs 10 12 hist 32
#../bin/xgb-speedup.sh ../bin/xgb-latest higgs 10 16 hist 32
#
#
#bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense
#../bin/xgb-speedup.sh ${bin} higgs 10 8 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 12 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 16 blockdense 32
#
#../bin/xgb-speedup.sh ../bin/xgb-latest synset 10 8 hist 32
#../bin/xgb-speedup.sh ../bin/xgb-latest synset 10 12 hist 32
#../bin/xgb-speedup.sh ../bin/xgb-latest synset 10 16 hist 32

#bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-short-blockdense
#../bin/xgb-speedup.sh ${bin} synset 10 8 blockdense 32 500000 16
#../bin/xgb-speedup.sh ${bin} synset 10 12 blockdense 32 500000 16
#../bin/xgb-speedup.sh ${bin} synset 10 16 blockdense 32 500000 16

#bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense
#../bin/xgb-speedup.sh ${bin} higgs 10 8 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 12 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 16 blockdense 32

#bin=../bin/xgboost-g++-omp-halftrick-noprefetch-short-blockdense-longcube
#../bin/xgb-speedup.sh ${bin} synset 10 8 blockdense 32 500000 16
#../bin/xgb-speedup.sh ${bin} synset 10 12 blockdense 32 500000 16
#../bin/xgb-speedup.sh ${bin} synset 10 13 blockdense 32 500000 16


#bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-release
#../bin/xgb-speedup.sh ${bin} higgs 10 8 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 12 blockdense 32
#../bin/xgb-speedup.sh ${bin} higgs 10 16 blockdense 32
#
#../bin/xgb-speedup.sh ${bin} synset 10 8 blockdense 32 
#../bin/xgb-speedup.sh ${bin} synset 10 12 blockdense 32 
#../bin/xgb-speedup.sh ${bin} synset 10 13 blockdense 32 

bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-speedup.sh ${bin} higgs 10 8 blockdense 32
../bin/xgb-speedup.sh ${bin} higgs 10 12 blockdense 32
../bin/xgb-speedup.sh ${bin} higgs 10 16 blockdense 32


bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-speedup.sh ${bin} higgs 10 8 blockdense 32
../bin/xgb-speedup.sh ${bin} higgs 10 12 blockdense 32
../bin/xgb-speedup.sh ${bin} higgs 10 16 blockdense 32


bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-speedup.sh ${bin} synset 10 8 blockdense 32 
../bin/xgb-speedup.sh ${bin} synset 10 12 blockdense 32 
../bin/xgb-speedup.sh ${bin} synset 10 13 blockdense 32 

bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-speedup.sh ${bin} synset 10 8 blockdense 32 
../bin/xgb-speedup.sh ${bin} synset 10 12 blockdense 32 
../bin/xgb-speedup.sh ${bin} synset 10 13 blockdense 32 

echo "================================"
echo " Speedup Test Results:"
echo "================================"
# binname, runid, trainingtime
echo -e "binname\trunid\ttrainingtime"
find . -name "SpeedUp*threadinit*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\n",$1,$2,$5)}' |sort

