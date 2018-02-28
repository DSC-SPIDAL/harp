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

package edu.iu.benchmark;

import edu.iu.fileformat.SingleFileInputFormat;
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
import java.util.concurrent.ExecutionException;

public class JobLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res = ToolRunner.run(new Configuration(),
      new JobLauncher(), argv);
    System.exit(res);
  }

  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 5) {
      System.err.println(
        "Usage: edu.iu.benchmark.JobLauncher <command>"
          + "<number of bytes per partition>"
          + "<number of partitions>"
          + "<number of mappers>"
          + "<number of iterations>");
      ToolRunner
        .printGenericCommandUsage(System.err);
      return -1;
    }
    String cmd = args[0];
    int bytesPerPartition =
      Integer.parseInt(args[1]);
    int numPartitions = Integer.parseInt(args[2]);
    int numMappers = Integer.parseInt(args[3]);
    int numIterations = Integer.parseInt(args[4]);
    boolean regenerateData = true;
    String workDirName = "benchmark";
    launch(cmd, bytesPerPartition, numPartitions,
      numMappers, numIterations, regenerateData,
      workDirName);
    return 0;
  }

  private void launch(String cmd,
    int bytesPerPartition, int numPartitions,
    int numMappers, int numIterations,
    boolean generateData, String workDirName)
    throws IOException, URISyntaxException,
    InterruptedException, ExecutionException,
    ClassNotFoundException {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    Path workDirPath = new Path(workDirName);
    Path inputDirPath =
      new Path(workDirPath, "input");
    Path outputDirPath =
      new Path(workDirPath, "output");
    if (fs.exists(outputDirPath)) {
      fs.delete(outputDirPath, true);
    }
    if (generateData) {
      System.out.println("Generate data.");
      DataGen.generateData(numMappers,
        inputDirPath, "/tmp/benchmark/", fs);
    }
    doBenchmark(cmd, bytesPerPartition,
      numPartitions, numMappers, numIterations,
      inputDirPath, outputDirPath);
  }

  private void doBenchmark(String cmd,
    int bytesPerPartition, int numPartitions,
    int numMappers, int numIterations,
    Path inputDirPath, Path outputDirPath) {
    try {
      Job benchamrkJob = configureBenchmarkJob(
        cmd, bytesPerPartition, numPartitions,
        numMappers, numIterations, inputDirPath,
        outputDirPath);
      benchamrkJob.waitForCompletion(true);
    } catch (IOException | URISyntaxException |
      ClassNotFoundException |
      InterruptedException e) {
      e.printStackTrace();
    }
  }

  private Job configureBenchmarkJob(String cmd,
    int bytesPerPartition, int numPartitions,
    int numMappers, int numIterations,
    Path inputDirPath, Path outputDirPath)
    throws IOException, URISyntaxException {
    Job job =
      Job.getInstance(getConf(), "benchmark_job");
    FileInputFormat.setInputPaths(job,
      inputDirPath);
    FileOutputFormat.setOutputPath(job,
      outputDirPath);
    job.setInputFormatClass(
      SingleFileInputFormat.class);
    job.setJarByClass(JobLauncher.class);
    job.setMapperClass(BenchmarkMapper.class);
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMappers);
    job.setNumReduceTasks(0);
    jobConf.set(Constants.BENCHMARK_CMD, cmd);
    jobConf.setInt(Constants.BYTES_PER_PARTITION,
      bytesPerPartition);
    jobConf.setInt(Constants.NUM_PARTITIONS,
      numPartitions);
    jobConf.setInt(Constants.NUM_MAPPERS,
      numMappers);
    jobConf.setInt(Constants.NUM_ITERATIONS,
      numIterations);
    return job;
  }
}
