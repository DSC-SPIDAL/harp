# HARP

@Copyright 2013-2017 Inidana University
Apache License 2.0

@Author: Bingjing Zhang, Langshi Chen, Bo Peng, Sabra Ossen

## WHAT IS HARP?

Harp is a HPC-ABDS (High Performance Computing Enhanced Apache Big Data Stack) framework aiming to provide distributed 
machine learning and other data intensive applications. 

## Highlights

1. Plug into Hadoop ecosystem.
2. Rich computation models for different machine learning/data intensive applications
2. MPI-like Collective Communication operations 
4. High performance native kernels supporting many-core processors (e.g., Intel Xeon Phi) 

## COMPILATION & INSTALLATION

#### 1. Install Maven by following the [maven official instruction](http://maven.apache.org/install.html)
#### 2. Enter "harp" home directory
#### 3. Compile harp

```bash
	cd harp/
    mvn clean package
```
#### 4. Install harp Core Module 

```bash
    cp core/harp-collective/target/harp-collective-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
    cp core/harp-hadoop/target/harp-hadoop-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
	cp core/harp-daal-interface/harp-daal-interface-1.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce/
```

### 5. Install third party dependencies  

```bash 

    cp third_party/*.jar $HADOOP_HOME/share/hadoop/mapreduce/
	cp third_party/daal-2018/daal.jar $HADOOP_HOME/share/hadoop/mapreduce/

```

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

