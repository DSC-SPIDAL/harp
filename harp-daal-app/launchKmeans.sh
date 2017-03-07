#!/bin/bash

cp build/harp-daal-app-hadoop-2.6.0.jar ${HADOOP_HOME}/

hdfs dfsadmin -safemode get | grep -q "ON"
if [[ "$?" = "0" ]]; then
	hdfs dfsadmin -safemode leave
fi

cd ${HADOOP_HOME}

bin/hadoop jar harp-daal-app-hadoop-2.6.0.jar edu.iu.kmeans.rotation.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans 


