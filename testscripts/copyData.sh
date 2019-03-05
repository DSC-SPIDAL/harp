#!/bin/bash

## copy R1M200MK3
path=/dev/shm/lc37/fascia-data/graphs

for nodeip in t-010.tempest.futuresystems.org
do

# scp ${path}/*.mmio lc37@$nodeip:${path}/ 
scp ${path}/miami.graph lc37@$nodeip:${path}/ 
scp ${path}/orkut.graph lc37@$nodeip:${path}/ 
scp ${path}/nyc.graph lc37@$nodeip:${path}/ 

# scp ${path}/miami-csr-binary.data lc37@$nodeip:${path}/ 
# scp ${path}/orkut-csr-binary.data lc37@$nodeip:${path}/ 
# scp ${path}/nyc-csr-binary.data lc37@$nodeip:${path}/ 

# scp ${path}/RMAT-Data-sk-3-nV-1000000-nE-100000000.fascia lc37@$nodeip:${path}/
# scp ${path}/RMAT-Data-sk-3-nV-4000000-nE-100000000.fascia lc37@$nodeip:${path}/ 
# scp ${path}/RMAT-Data-sk-5-nV-4000000-nE-100000000.fascia lc37@$nodeip:${path}/ 
# scp ${path}/RMAT-Data-sk-8-nV-4000000-nE-100000000.fascia lc37@$nodeip:${path}/ 
#
# scp ${path}/RMAT-Data-sk-3-nV-1000000-nE-100000000-csr-binary.data lc37@$nodeip:${path}/ 
# scp ${path}/RMAT-Data-sk-3-nV-4000000-nE-*.data lc37@$nodeip:${path}/ 
# scp ${path}/RMAT-Data-sk-5-nV-4000000-nE-*.data lc37@$nodeip:${path}/ 
# scp ${path}/RMAT-Data-sk-8-nV-4000000-nE-*.data lc37@$nodeip:${path}/ 

done

