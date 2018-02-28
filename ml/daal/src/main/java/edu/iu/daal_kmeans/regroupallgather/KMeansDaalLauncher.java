/*
 * Copyright 2013-2016 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.daal_kmeans.regroupallgather;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.hadoop.filecache.DistributedCache;
import java.net.URI;

import edu.iu.fileformat.MultiFileInputFormat;

public class KMeansDaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new KMeansDaalLauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches all the tasks in order.
   */
  @Override
  public int run(String[] args) throws Exception {

      /* Put shared libraries into the distributed cache */
      Configuration conf = this.getConf();

      DistributedCache.createSymlink(conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libJavaAPI.so#libJavaAPI.so"), conf);

      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so.2#libtbb.so.2"), conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so#libtbb.so"), conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so.2#libtbbmalloc.so.2"), conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so#libtbbmalloc.so"), conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libiomp5.so#libiomp5.so"), conf);
	  DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so#libhdfs.so"), conf);
      DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so.0.0.0#libhdfs.so.0.0.0"), conf);

      if (args.length < 10) {
      System.err
        .println("Usage: edu.iu.kmeans.KMeansDaalLauncher "
          + "<num Of DataPoints> <num of Centroids> <vector size> "
          + "<num of point files per worker>"
          + "<number of map tasks> <num threads><number of iteration> "
          + "<mem>"
          + "<work dir> <local points dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    int numOfDataPoints =
      Integer.parseInt(args[0]);
    int numCentroids = Integer.parseInt(args[1]);
    int vectorSize = Integer.parseInt(args[2]);
    int numPointFilePerWorker =
      Integer.parseInt(args[3]);
    int numMapTasks = Integer.parseInt(args[4]);
    int numThreads = Integer.parseInt(args[5]);
    int numIteration = Integer.parseInt(args[6]);
    int mem = Integer.parseInt(args[7]);
    String workDir = args[8];
    String localPointFilesDir = args[9];
    boolean regenerateData = true;
    if (args.length == 11) {
      regenerateData =
        Boolean.parseBoolean(args[10]);
    }
    System.out.println("Number of Map Tasks = "
      + numMapTasks);
    int numPointFiles =
      numMapTasks * numPointFilePerWorker;
    if (numOfDataPoints / numPointFiles == 0
      || numCentroids / numMapTasks == 0) {
      return -1;
    }
    if (numIteration == 0) {
      numIteration = 1;
    }
    launch(numOfDataPoints, numCentroids,
      vectorSize, numPointFiles, numMapTasks,
      numThreads, numIteration, mem, workDir,
      localPointFilesDir, regenerateData);
    return 0;
  }

  private void launch(int numOfDataPoints,
    int numCentroids, int vectorSize,
    int numPointFiles, int numMapTasks,
    int numThreads, int numIterations, int mem,
    String workDir, String localPointFilesDir,
    boolean generateData) throws IOException,
    URISyntaxException, InterruptedException,
    ExecutionException, ClassNotFoundException {
    Configuration configuration = getConf();
    Path workDirPath = new Path(workDir);
    FileSystem fs = FileSystem.get(configuration);
    Path dataDir = new Path(workDirPath, "data");
    Path cenDir =
      new Path(workDirPath, "centroids");

    fs.mkdirs(cenDir);
    Path outDir = new Path(workDirPath, "out");
    if (fs.exists(outDir)) {
      fs.delete(outDir, true);
    }

	// -------------- generate data if required --------------
    if (generateData) {
        System.out.println("Generate data.");
        KMUtil.generateData(numOfDataPoints,
                numCentroids, vectorSize, numPointFiles,
                configuration, fs, dataDir, cenDir,
                localPointFilesDir);

        if (fs.exists(cenDir)) {
            fs.delete(cenDir, true);
        }

        KMUtil.generateCentroids(numCentroids,
                vectorSize, configuration, cenDir, fs);
    }

    long startTime = System.currentTimeMillis();
    runKMeansAllReduce(numOfDataPoints,
      numCentroids, vectorSize, numPointFiles,
      numMapTasks, numThreads, numIterations, mem,
      dataDir, cenDir, outDir, configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total K-means Execution Time: "
        + (endTime - startTime));
  }

  private void runKMeansAllReduce(
    int numOfDataPoints, int numCentroids,
    int vectorSize, int numPointFiles,
    int numMapTasks, int numThreads,
    int numIterations, int mem, Path dataDir, Path cenDir,
    Path outDir, Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    System.out.println("Starting Job");
    // ----------------------------------------------------------------------
    long perJobSubmitTime =
      System.currentTimeMillis();
    System.out
      .println("Start Job "
        + new SimpleDateFormat("HH:mm:ss.SSS")
          .format(Calendar.getInstance()
            .getTime()));
    Job kmeansJob =
      configureKMeansJob(numOfDataPoints,
        numCentroids, vectorSize, numPointFiles,
        numMapTasks, numThreads, numIterations, mem,
        dataDir, cenDir, outDir, configuration);
    System.out
      .println("Job"
        + " configure in "
        + (System.currentTimeMillis() - perJobSubmitTime)
        + " miliseconds.");
    // ----------------------------------------------------------
    boolean jobSuccess =
      kmeansJob.waitForCompletion(true);
    System.out
      .println("end Jod "
        + new SimpleDateFormat("HH:mm:ss.SSS")
          .format(Calendar.getInstance()
            .getTime()));
    System.out
      .println("Job"
        + " finishes in "
        + (System.currentTimeMillis() - perJobSubmitTime)
        + " miliseconds.");
    // ---------------------------------------------------------
    if (!jobSuccess) {
      System.out.println("KMeans Job fails.");
    }
  }

  private Job configureKMeansJob(
    int numOfDataPoints, int numCentroids,
    int vectorSize, int numPointFiles,
    int numMapTasks, int numThreads,
    int numIterations, int mem, Path dataDir, Path cenDir,
    Path outDir, Configuration configuration)
    throws IOException, URISyntaxException {
    Job job =
      Job
        .getInstance(configuration, "kmeans_job");
    FileInputFormat.setInputPaths(job, dataDir);
    FileOutputFormat.setOutputPath(job, outDir);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(KMeansDaalLauncher.class);
    job
      .setMapperClass(KMeansDaalCollectiveMapper.class);
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);

    jobConf.setInt(
      "mapreduce.map.collective.memory.mb", mem);

    int xmx = (int) Math.ceil((mem)*0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);
    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");

    job.setNumReduceTasks(0);
    Configuration jobConfig =
      job.getConfiguration();
    jobConfig.setInt(Constants.POINTS_PER_FILE,
      numOfDataPoints / numPointFiles);
    jobConfig.setInt(Constants.NUM_CENTROIDS,
      numCentroids);
    jobConfig.setInt(Constants.VECTOR_SIZE,
      vectorSize);
    jobConfig.setInt(Constants.NUM_MAPPERS,
      numMapTasks);
    jobConfig.setInt(Constants.NUM_THREADS,
      numThreads);
    jobConfig.setInt(Constants.NUM_ITERATIONS,
      numIterations);
    jobConfig.set(Constants.CEN_DIR,
      cenDir.toString());
    return job;
  }
}
