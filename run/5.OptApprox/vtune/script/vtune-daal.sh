#!/bin/bash

CurDir=$(pwd)

# run parameters
if [ "$1" = "-h" ]; then
cat << EOF
usage: vtune-daal.sh <type> <totaliter> <histogram> <threadnum>
	type: collect type supported on haswell
	advanced-hotspots    Advanced Hotspots
	concurrency          Concurrency
	cpugpu-concurrency   CPU/GPU Concurrency
	general-exploration  General Exploration
	hotspots             Basic Hotspots
	locksandwaits        Locks and Waits
	memory-access        Memory Access
	tsx-exploration      TSX Exploration
	tsx-hotspots         TSX Hotspots
EOF

	exit 1
fi

type=$1
totaliter=$2
histogram=$3
threadnum=$4

if [ -z "$type" ]; then
    type=hotspots
fi

if [ -z "$totaliter" ]; then
    totaliter=100
fi
if [ -z "$histogram" ]; then
    histogram=1
fi
if [ -z "$threadnum" ]; then
    threadnum=0
fi

if [ $histogram -eq 1 ]; then
	gbtmode="inexact"
else
	gbtmode="exact"
fi
echo "Run experiments with type=$type,iter=$totaliter, histogram=$histogram, threadnum=$threadnum"

Arch=$HOSTNAME

## get name from arg of script
Name=daalgbt-icc-vtu

## action
action=collect
# action=collect-with

## collect type supported on haswell
#type=advanced-hotspots
#type=general-exploration
#type=concurrency
#type=locksandwaits
#type=hotspots
#type=memory-access

# advanced-hotspots    Advanced Hotspots
# concurrency          Concurrency
# cpugpu-concurrency   CPU/GPU Concurrency
# general-exploration  General Exploration
# hotspots             Basic Hotspots
# locksandwaits        Locks and Waits
# memory-access        Memory Access
# tsx-exploration      TSX Exploration
# tsx-hotspots         TSX Hotspots

## collect with type
# type=runsa

## options
sec=120 # around 10 itrs for warplad 8 threads 
#thread=50

# profiling mode, native, mixed, auto
mode=native 
# unlimited collection data
dlimit=0 
path_daal=/scratch_hdd/hpda/optgbt/test/vtune/bin/
path_tbb=/opt/intel/compilers_and_libraries/linux/tbb/lib/intel64_lin/gcc4.4
src_path_daal=/scratch_hdd/hpda/optgbt/daal2019-intel/daal/algorithms/
src_path_daalgbt=/N/u/pengb/hpda/gbt-test/sub/daalgbt/src/
src_path_tbb=/opt/intel/compilers_and_libraries/linux/tbb

## knob option
# knob_runsa_cache=MEM_LOAD_UOPS_RETIRED.L1_HIT,MEM_LOAD_UOPS_RETIRED.L2_HIT,MEM_LOAD_UOPS_RETIRED.L3_HIT,MEM_LOAD_UOPS_RETIRED.L1_MISS,MEM_LOAD_UOPS_RETIRED.L2_MISS,MEM_LOAD_UOPS_RETIRED.L3_MISS
knob_runsa_avx=INST_RETIRED.ANY,UOPS_EXECUTED.CORE,UOPS_RETIRED.ALL_PS,MEM_UOPS_RETIRED.ALL_LOADS_PS,MEM_UOPS_RETIRED.ALL_STORES_PS,AVX_INSTS.ALL

## result path
obj=R-$Arch-$Name-iter$totaliter-t$threadnum-$action-$type-$gbtmode
resDir=/scratch_hdd/hpda/optgbt/test/vtune/logs/$obj
echo "Result dir: $resDir"
if [ -d $resDir ]; then
    echo "remove existed result folder"
    rm -rf $resDir
fi

# check if the flag is triggered by the running program
# trigger_flag=/scratch/fg474admin/LDA-langshi-Exp/profiling/work/vtune-flag.txt
trigger_flag=./vtune-flag.txt
if [ -f $trigger_flag ]; then
    rm $trigger_flag 
    echo "old trigger flag cleaned"
fi

trigger_vtune=0
max_wait_time=0

# launch the program
#Usage: daalgbt <trainfile> <testfile> <epoch> <nClasses> <nFeatures> <treedepth> <splitMethod> <memorySaveMode> <shrinkage> <categoricalFeatures> <threadnum> 
#	splitMethod ;  1 by default inexact mode, set 0 to exact mode
#	memorySaveMode ; 0 by default
#	categoricalFeatures ; ',' separated feature ids, 'x' by default means non-categoriy features
#	threadnum   ; set the threadnumber, set to existing physical core number by default
echo "../bin/daalgbt-icc-vtune higgs_train.csv higgs_valid.csv $totaliter 2 28 6 $histogram 0 0.1 x $threadnum "
../bin/daalgbt-icc-vtune higgs_train.csv higgs_valid.csv $totaliter 2 28 6 $histogram 0 0.1 x $threadnum &
exec_pid=$!

# sleep 10

## get the process name
# pid=$(ps -u fg474admin | grep "$Name" | awk '{ print $1 }')
# echo "Profiling pid: $pid"

wait_file() {

    local file="$1"; shift
    local wait_seconds="${1:-10}"; shift # 10 seconds as default timeout
    max_wait_time=$wait_seconds; 
    until test $((wait_seconds--)) -eq 0 -o -f "$file" ; do sleep 1; done
    echo "current wait_seconds: $wait_seconds"
    if (( $wait_seconds != -1 )); then
        trigger_vtune=1
    fi
}

wait_file "$trigger_flag" 5000

if (( $trigger_vtune == 1 )); then
    echo "vtune triggered"
## get the process name
pid=$(ps -u $USER | grep "$Name" | awk '{ print $1 }')
echo "Profiling pid: $pid"

    # -k event-config=$knob_runsa_cache \
    # -k event-config=$knob_runsa_avx \
# start collect data
amplxe-cl -$action $type \
    -mrte-mode=$mode \
    -data-limit=$dlimit \
    -duration=$sec \
    -r $resDir \
    -search-dir $path_daal \
    -search-dir $path_tbb \
    -source-search-dir $src_path_daal \
    -source-search-dir $src_path_daalgbt \
    -source-search-dir $src_path_tbb \
    -target-pid $pid

## start generate reports
report_type=summary
group=function

amplxe-cl -report $report_type \
    -result-dir $resDir \
    -report-output $CurDir/$obj-$report_type-$group.csv -format csv -csv-delimiter comma

report_type=$type
group=thread,function
amplxe-cl -report $report_type \
    -result-dir $resDir \
    -report-output $CurDir/$obj-$report_type-$group.csv -format csv -csv-delimiter comma
else
    echo "vtune not triggered after $max_wait_time seconds elapsed"
fi

wait $exec_pid
echo -e "Profiling Finished"

#
#
