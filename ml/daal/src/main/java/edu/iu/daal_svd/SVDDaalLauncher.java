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

package edu.iu.daal_svd;

import edu.iu.fileformat.MultiFileInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class SVDDaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new SVDDaalLauncher(), argv);
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

      if (args.length < 8) {
      System.err
        .println("Usage: edu.iu.daal_svd.SVDDaalLauncher "
          + "<num Of DataPoints> <vector size> "
          + "<num of point files per worker>"
          + "<number of map tasks> <num threads> "
          + "<mem>"
          + "<work dir> <local points dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    int numOfDataPoints =
      Integer.parseInt(args[0]);
    // int numCentroids = Integer.parseInt(args[1]);
    int vectorSize = Integer.parseInt(args[1]);
    int numPointFilePerWorker =
      Integer.parseInt(args[2]);
    int numMapTasks = Integer.parseInt(args[3]);
    int numThreads = Integer.parseInt(args[4]);
    int mem = Integer.parseInt(args[5]);
    String workDir = args[6];
    String localPointFilesDir = args[7];
    boolean regenerateData = true;
    if (args.length == 9) {
      regenerateData =
        Boolean.parseBoolean(args[8]);
    }
    System.out.println("Number of Map Tasks = "
      + numMapTasks);
    int numPointFiles =
      numMapTasks * numPointFilePerWorker;
    if (numOfDataPoints / numPointFiles == 0) {
      return -1;
    }
    launch(numOfDataPoints,
      vectorSize, numPointFiles, numMapTasks,
      numThreads, mem, workDir,
      localPointFilesDir, regenerateData);
    return 0;
  }

  private void launch(int numOfDataPoints,
   int vectorSize, int numPointFiles, int numMapTasks,
    int numThreads, int mem,
    String workDir, String localPointFilesDir,
    boolean generateData) throws IOException,
    URISyntaxException, InterruptedException,
    ExecutionException, ClassNotFoundException {
    Configuration configuration = getConf();
    Path workDirPath = new Path(workDir);
    FileSystem fs = FileSystem.get(configuration);
    Path dataDir = new Path(workDirPath, "data");
    // Path cenDir =
    //   new Path(workDirPath, "centroids");
    // if (fs.exists(cenDir)) {
    //   fs.delete(cenDir, true);
    // }
    // fs.mkdirs(cenDir);
    Path outDir = new Path(workDirPath, "out");
    if (fs.exists(outDir)) {
      fs.delete(outDir, true);
    }
    if (generateData) {
      System.out.println("Generate data.");
      SVDUtil.generateData(numOfDataPoints,
        vectorSize, numPointFiles,
        configuration, fs, dataDir,
        localPointFilesDir);
    }

    long startTime = System.currentTimeMillis();
    runSVDAllReduce(numOfDataPoints,
      vectorSize, numPointFiles,
      numMapTasks, numThreads, mem,
      dataDir, outDir, configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total SVD Execution Time: "
        + (endTime - startTime));
  }

  private void runSVDAllReduce(
    int numOfDataPoints,
    int vectorSize, int numPointFiles,
    int numMapTasks, int numThreads,
    int mem, Path dataDir,
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
    Job svdJob =
      configureSVDJob(numOfDataPoints,
        vectorSize, numPointFiles,
        numMapTasks, numThreads,  mem,
        dataDir, outDir, configuration);
    System.out
      .println("Job"
        + " configure in "
        + (System.currentTimeMillis() - perJobSubmitTime)
        + " miliseconds.");
    // ----------------------------------------------------------
    boolean jobSuccess =
      svdJob.waitForCompletion(true);
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
      System.out.println("SVD Job fails.");
    }
  }

  private Job configureSVDJob(
    int numOfDataPoints,
    int vectorSize, int numPointFiles,
    int numMapTasks, int numThreads,
    int mem, Path dataDir,
    Path outDir, Configuration configuration)
    throws IOException, URISyntaxException {
    Job job =
      Job
        .getInstance(configuration, "svd_job");
    FileInputFormat.setInputPaths(job, dataDir);
    FileOutputFormat.setOutputPath(job, outDir);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(SVDDaalLauncher.class);
    job
      .setMapperClass(SVDDaalCollectiveMapper.class);
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);

    // mapreduce.map.collective.memory.mb
    // 125000
    jobConf.setInt(
      "mapreduce.map.collective.memory.mb", mem);

    jobConf.setInt("mapreduce.task.timeout", 60000000);
    int xmx = (int) Math.ceil((mem - 2000)*0.5);
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
    jobConfig.setInt(Constants.VECTOR_SIZE,
      vectorSize);
    jobConfig.setInt(Constants.NUM_MAPPERS,
      numMapTasks);
    jobConfig.setInt(Constants.NUM_THREADS,
      numThreads);
    return job;
  }
}
