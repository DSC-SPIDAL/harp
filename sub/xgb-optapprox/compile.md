Compiling Examples
===================


clean the dmlc-core and rabit library

```
make clean_lib
```

compile icc version and prepare for vtune profiling

```
#make clean_lib
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 CXX=icc USE_VTUNE=1
```

add print debug msg version

```
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_DEBUG=1
```

add debug info version for gdb tracing

```
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 DEBUG=1
```

debug and save intermediate data

```
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_DEBUG_SAVE=1
```

block scheduler .vs. omp

```
make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_HALFTRICK=1 USE_OMP_BUILDHIST=1

make -j 24 USE_COMPACT=1 USE_COMPACT_BINID=1 USE_HALFTRICK=1 

```
