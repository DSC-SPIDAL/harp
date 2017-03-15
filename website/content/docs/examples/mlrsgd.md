---
title: Harp Multiclass Logistic Regression with Stochastic Gradient Descent
---

<img src="/img/4-3-1.png" width="60%"  >

Multiclass logistic regression (MLR) is a classification method that generalizes logistic regression to multiclass problems, i.e. with more than two possible discrete outcomes. That is, it is a model that is used to predict the probabilities of the different possible outcomes of a categorically distributed dependent variable, given a set of independent variables.

The process of the MLR algorithm is:

1. Use the weight `W` to predict the label of current data point.

2. Compare the output and the answer.

3. Use SGD to approximate `W`.

4. Repeat step 1 to 3 with each label and their weights.

Stochastic gradient descent (SGD) is a stochastic approximation of the gradient descent optimization method for minimizing an objective function that is written as a sum of differentiable functions. In other words, SGD tries to find minimums or maximums by iteration. As the algorithm sweeps through the training set, it performs the update for each training example. Several passes can be made over the training set until the algorithm converges.

The SGD algorithm can be described as following:

1. Randomly assign the weight `W`.

2. Shuffle `N` data points.

3. Go through `N` data points and do gradient descent.

4. Repeat step 2 and 3 `K` times.

Definitions:

* `N` is the number of data points
* `T` is the number of labels
* `M` is the number of features
* `W` is the `T*M` weight matrix
* `K` is the number of iteration

## PARALLEL DESIGN

* What are the model? What kind of data structure?

    The weight vectors for classes are model. Because an ovr(one-versus-rest) approach is adopted, each weight vector are independent. It has a matrix structure.

* What are the characteristics of the data dependency in model update computation, can updates run concurrently?

    Model update computation here is the SGD update, in which for each data point it should update the model directly. Because of the ovr strategy, each row in the model matrix are independent, and can be updated in parallel.

* which kind of parallelism scheme is suitable, data parallelism or model parallelism?

    Data parallelism can be used, i.e., calculating different data points in parallel.

    Because the updates can run concurrently, model parallelism is a nature solution.  Each node get one partition of the model, which updates in parallel. And furthermore, thread level parallelism can also follows this model parallelism pattern, that each thread take a subset of partition and update in parallel independently.

* which collective communication operations is suitable to synchronize model?

    DynamicScheduler can be used for thread-level parallelism, and Rotate can be used in the inter-node model synchronization.

## DATAFLOW

![dataflow](/img/4-3-2.png)

## Step 0 --- Data preprocessing

Harp MLR will use the data in the vector format. Each vector in a file represented by the format `<did> [<fid>:<weight>]`:

* `<did>` is an unique document id
* `<fid>` is a positive feature id
* `<weight>` is the number feature value within document weight

After preprocessing, push the data set into HDFS by the following commands.
```bash
hdfs dfs -mkdir /input
hdfs dfs -put input_data/* /input
```

## Step 1 --- Initialize

For Harp MLR, we will use dynamic scheduling as mentioned above. Before we set up the dynamic scheduler, we need to initialize the weight matrix `W`, which will be partitioned into `T` parts representing to `T` labels which means that each label belongs to one partition and is treated as an independent task.
```Java
private void initTable() {
    wTable = new Table(0, new DoubleArrPlus());
    for (int i = 0; i < topics.size(); i++)
        wTable.addPartition(new Partition(i, DoubleArray.create(TERM + 1, false)));
}
```

After that we can initialize the dynamic scheduler. Each thread will be treated as a worker and be added into the scheduler. The only thing that needs to be done is that tasks has to be submitted during the computation.
```Java
private void initThread() {
    GDthread = new LinkedList<>();
    for (int i = 0; i < numThread; i++)
        GDthread.add(new GDtask(alpha, data, topics, qrels));
    GDsch = new DynamicScheduler<>(GDthread);
}
```

## Step 2 --- Mapper communication

In this main process, we use `regroup` to distribute the partitions to the workers first. The workers will get almost the same number of partitions. Then we start the scheduler. For each time we submit one partition to each thread in the scheduler and the threads will all use SGD to approximate `W` with each label. After the workers finish once with their own partitions, we will use `rotate` operation to swap the partitions among the workers. When finishing the all process, each worker should use its own data training the whole partition `K` times, of which `K` is the number of iteration. `allgather` operation collects all partitions in each worker, combines the partitions, and shares the outcome with all workers. Finally, the Master worker outputs the weight matrix `W`.
```Java
protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    LoadAll(reader);
    initTable();
    initThread();

    regroup("MLR", "regroup_wTable", wTable, new Partitioner(getNumWorkers()));

    GDsch.start();        
    for (int iter = 0; iter < ITER * numMapTask; iter++) {
        for (Partition par : wTable.getPartitions())
            GDsch.submit(par);
        while (GDsch.hasOutput())
            GDsch.waitForOutput();
            
        rotate("MLR", "rotate_" + iter, wTable, null);

        context.progress();
    }
    GDsch.stop();
        
    allgather("MLR", "allgather_wTable", wTable);

    if (isMaster())
        Util.outputData(outputPath, topics, wTable, conf);
    wTable.release();
}
```

## USAGE

```bash
$ hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.mlr.MLRMapCollective [alpha] [number of iteration] [number of features] [number of workers] [number of threads] [topic file path] [qrel file path] [input path in HDFS] [output path in HDFS]
#e.g. hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.mlr.MLRMapCollective 1.0 100 47236 2 16 /rcv1v2/rcv1.topics.txt /rcv1v2/rcv1-v2.topics.qrels /input /output
```

The output should be the weight matrix `W`.
