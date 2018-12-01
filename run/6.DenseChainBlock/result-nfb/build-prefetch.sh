
make clean_all
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_HALFTRICK=1 USE_VTUNE=1 USE_UNROLL=1
cp xgboost-g++-omp xgboost-g++-omp-halftrick-prefetch

make clean_all
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_HALFTRICK=1 USE_VTUNE=1 
cp xgboost-g++-omp xgboost-g++-omp-halftrick-noprefetch

make clean_all
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_VTUNE=1 USE_UNROLL=1
cp xgboost-g++-omp xgboost-g++-omp-nohalftrick-prefetch

make clean_all
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_OMP_BUILDHIST=1 USE_BINID=1 USE_VTUNE=1 
cp xgboost-g++-omp xgboost-g++-omp-nohalftrick-noprefetch

cp xgboost-*prefetch ~/tmp

