---
title: Quick Start Guide
description: Run a single-node Harp on your machine
aliases:
  - /docs/install.html
---

These instructions have only been tested on:

* Mac OS X
* Ubuntu

If you are using windows, we suggest you to install an Ubuntu system on a virtualization software (e.g. VirtualBox) with at least 4GB memory in it.

## Prerequisites

1. Vim (or any other text editor)

2. Java

3. [Maven](https://maven.apache.org/install.html) 

4. ssh


## Step 1 --- Install Hadoop 2.6.0

1. Download and extract the hadoop-2.6.0 binary into your machine. It's available at [hadoop-2.6.0.tar.gz](https://archive.apache.org/dist/hadoop/core/hadoop-2.6.0/hadoop-2.6.0.tar.gz).
    ```bash
    $ mkdir ~/Hadoop
    $ cd ~/Hadoop
    $ wget https://archive.apache.org/dist/hadoop/core/hadoop-2.6.0/hadoop-2.6.0.tar.gz
    $ tar -xvzf hadoop-2.6.0.tar.gz
    ```

2. Set the environment variables in file `~/.bashrc`.
    ```bash
    $ vim ~/.bashrc
    ```
    Add the following text to the file and update the values for `<where Java locates>` and 
    `<where hadoop locates>` with the path of where Java and Hadoop are located in your system. 
    ```bash
    export JAVA_HOME="<where Java locates>"
    #e.g. /usr/lib/jvm/java-1.8.0-openjdk-amd64
    export HADOOP_HOME="<where hadoop locates>"
    #e.g. ~/Hadoop/hadoop-2.6.0
    export YARN_HOME=$HADOOP_HOME
    export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
    export PATH=$HADOOP_HOME/bin:$JAVA_HOME/bin:$PATH
    ```

3. Run the following command to make sure the changes are applied.
    ```bash
    $ source ~/.bashrc
    ```

4. Check if environment variables are set correctly by running the following command.

    ```bash
    $ hadoop
    ```
    The results should look similar to the example below.
    ```bash
    Usage: hadoop [--config confdir] COMMAND
    where COMMAND is one of:
    fs                   run a generic filesystem user client
    version              print the version
    jar <jar>            run a jar file
    checknative [-a|-h]  check native hadoop and compression libraries availability
    distcp <srcurl> <desturl> copy file or directories recursively
    archive -archiveName NAME -p <parent path> <src>* <dest> create a hadoop archive
    classpath            prints the class path needed to get the
    credential           interact with credential providers
    Hadoop jar and the required libraries
    daemonlog            get/set the log level for each daemon
    trace                view and modify Hadoop tracing settings
    or
    CLASSNAME            run the class named CLASSNAME
    Most commands print help when invoked w/o parameters.
    ```

5. Follow steps (i)-(iv) to modify the following files in the Apache Hadoop distribution.

    (i). `$HADOOP_HOME/etc/hadoop/core-site.xml`:
    ```bash
    $ vim $HADOOP_HOME/etc/hadoop/core-site.xml
    ```
    Copy the following text into the file and replace ${user.name} with your user name.
    ```xml
    <configuration>
      <property>
         <name>fs.default.name</name>
         <value>hdfs://localhost:9010</value>
      </property>
      <property>
         <name>hadoop.tmp.dir</name>
         <value>/tmp/hadoop-${user.name}</value>
         <description>A base for other temporary directories.</description>
      </property>
    </configuration>
    ```
    
    (ii).`$HADOOP_HOME/etc/hadoop/hdfs-site.xml`:
    ```bash
    $ vim $HADOOP_HOME/etc/hadoop/hdfs-site.xml
    ```
    Copy the following text into the file.
    ```xml
    <configuration>
      <property>
        <name>dfs.replication</name>
        <value>1</value>
      </property>
    </configuration>
    ```
    
    (iii).`$HADOOP_HOME/etc/hadoop/mapred-site.xml`:
    You will be creating this file. It doesn’t exist in the original package.
    ```bash
    $ vim $HADOOP_HOME/etc/hadoop/mapred-site.xml
    ```
    Copy the following text into the file. 
    ```xml
    <configuration>
      <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
      </property>
      <property>
        <name>yarn.app.mapreduce.am.resource.mb</name>
        <value>512</value>
      </property>
      <property>
        <name>yarn.app.mapreduce.am.command-opts</name>
        <value>-Xmx256m -Xms256m</value>
      </property>
    </configuration>
    ```
    
    (iv).`$HADOOP_HOME/etc/hadoop/yarn-site.xml`:
    ```bash
    $ vim $HADOOP_HOME/etc/hadoop/yarn-site.xml
    ```
    Copy the following text into the file.
    
    ```xml
    <configuration>
      <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
      </property>
      <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>4096</value>
      </property>
      <property>
        <description>Whether virtual memory limits will be enforced for containers.</description>
        <name>yarn.nodemanager.vmem-check-enabled</name>
        <value>false</value>
      </property>
      <property>
        <name>yarn.scheduler.minimum-allocation-mb</name>
        <value>512</value>
      </property>
      <property>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>2048</value>
      </property>
    </configuration>
    ```

6. Format the file system using the following code.
    ```bash
    $ hdfs namenode -format
    ```
    
    You should be able to see it exit with status 0 as follows.
    ```bash
    ...
    ...
    xx/xx/xx xx:xx:xx INFO util.ExitUtil: Exiting with status 0
    xx/xx/xx xx:xx:xx INFO namenode.NameNode: SHUTDOWN_MSG:
    /************************************************************
    SHUTDOWN_MSG: Shutting down NameNode at xxx.xxx.xxx.xxx
    ```

7. Launch NameNode, SecondaryNameNode and DataNode daemons.
    ```bash
    $ $HADOOP_HOME/sbin/start-dfs.sh
    ```

8. Launch ResourceManager and NodeManager Daemons.
    ```bash
    $ $HADOOP_HOME/sbin/start-yarn.sh
    ```

9. Check if the daemons started successfully by running the following command.
    ```bash
    $ jps
    ```

    The output should look similar to the following text with `xxxxx` replaced by the 
    process ids for "NameNode", "SecondaryNameNode", etc. 
    ```bash
    xxxxx NameNode
    xxxxx SecondaryNameNode
    xxxxx DataNode
    xxxxx NodeManager
    xxxxx Jps
    xxxxx ResourceManager
    ```

    If all the processes listed above aren't in your output recheck your configurations and
    rerun steps 6 through 8 after executing the following commands.
    
    Replace ${user.name} with the user name given in step 5 (i).
    ```bash
    $ $HADOOP_HOME/sbin/stop-dfs.sh
    $ $HADOOP_HOME/sbin/stop-yarn.sh
    $ rm -r /tmp/hadoop-${user.name} 
    ```

10. You can browse the web interface for the NameNode at [http://localhost:50070](http://localhost:50070) and for the ResourceManager at [http://localhost:8080](http://localhost:8080).

## Step 2 --- Install Harp

1. Clone Harp repository using the following command. It is available at [DSC-SPIDAL/harp](https://github.com/DSC-SPIDAL/harp.git).
    ```bash
    $ git clone https://github.com/DSC-SPIDAL/harp.git
    ```

2. Set the environment variables in file `~/.bashrc`.
    ```bash
    $ vim ~/.bashrc
    ```

    Add the following text into the file. Replace `<where Harp locates>` with the path of where Harp is located in 
    your system. 
    ```bash
    export HARP_ROOT_DIR=<where Harp locates>
    #e.g. ~/harp
    export HARP_HOME=$HARP_ROOT_DIR/core/
    ```

3. Run the following command to make sure the changes are applied.
    ```bash
    $ source ~/.bashrc
    ```

4. If hadoop is still running, stop it first with the following code.
    ```bash
    $ $HADOOP_HOME/sbin/stop-dfs.sh
    $ $HADOOP_HOME/sbin/stop-yarn.sh
    ```

5. Enter "harp" home directory using the following command.
    ```bash
    $ cd $HARP_ROOT_DIR
    ```

6. Compile harp

    Select the profile related to your hadoop version (For ex: hadoop-2.6.0) and compile using maven. 
    Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0.
    ```bash
    $ mvn clean package -Phadoop-2.6.0
    ```

7. Install harp plugin to hadoop as demonstrated below. 
    
    ```bash
    $ cp core/harp-collective/target/harp-collective-0.1.0.jar $HADOOP_HOME/share/hadoop/mapreduce/
    $ cp core/harp-hadoop/target/harp-hadoop-0.1.0.jar $HADOOP_HOME/share/hadoop/mapreduce/
    $ cp third_party/fastutil-7.0.13.jar $HADOOP_HOME/share/hadoop/mapreduce/
    ```

8. Edit mapred-site.xml in $HADOOP_HOME/etc/hadoop by using the following code.

    ```bash
    $ vim $HADOOP_HOME/etc/hadoop/mapred-site.xml
    ```

    Add java opts settings for map-collective tasks as follows. For example:
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

    You have completed the Harp installation.
    
    **Note**
    
    To develop Harp applications add the following property when configuring the job.
    ```java
    jobConf.set("mapreduce.framework.name", "map-collective");
    ```

## Step 3 --- Run harp kmeans example

1. Copy harp examples to `$HADOOP_HOME` using the following code. 
    ```bash
    $ cp $HARP_ROOT_DIR/ml/java/target/harp-java-0.1.0.jar $HADOOP_HOME
    ```

2. Start Hadoop. 
    ```bash
    $ cd $HADOOP_HOME
    $ sbin/start-dfs.sh
    $ sbin/start-yarn.sh
    ```

3. Run Kmeans Map-collective job. Make sure you are in the `$HADOOP_HOME` folder. 
The usage is
    ```bash
    hadoop jar harp-java-0.1.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher <num of points> <num of centroids> 
    <vector size> <num of point files per worker> <number of map tasks> <num threads> <number of iteration> <work dir> <local points dir>
    ```

   * `<num of points>` --- the number of data points you want to generate randomly
   * `<num of centriods>` --- the number of centroids you want to clustering the data to
   * `<vector size>` --- the number of dimension of the data
   * `<num of point files per worker>` --- how many files which contain data points in each worker
   * `<number of map tasks>` --- number of map tasks
   * `<num threads>` --- how many threads to launch in each worker
   * `<number of iteration>` --- the number of iterations to run
   * `<work dir>` --- the root directory for this running in HDFS
   * `<local points dir>` --- the harp kmeans will firstly generate files which contain data points to local directory. Set this argument to determine the local directory.

       For example:
   ```bash
   hadoop jar harp-java-0.1.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans
   ```

4. To fetch the results, use the following command:
    ```bash
    $ hdfs dfs –get <work dir> <local dir>
    #e.g. hdfs dfs -get /kmeans ~/Document
    ```
