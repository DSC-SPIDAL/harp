3.BasicBenchmark
=======================

Basic benchmark on GBT implementations, refer details to [GBTTest-10172018](../../doc/meeting/1017-GBTTest/GBTTest-10172018.pdf).

### 1. environment

gcc (crosstool-NG fa8859cb) 7.2.0
icc (ICC) 19.0.0.117 20180804

### 2. prepare dataset

workdir: /scratch_hdd/hpda/optgbt/test/benchmark

generate dataset, following the [descriptions](../3.BasicBenchmark.md#create-benchmark-dataset)

### 3. prepare the code and gbt implementations

current root of project is : "$_gbtproject_"

xgboost, v0.80-2018.09.30, [diff log](../../sub/xgbv0.80-2018.09.30/diff.log)

    commit efc4f855055609e16745efc0a342103e7e2fdab1
    Author: weitian <5275150+weitian@users.noreply.github.com>
    Date:   Wed Oct 3 08:43:55 2018 -0700

daal, DAAL 2019. Revision: 30360

    commit 2e80c42b67e3d278217b6057761e155bf02f24f8
    Author: VasiliyR <vasily.rubtsov@intel.com>
    Date:   Fri Sep 21 12:57:33 2018 +0300

daalgbt, gbt application based on the example code.

```
# compile xgboost
cd $_gbtproject_/sub/xgbv0.80-2018.09.30
./build.sh

# compile daal + daalgbt
# workdir: 
mkdir -p /scratch_hdd/hpda/optgbt/daal2019-intel
cd /scratch_hdd/hpda/optgbt/daal2019-intel
git clone --recursive -b daal_2019 --single-branch https://github.com/intel/daal.git
cd daal

source $_gbtproject_/run/3.BasicBenchmark/vtune/bin/intel.sh

make daal -j 32 PLAT=lnx32e

# compile daalgbt
# ret: _results/intel_intel64_parallel_a/daalgbt.exe
cd $_gbtproject_/sub/daalgbt

source runit.sh
./build-intel.sh

```

### 4. run benchmark

```
cd /scratch_hdd/hpda/optgbt/test/benchmark

#copy files
cp -r $_gbtproject_/run/3.BasicBenchmark/benchmark/* .

#goto each dataset dir, and run test

```

### 5. run vtune

```
cd /scratch_hdd/hpda/optgbt/test/benchmark
cp -r $_gbtproject_/run/3.BasicBenchmark/vtune .

cd vtune/work

```




