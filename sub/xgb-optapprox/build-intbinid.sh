
tagname=-intbinid

make clean_all
make -j 24 USE_COMPACT=1  USE_OMP_BUILDHIST=1 USE_BINID=1 USE_HALFTRICK=1 USE_VTUNE=1 USE_UNROLL=1 TAGNAME=$tagname

make clean_all
make -j 24 USE_COMPACT=1  USE_OMP_BUILDHIST=1 USE_BINID=1 USE_HALFTRICK=1 USE_VTUNE=1 TAGNAME=$tagname


make clean_all
make -j 24 USE_COMPACT=1  USE_OMP_BUILDHIST=1 USE_BINID=1 USE_VTUNE=1 USE_UNROLL=1 TAGNAME=$tagname


make clean_all
make -j 24 USE_COMPACT=1  USE_OMP_BUILDHIST=1 USE_BINID=1 USE_VTUNE=1 TAGNAME=$tagname


cp xgboost-*prefetch$tagname ~/tmp

