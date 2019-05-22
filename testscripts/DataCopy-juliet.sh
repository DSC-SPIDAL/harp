#!/bin/sh

## copy data 
## j-010 is the base node 
tploc=/scratch_hdd/lc37/sc-vec/templates
gloc=/scratch_hdd/lc37/sc-vec/graphs

## failed connected nodes: j-090 j-091 j-070 j-099 has 18 cores

#for ip in j-007 j-008 j-009 j-013 j-014 j-016 j-028 j-038 j-057 j-079 j-095 j-099
for ip in j-007 j-008 j-009 j-013 j-014 j-016 j-028 j-038 j-057 j-079 j-095 j-099 j-090 j-091 j-070
do

echo "copy to ${ip}"

### copy templates
ssh ${ip} "mkdir -p ${tploc}"
scp ${tploc}/* ${ip}:${tploc}/   

## copy datasets
#ssh ${ip} "mkdir -p ${gloc}"
##for dataname in miami.graph orkut.graph nyc.graph \
    ##graph500-scale20-ef16_adj.mmio graph500-scale21-ef16_adj.mmio graph500-scale22-ef16_adj.mmio graph500-scale23-ef16_adj.mmio graph500-scale24-ef16_adj.mmio graph500-scale25-ef16_adj.mmio \
    ##RMAT-Data-sk-3-nV-1000000-nE-100000000.fascia RMAT-Data-sk-3-nV-4000000-nE-100000000.fascia RMAT-Data-sk-3-nV-4000000-nE-200000000.fascia RMAT-Data-sk-3-nV-4000000-nE-300000000.fascia 
#for dataname in twitter.graph
#do

#scp ${gloc}/${dataname} ${ip}:${gloc}/

#done
done
