

bin=../bin/xgboost-g++-omp-halftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-strongscale.sh ${bin} higgs 10 8 blockdense 
../bin/xgb-strongscale.sh ${bin} synset 10 8 blockdense 

bin=../bin/xgboost-g++-omp-nohalftrick-noprefetch-byte-blockdense-longcube-threadinit-release
../bin/xgb-strongscale.sh ${bin} higgs 10 8 blockdense 
../bin/xgb-strongscale.sh ${bin} synset 10 8 blockdense 

echo "================================"
echo " StrongScaling Test Results:"
echo "================================"
echo -e "binname\trunid\ttrainingtime"
find . -name "StrongScale*threadinit*.csv" -exec cat {} \; |gawk -F, '{printf("%s\t%s\t%s\t%s\n",$1,$2,$3,$6)}' |sort


