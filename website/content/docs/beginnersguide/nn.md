---
title: Neural Network Tutorial for Beginners
---
Neural networks are a set of algorithms, which is based on a large of neural units. Each neural unit is connected with many others, and forms a network structure. Computation happens in the neural unit, which combines all the inputs with a set of coefficients, or weights, and gives an output by an activation function. A layer is a group of neural units, that each layer’s output is the subsequent layer’s input. A learning algorithm tries to learn the weights from data, and then the network can be used to recognize patterns. Essentially, neural networks are composed of layers of computational units called neurons, with connections in different layers. These networks transform data until they can classify it as an output. Each neuron multiplies an initial value by some weight, sums results with other values coming into the same neuron, adjusts the resulting number by the neuron’s bias, and then normalizes the output with an activation function.




# **Dataset Description**

## **The Images**

The MNIST database was constructed from the databases which contain binary images of handwritten digits. The handwritten digits has a training set of 60,000 examples, and a test set of 10,000 examples. It is a subset of a larger set available from NIST. The digits have been size-normalized and centered in a fixed-size image.

![](https://d2mxuefqeaa7sj.cloudfront.net/s_B6F22FB13E188D4E8A7C8C8A787147196AB0BBCAE729048CA1FF94662824839E_1531339091940_file.png)
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






# Run Example

To be able to compile and run, you have to [install Harp and Hadoop](https://dsc-spidal.github.io/harp/docs/getting-started/):

## Put data on hdfs
```bash
#download the dataset
python $HARP_ROOT_DIR/datasets/tutorial/mnist/fetech_mnist.py
#split into 2 parts
python $HARP_ROOT_DIR/datasets/tutorial/mnist/split_mnist.py mnist_data 2
    
#upload data to hadoop
hadoop fs -mkdir -p /nn/batch
hadoop fs -put mnist_data_?.* /nn/batch
```


## Compile

Select the profile related to your hadoop version. For ex: hadoop-2.6.0. Supported hadoop versions are 2.6.0, 2.7.5 and 2.9.0
```bash
cd $HARP_ROOT_DIR
mvn clean package -Phadoop-2.6.0
cd $HARP_ROOT_DIR/contrib/target
cp contrib-0.1.0.jar $HADOOP_HOME
cp $HARP_ROOT_DIR/third_parity/jblas-1.2.4.jar $HADOOP_HOME/share/hadoop/mapreduce
cp $HARP_ROOT_DIR/third_parity/neuralNet-1.0.0-SNAPSHOT.jar $HADOOP_HOME/share/hadoop/mapreduce
cd $HADOOP_HOME
```

## Run
```bash
hadoop jar contrib-0.1.0.jar edu.iu.NN.NNMapCollective
Usage: NNMapCollective <number of map tasks> <epochs> <syncIterNum> <hiddenLayers> <minibatchsize> <lambda> <workDir>
```

Example
```bash
hadoop jar contrib-0.1.0.jar edu.iu.NN.NNMapCollective 2 20 5 100,32 2000 0 /nn
```
This command run harp neuralnetwork training on the input dataset under /nn, with 2 mappers. Training process goes through 20 times of the training dataset, averages the model every 5 iteration for each minibatch. The minibatch size is 2000, lambda is default value 0.689. There are 2 hidden layers, with 100 and 32 nodes each. Finally, it outputs the accuracy on the training set into the hadoop log.


