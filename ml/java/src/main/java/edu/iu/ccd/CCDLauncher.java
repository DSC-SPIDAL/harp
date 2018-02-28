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

package edu.iu.ccd;

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

public class CCDLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res = ToolRunner.run(new Configuration(),
      new CCDLauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches CCD workers.
   */
  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 9) {
      System.err
        .println("Usage: edu.iu.ccd.CCDLauncher "
          + "<input dir> "
          + "<r> <lambda> <num of iterations> "
          + "<num of map tasks> <num of threads per worker> "
          + "<num of model slices> "
          + "<work dir> <test dir>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    String inputDirPath = args[0];
    int r = Integer.parseInt(args[1]);
    double lambda = Double.parseDouble(args[2]);
    int numIteration = Integer.parseInt(args[3]);
    int numMapTasks = Integer.parseInt(args[4]);
    int numThreadsPerWorker =
      Integer.parseInt(args[5]);
    int numModelSlices =
      Integer.parseInt(args[6]);
    // Adjust r
    int minR = numMapTasks * numModelSlices;
    r =
      (int) Math.ceil((double) r / (double) minR)
        * minR;
    String workDirPath = args[7];
    String testFilePath = args[8];
    System.out.println(
      "Number of Map Tasks = " + numMapTasks);
    if (numIteration <= 0) {
      numIteration = 1;
    }
    launch(inputDirPath, r, lambda, numIteration,
      numMapTasks, numThreadsPerWorker,
      numModelSlices, workDirPath, testFilePath);
    return 0;
  }

  private void launch(String inputDirPath, int r,
    double lambda, int numIterations,
    int numMapTasks, int numThreadsPerWorker,
    int numModelSlices, String workDirPath,
    String testFilePath) throws IOException,
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
    Path modelDir =
      new Path(workDirPath, "model");
    fs.mkdirs(modelDir);
    // Do not make output dir
    Path outputDir =
      new Path(workDirPath, "output");
    long startTime = System.currentTimeMillis();
    runCCD(inputDir, r, lambda, numIterations,
      numMapTasks, numThreadsPerWorker,
      numModelSlices, modelDir, outputDir,
      testFilePath, configuration);
    long endTime = System.currentTimeMillis();
    System.out
      .println("Total SGD Execution Time: "
        + (endTime - startTime));
  }

  private void runCCD(Path inputDir, int r,
    double lambda, int numIterations,
    int numMapTasks, int numThreadsPerWorker,
    int numModelSlices, Path modelDir,
    Path outputDir, String testFilePath,
    Configuration configuration)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    System.out.println("Starting Job");
    int jobID = 0;
    do {
      // --------------------------------------------
      long perJobSubmitTime =
        System.currentTimeMillis();
      System.out
        .println("Start Job#" + jobID + " "
          + new SimpleDateFormat("HH:mm:ss.SSS")
            .format(
              Calendar.getInstance().getTime()));
      Job ccdJob = configureCCDJob(inputDir, r,
        lambda, numIterations, numMapTasks,
        numThreadsPerWorker, numModelSlices,
        modelDir, outputDir, testFilePath,
        configuration, jobID);
      boolean jobSuccess =
        ccdJob.waitForCompletion(true);
      System.out.println("End Jod#" + jobID + " "
        + new SimpleDateFormat("HH:mm:ss.SSS")
          .format(
            Calendar.getInstance().getTime()));
      System.out.println(
        "| Job#" + jobID + " Finished in "
          + (System.currentTimeMillis()
            - perJobSubmitTime)
          + " miliseconds |");
      // ----------------------------------------
      if (!jobSuccess) {
        ccdJob.killJob();
        System.out.println(
          "CCD Job failed. Job ID:" + jobID);
        jobID++;
        if (jobID == 3) {
          break;
        }
      } else {
        break;
      }
    } while (true);
  }

  private Job configureCCDJob(Path inputDir,
    int r, double lambda, int numIterations,
    int numMapTasks, int numThreadsPerWorker,
    int numModelSlices, Path modelDir,
    Path outputDir, String testFilePath,
    Configuration configuration, int jobID)
    throws IOException, URISyntaxException {
    configuration.setInt(Constants.R, r);
    configuration.setDouble(Constants.LAMBDA,
      lambda);
    configuration.setInt(Constants.NUM_ITERATIONS,
      numIterations);
    configuration.setInt(Constants.NUM_THREADS,
      numThreadsPerWorker);
    System.out.println(
      "Model Dir Path: " + modelDir.toString());
    configuration.set(Constants.MODEL_DIR,
      modelDir.toString());
    configuration.setInt(
      Constants.NUM_MODEL_SLICES, numModelSlices);
    configuration.set(Constants.TEST_FILE_PATH,
      testFilePath);
    Job job = Job.getInstance(configuration,
      "ccd_job_" + jobID);
    JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    jobConf.setInt(
      "mapreduce.job.max.split.locations", 10000);
    FileInputFormat.setInputPaths(job, inputDir);
    FileOutputFormat.setOutputPath(job,
      outputDir);
    job.setInputFormatClass(
      MultiFileInputFormat.class);
    job.setJarByClass(CCDLauncher.class);
    job.setMapperClass(
      CCDMPCollectiveMapper.class);
    job.setNumReduceTasks(0);
    return job;
  }
}
