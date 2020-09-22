#!/bin/bash

## this script profiles an program by attaching the process id to VTune
## it requires that a vtune-flag.txt will be generated from tuning program
## this script will periodically detect the existence of vtune-flag.txt file 
## the trigger should be placed at the startpoint of target code snippet

## write trigger file to disk in Java app
# import java.nio.file.*;
# import java.io.IOException;
# // write vtune flag files to disk
# java.nio.file.Path vtune_file = java.nio.file.Paths.get("vtune-flag.txt");
# String flag_trigger = "Start training process and trigger vtune profiling.";
# try {
#  java.nio.file.Files.write(vtune_file, flag_trigger.getBytes());
# }catch (IOException e)
# {}

## write trigger file to disk in cpp app
# #include <iostream>
# #include <fstream>
# using namespace std;
#
# ofstream vtune_trigger;
# vtune_trigger.open("vtune-flag.txt");
# vtune_trigger << "Start training process and trigger vtune profiling.\n";
# vtune_trigger.close();


## ------------------------------ set up general information ------------------------------
## load the vtune 2018
source /opt/intel/vtune_amplifier_2018/amplxe-vars.sh 1>>/dev/null

CurDir=$(pwd)
Arch=skl
lang=cpp
userid=lc37

## ------------------------------ set up vtune ------------------------------

## action type runss or runsa
action=collect
# action=collect-with

## collect type supported on Arch 
## use command: amplxe-cl -h collect to display the supported 

# hotspots            Hotspots                                                                                               | 84 # type=concurrency
# hpc-performance  HPC Performance Characterization                                                                          | 85 # type=locksandwaits
# io                  Input and Output                                                                                       | 86 else
# memory-access  Memory Access                                                                                               | 87     type=runsa
# memory-consumption  Memory Consumption                                                                                     | 88 fi
# threading           Threading                                                                                              | 92 # trigger_flag=/scratch/fg474admin/LDA-langshi-Exp/profiling/work/vtune-flag.txt

## collect-with type runsa requires you to give hardware counters specified in knob option -k event-config
## knob option
### for profiling memory access
## for hsw
# knob_runsa=MEM_LOAD_UOPS_RETIRED.L1_HIT,MEM_LOAD_UOPS_RETIRED.L2_HIT,MEM_LOAD_UOPS_RETIRED.L3_HIT,MEM_LOAD_UOPS_RETIRED.L1_MISS,MEM_LOAD_UOPS_RETIRED.L2_MISS,MEM_LOAD_UOPS_RETIRED.L3_MISS
## for skl
# knob_runsa=MEM_INST_RETIRED.ALL_LOADS_PS,MEM_INST_RETIRED.ALL_STORES_PS,MEM_LOAD_RETIRED.L1_HIT_PS,MEM_LOAD_RETIRED.L2_HIT_PS,MEM_LOAD_RETIRED.L3_HIT_PS,MEM_LOAD_RETIRED.L1_MISS_PS,MEM_LOAD_RETIRED.L2_MISS_PS,MEM_LOAD_RETIRED.L3_MISS_PS
### for profiling avx codes
## for hsw
# knob_runsa=INST_RETIRED.ANY,UOPS_EXECUTED.CORE,UOPS_RETIRED.ALL_PS,MEM_UOPS_RETIRED.ALL_LOADS_PS,MEM_UOPS_RETIRED.ALL_STORES_PS,AVX_INSTS.ALL
## for skl
knob_runsa=INST_RETIRED.ANY,FP_ARITH_INST_RETIRED.128B_PACKED_SINGLE,FP_ARITH_INST_RETIRED.256B_PACKED_SINGLE,FP_ARITH_INST_RETIRED.512B_PACKED_SINGLE

## vtune amplxe-cl options
sec=50 # around 10 itrs for warplad 8 threads 
# profiling mode, native, mixed, auto
# mode=native 
if [ "$lang" == "cpp" ];then
    mode=native 
else
    mode=mixed
fi

# unlimited collection data
dlimit=0 
## set up the search dir for lib and src  
path1=/opt/intel/mkl/lib/intel64
path2=/opt/intel/mkl/lib/intel64_lin
# path2=/opt/intel/compilers_and_libraries_2016/linux/tbb/lib/intel64_lin/gcc4.4
src_path1=/N/u/lc37/Project/sc-mat
# src_path1=/N/u/lc37/Project/sc-vec

if [ "$action" == "collect" ];then
type=hpc-performance
# type=hotspots
# type=memory-access
else
    type=runsa
fi

### ------------------------------- set up trigger func -------------------------------

# trigger_flag=/scratch/fg474admin/LDA-langshi-Exp/profiling/work/vtune-flag.txt
trigger_flag=$CurDir/vtune-flag.txt
# the maximal waited time seconds by vtune trigger
time_limit=5000

## $1 arg is the trigger flag filename 
## $2 arg is the maximal waited time
## each calling of shift will load next arg into $1
wait_file() {

    local file="$1"; shift
    local wait_seconds="${1:-10}"; shift # 10 seconds as default timeout
    max_wait_time=$wait_seconds; 
    until test $((wait_seconds--)) -eq 0 -o -f "$file" ; do sleep 1; done
    echo "current wait_seconds: $wait_seconds"
    if (( $wait_seconds != -1 )); then
        ## if trigger file is detected
        trigger_vtune=1
    fi
}

## ------------------------------- profiled programs or scripts -------------------------------
# exe_dir=/N/u/lc37/Project/sc-mat
exe_dir=${CurDir}
scriptName=sg2vec_run_t-006.sh
executor=sc-skl-icc.bin
# executor=sc-hsw-icc-bin
run_arg1=$1
run_arg2=$2
run_arg3=$3

## ------------------------------- set up result path and dir -------------------------------
# obj=R-SG2VEC-$Arch-$executor-$run_arg1-$run_arg2-$run_arg3-$action-$type-vectorization
# obj=R-SG2VEC-$Arch-$executor-$run_arg1-$run_arg2-$run_arg3-$action-$type-memcache
obj=R-SG2VEC-$Arch-$executor-$run_arg1-$run_arg2-$run_arg3-$action-$type
# resDir=/scratch_hdd/lc37/VTuneRes/$obj
# mkdir -p ${CurDir}/VTuneResult
mkdir -p ${CurDir}/VTuneResultNew 
resDir=${CurDir}/VTuneResultNew/$obj
reportDir=$CurDir

if [ -d $resDir ]; then
    echo "remove existed res folder"
    rm -rf $resDir
fi

mkdir -p ${resDir}

### ------------------------ start the script ------------------------

### clear the trigger file of last experiment
if [ -f $trigger_flag ]; then
    rm $trigger_flag 
    echo "old trigger flag cleaned"
fi

trigger_vtune=0
max_wait_time=0

## launch the program in background
# $exe_dir/$executor $run_arg1 $run_arg2 $run_arg3 &
$exe_dir/$scriptName &
exec_pid=$!

## launch the vtune trigger func
wait_file "$trigger_flag" $time_limit

## check the trigger val
if (( $trigger_vtune == 1 )); then
    echo "vtune triggered"

## fetch the executor process name
if [ "$lang" == "java" ];then
    pid=$(jps | grep "YarnChild" | awk '{ print $1  }')
else
## pay attention to executor name, may be different in ps -u 
    pid=$(ps -u "$userid" | grep "$executor" | awk '{ print $1 }')
fi

## check wether pid is captured
if [ -z "$pid" ];then
    echo "pid not capatured"
    exit
else
    echo "Profiling pid: $pid"
fi

## start the vtune profiling

if [ "$action" == "collect" ];then

    amplxe-cl -$action $type \
    -mrte-mode=$mode \
    -data-limit=$dlimit \
    -duration=$sec \
    -result-dir=$resDir \
    -search-dir $path1 \
    -search-dir $path2 \
    -source-search-dir $src_path1 \
    -target-pid $pid

else

    amplxe-cl -$action $type \
    -k event-config=$knob_runsa \
    -mrte-mode=$mode \
    -data-limit=$dlimit \
    -duration=$sec \
    -result-dir=$resDir \
    -search-dir $path1 \
    -search-dir $path2 \
    -source-search-dir $src_path1 \
    -target-pid $pid

fi

## start generating reports

for report_type in summary hotspots
do
    if [ "$report_type" == "summary" ];then
        group=function
    else
        group=thread,function
    fi

    amplxe-cl -report $report_type \
    -result-dir $resDir \
    -report-output $reportDir/$obj-$report_type-$group.csv -format csv -csv-delimiter comma
done

else
    ### vtune is not triggered 
    echo "vtune not triggered after $max_wait_time seconds elapsed"
fi

## wait for the termination of profiled executor
wait $exec_pid
echo -e "Profiling Finished"

