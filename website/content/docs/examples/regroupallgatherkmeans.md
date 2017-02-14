---
title: Harp regroup & allgather k-means
---

This k-means algorithm uses `regroup` and `allgather` to communicate among machines. `regroup` aims to combine partitions which belongs to the same machine and `allgather` collects all local results and is shared with all machines as the global centroids table.

Because Harp will use `ant` to compile the whole code base, we ask you to add your file path in `$HARP3_PROJECT_HOME/harp3-app/build.xml`.
```xml
...
<src path="src" />
    <include name="edu/iu/fileformat/**" />
    <include name="edu/iu/benchmark/**" />
    <include name="edu/iu/dymoro/**" />
    <include name="edu/iu/kmeans/**" />
    <include name="edu/iu/lda/**" />
    <include name="edu/iu/sgd/**" />
    <include name="edu/iu/ccd/**" />
    <include name="edu/iu/wdamds/**" />
    <include name="<your file path>" />
    ...
```

THen you can use `ant` to compile Harp and use the output `harp3-app-hadoop-2.6.0.jar` to run with Hadoop.

Definitions:

* `N` is the number of data points
* `M` is the number of centroids
* `D` is the dimension of centroids
* `Vi` refers to the `i`th data point (vector)
* `Cj` refers to the `j`th centroid

## Step 1 --- Generate data points

It is same as in `allreduce` k-means.

## Step 2 --- Generate initial centroids

It is also same as in `allreduce` k-means.

## Step 3 --- Mapper Collective

The structure of the iterative mapper procedure is like following:

```java
protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
    /*
     * vals in the keyval pairs from reader are data file paths.
     * read data from file paths.
     * load initial centroids
     * do{
     *     computations
     *  generate new centroids
     * }while(<maxIteration)
     */
    List<String> pointFiles = new ArrayList<String>();
    while (reader.nextKeyValue()) {
        String key = reader.getCurrentKey();
        String value = reader.getCurrentValue();
        pointFiles.add(value);
    }
    Configuration conf = context.getConfiguration();
    runKmeans(pointFiles, conf, context);
}

private void runKmeans(List<String> fileNames, Configuration conf, Context context) throws IOException {
    Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());
    if (this.isMaster())
        loadCentroids(cenTable, vectorSize, conf.get(KMeansConstants.CFILE), conf);
    broadcastCentroids(cenTable);
    ArrayList<DoubleArray> dataPoints = loadData(fileNames, vectorSize, conf);
    Table<DoubleArray> previousCenTable = null;
    for (int iter = 0; iter < iteration; iter++) {
        previousCenTable = cenTable;
        cenTable = new Table<>(0, new DoubleArrPlus());
        computation(cenTable, previousCenTable, dataPoints);
        regroup("main", "regroup_" + iter, cenTable, null);
        calculateCentroids(cenTable);
        allgather("main", "allgather_" + iter, cenTable); 
    }
    if (this.isMaster())
        outputCentroids(cenTable, conf, context);
}
```

Using `regroup` and `allgather` to calculate k-means is very similar to using `allreduce` as well. As above says, each machine uses `regroup` to add partitions, which only consists the results of the nodes necessarily. Then it will calculate local centroids and use allgather to synchronize.