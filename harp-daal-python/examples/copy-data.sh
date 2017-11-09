#!/usr/bin/env bash

hdfs dfs -copyFromLocal /harp/datasets/daal_cov /daal_cov && \
hdfs dfs -copyFromLocal /harp/datasets/daal_mom /daal_mom && \
hdfs dfs -mkdir -p /daal_naive/groundTruth /daal_naive/test /daal_naive/train && \
hdfs dfs -copyFromLocal /harp/datasets/daal_naive/naivebayes_train_dense_* /daal_naive/train/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_naive/naivebayes_test_dense.csv /daal_naive/test/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_naive/naivebayes_test_labels.csv /daal_naive/groundTruth/ && \
hdfs dfs -mkdir -p /daal_nn/groundTruth /daal_nn/test /daal_nn/train && \
hdfs dfs -copyFromLocal /harp/datasets/daal_nn/neural_network_train_dense_* /daal_nn/train/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_nn/neural_network_test.csv /daal_nn/test/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_nn/neural_network_test_ground_truth.csv /daal_nn/groundTruth/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_qr /daal_qr && \
hdfs dfs -mkdir -p /daal_reg/groundTruth /daal_reg/test /daal_reg/train && \
hdfs dfs -copyFromLocal /harp/datasets/daal_reg/linear_regression_train_* /daal_reg/train/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_reg/linear_regression_test.csv /daal_reg/test/ && \
hdfs dfs -copyFromLocal /harp/datasets/daal_reg/linear_regression_test_groundTruth.csv /daal_reg/groundTruth/
