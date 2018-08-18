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

import edu.iu.fileformat.MultiFileInputFormat;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

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
    options.addOption(createOption(Constants.ARGS_TASKS, true, "No of tasks", false));
    options.addOption(createOption(Constants.ARGS_ELEMENTS, true, "No of elements", false));
    options.addOption(createOption(Constants.ARGS_ITR, true, "Iteration", false));
    options.addOption(createOption(Constants.ARGS_OPERATION, true, "Operation", false));
    options.addOption(createOption(Constants.ARGS_FNAME, true, "File name", false));
    options.addOption(createOption(Constants.ARGS_PRINT_INTERVAL, true, "Threads", false));
    options.addOption(createOption(Constants.ARGS_DATA_TYPE, true, "Data", false));
    options.addOption(createOption(Constants.ARGS_INIT_ITERATIONS, true, "Data", false));

    CommandLineParser commandLineParser = new DefaultParser();
    CommandLine cmd = commandLineParser.parse(options, args);
    int tasks = Utils.getIntValue(Constants.ARGS_TASKS, 2, cmd);
    int size = Utils.getIntValue(Constants.ARGS_ELEMENTS, -1, cmd);
    int itr = Utils.getIntValue(Constants.ARGS_ITR, 10, cmd);
    String operation = Utils.getStringValue(Constants.ARGS_OPERATION, "allreduce", cmd);
    String fileName = Utils.getStringValue(Constants.ARGS_FNAME, null, cmd);
    int printInterval = Utils.getIntValue(Constants.ARGS_PRINT_INTERVAL, -1, cmd);
    int initItr = Utils.getIntValue(Constants.ARGS_INIT_ITERATIONS, -1, cmd);

    return new ExampleParameters(tasks, size, itr, operation, fileName, printInterval, initItr);
  }

  private void launch(ExampleParameters parameters, String workPathString)
      throws IOException, URISyntaxException,
      InterruptedException, ExecutionException,
      ClassNotFoundException {
    Configuration configuration = getConf();
    Path workPath = new Path(workPathString);
//    FileSystem fs = FileSystem.get(configuration);
    Path dataPath = new Path(workPath, "data");
    Path outputPath = new Path(workPath, "out");

    Class clzz = null;
    if (parameters.getOperation().equals("allreduce")) {
      clzz = AllReduce.class;
    }

    long startTime = System.currentTimeMillis();
    runExample(configuration, parameters, workPath, dataPath,
        outputPath, parameters.getOperation(), clzz);
    long endTime = System.currentTimeMillis();

    // print running time
    System.out.println("Total Harp Execution Time: " + (endTime - startTime));
  }

  private void runExample(
      Configuration configuration, ExampleParameters parameters, Path workPath,
      Path dataPath, Path outputPath, String name, Class<Mapper> clazz)
      throws IOException, URISyntaxException,
      InterruptedException, ClassNotFoundException {
    boolean jobSuccess = true;
    int jobRetryCount = 0;
    while (true) {
      Job exampleJob = configureExample(configuration,
          parameters.getTasks(), parameters.getIterations(), workPath,
          dataPath, outputPath, name, clazz);
      jobSuccess = exampleJob.waitForCompletion(true);
      if (!jobSuccess) {
        jobRetryCount++;
        if (jobRetryCount == 3) {
          System.err.println("Example job failed.");
          System.exit(-1);
        }
      } else {
        break;
      }
    }
  }

  private Job configureExample(
      Configuration configuration, int numOfTasks,
      int numOfIteration, Path workPath,
      Path dataPath, Path outputPath, String name, Class<Mapper> clazz)
      throws IOException {
    Job job =
        Job.getInstance(configuration, name);
    Configuration jobConfig =
        job.getConfiguration();
    FileSystem fs = FileSystem.get(configuration);
    if (fs.exists(outputPath)) {
      fs.delete(outputPath, true);
    }
    try {
      Utils.generateData(numOfTasks, fs, dataPath);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    FileInputFormat.setInputPaths(job, dataPath);
    FileOutputFormat.setOutputPath(job,
        outputPath);
    job.setInputFormatClass(
        MultiFileInputFormat.class);
    job.setJarByClass(clazz);
    job.setMapperClass(clazz);
    JobConf jobConf =
        (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
        "map-collective");
    jobConf.setNumMapTasks(numOfTasks);
    jobConf.setInt(
        "mapreduce.job.max.split.locations", 10000);
    job.setNumReduceTasks(0);

    jobConfig.setInt(Constants.ARGS_ITR, numOfIteration);
    return job;
  }

  public static Option createOption(String opt, boolean hasArg,
                                    String description, boolean required) {
    Option symbolListOption = new Option(opt, hasArg, description);
    symbolListOption.setRequired(required);
    return symbolListOption;
  }

}
