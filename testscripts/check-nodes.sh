#!/bin/sh

## check the cpu types of all reserved nodes

for ip in j-007 j-008 j-009 j-010 j-013 j-014 j-016 j-028 j-038 j-057 j-070 j-079 j-090 j-091 j-095 j-099
do

echo "Check ${ip}"
ssh ${ip} "lscpu" | grep "Core(s) per socket:" 

done  
