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

package edu.iu.daal_stump;

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

public class STUMPDaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new STUMPDaalLauncher(), argv);
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
        .println("Usage: edu.iu.daal_svd.STUMPDaalLauncher "
          + "<feature dim> "
          + "<file dim > "
	  + "<number of map tasks>"
	  + "<num threads> "
          + "<mem>"
          + "<input train dir>"
          + "<input test dir>"
          + "<work dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    int nFeature =
      Integer.parseInt(args[0]);
    int nFileDim = Integer.parseInt(args[1]);
    int numMapTasks = Integer.parseInt(args[2]);
    int numThreads = Integer.parseInt(args[3]);
    int mem = Integer.parseInt(args[4]);
    String trainDataDir = args[5];
    String testDataDir = args[6];
    String workDir = args[7];
   
    launch(nFeature, nFileDim, numMapTasks, numThreads, mem, trainDataDir, testDataDir, workDir);
    return 0;
  }

  private void launch(int nFeature, int nFileDim, 
		      int numMapTasks, int numThreads, int mem, 
		      String trainDataDir, String testDataDir, String workDir) throws IOException,
    URISyntaxException, InterruptedException,
    ExecutionException, ClassNotFoundException 
  {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    Path dataDir = new Path(trainDataDir);
    Path outDir = new Path(workDir);

    if (fs.exists(outDir)) {
      fs.delete(outDir, true);
    }

    long startTime = System.currentTimeMillis();

    runSTUMP(nFeature, nFileDim, 
		    numMapTasks, numThreads, mem, trainDataDir, testDataDir, dataDir, outDir, configuration);

    long endTime = System.currentTimeMillis();
    System.out
      .println("Total STUMP Multi Dense Execution Time: "
        + (endTime - startTime));
  }

  private void runSTUMP(
    int nFeature, int nFileDim,
    int numMapTasks, int numThreads, int mem, 
    String trainDataDir, String testDataDir,
    Path dataDir,
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

    Job STUMPJob =
      configureSTUMPJob(nFeature, nFileDim,
		    numMapTasks, numThreads, mem, trainDataDir, testDataDir, dataDir, outDir, configuration);

    System.out
      .println("Job"
        + " configure in "
        + (System.currentTimeMillis() - perJobSubmitTime)
        + " miliseconds.");

    // ----------------------------------------------------------
    boolean jobSuccess =
      STUMPJob.waitForCompletion(true);
    System.out
      .println("end job "
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
      System.out.println("STUMP Job fails.");
    }
  }

  private Job configureSTUMPJob(
    int nFeature, int nFileDim,
    int numMapTasks, int numThreads, int mem, 
    String trainDataDir, String testDataDir,
    Path dataDir,
    Path outDir, Configuration configuration
    )
    throws IOException, URISyntaxException {
    Job job =
      Job
        .getInstance(configuration, "STUMP_job");
    FileInputFormat.setInputPaths(job, dataDir);
    FileOutputFormat.setOutputPath(job, outDir);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(STUMPDaalLauncher.class);
    job
      .setMapperClass(STUMPDaalCollectiveMapper.class);
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

    int xmx = (int) Math.ceil((mem - 2000)*0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);

    jobConf.setInt("mapreduce.task.timeout", 60000000);

    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");

    job.setNumReduceTasks(0);
    Configuration jobConfig =
      job.getConfiguration();


    // set constant
    jobConfig.setInt(Constants.FEATURE_DIM,
      nFeature);
    jobConfig.setInt(Constants.FILE_DIM,
      nFileDim);
    jobConfig.setInt(Constants.NUM_MAPPERS,
      numMapTasks);
    jobConfig.setInt(Constants.NUM_THREADS,
      numThreads);
    jobConfig.set(Constants.TEST_FILE_PATH,
      testDataDir);

    return job;
  }
}
