profiling optapprox
======================

hostspot and memory-access profiling to the different versions of optapprox

results logs at: /share/jproject/fg474/share/optgbt/experiments/fullcompact

```
./vtune-xgb.sh memory-access higgs approx 10 24
./vtune-xgb.sh memory-access higgs hist 10 24
./vtune-xgb.sh memory-access higgs fullcompact 10 24
./vtune-xgb.sh memory-access higgs pmatfasthist 10 24

./vtune-daal.sh memory-access 10 1 24
```

### Results on j-128

    HalfTrick
    ==============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 33.016s
    CPU Time: 640.260s
    Memory Bound: 75.4% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 0.0% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 4.3% of Elapsed Time
    NUMA: % of Remote Accesses: 49.2%
    QPI Bandwidth Bound: 66.0% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 508,638,758,705
    Stores: 146,797,903,805
    LLC Miss Count: 18,069,834,125
    Local DRAM Access Count: 6,615,198,450
    Remote DRAM Access Count: 6,394,691,835
    Remote Cache Access Count: 4,571,137,130
    
    fullcompact
    ==============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 34.015s
    CPU Time: 663.485s
    Memory Bound: 75.3% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 0.0% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 2.7% of Elapsed Time
    NUMA: % of Remote Accesses: 49.6%
    QPI Bandwidth Bound: 66.4% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 517,826,534,330
    Stores: 140,060,201,680
    LLC Miss Count: 18,302,598,090
    Local DRAM Access Count: 6,737,702,125
    Remote DRAM Access Count: 6,643,199,290
    Remote Cache Access Count: 4,347,130,410
    
    approx
    ==============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 44.021s
    CPU Time: 846.035s
    Memory Bound: 72.5% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 0.8% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 0.0% of Elapsed Time
    NUMA: % of Remote Accesses: 49.6%
    QPI Bandwidth Bound: 60.3% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 679,058,871,155
    Stores: 207,804,733,955
    LLC Miss Count: 20,712,492,675
    Local DRAM Access Count: 7,945,238,350
    Remote DRAM Access Count: 7,815,734,465
    Remote Cache Access Count: 4,714,641,435
    Average Latency (cycles): 23
    Total Thread Count: 24
    Paused Time: 0s
    
    Hist
    ==============
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 13.007s
    CPU Time: 169.485s
    Memory Bound: 47.2% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 13.1% of Clockticks
    | This metric shows how often machine was stalled without missing the
    | L1 data cache. The L1 cache typically has the shortest latency.
    | However, in certain cases like loads blocked on older stores, a load
    | might suffer a high latency even though it is being satisfied by the
    | L1.
    |
    DRAM Bound
    DRAM Bandwidth Bound: 8.2% of Elapsed Time
    NUMA: % of Remote Accesses: 56.6%
    QPI Bandwidth Bound: 1.8% of Elapsed Time
    Loads: 82,255,967,605
    Stores: 29,302,879,060
    LLC Miss Count: 570,534,230
    Local DRAM Access Count: 161,004,830
    Remote DRAM Access Count: 210,006,300
    Remote Cache Access Count: 7,000,210
    Average Latency (cycles): 26
    Total Thread Count: 24
    Paused Time: 0s
    
    --
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 10.506s
    CPU Time: 143.750s
    Memory Bound: 45.2% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 9.7% of Clockticks
    | This metric shows how often machine was stalled without missing the
    | L1 data cache. The L1 cache typically has the shortest latency.
    | However, in certain cases like loads blocked on older stores, a load
    | might suffer a high latency even though it is being satisfied by the
    | L1.
    |
    DRAM Bound
    DRAM Bandwidth Bound: 3.6% of Elapsed Time
    NUMA: % of Remote Accesses: 21.5%
    QPI Bandwidth Bound: 1.3% of Elapsed Time
    Loads: 72,322,669,615
    Stores: 23,499,704,970
    LLC Miss Count: 596,785,805
    Local DRAM Access Count: 371,011,130
    Remote DRAM Access Count: 101,503,045
    Remote Cache Access Count: 10,500,315
    Average Latency (cycles): 29
    Total Thread Count: 24
    Paused Time: 0s
    
    daalgbt-icc
    ===================
    amplxe: Executing actions 75 % Generating a report Elapsed Time: 12.008s
    CPU Time: 137.585s
    Memory Bound: 79.1% of Pipeline Slots
    | The metric value is high. This may indicate that a significant fraction
    | of execution pipeline slots could be stalled due to demand memory load
    | and stores. Explore the metric breakdown by memory hierarchy, memory
    | bandwidth information, and correlation by memory objects.
    |
    L1 Bound: 1.9% of Clockticks
    DRAM Bound
    DRAM Bandwidth Bound: 41.6% of Elapsed Time
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
    NUMA: % of Remote Accesses: 44.9%
    QPI Bandwidth Bound: 44.3% of Elapsed Time
    | The system spent much time heavily utilizing QPI/UPI bandwidth.
    | Improve data accesses using NUMA optimizations on a multi-socket
    | system.
    |
    Loads: 61,871,356,085
    Stores: 19,033,570,990
    LLC Miss Count: 1,267,076,020
    Local DRAM Access Count: 567,017,010
    Remote DRAM Access Count: 462,013,860
    Remote Cache Access Count: 35,001,050
    Average Latency (cycles): 66
    Total Thread Count: 24
    Paused Time: 0s
