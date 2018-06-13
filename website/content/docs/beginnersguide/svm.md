---
title: Support Vector Machine Tutorial for Beginners
---

Support Vector Machine is a supervised machine learning algorithm that analyzes data used for classification and regression analysis. An SVM training algorithm builds a model that assigns new examples to one category or the other, making it a non-probabilistic binary linear classifier. An SVM model is a representation of the examples as points in space, mapped so that the examples of the separate categories are divided by a clear gap that is as wide as possible. New examples are then mapped into that same space and predicted to belong to a category based on which side of the gap they fall. In addition to performing linear classification, SVMs can efficiently perform a non-linear classification using what is called the kernel trick, implicitly mapping their inputs into high-dimensional feature spaces.
Support Vector Machine typically needs a set of training examples, each marked as belonging to one or the other of two categories. 



# **Dataset Description**

## **The Images**

The MNIST database was constructed from the databases which contain binary images of handwritten digits. The handwritten digits has a training set of 60,000 examples, and a test set of 10,000 examples. It is a subset of a larger set available from NIST. The digits have been size-normalized and centered in a fixed-size image.


## **The Categories**

The MNIST training set  is stored in a very simple file format designed for storing vectors and multidimensional matrices. The images are first transformed into a dataset of feature vectors(shown below) through preprocessing steps. The original black and white images from NIST were size normalized to fit in a 20x20 pixel box while preserving their aspect ratio. The resulting images contain grey levels as a result of the anti-aliasing technique used by the normalization algorithm. the images were centered in a 28x28 image by computing the center of mass of the pixels, and translating the image so as to position this point at the center of the 28x28 field. Please refer to [THE MNIST DATABASE of handwritten digits](http://yann.lecun.com/exdb/mnist/) for more details.

Here’s what the dataset should look like: 
```bash
7 203:84 204:185 205:159 ... 741:207 742:18
```
Format:
*\<label\> [\<fid\>:\<feature\>]+*

- *\<label\>*: digit between 0-9 (example: 7)
- *\<fid\>*: positive feature id (example: 203)
- *\<feature\>*: the feature value (example: 84)






# Compile
The dataset used is a subset of [MNIST]([https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/multiclass.html#mnist](https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/multiclass.html#mnist) with 6000 examples selected.

<mark>To be able to compile and run, you have to [install Harp and Hadoop](https://dsc-spidal.github.io/harp/docs/getting-started/)</mark>:

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0
```bash
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0
cd $HARP_ROOT_DIR/contrib/target
cp contrib-0.1.0.jar $HADOOP_HOME
cd $HADOOP_HOME
cp $HARP_ROOT_DIR/third_party/libsvm-3.17.jar $HADOOP_HOME/share/hadoop/mapreduce/
```

Get the dataset and put data onto hdfs
```bash
wget https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/multiclass/mnist.t.bz2
bunzip2 mnist.t.bz2
hdfs dfs -mkdir -p /harp-test/svm/data
rm -rf data
mkdir -p data
cd data
split -l 5000 ../mnist.t
cd ..
hdfs dfs -put data /harp-test/svm/
```

# Run
```bash
hadoop jar contrib-0.1.0.jar edu.iu.svm.IterativeSVM <number of mappers> <number of iteration> <work path in HDFS> <local data set path>
```

For this dataset:
```bash
hadoop jar contrib-0.1.0.jar edu.iu.svm.IterativeSVM 2 5 /harp-test/svm nolocalfile
```



# Results
To Fetch the result:
```bash
hdfs dfs -get /harp-test/svm/out
```
The result is the support vectors.
To view the support vectors from the dataset:
```bash
more out/part-m-00000
more out/part-m-00001
```

To get the number of SVM in the data: 
```bash
wc -l out/part-m-00001
```
There are 2498 support vectors. 

