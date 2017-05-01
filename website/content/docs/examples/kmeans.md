---
title: Clustering using K Means
---

&nbsp;&nbsp;&nbsp;&nbsp; Clustering is an example of unsupervised Machine learning algorithm which tries to cluster the data points (vector) into groups based on some similarity measure.  The goal of a clustering algorithm is to group similar examples into a cluster. For a given data set, it is possible to have different number of clusters depending upon the number of data points and the threshold for similarity of data points within a cluster.  

&nbsp;&nbsp;&nbsp;&nbsp; There are many applications of K Means algorithm. A retail company may want to segment its customers based on geographical area and their purchase patterns. A librarian working with a digital library might want to cluster similar documents and group them together. All these applications are perfect examples of clustering where the user has already some idea about the number of clusters that are supposed to be in the data set.  

&nbsp;&nbsp;&nbsp;&nbsp; There are various types of clustering algorithms. Of all of them, we will discuss about a partition based clustering algorithm called K Means. Partition based clustering simply divides the data points (or the d-dimensional space that the data points reside) into various clusters or groups. K means is an example of exclusive clustering in which each data point is assigned only to one cluster. The `K` in the `K` Means algorithm denotes the number of clusters. This is a user specified parameter. Regardless of the number of clusters that are actually in the data (which we do not know), we will specify the number of clusters as an input to the algorithm.  

&nbsp;&nbsp;&nbsp;&nbsp; In order to better understand K Means algorithm, we will have to understand the following terminologies,  
1. `Centroids`: Centroids are referred to as the prototype of any clusters. If the cluster prototype is mean, then the algorithm is referred to as K Means algorithm and if the cluster prototype is median, then the algorithm is K Median clustering.
2. `Distance`: To measure the similarity between the data points, we will calculate the distance between each pair of data points to check their similarity. This means that the points are very close to each other (less distance) in the *d* dimensional space (where *d* refers to the number of dimensions in the data) are similar to each other.

&nbsp;&nbsp;&nbsp;&nbsp; The K Means algorithm works as follows,  
1. Choose `K` number of *d* dimensional data points randomly. These will be our initial set of centroids (cluster prototypes)
2. Assign each data point to a cluster. This is done by finding the distance between the data point and all the cluster centroids and whichever centroid has less distance with the data points take the corresponding data point into its cluster. 
3. Update the new centroids by taking an average over the *d* dimensional data points that are assigned to it. 
4. Repeat steps 2 and 3 until convergence. Here, convergence refers to the fact that none of the data points change their clusters. 

&nbsp;&nbsp;&nbsp;&nbsp; The performance of the K means algorithm greatly depends on the number of clusters that we choose and initially selecting the cluster centroids randomly. K means algorithm is also a greedy algorithm which does not guarantee convergence to the gloabl minimum. It is always prone to converge to a local minimum in which case, there is no guarantee to check whether the obtained convergence point is the global minimum or local minimum. 

&nbsp;&nbsp;&nbsp;&nbsp; To better help you understand how the k means algorithm works, we have developed a video tutorial in which we initially create random cluster of data points and provide visual interpretation of k means algorithm. The link to the URL is mentioned below.  

[YouTube Link](https://www.youtube.com/watch?v=0hpeium8E0g)

## Parallel Design of K Means Clustering  

<img src="/img/kmeans.png" width="80%" >

&nbsp;&nbsp;&nbsp;&nbsp; As illustrated above, the algorithm for K means updates the cluster prototypes independently. The centroid updates can happen independently of other centroids. In vanilla kmeans, the centroids are the output of the algorithm. A centroid is a data point living in a *d* dimensional space where the data vectors are present. Hence, the output of our kmeans algorithm would be ***K*** *d*-dimensional vectors, each representing a cluster prototype (centroid).

1. What is the model? What kind of data structure is used?  
&nbsp;&nbsp;&nbsp;&nbsp; In vanilla k means implementation, centroids are the output of kmeans algorithm. This can be easily extended to enhanced version where each centroid is given a ID indicating the cluster and assigning the cluster ID to the data points that fall inside these clusters.  
&nbsp;&nbsp;&nbsp;&nbsp; In our implementation, the centroids have a vector structure as a double array.  

2. What are the characteristic of data dependency in model update computation, can updates run concurrently?  
&nbsp;&nbsp;&nbsp;&nbsp; In the core model update computation, each data points needs to have access to the complete model. This access is required to calculate the distance between every data point and the cluster centroid. Since harp uses model concurrent update, each data point finds its nearest centroid and partially updates the model. The true final update occurs only when all the data points finish their part of model update. 

3. Which kind of parallellism is suitable, data parallelism or model parallelism?  
&nbsp;&nbsp;&nbsp;&nbsp; Since Harp is built on top of Hadoop, Data parallelism comes for free. The data points are split depending on the chunk size configured in Hadoop and gets loaded into Mappers. There are two different solutions with which model parallelism can be achieved.
	+ Each node gets one replica of the entire model and they update the model based on the chunk of the data that they receive.  These are local updates and it happens in parallel. Each node will have a copy of the local model and they synchronize when all the nodes complete their local model update computations. Here, the model update is not parallelized, but they are replicated and updated based on the local data 
	+ The second method achieves the model parallelism by partitioning the model data and providing every node with one partition of the model. This type of mechanism rotates the model data once the local computation in a single node is completed. This process is repeated until each partition returns back to the original node after which the final model update happens. The final model update is gathering all the partitioned model data and combining it into a single model.  

4. Which collective communication operation is suitable to synchronize the model?  
&nbsp;&nbsp;&nbsp;&nbsp; The nature of the kmeans algorithm provides us to use different collective communication operation using Harp. For the solution without model parallelism, we are using model replicas that are being sent to every mapper where each replica will be updated based on the local data. The replicas of the model can be synchronized using `allreduce`, after which we can calculate the new centroids to perform the next iteration. For vanilla kmeans, the combination of reduce/broadcast, push/pull, regroup/allgather are similar to allreduce.  

&nbsp;&nbsp;&nbsp;&nbsp; For solutions employing model parallelism model is not replicated between mappers, instead the model data is partitioned between nodes. The movemement of model partitions are a kind of synchronized collective operation achieved by `rotate` collective communication operation supported in Harp. 


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
