mnist dataset
================

This is the dataset for neuralnetwork test.

```py
    python fetech_mnist.py
    python split_mnist.py mnist_data 2

    #upload data to hadoop
    hadoop fs -mkdir -p /nn/batch
    hadoop fs -put mnist_data_?.* /nn/batch

    #test harpnn
    hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.NN.NNMapCollective 2 350 5 100,32 2000 0 /nn
```
