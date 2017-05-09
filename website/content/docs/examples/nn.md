# Harp Neural Network

&nbsp;&nbsp;&nbsp;&nbsp; Artificial neural networks (ANNs) are very powerful machine learning algorithm for learning real-valued, discrete-valued and vector-alued functions from the training data. Algorithms such as `BackPropagation` use gradient descent to deduce the network parameters for fitting the training data (even when they are non linear). Neural Networks are useful in interpreting visual scenes, speech recognition, and learning robot control strategies.

&nbsp;&nbsp;&nbsp;&nbsp;Neural networks are well suited for problems in which training data is noisy and complex sensor data such as inputs from cameras and microphones. Multi layered neural networks are built from large number of neural units. Neural networks are built out of densely interconnected set of simple units, where each unit takes a number of real-valued inputs (possible the outputs of other units) and produces a single real valued output which is used as input for further present neural units. These single neural units can be a perceptron which learns the parameters using perceptron rule or delta rule (based on gradient descent) . It can also be a sigmoid unit where each unit takes a linear set of inputs and produces a non linear differentiable output which helps in performing gradient descent to reduce the error. 

&nbsp;&nbsp;&nbsp;&nbsp; The learning algorithm tries to learn the weights from the data, and then the network can be used to recognize patterns. Here, we give a simple tutorial on how to parallel a standard implementation of the BackPropagation algorithm for a multi layered feed forward network.  

## PARALLEL DESIGN

* What are the model? What kind of data structure?

    Weights matrices, including the biases for each node, between each adjacent layers are the model in neural network. It is a vector of double matrix.

* What are the characteristics of the data dependency in model update computation, can updates run concurrently?

    In the core model update computation in BP training algorithm, each data point, or a minibatch, should access all the model, compute gradients and update model layer by layer from the output layer back to the input layer. 

    The nodes in the same layer can be updated in parallel without conflicts, but there are dependency between the layers. But generally, it is not easy to utilize these network structure related parallelism.

* which kind of parallelism scheme is suitable, data parallelism or model parallelism?

    Data parallelism can be used, i.e., calculating descent from different data points in parallel. 

    No model parallelism, each node get one replica of the whole model, which updates locally in parallel, and then synchronizes and averages when local computation all finish.

* which collective communication operations is suitable to synchronize model?

    Synchronize replicas of the model by allreduce is an simple solution. 

## DATAFLOW

<img src="/img/nn-dataflow.png" width="60%"  >

## Step 1 --- Set Table
The data format wrapper code is in charge of the conversion between the native DoubleMatrix and Harp Table.

```java
public Table<DoubleArray> train(DoubleMatrix X, DoubleMatrix Y, Table<DoubleArray> localWeightTable, int mini_epochs, int numMapTasks, double lambda) throws IOException
{
    Vector<DoubleMatrix> weightMatrix = reshapeTableToList(localWeightTable);
    setTheta(weightMatrix);

    trainBP(X, Y, lambda, mini_epochs, true);

    Table<DoubleArray> newWeightTable = reshapeListToTable(this.getTheta());
    return newWeightTable;
}
```

## Step 2 ---Communication
The code snippet for the core part of computation in the iterative training. There  are only a few lines of differences between the harp distributed version and the original sequential version.

```java
// Calculate the new weights 
// argument data type conversion and call the train() in the underlie library
weightTable = localNN.train(X, Y, weightTable, n, numMapTasks, lambda);

// reduce and broadcast
allreduce("main", "allreduce" + i, weightTable);

// Average the weight table by the numMapTasks
weightTable = localNN.modelAveraging(weightTable, numMapTasks);
```

## DATA
The MNIST dataset is used in this tutorial. Refer the [dataset script](https://github.com/DSC-SPIDAL/harp/tree/master/data/tutorial/mnist) for more details.

## USAGE
```bash
$ hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.NN.NNMapCollective
Usage: NNMapCollective <number of map tasks> <epochs> <syncIterNum> <hiddenLayers> <minibatchsize> <lambda> <workDir>
# hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.NN.NNMapCollective 2 20 5 100,32 2000 0 /nn
```

This command run harp neuralnetwork training on the input dataset under /nn, with 2 mappers. Training process goes through 20 times of the training dataset, averages the model every 5 iteration for each minibatch. The minibatch size is 2000, lambda is default value 0.689. There are 2 hidden layers, with 100 and 32 nodes each. Finally, it outputs the accuracy on the training set into the hadoop log.
