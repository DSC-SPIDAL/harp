---
title: K-Means
---

K-Means clustering is a method of vector quantization, originally from signal processing, that is popular for cluster analysis in data mining. K-Means clustering aims to partition `N` observations into `K` clusters in which each observation belongs to the cluster with the nearest mean, serving as a prototype of the cluster.

The problem is an NP-hard question. However, there are efficient heuristic algorithms that are commonly employed and converge quickly to a local optimum. These are usually similar to the expectation-maximization algorithm for mixtures of Gaussian distributions via an iterative refinement approach employed by both algorithms. Additionally, they both use cluster centers to model the data; however, k-means clustering tends to find clusters of comparable spatial extent, while the expectation-maximization mechanism allows clusters to have different shapes.

The K-Means algorithm simply repeats the following set of steps until the maximum number of iterations, or the changes on centroids are less than a threshold:
```java
1. Choose K points as the initial set of centroids.

2. Assign each data point in the data set to the closest centroid (this is done by calculating the distance between the data point and each centroid).

3. Calculate the new centroids based on the clusters that were generated in step 2. Normally this is done by calculating the mean of each cluster.

4. Repeat steps 2 and 3 until the maximum number of iterations, or the changes on centroids are less than a threshold.
```

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

## Step 3 --- Map Collective

Harp provides several collective communication operations. Here are some examples provided to show how to apply these collective communication methods to K-Means.


  <ul class="nav nav-pills">
    <li class="active"><a data-toggle="pill" href="#allreduce">Allreduce</a></li>
    <li><a data-toggle="pill" href="#broadcast-reduce">Broadcast-Reduce</a></li>
    <li><a data-toggle="pill" href="#push-pull">Push-Pull</a></li>
    <li><a data-toggle="pill" href="#regroup-allgather">Regroup-Allgather</a></li>
  </ul>

  <div class="tab-content">
    <div id="allreduce" class="tab-pane fade in active">
      <h3>HOME</h3>
      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>
    </div>
    <div id="broadcast-reduce" class="tab-pane fade">
      <h3>Menu 1</h3>
      <p>Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>
    </div>
    <div id="push-pull" class="tab-pane fade">
      <h3>Menu 2</h3>
      <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam.</p>
    </div>
    <div id="regroup-allgather" class="tab-pane fade">
      <h3>Menu 3</h3>
      <p>Eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.</p>
    </div>
  </div>

