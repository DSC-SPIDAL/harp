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

package edu.iu.lda;

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

public class LDALauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res = ToolRunner.run(new Configuration(),
      new LDALauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches LDA workers.
   */
  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 13) {
      System.err
        .println("Usage: edu.iu.lda.LDALauncher "
          + "<doc dir> "
          + "<num of topics> <alpha> <beta> <num of iterations> "
          + "<min training percentage> "
          + "<max training percentage> "
          + "<num of mappers> <num of threads per worker> <schedule ratio> "
          + "<memory (MB)> "
          + "<work dir> <print model>");
      return -1;
    }
    String docDirPath = args[0];
    int numTopics = Integer.parseInt(args[1]);
    double alpha = Double.parseDouble(args[2]);
    double beta = Double.parseDouble(args[3]);
    int numIteration = Integer.parseInt(args[4]);
    int minBound = Integer.parseInt(args[5]);
    int maxBound = Integer.parseInt(args[6]);
    int numMapTasks = Integer.parseInt(args[7]);
    int numThreadsPerWorker =
      Integer.parseInt(args[8]);
    double scheduleRatio =
      Double.parseDouble(args[9]);
    int mem = Integer.parseInt(args[10]);
    String workDirPath = args[11];
    boolean printModel =
      Boolean.parseBoolean(args[12]);
    System.out.println(
      "Number of Mappers = " + numMapTasks);
    if (numIteration <= 0) {
      numIteration = 1;
    }
    if (mem < 1000) {
      return -1;
    }
    launch(docDirPath, numTopics, alpha, beta,
      numIteration, minBound, maxBound,
      numMapTasks, numThreadsPerWorker,
      scheduleRatio, mem, workDirPath,
      printModel);
    return 0;
  }

  private void launch(String docDirPath,
    int numTopics, double alpha, double beta,
    int numIterations, int minBound, int maxBound,
    int numMapTasks, int numThreadsPerWorker,
    double scheduleRatio, int mem,
    String workDirPath, boolean printModel)
    throws IOException, URISyntaxException,
    InterruptedException, ExecutionException,
    ClassNotFoundException {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    Path docDir = new Path(docDirPath);
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
    runLDA(docDir, numTopics, alpha, beta,
      numIterations, minBound, maxBound,
      numMapTasks, numThreadsPerWorker,
      scheduleRatio, mem, printModel, modelDir,
      outputDir, configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total LDA Execution Time: "
        + (endTime - startTime));
  }

  private void runLDA(Path docDir, int numTopics,
    double alpha, double beta, int numIterations,
    int minBound, int maxBound, int numMapTasks,
    int numThreadsPerWorker, double scheduleRatio,
    int mem, boolean printModel, Path modelDir,
    Path outputDir, Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    System.out.println("Starting Job");
    int jobID = 0;
    // --------------------------------------------
    long perJobSubmitTime =
      System.currentTimeMillis();
    System.out.println("Start Job#" + jobID + " "
      + new SimpleDateFormat("HH:mm:ss.SSS")
        .format(
          Calendar.getInstance().getTime()));
    Job ldaJob =
      configureLDAJob(docDir, numTopics, alpha,
        beta, numIterations, minBound, maxBound,
        numMapTasks, numThreadsPerWorker,
        scheduleRatio, mem, printModel, modelDir,
        outputDir, configuration, jobID);
    boolean jobSuccess =
      ldaJob.waitForCompletion(true);
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
      ldaJob.killJob();
      System.out.println(
        "LDA Job failed. Job ID:" + jobID);
    }
  }

  private Job configureLDAJob(Path docDir,
    int numTopics, double alpha, double beta,
    int numIterations, int minBound, int maxBound,
    int numMapTasks, int numThreadsPerWorker,
    double scheduleRatio, int mem,
    boolean printModel, Path modelDir,
    Path outputDir, Configuration configuration,
    int jobID)
    throws IOException, URISyntaxException {
    configuration.setInt(Constants.NUM_TOPICS,
      numTopics);
    configuration.setDouble(Constants.ALPHA,
      alpha);
    configuration.setDouble(Constants.BETA, beta);
    configuration.setInt(Constants.NUM_ITERATIONS,
      numIterations);
    configuration.setInt(Constants.MIN_BOUND,
      minBound);
    configuration.setInt(Constants.MAX_BOUND,
      maxBound);
    configuration.setInt(Constants.NUM_THREADS,
      numThreadsPerWorker);
    configuration.setDouble(
      Constants.SCHEDULE_RATIO, scheduleRatio);
    System.out.println(
      "Model Dir Path: " + modelDir.toString());
    configuration.set(Constants.MODEL_DIR,
      modelDir.toString());
    configuration.setBoolean(
      Constants.PRINT_MODEL, printModel);
    Job job = Job.getInstance(configuration,
      "lda_job_" + jobID);
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
    FileInputFormat.setInputPaths(job, docDir);
    FileOutputFormat.setOutputPath(job,
      outputDir);
    job.setInputFormatClass(
      MultiFileInputFormat.class);
    job.setJarByClass(LDALauncher.class);
    job.setMapperClass(
      LDAMPCollectiveMapper.class);
    job.setNumReduceTasks(0);
    return job;
  }
}
