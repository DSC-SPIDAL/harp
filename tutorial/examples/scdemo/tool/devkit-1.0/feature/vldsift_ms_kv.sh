#!/bin/bash


while read line
do
    ./vldsift_ms.sh $line | ./padkey
done