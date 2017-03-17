# HARP

@Copyright 2013-2017 Inidana University
Apache License 2.0

@Author: Bingjing Zhang

## WHAT IS HARP?
Harp is a framework for machine learning applications.

## FEATURES
1. A Hadoop plugin. It currently supports hadoop 2.6.0 ~ 2.7.3 version.
2. Hierarchical data abstraction (arrays/objects, partitions/tables)
3. Pool based memory management
4. Collective + event-driven programming model (distributed computing)
5. Dynamic Scheduler + Static Scheduler (multi-threading)

## COMPILATION & INSTALLATION

#### 1. Install Maven by following the [maven official instruction](http://maven.apache.org/install.html)
#### 2. Enter "harp" home directory
#### 3. Compile harp
    mvn clean package

#### 4. Install harp plugin to hadoop
    cp harp-project/target/harp-project-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
    cp third_party/fastutil-7.0.13.jar $HADOOP_HOME/share/hadoop/mapreduce/

#### 5. Configure Hadoop environment for settings required to run Hadoop

#### 6. Edit mapred-site.xml in $HADOOP_HOME/etc/hadoop, add java opts settings for map-collective tasks. For example:
  ```xml
   <property>
     <name>mapreduce.map.collective.memory.mb</name>
     <value>512</value>
   </property>
   <property>
     <name>mapreduce.map.collective.java.opts</name>
     <value>-Xmx256m -Xms256m</value>
   </property>
   ```

#### 7. To develop Harp applications, remember to add the following property in job configuration:
    jobConf.set("mapreduce.framework.name", "map-collective");

## EXAMPLE

#### 1. copy harp examples to $HADOOP_HOME
    cp harp-app/target/harp-app-1.0-SNAPSHOT.jar $HADOOP_HOME

#### 2. Start Hadoop
    cd $HADOOP_HOME
    sbin/start-dfs.sh
    sbin/start-yarn.sh

#### 3. Run Kmeans Map-collective job
##### The usage is
```bash
hadoop jar harp-app-1.0-SNAPSHOT.jar edu.iu.kmeans.regroupallgather.KMeansLauncher <num of points> <num of centroids> <vector size> <num of point files per worker> <number of map tasks> <num threads> <number of iteration> <work dir> <local points dir>
```
##### For example:
```bash
hadoop jar harp-app-1.0-SNAPSHOT.jar edu.iu.kmeans.regroupallgather.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans
```
