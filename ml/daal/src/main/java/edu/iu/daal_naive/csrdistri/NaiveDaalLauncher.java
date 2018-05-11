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

package edu.iu.daal_naive.csrdistri;

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

public class NaiveDaalLauncher extends Configured
implements Tool {

  public static void main(String[] argv)
  throws Exception {
    int res =
    ToolRunner.run(new Configuration(),
      new NaiveDaalLauncher(), argv);
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
      System.err.println("Usage: edu.iu.daal_naive "
                + "<num classes>"
		+ "<num mappers>"
		+ "<thread per worker>"
                + "<mem per mapper>"
                + "<input train dir> "
                + "<input test dir>"
                + "<input ground truth dir>"
                + "<workDirPath> "
		);
      ToolRunner.printGenericCommandUsage(System.err);
      return -1;
    }

    int nClasses = Integer.parseInt(args[0]);
    int numMapTasks = Integer.parseInt(args[1]);      
    int numThreads = Integer.parseInt(args[2]);
    int mem = Integer.parseInt(args[3]);

    String inputDirPath = args[4];            
    String testDirPath = args[5];            
    String testGroundTruthDirPath = args[6];            
    String workDirPath = args[7];    
	
    System.out.println("Mem (MB) per mapper = "+ mem);
    System.out.println("num of classes = "+ nClasses);
    System.out.println("Number of Map Tasks = "+ numMapTasks);
    System.out.println("Threads per mapper = "+ numThreads);

    launch(nClasses, numMapTasks, numThreads, mem, inputDirPath, testDirPath, 
		    testGroundTruthDirPath, workDirPath);
    return 0;
}

private void launch(int nClasses, int numMapTasks, int numThreads, int mem, String inputDirPath, String testDirPath, String testGroundTruthDirPath, String workDirPath) throws IOException,
URISyntaxException, InterruptedException,
ExecutionException, ClassNotFoundException {

  Configuration configuration = getConf();
  FileSystem fs = FileSystem.get(configuration);
  Path inputDir = new Path(inputDirPath);
  Path workDir = new Path(workDirPath);

  if (fs.exists(workDir)) {
    fs.delete(workDir, true);
    fs.mkdirs(workDir);
  }

  Path modelDir = new Path(workDirPath, "model");
  fs.mkdirs(modelDir);
    // Do not make output dir
  Path outputDir = new Path(workDirPath, "output");
  long startTime = System.currentTimeMillis();

  runNaive(nClasses, numMapTasks, numThreads, mem, inputDir, testDirPath, testGroundTruthDirPath,
		  modelDir, outputDir, configuration);

    long endTime = System.currentTimeMillis();
    System.out
      .println("Total Naive Execution Time: "
        + (endTime - startTime));
}

private void runNaive(int nClasses, int numMapTasks, int numThreads, int mem, Path inputDir, String testDirPath, String testGroundTruthDirPath, Path modelDir, Path outputDir,
    Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {

    System.out.println("Starting Job");

    long perJobSubmitTime =
      System.currentTimeMillis();
    System.out.println("Start Job#"  + " "
      + new SimpleDateFormat("HH:mm:ss.SSS")
        .format(
          Calendar.getInstance().getTime()));

    Job naiveJob =
      configureNaiveJob(nClasses, numMapTasks, numThreads, mem, inputDir, 
		      testDirPath, testGroundTruthDirPath, modelDir, outputDir, configuration);

    boolean jobSuccess =
      naiveJob.waitForCompletion(true);

    System.out.println("End Job#"  + " "
      + new SimpleDateFormat("HH:mm:ss.SSS")
        .format(
          Calendar.getInstance().getTime()));

    System.out
      .println(
        "| Job#"  + " Finished in "
          + (System.currentTimeMillis()
            - perJobSubmitTime)
          + " miliseconds |");
    // ----------------------------------------
    if (!jobSuccess) {
      naiveJob.killJob();
      System.out.println(
        "Naive Job failed");
    }
  }

  private Job configureNaiveJob(int nClasses, int numMapTasks, int numThreads, int mem, Path inputDir, String testDirPath, String testGroundTruthDirPath, Path modelDir, Path outputDir,
    Configuration configuration)
    throws IOException, URISyntaxException {

    configuration.set(Constants.TEST_FILE_PATH, testDirPath);
    configuration.set(Constants.TEST_TRUTH_PATH, testGroundTruthDirPath);

    configuration.setInt(Constants.NUM_MAPPERS, numMapTasks);
    configuration.setInt(Constants.NUM_THREADS, numThreads);

    configuration.setInt(Constants.NUM_CLASS, nClasses);

    Job job = Job.getInstance(configuration, "naive_job");
    JobConf jobConf = (JobConf) job.getConfiguration();

    jobConf.set("mapreduce.framework.name",
      "map-collective");

    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);

    // mapreduce.map.collective.memory.mb
    // 125000
    jobConf.setInt(
      "mapreduce.map.collective.memory.mb", mem);

    jobConf.setInt("mapreduce.task.timeout", 60000000);
    // mapreduce.map.collective.java.opts
    // -Xmx120000m -Xms120000m
    // int xmx = (mem - 5000) > (mem * 0.5)
    //   ? (mem - 5000) : (int) Math.ceil(mem * 0.5);
    int xmx = (int) Math.ceil((mem - 5000)*0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);
    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");

	jobConf.setInt("mapred.task.timeout", 1800000);

    jobConf.setNumMapTasks(numMapTasks);

    FileInputFormat.setInputPaths(job, inputDir);                         
    FileOutputFormat.setOutputPath(job, outputDir);

    job.setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(NaiveDaalLauncher.class);
    job.setMapperClass(NaiveDaalCollectiveMapper.class);
    job.setNumReduceTasks(0);
    
    System.out.println("Launcher launched");
    return job;
  }
}
