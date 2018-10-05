#!/bin/bash

# root directory of the lab session harp installation 
export MyLabSRoot=/N/u/lc37/Project/LabSession/harp

## set up env vars 
export HARP_JAR=$MyLabSRoot/ml/java/target/harp-java-0.1.0.jar
export HARP_DAAL_JAR=$MyLabSRoot/ml/daal/target/harp-daal-0.1.0.jar
export DAALROOT=$MyLabSRoot/third_party/daal-2018
export PYTHONPATH=$MyLabSRoot/harp-python

## load daal so files into hdfs 
hdfs dfs -mkdir -p /Hadoop
hdfs dfs -mkdir -p /Hadoop/Libraries
hdfs dfs -rm /Hadoop/Libraries/*
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/

# bash copy-data.sh
python examples/daal/run_harp_daal_KMeansDaal.py
# python examples/daal/run_harp_daal_COVDaal.py
# python examples/daal/run_harp_daal_LinRegDaal.py
# python examples/daal/run_harp_daal_MOMDaal.py
# python examples/daal/run_harp_daal_NaiveDaal.py
# python examples/daal/run_harp_daal_NNDaal.py
# python examples/daal/run_harp_daal_PCADaal.py
# python examples/daal/run_harp_daal_QRDaal.py
# python examples/daal/run_harp_daal_RidgeRegDaal.py
# python examples/daal/run_harp_daal_SVDDaal.py
