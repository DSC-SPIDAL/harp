#COMPILE

cd $HARP3_PROJECT_HOME/harp3-app
ant
cp build/harp-app-hadoop-2.6.0.jar $HADOOP_HOME

#RUN
cd $HADOOP_HOME
   
hadoop jar harp3-app-hadoop-2.6.0.jar edu.iu.kmexample.common.KmeansMapCollective <numOfDataPoints> <num of Centroids> <size of vector> <number of map tasks> <number of iteration> <workDir> <localDir> <communication operation> 

 
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
					
#EXAMPLE
hadoop jar harp3-app-hadoop-2.6.0.jar edu.iu.kmexample.common.KmeansMapCollective 1000 10 10 2 10 /kmeans /tmp/kmeans allreduce


#FETCH RESULTS
hdfs dfs -ls /
hdfs dfs -cat /kmeans/centroids/*




