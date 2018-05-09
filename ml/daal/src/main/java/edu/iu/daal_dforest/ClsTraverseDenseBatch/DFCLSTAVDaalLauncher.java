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

package edu.iu.daal_dforest.ClsTraverseDenseBatch;

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

public class DFCLSTAVDaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new DFCLSTAVDaalLauncher(), argv);
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

      if (args.length < 11) {
      System.err
        .println("Usage: edu.iu.daal_svd.DFCLSTAVDaalLauncher "
          + "<feature dim> "
          + "<file dim > "
	  + "<nClass>"
	  + "<ntrees>"
	  + "<minobsleafnode>"
	  + "<maxTreeDepth>"
	  + "<number of map tasks>"
	  + "<num threads> "
          + "<mem>"
          + "<input train dir>"
          + "<work dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    int nFeature =
      Integer.parseInt(args[0]);
    int nFileDim = Integer.parseInt(args[1]);
    int nClass = Integer.parseInt(args[2]);
    int nTrees = Integer.parseInt(args[3]);
    int minObsLeafNodes = Integer.parseInt(args[4]);
    int maxTreeDepth = Integer.parseInt(args[5]);
    int numMapTasks = Integer.parseInt(args[6]);
    int numThreads = Integer.parseInt(args[7]);
    int mem = Integer.parseInt(args[8]);
    String trainDataDir = args[9];
    String workDir = args[10];
   
    launch(nFeature, nFileDim, nClass, nTrees, minObsLeafNodes, maxTreeDepth, numMapTasks, numThreads, mem, trainDataDir, workDir);
    return 0;
  }

  private void launch(int nFeature, int nFileDim, int nClass, int nTrees, int minObsLeafNodes, int maxTreeDepth, 
		      int numMapTasks, int numThreads, int mem, 
		      String trainDataDir, String workDir) throws IOException,
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

    runDFCLSTAVMultiDense(nFeature, nFileDim, nClass, nTrees, minObsLeafNodes, maxTreeDepth, 
		    numMapTasks, numThreads, mem, trainDataDir, dataDir, outDir, configuration);

    long endTime = System.currentTimeMillis();
    System.out
      .println("Total DFCLSTAV Multi Dense Execution Time: "
        + (endTime - startTime));
  }

  private void runDFCLSTAVMultiDense(
    int nFeature, int nFileDim, int nClass, int nTrees, int minObsLeafNodes, int maxTreeDepth, 
    int numMapTasks, int numThreads, int mem, 
    String trainDataDir,
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

    Job DFCLSTAVJob =
      configureDFCLSTAVJob(nFeature, nFileDim, nClass, nTrees, minObsLeafNodes, maxTreeDepth, 
		    numMapTasks, numThreads, mem, trainDataDir, dataDir, outDir, configuration);

    System.out
      .println("Job"
        + " configure in "
        + (System.currentTimeMillis() - perJobSubmitTime)
        + " miliseconds.");

    // ----------------------------------------------------------
    boolean jobSuccess =
      DFCLSTAVJob.waitForCompletion(true);
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
      System.out.println("DFCLSTAV Job fails.");
    }
  }

  private Job configureDFCLSTAVJob(
    int nFeature, int nFileDim, int nClass, int nTrees, int minObsLeafNodes, int maxTreeDepth,
    int numMapTasks, int numThreads, int mem, 
    String trainDataDir,
    Path dataDir,
    Path outDir, Configuration configuration
    )
    throws IOException, URISyntaxException {
    Job job =
      Job
        .getInstance(configuration, "DFCLSTAV_job");
    FileInputFormat.setInputPaths(job, dataDir);
    FileOutputFormat.setOutputPath(job, outDir);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(DFCLSTAVDaalLauncher.class);
    job
      .setMapperClass(DFCLSTAVDaalCollectiveMapper.class);
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


    // set constant
    jobConfig.setInt(Constants.FEATURE_DIM,
      nFeature);
    jobConfig.setInt(Constants.FILE_DIM,
      nFileDim);
    jobConfig.setInt(Constants.NUM_CLASS,
      nClass);
    jobConfig.setInt(Constants.NUM_TREES,
      nTrees);
    jobConfig.setInt(Constants.MIN_OBS_LEAFNODE,
      minObsLeafNodes);
    jobConfig.setInt(Constants.MAX_TREE_DEPTH,
      maxTreeDepth);
    jobConfig.setInt(Constants.NUM_MAPPERS,
      numMapTasks);
    jobConfig.setInt(Constants.NUM_THREADS,
      numThreads);

    return job;
  }
}
