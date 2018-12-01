DenseChainBlock
===================

@11302018

@goal: check the influence of using different model layout

chainblock version write model in <fid, nid, binid>, then adjust the model layout to <fid, nid, binid>

updater version:

    commit 09a129576759e1409d35f16c6eccc65d516e6e73
    Date:   Sat Dec 1 11:10:35 2018 -0500
    blockscheduler:: change model layout to fid-nid-binid, get performance boost

### experiment setting

j-127

nthread = 28  ;  make it load balancing
higgs   ; dense version

nthread = 25  ;  make it load balancing
synset   ; (old)dense version


### Conclusion:

1. model layout is important. To avoid the intervenes of threads writes, each thread should have its own writing memory.
fid-nid-binid is a better layout for feature wised parallelism (chainblock).

### todo:

+ nohalftrick is still much faster than halftrick, why?
+ As the aux_time shows that the parent-leftsum is quite fast, less than 1 s in higgs, the only difference is the condition check


### results

    trainer                     updater             traintime   bulidhist   buildposset

    synset

    nohalftrick-noprefetch-fnb  hist                232.944 203.179 21.7555
    nohalftrick-prefetch-fnb    hist                223.934 202.653 13.4872
    halftrick-noprefetch-fnb    hist                105.143 78.7093 18.3366
    halftrick-prefetch-fnb      hist                108.703 82.9224 17.8879
    nohalftrick-noprefetch-fnb  pmatfasthist        103.039 82.1083 14.9791
    nohalftrick-prefetch-fnb    pmatfasthist        91.9445 71.1129 15.2608
    halftrick-noprefetch-fnb    pmatfasthist        134.856 114.473 14.9713
    halftrick-prefetch-fnb      pmatfasthist        122.003 101.036 15.4438
    
    higgs 
    
    nohalftrick-noprefetch-fnb  hist                95.1211 65.6564 19.0612
    nohalftrick-prefetch-fnb    hist                93.5479 65.8921 17.4345
    halftrick-noprefetch-fnb    hist                52.845  24.282  18.2644
    halftrick-prefetch-fnb      hist                58.0804 26.2135 19.1851
    nohalftrick-noprefetch-fnb  pmatfasthist        44.3397 25.5682 13.0329
    nohalftrick-prefetch-fnb    pmatfasthist        43.5322 24.7114 12.5475
    halftrick-noprefetch-fnb    pmatfasthist        57.4447 38.2115 12.9463
    halftrick-prefetch-fnb      pmatfasthist        48.6709 30.1151 12.7773
                         
    nohalftrick-noprefetch-fnb  hist                90.0696 59.7317 19.0894
    nohalftrick-prefetch-fnb    hist                93.1657 63.9184 18.6072
    halftrick-noprefetch-fnb    hist                56.5455 25.9162 20.0284
    halftrick-prefetch-fnb      hist                59.1924 27.2287 20.1854
    nohalftrick-noprefetch-fnb  pmatfasthist        46.8577 27.5184 12.9358
    nohalftrick-prefetch-fnb    pmatfasthist        39.984  22.2434 12.5894
    halftrick-noprefetch-fnb    pmatfasthist        52.3913 34.8536 12.4687
    halftrick-prefetch-fnb      pmatfasthist        51.4162 31.9098 12.9477
    

