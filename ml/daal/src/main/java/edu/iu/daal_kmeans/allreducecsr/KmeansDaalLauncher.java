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

package edu.iu.daal_kmeans.allreducecsr;

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

public class KmeansDaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new KmeansDaalLauncher(), argv);
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
        .println("Usage: edu.iu.kmeans.KmeansDaalLauncher "
	  + "<nClasses>"
          + "<nVectorsInBlock>"
	  + "<nIterations>"
	  + "<number of map tasks> "
	  + "<num threads>"
          + "<mem>"
	  + "<imput dir>"
          + "<work dir>");

      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    
    int nClasses = Integer.parseInt(args[0]);
    int nVectorsInBlock =  Integer.parseInt(args[1]);
    int nIterations = Integer.parseInt(args[2]);
    int numMapTasks = Integer.parseInt(args[3]);
    int numThreads = Integer.parseInt(args[4]);
    int mem = Integer.parseInt(args[5]);
    String inputPath = args[6]; 
    String workDir = args[7];
    
    launch(nClasses, nVectorsInBlock, nIterations, numMapTasks, numThreads, mem, inputPath, workDir);
    return 0;
  }

  private void launch(int nClasses, int nVectorsInBlock, int nIterations, int numMapTasks, 
		  int numThreads, int mem, String inputPath, String workDir) throws IOException,
    URISyntaxException, InterruptedException,
    ExecutionException, ClassNotFoundException 
    {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    Path dataDir = new Path(inputPath);
    Path workDirPath = new Path(workDir);
    
    if (fs.exists(workDirPath)) {
      fs.delete(workDirPath, true);
    }

	// -------------- generate data if required --------------

    long startTime = System.currentTimeMillis();

    runKmeansAllReduce(nClasses, nVectorsInBlock, nIterations, numMapTasks, numThreads, mem, 
      dataDir, workDirPath, configuration);

    long endTime = System.currentTimeMillis();
    System.out
      .println("Total K-means Execution Time: "
        + (endTime - startTime));
  }

  private void runKmeansAllReduce(
    int nClasses, int nVectorsInBlock, int nIterations, int numMapTasks, 
    int numThreads, int mem, Path dataDir, 
    Path workDirPath, Configuration configuration)
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
      configureKmeansJob(nClasses, nVectorsInBlock, nIterations, numMapTasks, numThreads, mem,
        dataDir, workDirPath, configuration);
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
      System.out.println("Kmeans Job fails.");
    }
  }

  private Job configureKmeansJob(
    int nClasses, int nVectorsInBlock, int nIterations, int numMapTasks, 
    int numThreads, int mem, 
    Path dataDir, 
    Path workDirPath, Configuration configuration)
    throws IOException, URISyntaxException {
    Job job =
      Job
        .getInstance(configuration, "kmeans_job");
    FileInputFormat.setInputPaths(job, dataDir);
    FileOutputFormat.setOutputPath(job, workDirPath);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(KmeansDaalLauncher.class);
    job
      .setMapperClass(KmeansDaalCollectiveMapper.class);
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);

    jobConf.setInt(
      "mapreduce.map.collective.memory.mb", mem);

    jobConf.setInt("mapreduce.task.timeout", 60000000);
    int xmx = (int) Math.ceil((mem)*0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);
    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");

    job.setNumReduceTasks(0);
    Configuration jobConfig =
      job.getConfiguration();

    
    jobConfig.setInt(Constants.NUM_CLASS,
      nClasses);
    jobConfig.setInt(Constants.NUM_VEC_BLOCK,
      nVectorsInBlock);
    jobConfig.setInt(Constants.NUM_MAPPERS,
      numMapTasks);
    jobConfig.setInt(Constants.NUM_THREADS,
      numThreads);
    jobConfig.setInt(Constants.NUM_ITERATIONS,
      nIterations);
   
    return job;
  }
}
