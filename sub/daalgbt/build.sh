source /scratch_hdd/hpda/optgbt/daal2019/daal/__release_lnx_gnu/daal/bin/daalvars.sh intel64
rm -f _results/gnu_intel64_parallel_a/daalgbt.exe
rm -f  _results/gnu_intel64_sequential_a/daalgbt.exe
make libintel64 compiler=gnu mode=build $1
