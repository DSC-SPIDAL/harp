# Distributed LDA on Harp

### Background

LDA is a powerful topic modelling algorithm for clustering words into topics and documents into mixtures of topics. Even though the sequential LDA model is theoretically effective, a significant drawback evident while using LDA is the amount of time taken and memory requirements for inference while dealing with a very large scaled and dynam- ically expanding corpus. Moreover the huge data-sets won’t fit on a single machine. Thus a distributed multiprocessor system framework is essential for solving topic modelling LDA inference for a large data corpus as an efficient way of distributing the computation across multiple machines. The primary aim of the project is to build a scalable topic modelling tool for a large corpus of textual data devised by implementing LDA model in a distributed environment. We follow the [Mr. LDA](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.642.2336&rep=rep1&type=pdf) to implement distributed variational bayes inference LDA on Harp. Harp modules particularly of our interests are dynamic scheduler, all-reduce and push-pull communication models.

The dataset is used is sampled from [wikipedia](https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2) dataset.

### Usage

Put data on hdfs
```bash
hdfs dfs -put $HARP_ROOT_DIR/data/tutorial/lda-cvb/sample-sparse-data/sample-sparse-metadata .
hdfs dfs -mkdir sample-sparse-data
hdfs dfs -put $HARP_ROOT_DIR/data/tutorial/lda-cvb/sample-sparse-data/sample-sparse-data-part-1.txt sample-sparse-data
hdfs dfs -put $HARP_ROOT_DIR/data/tutorial/lda-cvb/sample-sparse-data/sample-sparse-data-part-0.txt sample-sparse-data
```

Compile
```bash
cd $HARP_ROOT_DIR
mvn clean package
cp $HARP_ROOT_DIR/harp-tutorial-app/target/harp-tutorial-app-1.0.SNAPSHOT.jar $HADOOP_HOME
cp $HARP_ROOT_DIR/third_parity/cloud9-1.4.17.jar $HADOOP_HOME/share/hadoop/mapreduce
```

Run
```bash
hadoop jar harp-tutorial-app-1.0.SNAPSHOT.jar  edu.iu.lda.LdaMapCollective <input dir>  <metafile>  <output dir> <number of terms> <number of topics> <number of docs> <number of MapTasks> <number of iterations> <number of threads> <mode, 1=multithreading>
```

Example
```
hadoop jar harp-tutorial-app-1.0.SNAPSHOT.jar  edu.iu.lda.LdaMapCollective sample-sparse-data sample-sparse-metadata  sample-sparse-output 11 2 12 2 5 4 1
```


Please be noted:

1. if you are running with mode=0 (sequential version), you need data with dense format, and the parameter "number of threads" will not be used. If you are running with mode=1, you will need data with sparse format.

2. metadata is used for indicating the beginning index of documents in partitions.


