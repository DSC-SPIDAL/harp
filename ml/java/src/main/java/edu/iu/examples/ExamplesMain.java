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
package edu.iu.examples;

import edu.iu.benchmark.DataGen;
import edu.iu.fileformat.SingleFileInputFormat;
import org.apache.commons.cli.*;
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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * These are set of examples to demonstrate the collective communications APIs of harp.
 */
public class ExamplesMain extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(ExamplesMain.class.getName());

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(),
        new ExamplesMain(), args);
  }

  @Override
  public int run(String[] args) throws Exception {
    // check the arguments length

    ExampleParameters parameters = parseArguments(args);
    launch(parameters, "worker");

    // eaxmple finished
    System.out.println("Harp Example Completed!");
    return 0;
  }

  public ExampleParameters parseArguments(String[] args) throws ParseException {
    // first load the configurations from command line and config files
    Options options = new Options();
    options.addOption(createOption(Constants.ARGS_MAPPERS, true, "No of mappers", false));
    options.addOption(createOption(Constants.ARGS_ELEMENTS, true, "No of elements", false));
    options.addOption(createOption(Constants.ARGS_ITERATIONS, true, "Iterations", false));
    options.addOption(createOption(Constants.ARGS_OPERATION, true, "Operation", false));
    options.addOption(createOption(Constants.ARGS_PARTITIONS, true, "Partitions", false));
    options.addOption(createOption(Constants.ARGS_DATA_TYPE, true, "Data", false));
    options.addOption(createOption(Constants.ARGS_VERIFY, false, "Verify", false));

    CommandLineParser commandLineParser = new BasicParser();
    CommandLine cmd = commandLineParser.parse(options, args);
    int tasks = Utils.getIntValue(Constants.ARGS_MAPPERS, 2, cmd);
    int size = Utils.getIntValue(Constants.ARGS_ELEMENTS, 1000, cmd);
    int itr = Utils.getIntValue(Constants.ARGS_ITERATIONS, 100, cmd);
    int partitions = Utils.getIntValue(Constants.ARGS_PARTITIONS, 1, cmd);
    String operation = Utils.getStringValue(Constants.ARGS_OPERATION, "allreduce", cmd);
    String dataType = Utils.getStringValue(Constants.ARGS_DATA_TYPE, "int", cmd);
    boolean verify = cmd.hasOption(Constants.ARGS_VERIFY);

    return new ExampleParameters(tasks, size, itr, operation, partitions, dataType, verify);
  }

  private void launch(ExampleParameters parameters, String workDirName)
      throws IOException,
      InterruptedException, ExecutionException {
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
    System.out.println("Generate data.");
    DataGen.generateData(parameters.getMappers(),
        inputDirPath, "/tmp/harp/examples", fs);
    doBenchmark(parameters.getOperation(), parameters.getSize(),
        parameters.getPartitions(), parameters.getMappers(), parameters.getIterations(),
        parameters.isVerify(),
        inputDirPath, outputDirPath);
  }

  private void doBenchmark(String cmd,
                           int bytesPerPartition, int numPartitions,
                           int numMappers, int numIterations, boolean verify,
                           Path inputDirPath, Path outputDirPath) {
    try {
      Job benchamrkJob = configureBenchmarkJob(
          cmd, bytesPerPartition, numPartitions,
          numMappers, numIterations, verify, inputDirPath,
          outputDirPath);
      benchamrkJob.waitForCompletion(true);
    } catch (IOException |
        ClassNotFoundException |
        InterruptedException e) {
      e.printStackTrace();
    }
  }

  private Job configureBenchmarkJob(String cmd,
                                    int bytesPerPartition, int numPartitions,
                                    int numMappers, int numIterations, boolean verify,
                                    Path inputDirPath, Path outputDirPath) throws IOException {
    Job job = Job.getInstance(getConf(), "example_job");
    FileInputFormat.setInputPaths(job,
        inputDirPath);
    FileOutputFormat.setOutputPath(job,
        outputDirPath);
    job.setInputFormatClass(
        SingleFileInputFormat.class);
    job.setJarByClass(ExamplesMain.class);
    if (cmd.equals("allreduce")) {
      job.setMapperClass(AllReduce.class);
    } else if (cmd.equals("allgather")) {
      job.setMapperClass(AllGather.class);
    } else if (cmd.equals("reduce")) {
      job.setMapperClass(Reduce.class);
    } else if (cmd.equals("bcast")) {
      job.setMapperClass(BCast.class);
    } else if (cmd.equals("rotate")) {
      job.setMapperClass(Rotate.class);
    }
    org.apache.hadoop.mapred.JobConf jobConf =
        (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
        "map-collective");
    jobConf.setNumMapTasks(numMappers);
    job.setNumReduceTasks(0);
    jobConf.set(Constants.ARGS_OPERATION, cmd);
    jobConf.setInt(Constants.ARGS_ELEMENTS, bytesPerPartition);
    jobConf.setInt(Constants.ARGS_PARTITIONS, numPartitions);
    jobConf.setInt(Constants.ARGS_MAPPERS, numMappers);
    jobConf.setInt(Constants.ARGS_ITERATIONS, numIterations);
    jobConf.setBoolean(Constants.ARGS_VERIFY, verify);
    return job;
  }

  private static Option createOption(String opt, boolean hasArg,
                                    String description, boolean required) {
    Option symbolListOption = new Option(opt, hasArg, description);
    symbolListOption.setRequired(required);
    return symbolListOption;
  }
}
