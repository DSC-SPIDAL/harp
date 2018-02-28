/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.sgd;

import edu.iu.fileformat.MultiFileInputFormat;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class SGDLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res = ToolRunner.run(new Configuration(),
      new SGDLauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches LDA workers.
   */
  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 12) {
      System.err
        .println("Usage: edu.iu.sgd.SGDLauncher "
          + "<input dir> "
          + "<r> <lambda> <epsilon> <num of iterations> "
          + "<training ratio> "
          + "<num of map tasks> <num of threads per worker> <schedule ratio>"
          + "<memory (MB)>  <work dir> <test dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    String inputDirPath = args[0];
    int r = Integer.parseInt(args[1]);
    double lambda = Double.parseDouble(args[2]);
    double epsilon = Double.parseDouble(args[3]);
    int numIteration = Integer.parseInt(args[4]);
    int trainRatio = Integer.parseInt(args[5]);
    int numMapTasks = Integer.parseInt(args[6]);
    int numThreadsPerWorker =
      Integer.parseInt(args[7]);
    double scheduleRatio =
      Double.parseDouble(args[8]);
    int mem = Integer.parseInt(args[9]);
    String workDirPath = args[10];
    String testFilePath = args[11];
    System.out.println(
      "Number of Map Tasks = " + numMapTasks);
    if (numIteration <= 0) {
      numIteration = 1;
    }
    if (mem < 1000) {
      return -1;
    }
    launch(inputDirPath, r, lambda, epsilon,
      numIteration, trainRatio, numMapTasks,
      numThreadsPerWorker, scheduleRatio, mem,
      workDirPath, testFilePath);
    return 0;
  }

  private void launch(String inputDirPath, int r,
    double lambda, double epsilon,
    int numIterations, int trainRatio,
    int numMapTasks, int numThreadsPerWorker,
    double scheduleRatio, int mem,
    String workDirPath, String testFilePath)
    throws IOException, URISyntaxException,
    InterruptedException, ExecutionException,
    ClassNotFoundException {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    Path inputDir = new Path(inputDirPath);
    Path workDir = new Path(workDirPath);
    if (fs.exists(workDir)) {
      fs.delete(workDir, true);
      fs.mkdirs(workDir);
    }
    Path modelDir =
      new Path(workDirPath, "model");
    fs.mkdirs(modelDir);
    // Do not make output dir
    Path outputDir =
      new Path(workDirPath, "output");
    long startTime = System.currentTimeMillis();
    runSGD(inputDir, r, lambda, epsilon,
      numIterations, trainRatio, numMapTasks,
      numThreadsPerWorker, scheduleRatio, mem,
      modelDir, outputDir, testFilePath,
      configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total SGD Execution Time: "
        + (endTime - startTime));
  }

  private void runSGD(Path inputDir, int r,
    double lambda, double epsilon,
    int numIterations, int trainRatio,
    int numMapTasks, int numThreadsPerWorker,
    double scheduleRatio, int mem, Path modelDir,
    Path outputDir, String testFilePath,
    Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    System.out.println("Starting Job");
    int jobID = 0;
    long perJobSubmitTime =
      System.currentTimeMillis();
    System.out.println("Start Job#" + jobID + " "
      + new SimpleDateFormat("HH:mm:ss.SSS")
        .format(
          Calendar.getInstance().getTime()));
    Job sgdJob = configureSGDJob(inputDir, r,
      lambda, epsilon, numIterations, trainRatio,
      numMapTasks, numThreadsPerWorker,
      scheduleRatio, mem, modelDir, outputDir,
      testFilePath, configuration, jobID);
    boolean jobSuccess =
      sgdJob.waitForCompletion(true);
    System.out.println("End Jod#" + jobID + " "
      + new SimpleDateFormat("HH:mm:ss.SSS")
        .format(
          Calendar.getInstance().getTime()));
    System.out
      .println(
        "| Job#" + jobID + " Finished in "
          + (System.currentTimeMillis()
            - perJobSubmitTime)
          + " miliseconds |");
    // ----------------------------------------
    if (!jobSuccess) {
      sgdJob.killJob();
      System.out.println(
        "SGD Job failed. Job ID:" + jobID);
    }
  }

  private Job configureSGDJob(Path inputDir,
    int r, double lambda, double epsilon,
    int numIterations, int trainRatio,
    int numMapTasks, int numThreadsPerWorker,
    double scheduleRatio, int mem, Path modelDir,
    Path outputDir, String testFilePath,
    Configuration configuration, int jobID)
    throws IOException, URISyntaxException {
    configuration.setInt(Constants.R, r);
    configuration.setDouble(Constants.LAMBDA,
      lambda);
    configuration.setDouble(Constants.EPSILON,
      epsilon);
    configuration.setInt(Constants.NUM_ITERATIONS,
      numIterations);
    configuration.setInt(Constants.TRAIN_RATIO,
      trainRatio);
    configuration.setInt(Constants.NUM_THREADS,
      numThreadsPerWorker);
    configuration.setDouble(
      Constants.SCHEDULE_RATIO, scheduleRatio);
    configuration.set(Constants.MODEL_DIR,
      modelDir.toString());
    configuration.set(Constants.TEST_FILE_PATH,
      testFilePath);
    Job job = Job.getInstance(configuration,
      "sgd_job_" + jobID);
    JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    // mapreduce.map.collective.memory.mb
    // 125000
    jobConf.setInt(
      "mapreduce.map.collective.memory.mb", mem);
    // mapreduce.map.collective.java.opts
    // -Xmx120000m -Xms120000m
    int xmx = (mem - 5000) > (mem * 0.9)
      ? (mem - 5000) : (int) Math.ceil(mem * 0.9);
    int xmn = (int) Math.ceil(0.25 * xmx);
    jobConf.set(
      "mapreduce.map.collective.java.opts",
      "-Xmx" + xmx + "m -Xms" + xmx + "m"
        + " -Xmn" + xmn + "m");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);
    FileInputFormat.setInputPaths(job, inputDir);
    FileOutputFormat.setOutputPath(job,
      outputDir);
    job.setInputFormatClass(
      MultiFileInputFormat.class);
    job.setJarByClass(SGDLauncher.class);
    job.setMapperClass(SGDCollectiveMapper.class);
    job.setNumReduceTasks(0);
    return job;
  }
}
