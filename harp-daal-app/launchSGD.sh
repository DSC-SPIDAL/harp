#!/bin/bash

cp build/harp-daal-app-hadoop-2.6.0.jar ${HADOOP_HOME}/

export LIBJARS=${DAALROOT}/lib/daal.jar
export Time=$(date +%y-%m-%d-%H:%M:%S)

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi

hdfs dfs -mkdir -p /Hadoop/Libraries                                                        
hdfs dfs -rm /Hadoop/Libraries/*                                                        

# Comma-separated list of shared libs
hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/               
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/   
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/     

# hdfs dfs -mkdir -p /Hadoop/sgd-input
# hdfs dfs -rm -r /Hadoop/sgd-input/*                                                        

# hdfs dfs -put /N/u/lc37/TrainingData/netflix-train /Hadoop/sgd-input/
# hdfs dfs -put /N/u/lc37/TrainingData/netflix-test /Hadoop/sgd-input/

# hdfs dfs -put /N/u/lc37/TrainingData/yahoomusic-train /Hadoop/sgd-input/
# hdfs dfs -put /N/u/lc37/TrainingData/yahoomusic-test /Hadoop/sgd-input/

# hdfs dfs -put /N/u/lc37/TrainingData/movielens-train /Hadoop/sgd-input/
# hdfs dfs -put /N/u/lc37/TrainingData/movielens-test /Hadoop/sgd-input/

hdfs dfs -mkdir -p /Hadoop/sgd-work
hdfs dfs -rm -r /Hadoop/sgd-work/*                                                        

cd ${HADOOP_HOME}

# ---------------------- test Harp-DAAL ----------------------

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/netflix-train 40 0.05 0.002 10 2 40 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/netflix-test  

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/movielens-train 128 0.003 0.005 5 2 40 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/movielens-test  

bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/yahoomusic-train 128 1 0.0001 5 2 40 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/yahoomusic-test  

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /f-hugewiki-train-II 128 0.01 0.004 10 30 30 0 2 false /Hadoop/sgd-work /f-hugewiki-test-II  

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /f-hugewiki-train 128 0.01 0.0008 10 30 30 0 2 false /Hadoop/sgd-work /f-hugewiki-test  

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /hugewiki-train 128 0.01 0.004 5 30 30 0 2 false /Hadoop/sgd-work /hugewiki-test  




