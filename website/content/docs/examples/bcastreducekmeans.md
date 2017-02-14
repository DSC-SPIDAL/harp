---
title: Harp broadcast & reduce k-means
---

Using `broadcast` and `reduce` to calculate k-means algorithm in Harp is quite similar to using `allreduce`. That is because `allreduce` can be treated as first one machine combines others' partition (`reduce`) and then shares with others (`broadcast`). The only difference is in this method, the calculation part is only done by the master machine while `allreduce` method will do on everyone.

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
    Table<DoubleArray> previousCenTable =  null;
    for (int iter = 0; iter < iteration; iter++) {
        previousCenTable = cenTable;
        cenTable = new Table<>(0, new DoubleArrPlus());
        computation(cenTable, previousCenTable, dataPoints);
        reduce("main", "reduce_"+iter, cenTable, this.getMasterID());
        if (this.isMaster())
            calculateCentroids(cenTable);
        broadcast("main", "bcast_"+iter, cenTable, this.getMasterID(), false);  
    }
    if (this.isMaster())
        outputCentroids(cenTable, conf, context);
}
```

`broadcast` method is encapsulated into method `bcastCentroids`:

```java
private void bcastCentroids(Table<DoubleArray> table, int bcastID) throws IOException {
    boolean isSuccess = false;
    try
        isSuccess = this.broadcast("main", "broadcast-centroids", table, bcastID, false);
    catch (Exception e)
        e.printStackTrace();
    if (!isSuccess)
        throw new IOException("Fail to bcast");
}
```

And as mentioned above, the only difference between using `broadcast` & `reduce` and using `allreduce` is how to synchronize the local results. So in this step, load centroids and data, find the nearest centroid and update, and write centroids back to HDFS are all same as in `allreduce` k-means.