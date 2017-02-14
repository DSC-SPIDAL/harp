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

package edu.iu.lda;

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

import edu.iu.fileformat.MultiFileInputFormat;

public class LDALauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new LDALauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches LDA workers.
   */
  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 12) {
      System.err
        .println("Usage: edu.iu.lda.LDALauncher "
          + "<doc dir> "
          + "<num of topics> <alpha> <beta> <num of iterations> "
          + "<num of map tasks> <num of threads per worker> "
          + "<timer> <num of model slices> "
          + "<enable timer tuning>"
          + "<work dir> <print model>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    String docDirPath = args[0];
    int numTopics = Integer.parseInt(args[1]);
    double alpha = Double.parseDouble(args[2]);
    double beta = Double.parseDouble(args[3]);
    int numIteration = Integer.parseInt(args[4]);
    int numMapTasks = Integer.parseInt(args[5]);
    int numThreadsPerWorker =
      Integer.parseInt(args[6]);
    long time = Long.parseLong(args[7]);
    int numModelSlices =
      Integer.parseInt(args[8]);
    boolean enableTuning =
      Boolean.parseBoolean(args[9]);
    String workDirPath = args[10];
    boolean printModel =
      Boolean.parseBoolean(args[11]);
    System.out.println("Number of Map Tasks = "
      + numMapTasks);
    if (numIteration <= 0) {
      numIteration = 1;
    }
    launch(docDirPath, numTopics, alpha, beta,
      numIteration, numMapTasks,
      numThreadsPerWorker, time, numModelSlices,
      enableTuning, workDirPath, printModel);
    return 0;
  }

  private void launch(String docDirPath,
    int numTopics, double alpha, double beta,
    int numIterations, int numMapTasks,
    int numThreadsPerWorker, long time,
    int numModelSlices, boolean enableTuning,
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
      numIterations, numMapTasks,
      numThreadsPerWorker, time, numModelSlices,
      enableTuning, modelDir, outputDir,
      printModel, configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total SGD Execution Time: "
        + (endTime - startTime));
  }

  private void runLDA(Path docDir, int numTopics,
    double alpha, double beta, int numIterations,
    int numMapTasks, int numThreadsPerWorker,
    long miniBatch, int numModelSlices,
    boolean enableTuning, Path modelDir,
    Path outputDir, boolean printModel,
    Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    System.out.println("Starting Job");
    int jobID = 0;
    do {
      // --------------------------------------------
      long perJobSubmitTime =
        System.currentTimeMillis();
      System.out.println("Start Job#"
        + jobID
        + " "
        + new SimpleDateFormat("HH:mm:ss.SSS")
          .format(Calendar.getInstance()
            .getTime()));
      Job ldaJob =
        configureLDAJob(docDir, numTopics, alpha,
          beta, numIterations, numMapTasks,
          numThreadsPerWorker, miniBatch,
          numModelSlices, enableTuning, modelDir,
          outputDir, printModel, configuration,
          jobID);
      boolean jobSuccess =
        ldaJob.waitForCompletion(true);
      System.out.println("End Jod#"
        + jobID
        + " "
        + new SimpleDateFormat("HH:mm:ss.SSS")
          .format(Calendar.getInstance()
            .getTime()));
      System.out
        .println("| Job#"
          + jobID
          + " Finished in "
          + (System.currentTimeMillis() - perJobSubmitTime)
          + " miliseconds |");
      // ----------------------------------------
      if (!jobSuccess) {
        ldaJob.killJob();
        System.out
          .println("LDA Job failed. Job ID:"
            + jobID);
        jobID++;
        if (jobID == 1) {
          break;
        }
      } else {
        break;
      }
    } while (true);
  }

  private Job configureLDAJob(Path docDir,
    int numTopics, double alpha, double beta,
    int numIterations, int numMapTasks,
    int numThreadsPerWorker, long time,
    int numModelSlices, boolean enableTuning,
    Path modelDir, Path outputDir,
    boolean printModel,
    Configuration configuration, int jobID)
    throws IOException, URISyntaxException {
    configuration.setInt(Constants.NUM_TOPICS,
      numTopics);
    configuration.setDouble(Constants.ALPHA,
      alpha);
    configuration.setDouble(Constants.BETA, beta);
    configuration.setInt(
      Constants.NUM_ITERATIONS, numIterations);
    configuration.setInt(Constants.NUM_THREADS,
      numThreadsPerWorker);
    configuration.setLong(Constants.TIME, time);
    configuration.setInt(
      Constants.NUM_MODEL_SLICES, numModelSlices);
    configuration.setBoolean(
      Constants.ENABLE_TUNING, enableTuning);
    System.out.println("Model Dir Path: "
      + modelDir.toString());
    configuration.set(Constants.MODEL_DIR,
      modelDir.toString());
    configuration.setBoolean(
      Constants.PRINT_MODEL, printModel);
    Job job =
      Job.getInstance(configuration, "lda_job_"
        + jobID);
    JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);
    FileInputFormat.setInputPaths(job, docDir);
    FileOutputFormat
      .setOutputPath(job, outputDir);
    job
      .setInputFormatClass(MultiFileInputFormat.class);
    job.setJarByClass(LDALauncher.class);
    job
      .setMapperClass(LDAMPCollectiveMapper.class);
    job.setNumReduceTasks(0);
    return job;
  }
}
