profiling optapprox
======================

hostspot and memory-access profiling to the different versions of optapprox

results logs at: 

noinittime: /share/jproject/fg474/share/optgbt/experiments/noinittime
withinittime: /share/jproject/fg474/share/optgbt/experiments/fullcompact

```
./vtune-xgb.sh memory-access higgs approx 10 24
./vtune-xgb.sh memory-access higgs hist 10 24
./vtune-xgb.sh memory-access higgs fullcompact 10 24
./vtune-xgb.sh memory-access higgs pmatfasthist 10 24

./vtune-daal.sh memory-access 10 1 24
```

### Results on j-128

+ more accurate profiling for 10 iterations by adding wait time for vtune to be ready, removing the initialization time

    HalfTrick
    ==============
    
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 33.517s
    CPU Time: 645.345s
    Memory Bound: 76.0% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 0.1% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 2.7% of Elapsed Time
    NUMA: % of Remote Accesses: 49.4%
    QPI Bandwidth Bound: 65.9% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 508,323,749,255
    Stores: 147,018,410,420
    LLC Miss Count: 17,761,815,645
    Local DRAM Access Count: 6,818,204,540
    Remote DRAM Access Count: 6,667,700,025
    Remote Cache Access Count: 4,445,133,350
    Average Latency (cycles): 28
    Total Thread Count: 24
    Paused Time: 0s
    
    Elapsed Time: 35.355 
    Paused Time: 0.0 
    CPU Time: 685.860
    Average CPU Utilization: 18.962 
    CPI Rate: 1.695 
    
    FullCompact
    ==============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 34.517s
    CPU Time: 668.590s
    Memory Bound: 75.2% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 0.0% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 0.0% of Elapsed Time
    NUMA: % of Remote Accesses: 49.5%
    QPI Bandwidth Bound: 66.3% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 509,093,772,355
    Stores: 136,738,602,035
    LLC Miss Count: 18,124,087,380
    Local DRAM Access Count: 6,541,696,245
    Remote DRAM Access Count: 6,412,192,360
    Remote Cache Access Count: 4,578,137,340
    Average Latency (cycles): 27
    Total Thread Count: 24
    Paused Time: 0s
    
    Elapsed Time: 36.795 
    Paused Time: 0.0 
    CPU Time: 709.967
    Average CPU Utilization: 18.877 
    CPI Rate: 1.714 
    
    hist
    ================
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 11.006s
    CPU Time: 133.000s
    Memory Bound: 50.9% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 10.5% of Clockticks
    | This metric shows how often machine was stalled without missing the
    | L1 data cache. The L1 cache typically has the shortest latency.
    | However, in certain cases like loads blocked on older stores, a load
    | might suffer a high latency even though it is being satisfied by the
    | L1.
    |
    DRAM Bound
    DRAM Bandwidth Bound: 4.3% of Elapsed Time
    NUMA: % of Remote Accesses: 18.8%
    QPI Bandwidth Bound: 0.2% of Elapsed Time
    Loads: 63,127,893,780
    Stores: 17,882,036,445
    LLC Miss Count: 535,532,130
    Local DRAM Access Count: 273,008,190
    Remote DRAM Access Count: 63,001,890
    Remote Cache Access Count: 10,500,315
    Average Latency (cycles): 26
    Total Thread Count: 24
    Paused Time: 0s
    
    Elapsed Time: 9.774 
    Paused Time: 0.0 
    CPU Time: 132.242
    Average CPU Utilization: 8.391 
    CPI Rate: 1.805 
    daal-icc
    ===============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 15.011s
    CPU Time: 283.375s
    Memory Bound: 82.9% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 1.2% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 65.7% of Elapsed Time
    | The system spent much time heavily utilizing DRAM bandwidth.
    | Improve data accesses to reduce cacheline transfers from/to
    | memory using these possible techniques: 1) consume all bytes of
    | each cacheline before it is evicted (for example, reorder
    | structure elements and split non-hot ones); 2) merge compute-
    | limited and bandwidth-limited loops; 3) use NUMA optimizations on
    | a multi-socket system. Note: software prefetches do not help a
    | bandwidth-limited application. Run Memory Access analysis to
    | identify data structures to be allocated in High Bandwidth Memory
    | (HBM), if available.
    |
    NUMA: % of Remote Accesses: 47.0%
    QPI Bandwidth Bound: 74.6% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 117,243,017,185
    Stores: 34,297,528,895
    LLC Miss Count: 2,521,901,305
    Local DRAM Access Count: 1,228,536,855
    Remote DRAM Access Count: 1,088,532,655
    Remote Cache Access Count: 133,003,990
    Average Latency (cycles): 74
    Total Thread Count: 24
    Paused Time: 0s
    
    Elapsed Time: 15.838 
    Paused Time: 0.0 
    CPU Time: 279.765
    Average CPU Utilization: 17.476 
    CPI Rate: 2.977 
