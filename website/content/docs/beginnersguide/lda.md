---
title: Latent Dirichlet Allocation Tutorial for Beginners
---

In natural language processing, latent Dirichlet allocation (LDA) is a generative statistical model that allows sets of observations to be explained by unobserved groups that explain why some parts of the data are similar. For example, if observations are words collected into documents, it posits that each document is a mixture of a small number of topics and that each word's creation is attributable to one of the document's topics. LDA is an example of a topic model. In LDA, each document may be viewed as a mixture of various topics where each document is considered to have a set of topics that are assigned to it via LDA. This is identical to probabilistic latent semantic analysis (pLSA), except that in LDA the topic distribution is assumed to have a sparse Dirichlet prior. The sparse Dirichlet priors encode the intuition that documents cover only a small set of topics and that topics use only a small set of words frequently. In practice, this results in a better disambiguation of words and a more precise assignment of documents to topics. LDA is a generalization of the pLSA model, which is equivalent to LDA under a uniform Dirichlet prior distribution.



# **The Dataset**

The dataset used is sampled from wikipedia dataset.
The format of the data should look like: (from [sample-sparse-data-part-0.txt](https://github.com/DSC-SPIDAL/harp/blob/master/datasets/tutorial/lda-cvb/sample-sparse-data/sample-sparse-data-part-0.txt))
```bash
0:1 1:2 2:6 4:2 5:3 6:1 7:1 10:3
0:1 1:3 3:1 4:3 7:2 10:1
0:1 1:4 2:1 5:4 6:9 8:1 9:2
0:2 1:1 3:3 6:5 8:2 9:3 10:9
0:3 1:1 2:1 3:9 4:3 6:2 9:1 10:3
0:4 1:2 3:3 4:4 5:5 6:1 7:1 8:1 9:4
```

Each row is one document in Wikipedia. 

*\<wordid\>:\<wordcount\>*

- *\<wordid\>*: word ID (Example: 0)
- *\<wordcount\>*: word count/frequency (Example: 1)

[sample-sparse-metadata](https://github.com/DSC-SPIDAL/harp/blob/master/datasets/tutorial/lda-cvb/sample-sparse-data/sample-sparse-metadata) shows the document ID for the first document of each file. 
```bash
sample-sparse-data-part-0.txt 0
sample-sparse-data-part-1.txt 6
```

# Run Example
<mark>To be able to compile and run, you have to [install Harp and Hadoop](https://dsc-spidal.github.io/harp/docs/getting-started/)</mark>:
## Put data on hdfs
```bash
hdfs dfs -put $HARP_ROOT_DIR/datasets/tutorial/lda-cvb/sample-sparse-data/sample-sparse-metadata .
hdfs dfs -mkdir sample-sparse-data
hdfs dfs -put $HARP_ROOT_DIR/datasets/tutorial/lda-cvb/sample-sparse-data/sample-sparse-data-part-1.txt sample-sparse-data
hdfs dfs -put $HARP_ROOT_DIR/datasets/tutorial/lda-cvb/sample-sparse-data/sample-sparse-data-part-0.txt sample-sparse-data
```

## Compile

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0
```bash
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0
cd $HARP_ROOT_DIR/contrib/target
cp contrib-0.1.0.jar $HADOOP_HOME
cp $HARP_ROOT_DIR/third_parity/cloud9-1.4.17.jar $HADOOP_HOME/share/hadoop/mapreduce
cd $HADOOP_HOME
```
## Run
```bash
hadoop jar contrib-1.0.SNAPSHOT.jar  edu.iu.lda.LdaMapCollective <input dir>  <metafile>  <output dir> <number of terms> <number of topics> <number of docs> <number of MapTasks> <number of iterations> <number of threads> <mode, 1=multithreading>
```
Example
```bash
hadoop jar contrib-1.0.SNAPSHOT.jar  edu.iu.lda.LdaMapCollective sample-sparse-data sample-sparse-metadata  sample-sparse-output 11 2 12 2 5 4 1
```
Please be noted:
If you are running with mode=0 (sequential version), you will need data with dense format, and the parameter “number of threads” will not be used. If you are running with mode=1, you will need data with sparse format.
Metadata is used for indicating the beginning index of documents in partitions.

