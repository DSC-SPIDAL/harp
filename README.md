HARP PROJECT

Copyright 2013-2017 Inidana University
Apache License 2.0

Author: Bingjing Zhang

WHAT IS HARP?
Harp is a framework for machine learning applications.

FEATURES
1. Hadoop 2.6.0 plugin
2. Hierarchal data abstraction (arrays/objects, partitions/tables)
3. Pool based memory management
4. Collective + event-driven programming model (distributed computing)
5. Dynamic Scheduler + Static Scheduler (multi-threading)

APPLICATIONS
K-means Clustering

COMPILATION & INSTALLATION
hadoop-2.6.0
1. Enter "harp" home directory and execute "ant"
2. Copy harp-0.3.0-hadoop-2.6.0.jar and fastutil-7.0.13.jar from lib/ into hadoop-2.6.0/share/hadoop/mapreduce
3. Configure Hadoop environment for settings required to run Hadoop
4. Edit mapred-site.xml in hadoop-2.6.0/etc/hadoop, add java opts settings for map-collective tasks
   (The following is an example)
   <property>
     <name>mapreduce.map.collective.memory.mb</name>
     <value>512</value>
   </property>
   <property>
     <name>mapreduce.map.collective.java.opts</name>
     <value>-Xmx256m -Xms256m</value>
   </property>
5. To develop Harp applications, remember to add the following property in job configuration:
   jobConf.set("mapreduce.framework.name", "map-collective");
6. Enter "harp-app" home directory and execute "ant" 
7. Copy build/harp-app-hadoop-2.6.0.jar to hadoop-2.6.0/
8. Start Hadoop environment
9. Run Kmeans job 
   hadoop jar harp3-app-hadoop-2.6.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher <num of points> <num of centroids> <vector size> <num of point files per worker> <number of map tasks> <num threads> <number of iteration> <work dir> <local points dir>
   e.g. bin/hadoop jar harp3-app-hadoop-2.6.0.jar edu.iu.kmeans.regroupallgather.KMeansLauncher 1000 10 100 5 2 2 10 /kmeans /tmp/kmeans
