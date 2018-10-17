BasicBenchmark:: VTune
===========================

Using vtune to do hotspot analysis and mem-access analysis.

workdir: /scratch_hdd/hpda/optgbt/test/vtune/work/

higgs dataset, [generate dataset](/blob/master/run/3.BasicBenchmark.md)

```
. ../bin/intel.sh
. ../bin/runit.sh

../script/vtune-xgb-hotspots.sh
```

logs file under:  /scratch_hdd/hpda/optgbt/test/vtune/logs/
