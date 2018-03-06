####COMPILE

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 
and 2.9.0

```
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0
cd $HARP_ROOT_DIR/contrib
cp $HARP_ROOT_DIR/distribution/hadoop-2.6.0/contrib-1.0.SNAPSHOT.jar $HADOOP_HOME
```

####RUN EXAMPLE
```bash
cd $HADOOP_HOME
```

Usage:
```bash
hadoop jar contrib-1.0.SNAPSHOT.jar edu.iu.kmeans.common.KmeansMapCollective <numOfDataPoints> <num of Centroids> <size of vector> <number of map tasks> <number of iteration> <workDir> <localDir> <communication operation>

 
   <numOfDataPoints>: the number of data points you want to generate randomly
   <num of centriods>: the number of centroids you want to clustering the data to
   <size of vector>: the number of dimension of the data
   <number of map tasks>: number of map tasks
   <number of iteration>: the number of iterations to run
   <work dir>: the root directory for this running in HDFS
   <local dir>: the harp kmeans will firstly generate files which contain data points to local directory. Set this argument to determine the local directory.
   <communication operation> includes:
		[allreduce]: use allreduce operation to synchronize centroids
		[regroup-allgather]: use regroup and allgather operation to synchronize centroids 
		[broadcast-reduce]: use broadcast and reduce operation to synchronize centroids
		[push-pull]: use push and pull operation to synchronize centroids
```

Run:

```
hadoop jar contrib-1.0.SNAPSHOT.jar edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 10 /kmeans /tmp/kmeans allreduce
```

####FETCH RESULTS
```
hdfs dfs -ls /
hdfs dfs -cat /kmeans/centroids/*
```



