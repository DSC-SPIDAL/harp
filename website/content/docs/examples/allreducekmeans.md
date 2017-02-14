---
title: Harp Kmeans
---
The K-Means algorithm simply repeats the following set of steps until there is no change in the partition:

* Choose `K` points as the initial set of centroids.
* Assign each data point in the data set to the closest centroid (this is done by calculating the distance between the data point and each centroid).
* Calculate the new centroids based on the clusters that were generated in step 2. Normally this is done by calculating the mean of each cluster.
* Repeat steps 2 and 3 until data points do not change cluster assignments, meaning their centroids are set.

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

The actual method is in `Utils.java`. This method generates a set of data points and writes them into the specified folder. The method also takes in parameters to specify the number of files and number of data points that need to be generated. The data points are divided among the data files. You will generate `numMapTasks` data files, with each data file containing a part of the data points. Then you can generate data to `localDirStr` and use `fs` to copy the data to `dataDir`.

```java
static void generateData(int numOfDataPoints, int vectorSize, int numMapTasks, FileSystem fs, String localDirStr, Path dataDir) throws IOException, InterruptedException, ExecutionException {
    int pointsPerFile = numOfDataPoints / numMapTasks;
    int pointsRemainder = numOfDataPoints % numMapTasks;
    if (fs.exists(dataDir))
        fs.delete(dataDir, true);
    File localDir = new File(localDirStr);
    if (localDir.exists() && localDir.isDirectory()) {
        for (File file : localDir.listFiles())
            file.delete();
        localDir.delete();
    }
    localDir.mkdir();
    if (pointsPerFile == 0)
        throw new IOException("No point to write.");
    double point;
    int hasRemainder = 0;
    Random random = new Random();
    for (int k = 0;k < numMapTasks;k++) {
        try {
            String filename = Integer.toString(k);
            File file = new File(localDirStr + File.separator + "data_" + filename);
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            if (pointsRemainder > 0) {
                hasRemainder = 1;
                pointsRemainder--;
            }
            else
                hasRemainder = 0;
            int pointsForThisFile = pointsPerFile + hasRemainder;
            for (int i = 0; i < pointsForThisFile; i++)
                for (int j = 0; j < vectorSize; j++) {
                    point = random.nextDouble() * DATA_RANGE;
                    if (j == vectorSize - 1) {
                        bw.write(point + "");
                        bw.newLine();
                    }
                    else
                        bw.write(point + " ");
                }
            bw.close();
        }
        catch (FileNotFoundException e)
            e.printStackTrace();
        catch (IOException e)
            e.printStackTrace();
    }
    Path localPath = new Path(localDirStr);
    fs.copyFromLocalFile(localPath, dataDir);
}
```

## Step 2 --- Generate initial centroids

K-Means needs a set of initial centroids. The `generateInitialCentroids` method in `Utils.java` will generate a set of random centroids. You can generate one file contains centroids and use `fs` to write it to `cDir`. `JobID` indicates the current job. It is optional.

```java
static void generateInitialCentroids(int numCentroids, int vectorSize, Configuration configuration, Path cDir, FileSystem fs, int JobID) throws IOException {
    Random random = new Random();
    double[] data = null;
    if (fs.exists(cDir))
        fs.delete(cDir, true);
    if (!fs.mkdirs(cDir))
        throw new IOException(cDir.toString() + "fails to be created.");
    data = new double[numCentroids * vectorSize];
    for (int i = 0;i < data.length;i++)
        data[i] = random.nextDouble() * DATA_RANGE;
    Path initClustersFile = new Path(cDir, KMeansConstants.CENTROID_FILE_NAME);
    FSDataOutputStream out = fs.create(initClustersFile, true);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
    for (int i = 0;i < data.length;i++)
        if ((i % vectorSize) == (vectorSize - 1)) {
            bw.write(data[i] + "");
            bw.newLine();
        }
        else
            bw.write(data[i] + " ");
    bw.flush();
    bw.close();
}
```

## Step 3 --- Mapper Collective

After the completion of initialization steps, the main class will run a set of map reduce jobs iteratively. The number of iterations are specified by the user. The following code block at each iteration configure a job and run it. When configuring a job, you can use `MultiFileInputFormat` class in `edu.iu.fileformat` to set `inputFormatClass`. In this way, every map task will load data file paths. Then you can read data from the file paths in map task.

Each job needs to do these tasks:

* Read data from point files.

```java
static ArrayList<DoubleArray> loadData(List<String> fileNames, int vectorSize, Configuration conf) throws IOException {
    ArrayList<DoubleArray> data = new ArrayList<DoubleArray>();
    for (String filename : fileNames) {
        FileSystem fs = FileSystem.get(conf);
        Path dPath = new Path(filename);
        FSDataInputStream in = fs.open(dPath);
        BufferedReader br = new BufferedReader( new InputStreamReader(in));
        String line = "";
        String[] vector = null;
        while ((line = br.readLine()) != null) {
            vector = line.split("\\s+");
            if (vector.length != vectorSize)
                System.exit(-1);
            else {
                double[] aDataPoint = new double[vectorSize];
                for (int i = 0;i < vectorSize;i++)
                    aDataPoint[i] = Double.parseDouble(vector[i]);
                DoubleArray da = new DoubleArray(aDataPoint, 0, vectorSize);
                data.add(da);
            }
        }
    }
    return data;
}
```

* load centroids

```java
static void loadCentroids(Table<DoubleArray> cenTable, int vectorSize, String cFileName, Configuration configuration) throws IOException {
    Path cPath = new Path(cFileName);
    FileSystem fs = FileSystem.get(configuration);
    FSDataInputStream in = fs.open(cPath);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String line = "";
    String[] vector = null;
    int partitionId = 0;
    while ((line = br.readLine()) != null) {
        vector = line.split("\\s+");
        if (vector.length != vectorSize)
            System.exit(-1);
        else {
            double[] aCen = new double[vectorSize + 1];
            for (int i = 0;i < vectorSize;i++)
                aCen[i] = Double.parseDouble(vector[i]);
            aCen[vectorSize] = 0;
            Partition<DoubleArray> ap = new Partition<DoubleArray>(partitionId, new DoubleArray(aCen, 0, vectorSize + 1));
            cenTable.addPartition(ap);
            partitionId++;
        }
    }
}
```

* broadcast centroids

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

* find the nearest centroid `Cj` for all data points `V`

```java
private void findNearestCenter(Table<DoubleArray> cenTable, Table<DoubleArray> previousCenTable, ArrayList<DoubleArray> dataPoints) {
      double err = 0;
    for (DoubleArray aPoint : dataPoints) {
        //for each data point, find the nearest centroid
        double minDist = -1;
        double tempDist = 0;
        int nearestPartitionID = -1;
        for (Partition ap : previousCenTable.getPartitions()) {
            DoubleArray aCentroid = (DoubleArray)ap.get();
            double dist = 0;
            for (int i = 0; i < vectorSize; i++)
                dist += Math.pow(aPoint.get()[i] - aCentroid.get()[i], 2);
            tempDist = Math.sqrt(dist);
            if (minDist == -1 || tempDist < minDist) {
                minDist = tempDist;
                nearestPartitionID = ap.id();
            }
        }
        err += minDist;
        //for the certain data point, found the nearest centroid.
        // add the data to a new cenTable.
        double[] partial = new double[vectorSize + 1];
        for (int j = 0;j < vectorSize;j++)
            partial[j] = aPoint.get()[j];
        partial[vectorSize] = 1;
        if (cenTable.getPartition(nearestPartitionID) == null) {
            Partition<DoubleArray> tmpAp = new Partition<DoubleArray>(nearestPartitionID, new DoubleArray(partial, 0, vectorSize + 1));
            cenTable.addPartition(tmpAp);
        }
        else {
            Partition<DoubleArray> apInCenTable = cenTable.getPartition(nearestPartitionID);
            for (int i = 0;i < vectorSize + 1;i++)
                apInCenTable.get().get()[i] += partial[i];
        }
    }
}
```

* allreduce centroids
* update centroids

```java
private void updateCenters(Table<DoubleArray> cenTable) {
    for (Partition<DoubleArray> partialCenTable : cenTable.getPartitions()) {
        double[] doubles = partialCenTable.get().get();
        for (int h = 0;h < vectorSize;h++)
            doubles[h] /= doubles[vectorSize];
        doubles[vectorSize] = 0;
    }
}
```

* repeat step 4 to 6
* write centroids to HDFS

```java
private void outputResults(Table<DoubleArray> dataTable,Configuration conf, Context context) {
    String output = "";
    for (Partition<DoubleArray> ap : dataTable.getPartitions()) {
        double res[] = ap.get().get();
        for (int i = 0;i < vectorSize;i++)
            output += res[i] + "\t";
        output += "\n";
    }
    try
        context.write(null, new Text(output));
    catch (IOException e)
        e.printStackTrace();
    catch (InterruptedException e)
        e.printStackTrace();
}
```

For the java implementation, a setup method will be called to initialize the mapper class. In the setup method, all the needed configurations will be loaded. The main map task is handled in the mapCollective function. In the mapCollective function, it first gets data files from `KeyValReader` and then reads data from files. It then loads initialization centroids from HDFS. For each data point, it calculates distances between the current data point and each centroid to determine the closest centroid to the data point.

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
     * 
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
    ArrayList<DoubleArray> dataPoints = Utils.loadData(fileNames, vectorSize, conf);
    Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());
    if (this.isMaster())
        Utils.loadCentroids(cenTable, vectorSize, cFile, conf);
    bcastCentroids(cenTable, this.getMasterID());
    Table<DoubleArray> previousCenTable = null;
    for (int iter = 0;iter < iteration;iter++) {
        previousCenTable = cenTable;
        cenTable = new Table<>(0, new DoubleArrPlus());
        findNearestCenter(cenTable, previousCenTable, dataPoints);
        allreduce("main", "allreduce_" + iter, cenTable);
        updateCenters(cenTable);
    }
    if (this.isMaster())
        outputResults(cenTable, conf, context);
    }
}
```
