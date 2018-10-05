#!/usr/bin/env bash

if [ -z "$MyLabSRoot" ];then
    export MyLabSRoot=/N/u/lc37/Project/LabSession/harp
fi

hdfs dfs -mkdir -p /daal_cov
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_cov/daal_cov_dense/* /daal_cov

hdfs dfs -mkdir -p /daal_mom
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_mom/daal_mom_dense/* /daal_mom

hdfs dfs -mkdir -p /daal_naive
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_naive/densedistri/train /daal_naive
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_naive/densedistri/test /daal_naive
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_naive/densedistri/groundTruth /daal_naive

hdfs dfs -mkdir -p /daal_nn
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_nn/train /daal_nn
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_nn/test /daal_nn
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_nn/groundTruth /daal_nn

hdfs dfs -mkdir -p /daal_qr
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_qr/daal_qr_dense/* /daal_qr

hdfs dfs -mkdir -p /daal_reg
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_reg/train /daal_reg
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_reg/test /daal_reg
hdfs dfs -copyFromLocal $MyLabSRoot/datasets/daal_reg/groundTruth /daal_reg
