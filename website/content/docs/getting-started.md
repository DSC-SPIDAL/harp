---
title: Quick Start Guide
description: Run a single-node Harp on your machine
aliases:
  - /docs/install.html
---

This instruction is only available for:

* Mac OS X
* Ubuntu

If you are using windows, we suggest you to install an Ubuntu system on a virtualization software (e.g. VirtualBox) with at least 4GB memory in it.

## Step 1 --- Install Hadoop 2.6.0

First of all, make sure your computer can use `ssh` to access `localhost` and install `Java` as well.

Download and extract the hadoop-2.6.0 binary into your machine. These are available at [hadoop-2.6.0.tar.gz](https://dist.apache.org/repos/dist/release/hadoop/common/hadoop-2.6.0/hadoop-2.6.0.tar.gz).

Then set the environment variables in `~/.bashrc`.

```bash
export JAVA_HOME=<where Java locates>
#e.g. ~/jdk1.8.0_91
export HADOOP_HOME=<where hadoop-2.6.0 locates>
#e.g. ~/hadoop-2.6.0
export YARN_HOME=$HADOOP_HOME
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$HADOOP_HOME/bin:$JAVA_HOME/bin:$PATH
```
Now run

```bash
$ source ~/.bashrc
```
in order to make sure the changes are applied.

Check if you can successfully run Hadoop command and get the following output.

```bash
$ hadoop
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

Modify the following files in Apache Hadoop distribution.

`$HADOOP_HOME/etc/hadoop/core-site.xml`:

```html
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

`$HADOOP_HOME/etc/hadoop/hdfs-site.xml`:
```html
<configuration>
  <property>
    <name>dfs.replication</name>
    <value>1</value>
  </property>
  <property>
    <name>dfs.namenode.http-address</name>
    <value>localhost:50070</value>
  </property>
  <property>
    <name>dfs.namenode.secondary.http-address</name>
    <value>localhost:50190</value>
  </property>
</configuration>
```

`$HADOOP_HOME/etc/hadoop/mapred-site.xml`:

You will be creating this file. It doesn’t exist in the original package.
```html
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

`$HADOOP_HOME/etc/hadoop/yarn-site.xml`:
```html
<configuration>
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>localhost</value>
  </property>
  <property>
    <name>yarn.resourcemanager.address</name>
    <value>localhost:8132</value>
  </property>
  <property>
    <name>yarn.resourcemanager.scheduler.address</name>
    <value>localhost:8130</value>
  </property>
  <property>
    <name>yarn.resourcemanager.resource-tracker.address</name>
    <value>localhost:8131</value>
  </property>
  <property>
    <name>yarn.resourcemanager.admin.address</name>
    <value>localhost:8133</value>
  </property>
  <property>
    <name>yarn.resourcemanager.webapp.address</name>
    <value>localhost:8080</value>
  </property>
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

Next we format the file system and you should be able to see it exits with status 0.
```bash
$ hdfs namenode -format
...
xx/xx/xx xx:xx:xx INFO util.ExitUtil: Exiting with status 0
xx/xx/xx xx:xx:xx INFO namenode.NameNode: SHUTDOWN_MSG:
/************************************************************
SHUTDOWN_MSG: Shutting down NameNode at xxx.xxx.xxx.xxx
```

Launch NameNode daemon, DataNode daemon, ResourceManager daemon and NodeManager Daemon.

```bash
$ $HADOOP_HOME/sbin/start-dfs.sh
$ $HADOOP_HOME/sbin/start-yarn.sh
```

Check if the daemons started successfully with the following output.
```bash
$ jps
xxxxx NameNode
xxxxx SecondaryNameNode
xxxxx DataNode
xxxxx NodeManager
xxxxx Jps
xxxxx ResourceManager
```

You can browse the web interface for the NameNode at [http://localhost:50070](http://localhost:50070) and for the ResourceManager at [http://localhost:8080](http://localhost:8080).

## Step 2 --- Install Harp

Download Harp3-Project. It is available at [DSC-SPIDAL/Harp](https://github.com/DSC-SPIDAL/Harp.git).

We will use `ant` with Harp. Make sure you can run ant in your machine.

Then add environment variables in `~/.bashrc`.
```bash
export HARP3_PROJECT_HOME=<where Harp locates>
#e.g. ~/Harp3-Project
export HARP3_HOME=$HARP3_PROJECT_HOME/harp3
export ANT_HOME=<where ant locates>
#e.g. ~/apache-ant-1.9.7
export PATH=$ANT_HOME/bin:$PATH
```

If hadoop is still running, stop it first.
```bash
$ $HADOOP_PREFIX/sbin/stop-dfs.sh
$ $HADOOP_PREFIX/sbin/stop-yarn.sh
```

Next, we need to compile Harp.
```bash
$ cd $HARP3_HOME
$ ant
```

Copy `harp-0.3.0-hadoop-2.6.0.jar` and `fastutil-7.0.13.jar` from `$HARP3_HOME/lib/` into `$HADOOP_HOME/share/hadoop/mapreduce`.

```bash
$ cp $HARP3_HOME/lib/harp-0.3.0-hadoop-2.6.0.jar $HADOOP_HOME/share/hadoop/mapreduce
$ cp $HARP3_HOME/lib/fastutil-7.0.13.jar $HADOOP_HOME/share/hadoop/mapreduce
```

Edit `$HADOOP_HOME/etc/hadoop/mapred-site.xml`, add java opts settings for map-collective tasks.
```html
<property>
  <name>mapreduce.map.collective.memory.mb</name>
  <value>512</value>
</property>
<property>
  <name>mapreduce.map.collective.java.opts</name>
  <value>-Xmx256m -Xms256m</value>
</property>
```

## Step 3 --- Harp installation test

We have kmeans compiled for you. Copy `harp3-app-hadoop-2.6.0-kmeans.jar` to `$HADOOP_HOME`.
```bash
$ cp $HARP3_PROJECT_HOME/harp3-app/example/harp3-app-hadoop-2.6.0-kmeans.jar $HADOOP_HOME
```

Then start Hadoop.
```bash
$ $HADOOP_HOME/sbin/start-dfs.sh
$ $HADOOP_HOME/sbin/start-yarn.sh
```

Next use the following command to run harp kmeans.
```bash
$ cd $HADOOP_HOME
$ hadoop jar harp3-app-hadoop-2.6.0-kmeans.jar edu.iu.kmeans.KMeansLauncher <num of points> <num of centroids> <vector size> <num of point files per worker> <number of map tasks> <num threads> <number of iteration> <work dir> <local points dir>
#e.g. hadoop jar harp3-app-hadoop-2.6.0-kmeans.jar edu.iu.kmeans.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans
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

To fetch the results, use the following command.
```bash
$ hdfs dfs –get <work dir> <local dir>
#e.g. hdfs dfs -get /kmeans ~/Document
```
