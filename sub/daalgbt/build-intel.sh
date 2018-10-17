source /scratch_hdd/hpda/optgbt/daal2019-intel/daal/__release_lnx/daal/bin/daalvars.sh intel64
rm -f _results/intel_intel64_parallel_a/daalgbt.exe
rm -f  _results/intel_intel64_sequential_a/daalgbt.exe
make -j 8 libintel64 mode=build $1
