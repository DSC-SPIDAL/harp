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

package edu.iu.daal_naive;

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
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libiomp5.so#libiomp5.so"), conf);

    if (args.length < 8) {
      System.err.println("Usage: edu.iu.daal_naive "
                + "<input train dir> "
                + "<input test dir>"
                + "<input ground truth dir>"
                + "<workDirPath> "
                + "<mem per mapper>"
                + "<num mappers> <thread per worker>"
                + "<class number=20> <vectorSize=20> <pointsPerFile=2000> <testSize=2000>");
      ToolRunner.printGenericCommandUsage(System.err);
      return -1;
    }
    String inputDirPath = args[0];            
    String testDirPath = args[1];            
    String testGroundTruthDirPath = args[2];            
    String workDirPath = args[3];    
    int mem = Integer.parseInt(args[4]);
    int batchSize = Integer.parseInt(args[5]);     //no use, only for compatibility 
    int numMapTasks = Integer.parseInt(args[6]);      
    int numThreadsPerWorker = Integer.parseInt(args[7]);
    int pointsPerFile = 2000;
    int vectorSize = 20;
    int nClasses = 20;
    int testSize = 2000;

    if (args.length == 12) {
        nClasses = Integer.parseInt(args[8]);
        vectorSize = Integer.parseInt(args[9]); 
        pointsPerFile = Integer.parseInt(args[10]); 
        testSize = Integer.parseInt(args[11]); 
    }

    System.out.println("Number of Map Tasks = "+ numMapTasks);
    System.out.println("Classes=" + nClasses + ",vectorSize=" + vectorSize +
            ",pointsPerFile=" + pointsPerFile + ",testSize=" + testSize);

    if (mem < 1000) {
      return -1;
    }

    launch(inputDirPath, testDirPath, testGroundTruthDirPath, workDirPath, mem, 
            nClasses, vectorSize, pointsPerFile, testSize,
            numMapTasks, numThreadsPerWorker);
    return 0;
}

private void launch(String inputDirPath, 
  String testDirPath, 
  String testGroundTruthDirPath, 
  String workDirPath, int mem, int nClasses, int vectorSize, int pointsPerFile,int testSize, 
  int numMapTasks, int numThreadsPerWorker) throws IOException,
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

  runNaive(inputDir, testDirPath, testGroundTruthDirPath, workDirPath, mem, 
          nClasses, vectorSize, pointsPerFile, testSize,
          numMapTasks,
      numThreadsPerWorker, modelDir,
      outputDir, configuration);

    long endTime = System.currentTimeMillis();
    System.out
      .println("Total Naive Execution Time: "
        + (endTime - startTime));
}

private void runNaive(Path inputDir, String testDirPath, String testGroundTruthDirPath, String workDirPath,
        int mem, 
        int nClasses, int vectorSize, int pointsPerFile, int testSize, 
    int numMapTasks, int numThreadsPerWorker,
     Path modelDir, Path outputDir,
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
      configureNaiveJob(inputDir, testDirPath, testGroundTruthDirPath, workDirPath, mem, 
              nClasses, vectorSize, pointsPerFile, testSize,
        numMapTasks, numThreadsPerWorker,
        modelDir, outputDir, configuration);

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

  private Job configureNaiveJob(Path inputDir,
    String testDirPath, String testGroundTruthDirPath, String workDirPath,
    int mem, int nClasses, int vectorSize, int pointsPerFile, int testSize,
    int numMapTasks, int numThreadsPerWorker,
    Path modelDir, Path outputDir,
    Configuration configuration)
    throws IOException, URISyntaxException {

    configuration.set(Constants.TEST_FILE_PATH, testDirPath);
    configuration.set(Constants.TEST_TRUTH_PATH, testGroundTruthDirPath);
    configuration.set(Constants.WORK_DIR, workDirPath);
    configuration.setInt(Constants.NUM_MAPPERS, numMapTasks);
    configuration.setInt(Constants.NUM_THREADS, numThreadsPerWorker);
    configuration.setInt(Constants.NUM_CLASSES, nClasses);
    configuration.setInt(Constants.VECTOR_SIZE, vectorSize);
    configuration.setInt(Constants.POINTS_PERFILE, pointsPerFile);
    configuration.setInt(Constants.TEST_SIZE, testSize);

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

    int xmx = (int) Math.ceil((mem - 2000)*0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);
    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");

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
