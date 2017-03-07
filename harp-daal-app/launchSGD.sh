#!/bin/bash

cp build/harp-daal-app-hadoop-2.6.0.jar ${HADOOP_HOME}/

export LIBJARS=${DAALROOT}/lib/daal.jar
export Time=$(date +%y-%m-%d-%H:%M:%S)

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
    hdfs dfsadmin -safemode leave
fi

# Comma-separated list of shared libs
hdfs dfs -mkdir -p /Hadoop/Libraries                                                        
hdfs dfs -rm /Hadoop/Libraries/*                                                        

hdfs dfs -put ${DAALROOT}/lib/intel64_lin/libJavaAPI.so /Hadoop/Libraries/               
hdfs dfs -put ${DAALROOT}/../tbb/lib/intel64_lin/gcc4.4/libtbb* /Hadoop/Libraries/   
hdfs dfs -put ${DAALROOT}/../../daal-misc/lib/libiomp5.so /Hadoop/Libraries/     

hdfs dfs -mkdir -p /Hadoop/sgd-work
hdfs dfs -rm -r /Hadoop/sgd-work/*                                                        

cd ${HADOOP_HOME}

# ---------------------- test Harp-DAAL ----------------------

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/netflix-train 40 0.05 0.002 10 2 40 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/netflix-test  
# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/movielens-train 128 0.003 0.005 5 2 40 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/movielens-test  
bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /Hadoop/sgd-input/yahoomusic-train 400 1 0.0001 5 6 30 0 2 false /Hadoop/sgd-work /Hadoop/sgd-input/yahoomusic-test  

# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /f-hugewiki-train-II 200 0.01 0.004 5 30 30 0 2 false /Hadoop/sgd-work /f-hugewiki-test-II  
# bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.daal_sgd.SGDDaalLauncher -libjars ${LIBJARS} /clueweb-30B-ccd-sgd-2 2000 0.01 0.001 100 30 30 1000 2 true /Hadoop/sgd-work /clueweb-30B-ccd-sgd-test-2  





