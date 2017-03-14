---
title: K-Means
---

<img src="/img/kmeans.png" width="80%" >

K-Means is a powerful and easily understood clustering algorithm. The aim of the algorithm is to divide a given set of points into `K` partitions. `K` needs to be specified by the user. In order to understand K-Means, first you need to understand the proceeding concepts and their meanings.

* `Centroids`:
    Centroids can be defined as the center of each cluster. If we are performing clustering with k=3, we will have 3 centroids. To perform K-Means clustering, the users needs to provide an initial set of centroids.

* `Distance`:
    In order to group data points as close together or as far-apart, we need to define a distance between two given data points. In K-Means, clustering distance is normally calculated as the Euclidean Distance between two data points.

The K-Means algorithm simply repeats the following set of steps until there is no change in the partition assignments. In that, it has clarified which data point is assigned to which partition.

1. Choose K points as the initial set of centroids.

2. Assign each data point in the data set to the closest centroid (this is done by calculating the distance between the data point and each centroid).

3. Calculate the new centroids based on the clusters that were generated in step 2. Normally this is done by calculating the mean of each cluster.

4. Repeat step 2 and 3 until data points do not change cluster assignments, which means that their centroids are set.

## PARALLEL DESIGN

* What are the model? What kind of data structure?

    Centroids of the clusters are model in vanilla kmeans. It has a vector structure as a double array.

* What are the characteristics of the data dependency in model update computation, can updates run concurrently?

    In the core model update computation, each data point should access all the model, compute distance with each centroid, find the nearest one and partially update model. The true update only occurs when all data points finish their search.

* which kind of parallelism scheme is suitable, data parallelism or model parallelism?

    Data parallelism can be used, i.e., calculating different data points in parallel.

    For model parallelism, there are two different solutions.

    1). Without model parallelism, each node get one replica of the whole model, which updates locally in parallel, and then synchronizes when local computation all finish

    2). With model parallelism, each node get one partition of the model, which updates in parallel, and then rotates to the neighbor node when local computation for all local data points finish. Repeat until each partition returns back to the original node, then do the final model update.

* which collective communication operations is suitable to synchronize model?

    For solution without model parallelism, Synchronize replicas of the model by allreduce, then calculate the new centroids and go to the next iteration. For vanilla kmeans, the combination of reduce/broadcast, push/pull, regroup/allgather are similar to allreduce.

    For solution with model parallelism, there is no replica exists, but the movement of the model partitions are a kind of synchronized collective operation, supported in Harp by an abstraction of rotate.

## DATAFLOW

![dataflow](/img/4-2-2.png)

## Step 1 --- The Main Method
The tasks of the main class is to configure and run the job iteratively.
```java
generate N data points (D dimensions), write to HDFS
generate M centroids, write to HDFS
for iterations{
    configure a job
    launch the job
}
```

## Step 2 --- The mapCollective function
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


## Step 3 --- The runKmeans function

Harp provides several collective communication operations. Here are some examples provided to show how to apply these collective communication methods to K-Means.

  <ul class="nav nav-pills">
    <li class="active"><a data-toggle="pill" href="#allreduce">Allreduce</a></li>
    <li><a data-toggle="pill" href="#broadcast-reduce">Broadcast-Reduce</a></li>
    <li><a data-toggle="pill" href="#push-pull">Push-Pull</a></li>
    <li><a data-toggle="pill" href="#regroup-allgather">Regroup-Allgather</a></li>
  </ul>

  <div class="tab-content">
    <div id="allreduce" class="tab-pane fade in active">
      <h4>Use AllReduce collective communication to do synchronization</h4>
      <div class="highlight" style="background: #272822"><pre style="line-height: 125%"><span></span><span style="color: #66d9ef">private</span> <span style="color: #66d9ef">void</span> <span style="color: #a6e22e">runKmeans</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">List</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">String</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Configuration</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Context</span> <span style="color: #f8f8f2">context</span><span style="color: #f92672">)</span> <span style="color: #66d9ef">throws</span> <span style="color: #f8f8f2">IOException</span> <span style="color: #f92672">{</span>
      <span style="color: #75715e">// -----------------------------------------------------</span>
      <span style="color: #75715e">// Load centroids</span>
      <span style="color: #75715e">//for every partition in the centoid table, we will use the last element to store the number of points </span>
      <span style="color: #75715e">// which are clustered to the particular partitionID</span>
      <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
      <span style="color: #66d9ef">if</span> <span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">())</span> <span style="color: #f92672">{</span>
      		<span style="color: #f8f8f2">loadCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">.</span><span style="color: #a6e22e">get</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">KMeansConstants</span><span style="color: #f92672">.</span><span style="color: #a6e22e">CFILE</span><span style="color: #f92672">),</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
      <span style="color: #f92672">}</span>
      <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After loading centroids&quot;</span><span style="color: #f92672">);</span>
      <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      <span style="color: #75715e">//broadcast centroids</span>
      <span style="color: #f8f8f2">broadcastCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      <span style="color: #75715e">//after broadcasting</span>
      <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After brodcasting centroids&quot;</span><span style="color: #f92672">);</span>
      <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      <span style="color: #75715e">//load data </span>
      <span style="color: #f8f8f2">ArrayList</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">dataPoints</span> <span style="color: #f92672">=</span> <span style="color: #f8f8f2">loadData</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
      <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #66d9ef">null</span><span style="color: #f92672">;</span>
      <span style="color: #75715e">//iterations</span>
      <span style="color: #66d9ef">for</span><span style="color: #f92672">(</span><span style="color: #66d9ef">int</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">=</span><span style="color: #ae81ff">0</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span> <span style="color: #f92672">&lt;</span> <span style="color: #f8f8f2">iteration</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">++){</span>
            <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">;</span>
      	    <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
      	    <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;Iteraton No.&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">);</span>
      		<span style="color: #75715e">//compute new partial centroid table using previousCenTable and data points</span>
      		<span style="color: #f8f8f2">computation</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">previousCenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">dataPoints</span><span style="color: #f92672">);</span>
      		<span style="color: #75715e">//AllReduce; </span>
      		<span style="color: #75715e">/****************************************/</span>
      		<span style="color: #f8f8f2">allreduce</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;allreduce_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      		<span style="color: #75715e">//we can calculate new centroids</span>
      		<span style="color: #f8f8f2">calculateCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      		<span style="color: #75715e">/****************************************/</span>
      		<span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
      	<span style="color: #f92672">}</span>
      	<span style="color: #75715e">//output results</span>
     <span style="color: #66d9ef">if</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">()){</span>
      	<span style="color: #f8f8f2">outputCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span>  <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span>   <span style="color: #f8f8f2">context</span><span style="color: #f92672">);</span>
      <span style="color: #f92672">}</span>
<span style="color: #f92672">}</span>
 </pre></div>
    </div>
    <div id="broadcast-reduce" class="tab-pane fade">
      <h4>Use broadcast and reduce collective communication to do synchronization</h4>
     
<p>The video below is the step by step guide on how this collective communication works for K-means. The data is partitions into K different partitions with K centroids. Data is then broadcasted to all the different partitions. And the centroids for each of the partition is grouped together and sent to the master node.</p>

<p>Once all the local centroids from the partition is collected in the global centroid table, the updated table is transferred to the root node and then broadcasted again. This step keeps repeating itself till the convergence is reached.
</p>
<div class="highlight" style="background: #272822"><pre style="line-height: 125%"><span></span><span style="color: #66d9ef">private</span> <span style="color: #66d9ef">void</span> <span style="color: #a6e22e">runKmeans</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">List</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">String</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Configuration</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Context</span> <span style="color: #f8f8f2">context</span><span style="color: #f92672">)</span> <span style="color: #66d9ef">throws</span> <span style="color: #f8f8f2">IOException</span> <span style="color: #f92672">{</span>
     <span style="color: #75715e">// -----------------------------------------------------</span>
     <span style="color: #75715e">// Load centroids</span>
     <span style="color: #75715e">//for every partition in the centoid table, we will use the last element to store the number of points </span>
     <span style="color: #75715e">// which are clustered to the particular partitionID</span>
     <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
     <span style="color: #66d9ef">if</span> <span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">())</span> <span style="color: #f92672">{</span>
        <span style="color: #f8f8f2">loadCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">.</span><span style="color: #a6e22e">get</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">KMeansConstants</span><span style="color: #f92672">.</span><span style="color: #a6e22e">CFILE</span><span style="color: #f92672">),</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
     <span style="color: #f92672">}</span>
     <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After loading centroids&quot;</span><span style="color: #f92672">);</span>
     <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
     <span style="color: #75715e">//broadcast centroids</span>
     <span style="color: #f8f8f2">broadcastCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
     <span style="color: #75715e">//after broadcasting</span>
     <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After brodcasting centroids&quot;</span><span style="color: #f92672">);</span>
     <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
     <span style="color: #75715e">//load data </span>
     <span style="color: #f8f8f2">ArrayList</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">dataPoints</span> <span style="color: #f92672">=</span> <span style="color: #f8f8f2">loadData</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
     <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #66d9ef">null</span><span style="color: #f92672">;</span>
     <span style="color: #75715e">//iterations</span>
     <span style="color: #66d9ef">for</span><span style="color: #f92672">(</span><span style="color: #66d9ef">int</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">=</span><span style="color: #ae81ff">0</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span> <span style="color: #f92672">&lt;</span> <span style="color: #f8f8f2">iteration</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">++){</span>
        <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">;</span>
     	<span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
     	<span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;Iteraton No.&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">);</span>
     	<span style="color: #75715e">//compute new partial centroid table using previousCenTable and data points</span>
     	<span style="color: #f8f8f2">computation</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">previousCenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">dataPoints</span><span style="color: #f92672">);</span>
     	<span style="color: #75715e">/****************************************/</span>
     	<span style="color: #f8f8f2">reduce</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;reduce_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">getMasterID</span><span style="color: #f92672">());</span>
     	<span style="color: #66d9ef">if</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">())</span>
     		<span style="color: #f8f8f2">calculateCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
     	<span style="color: #f8f8f2">broadcast</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;bcast_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">getMasterID</span><span style="color: #f92672">(),</span> <span style="color: #66d9ef">false</span><span style="color: #f92672">);</span>
     	<span style="color: #75715e">/****************************************/</span>
     	<span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
    <span style="color: #f92672">}</span>
     	<span style="color: #75715e">//output results</span>
    <span style="color: #66d9ef">if</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">()){</span>
     	<span style="color: #f8f8f2">outputCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span>  <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span>   <span style="color: #f8f8f2">context</span><span style="color: #f92672">);</span>
    <span style="color: #f92672">}</span>
<span style="color: #f92672">}</span>
     </pre></div>
     </div>
    <div id="push-pull" class="tab-pane fade">
      <h4>Use push and pull collective communication to do synchronization</h4> 
    <div class="highlight" style="background: #272822"><pre style="line-height: 125%"><span></span><span style="color: #66d9ef">private</span> <span style="color: #66d9ef">void</span> <span style="color: #a6e22e">runKmeans</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">List</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">String</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Configuration</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Context</span> <span style="color: #f8f8f2">context</span><span style="color: #f92672">)</span> <span style="color: #66d9ef">throws</span> <span style="color: #f8f8f2">IOException</span> <span style="color: #f92672">{</span>
		  <span style="color: #75715e">// -----------------------------------------------------</span>
		  <span style="color: #75715e">// Load centroids</span>
		  <span style="color: #75715e">//for every partition in the centoid table, we will use the last element to store the number of points </span>
		  <span style="color: #75715e">// which are clustered to the particular partitionID</span>
		  <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
		  <span style="color: #66d9ef">if</span> <span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">())</span> <span style="color: #f92672">{</span>
			  <span style="color: #f8f8f2">loadCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">.</span><span style="color: #a6e22e">get</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">KMeansConstants</span><span style="color: #f92672">.</span><span style="color: #a6e22e">CFILE</span><span style="color: #f92672">),</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
		  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After loading centroids&quot;</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//broadcast centroids</span>
		  <span style="color: #f8f8f2">broadcastCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//after broadcasting</span>
		  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After brodcasting centroids&quot;</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//load data </span>
		  <span style="color: #f8f8f2">ArrayList</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">dataPoints</span> <span style="color: #f92672">=</span> <span style="color: #f8f8f2">loadData</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">globalTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span>  <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
		  <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #66d9ef">null</span><span style="color: #f92672">;</span>
		  <span style="color: #75715e">//iterations</span>
		  <span style="color: #66d9ef">for</span><span style="color: #f92672">(</span><span style="color: #66d9ef">int</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">=</span><span style="color: #ae81ff">0</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span> <span style="color: #f92672">&lt;</span> <span style="color: #f8f8f2">iteration</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">++){</span>
			  <span style="color: #75715e">// clean contents in the table.</span>
			  <span style="color: #f8f8f2">globalTable</span><span style="color: #f92672">.</span><span style="color: #a6e22e">release</span><span style="color: #f92672">();</span>
			  <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">;</span>
			  <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
			  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;Iteraton No.&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">//compute new partial centroid table using previousCenTable and data points</span>
			  <span style="color: #f8f8f2">computation</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">previousCenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">dataPoints</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">/****************************************/</span>
			  <span style="color: #f8f8f2">push</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;push_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">globalTable</span> <span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Partitioner</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">getNumWorkers</span><span style="color: #f92672">()));</span>
			  <span style="color: #75715e">//we can calculate new centroids</span>
			  <span style="color: #f8f8f2">calculateCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">globalTable</span><span style="color: #f92672">);</span>
			  <span style="color: #f8f8f2">pull</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;pull_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">globalTable</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">true</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">/****************************************/</span>
			  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
		  <span style="color: #75715e">//output results</span>
		  <span style="color: #66d9ef">if</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">()){</span>
			  <span style="color: #f8f8f2">outputCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span>  <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span>   <span style="color: #f8f8f2">context</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
	 <span style="color: #f92672">}</span>
</pre></div>
    </div>
    <div id="regroup-allgather" class="tab-pane fade">
      <h3>Use Regroup and allgather collective communication to do synchronization</h3>
	<div class="highlight" style="background: #272822"><pre style="line-height: 125%"><span></span><span style="color: #66d9ef">private</span> <span style="color: #66d9ef">void</span> <span style="color: #a6e22e">runKmeans</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">List</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">String</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Configuration</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">Context</span> <span style="color: #f8f8f2">context</span><span style="color: #f92672">)</span> <span style="color: #66d9ef">throws</span> <span style="color: #f8f8f2">IOException</span> <span style="color: #f92672">{</span>
		  <span style="color: #75715e">// -----------------------------------------------------</span>
		  <span style="color: #75715e">// Load centroids</span>
		  <span style="color: #75715e">//for every partition in the centoid table, we will use the last element to store the number of points </span>
		  <span style="color: #75715e">// which are clustered to the particular partitionID</span>
		  <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
		  <span style="color: #66d9ef">if</span> <span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">())</span> <span style="color: #f92672">{</span>
			  <span style="color: #f8f8f2">loadCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">.</span><span style="color: #a6e22e">get</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">KMeansConstants</span><span style="color: #f92672">.</span><span style="color: #a6e22e">CFILE</span><span style="color: #f92672">),</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
		  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After loading centroids&quot;</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//broadcast centroids</span>
		  <span style="color: #f8f8f2">broadcastCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//after broadcasting</span>
		  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;After brodcasting centroids&quot;</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #75715e">//load data </span>
		  <span style="color: #f8f8f2">ArrayList</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">dataPoints</span> <span style="color: #f92672">=</span> <span style="color: #f8f8f2">loadData</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">fileNames</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">vectorSize</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">conf</span><span style="color: #f92672">);</span>
		  <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;</span><span style="color: #f8f8f2">DoubleArray</span><span style="color: #f92672">&gt;</span> <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #66d9ef">null</span><span style="color: #f92672">;</span>
		  <span style="color: #75715e">//iterations</span>
		  <span style="color: #66d9ef">for</span><span style="color: #f92672">(</span><span style="color: #66d9ef">int</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">=</span><span style="color: #ae81ff">0</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span> <span style="color: #f92672">&lt;</span> <span style="color: #f8f8f2">iteration</span><span style="color: #f92672">;</span> <span style="color: #f8f8f2">iter</span><span style="color: #f92672">++){</span>
			  <span style="color: #f8f8f2">previousCenTable</span> <span style="color: #f92672">=</span>  <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">;</span>
			  <span style="color: #f8f8f2">cenTable</span> <span style="color: #f92672">=</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">Table</span><span style="color: #f92672">&lt;&gt;(</span><span style="color: #ae81ff">0</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">new</span> <span style="color: #f8f8f2">DoubleArrPlus</span><span style="color: #f92672">());</span>
			  <span style="color: #f8f8f2">System</span><span style="color: #f92672">.</span><span style="color: #a6e22e">out</span><span style="color: #f92672">.</span><span style="color: #a6e22e">println</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;Iteraton No.&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">//compute new partial centroid table using previousCenTable and data points</span>
			  <span style="color: #f8f8f2">computation</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">previousCenTable</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">dataPoints</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">/****************************************/</span>
			  <span style="color: #75715e">//regroup and allgather to synchronized centroids</span>
			  <span style="color: #f8f8f2">regroup</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;regroup_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span> <span style="color: #66d9ef">null</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">//we can calculate new centroids</span>
			  <span style="color: #f8f8f2">calculateCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
			  <span style="color: #f8f8f2">allgather</span><span style="color: #f92672">(</span><span style="color: #e6db74">&quot;main&quot;</span><span style="color: #f92672">,</span> <span style="color: #e6db74">&quot;allgather_&quot;</span><span style="color: #f92672">+</span><span style="color: #f8f8f2">iter</span><span style="color: #f92672">,</span> <span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
			  <span style="color: #75715e">/****************************************/</span>
			  <span style="color: #f8f8f2">printTable</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
		  <span style="color: #75715e">//output results</span>
		  <span style="color: #66d9ef">if</span><span style="color: #f92672">(</span><span style="color: #66d9ef">this</span><span style="color: #f92672">.</span><span style="color: #a6e22e">isMaster</span><span style="color: #f92672">()){</span>
			  <span style="color: #f8f8f2">outputCentroids</span><span style="color: #f92672">(</span><span style="color: #f8f8f2">cenTable</span><span style="color: #f92672">,</span>  <span style="color: #f8f8f2">conf</span><span style="color: #f92672">,</span>   <span style="color: #f8f8f2">context</span><span style="color: #f92672">);</span>
		  <span style="color: #f92672">}</span>
	 <span style="color: #f92672">}</span>
</pre></div>
   </div>
  </div>


## Step 4 --- Compute local centroids

```java
private void computation(Table<DoubleArray> cenTable, Table<DoubleArray> previousCenTable,ArrayList<DoubleArray> dataPoints){
    double err=0;
    for(DoubleArray aPoint: dataPoints){
    //for each data point, find the nearest centroid
        double minDist = -1;
        double tempDist = 0;
        int nearestPartitionID = -1;
        for(Partition ap: previousCenTable.getPartitions()){
            DoubleArray aCentroid = (DoubleArray) ap.get();
            tempDist = calcEucDistSquare(aPoint, aCentroid, vectorSize);
            if(minDist == -1 || tempDist < minDist){
                minDist = tempDist;
                nearestPartitionID = ap.id();
            }
        }
        err+=minDist;

        //for the certain data point, found the nearest centroid.
        // add the data to a new cenTable.
        double[] partial = new double[vectorSize+1];
        for(int j=0; j < vectorSize; j++){
            partial[j] = aPoint.get()[j];
        }
        partial[vectorSize]=1;

        if(cenTable.getPartition(nearestPartitionID) == null){
            Partition<DoubleArray> tmpAp = new Partition<DoubleArray>(nearestPartitionID, new DoubleArray(partial, 0, vectorSize+1));
            cenTable.addPartition(tmpAp);
        }else{
             Partition<DoubleArray> apInCenTable = cenTable.getPartition(nearestPartitionID);
             for(int i=0; i < vectorSize +1; i++){
             apInCenTable.get().get()[i] += partial[i];
             }
        }
    }
    System.out.println("Errors: "+err);
}
```

## Step 5 --- Calculate new centroids

```java
private void calculateCentroids( Table<DoubleArray> cenTable){
    for( Partition<DoubleArray> partialCenTable: cenTable.getPartitions()){
        double[] doubles = partialCenTable.get().get();
        for(int h = 0; h < vectorSize; h++){
            doubles[h] /= doubles[vectorSize];
        }
        doubles[vectorSize] = 0;
	}
	System.out.println("after calculate new centroids");
    printTable(cenTable);
}
```

## COMPILE
```bash
cd $HARP_ROOT_DIR
mvn clean package
cd $HARP_ROOT_DIR/harp-tutorial-app
cp target/harp-tutorial-app-1.0-SNAPSHOT.jar $HADOOP_HOME
cd $HADOOP_HOME
```

## USAGE
Run Harp K-Means:
```bash
hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.kmeans.common.KmeansMapCollective <numOfDataPoints> <num of Centroids> <size of vector> <number of map tasks> <number of iteration> <workDir> <localDir> <communication operation>

   <numOfDataPoints>: the number of data points you want to generate randomly
   <num of centriods>: the number of centroids you want to clustering the data to
   <size of vector>: the number of dimension of the data
   <number of map tasks>: number of map tasks
   <number of iteration>: the number of iterations to run
   <work dir>: the root directory for this running in HDFS
   <local dir>: the harp kmeans will firstly generate files which contain data points to local directory. Set this argument to determine the local directory.
   <communication operation> includes:
		[allreduce]: use allreduce operation to synchronize centroids
		[regroup-allgather]: use regroup and allgather operation to synchronize centroids
		[broadcast-reduce]: use broadcast and reduce operation to synchronize centroids
		[push-pull]: use push and pull operation to synchronize centroids
```

For example:

```bash
hadoop jar harp-tutorial-app-1.0-SNAPSHOT.jar edu.iu.kmeans.common.KmeansMapCollective 1000 10 10 2 10 /kmeans /tmp/kmeans allreduce
```

Fetch the results:
```bash
hdfs dfs -ls /
hdfs dfs -cat /kmeans/centroids/*
```






