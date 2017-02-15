---
title: K-Means
---

This section describes how to implement the K-means algorithm using Harp.


# Understanding K-Means

K-Means is a very powerful and easily understood clustering algorithm. The aim of the algorithm is to divide a given set of points into “K” partitions. “K” needs to be specified by the user. In order to understand K-Means, first you need to understand the proceeding concepts and their meaning.

1. `Centroids`:
    Centroids can be defined as the center of each cluster. If we are performing clustering with k=3, we will have 3 centroids. To perform K-Means clustering, the users needs to provide the initial set of centroids.

2. `Distance`:
    In order to group data points as close together or as far-apart we need to define a distance between two given data points. In K-Means clustering distance is normally calculated as the Euclidean Distance between two data points.

The K-Means algorithm simply repeats the following set of steps until there is no change in the partition assignments, in that it has clarified which data point is assigned to which partition.
```java
Choose K points as the initial set of centroids.
Assign each data point in the data set to the closest centroid (this is done by calculating the distance between the data point and each centroid).
Calculate the new centroids based on the clusters that were generated in step 2. Normally this is done by calculating the mean of each cluster.
Repeat steps 2 and 3 until data points do not change cluster assignments, meaning their centroids are set.
```

# Pseduo Code and Java Code

## The Main Method
The tasks of the main class is to configure and run the job iteratively.
```java
generate N data points (D dimensions), write to HDFS
generate M centroids, write to HDFS
for iterations{
    configure a job
    launch the job
}
```

## The mapCollective function
This is the definition of map-collective task. It reads data from context and then call runKmeans function to actually run kmeans Mapper task.
```java
protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
		LOG.info("Start collective mapper.");
	    long startTime = System.currentTimeMillis();
	    List<String> pointFiles = new ArrayList<String>();
	    while (reader.nextKeyValue()) {
	    	String key = reader.getCurrentKey();
	    	String value = reader.getCurrentValue();
	    	LOG.info("Key: " + key + ", Value: " + value);
	    	pointFiles.add(value);
	    }
	    Configuration conf = context.getConfiguration();
	    runKmeans(pointFiles, conf, context);
	    LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
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

