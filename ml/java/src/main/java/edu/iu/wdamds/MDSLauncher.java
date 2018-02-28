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

package edu.iu.wdamds;

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

public class MDSLauncher extends Configured
  implements Tool {

  public static void main(String[] argv)
    throws Exception {
    int res = ToolRunner.run(new Configuration(),
      new MDSLauncher(), argv);
    System.exit(res);
  }

  /**
   * Launches all the tasks in order.
   */
  @Override
  public int run(String[] args) throws Exception {
    // Command line example
    // input files are in a shared directory
    // hadoop jar
    // harp-app-hadoop-2.6.0.jar
    // edu.iu.wdamds.MDSLauncher 8
    // /mds_data_split/data/
    // distance_ weight_ v_
    // /mds_data_split/ids/distance_ids
    // NoLabel 0.01 3 0.01 4640 10 8
    if (args.length != 13) {
      System.out.println("Usage: ");
      System.out.println("[1. Num map tasks ]");
      System.out.println("[2. Input Folder]");
      System.out
        .println("[3. Input File Prefix]");
      System.out
        .println("[4. Input Weight Prefix]");
      System.out.println("[5. Input V Prefix]");
      System.out.println("[6. IDs File]");
      System.out.println("[7. Label Data File]");
      System.out.println("[8. Threshold value]");
      System.out
        .println("[9. The Target Dimension]");
      System.out.println(
        "[10. Cooling parameter (alpha)]");
      System.out.println("[11. Input Data Size]");
      System.out
        .println("[12. CG iteration num]");
      System.out
        .println("[13. Num threads per mapper]");
      System.exit(0);
    }
    // Args
    int numMapTasks = Integer.valueOf(args[0]);
    String inputFolder = args[1];
    String inputPrefix = args[2];
    String weightPrefix = args[3];
    String vPrefix = args[4];
    String idsFile = args[5];
    String labelsFile = args[6];
    double threshold = Double.valueOf(args[7]);
    int d = Integer.valueOf(args[8]);
    double alpha = Double.valueOf(args[9]);
    int n = Integer.parseInt(args[10]);
    int cgIter = Integer.parseInt(args[11]);
    int numThreads = Integer.parseInt(args[12]);

    System.out.println(
      "[1. Num map tasks ]:\t" + numMapTasks);
    System.out.println(
      "[2. Input Folder]:\t" + inputFolder);
    System.out.println(
      "[3. Input File Prefix]:\t" + inputPrefix);
    System.out
      .println("[4. Weighted File Prefix]:\t"
        + weightPrefix);
    System.out
      .println("[5. V File Prefix]:\t" + vPrefix);
    System.out
      .println("[6. IDs File ]:\t" + idsFile);
    System.out.println(
      "[7. Label Data File ]:\t" + labelsFile);
    System.out.println(
      "[8. Threshold value ]:\t" + threshold);
    System.out.println(
      "[9. The Target Dimension ]:\t" + d);
    System.out.println(
      "[10. Cooling parameter (alpha) ]:\t"
        + alpha);
    System.out
      .println("[11. Input Data Size]:\t" + n);
    System.out
      .println("[12. CG Iterations]:\t" + cgIter);
    System.out
      .println("[13. Num threads per mapper]:\t"
        + numThreads);
    boolean jobSuccess = launch(numMapTasks,
      inputFolder, inputPrefix, weightPrefix,
      vPrefix, idsFile, labelsFile, threshold, d,
      alpha, n, cgIter, numThreads);
    return 0;
  }

  boolean launch(int numMapTasks,
    String inputFolder, String inputPrefix,
    String weightPrefix, String vPrefix,
    String idsFile, String labelsFile,
    double threshold, int d, double alpha, int n,
    int cgIter, int numThreads)
    throws IOException, URISyntaxException,
    InterruptedException, ExecutionException,
    ClassNotFoundException {
    Configuration configuration = getConf();
    FileSystem fs = FileSystem.get(configuration);
    // Generate working directory, for partition
    // files
    String workDirName =
      "wdamds" + System.currentTimeMillis();
    Path workDirPath = new Path(workDirName);
    // if (fs.exists(workDirPath)) {
    // fs.delete(workDirPath, true);
    // }
    fs.mkdirs(workDirPath);
    Path dataDirPath =
      new Path(workDirPath, "data");
    fs.mkdirs(dataDirPath);
    Path xDirPath = new Path(workDirPath, "x");
    fs.mkdirs(xDirPath);
    Path xFilePath = new Path(xDirPath, "x");
    Path xOutFilePath =
      new Path(xDirPath, "x_out");
    Path outDirPath =
      new Path(workDirPath, "out");
    // Generate initial mapping
    // Ignore initial mapping generating on HDFS
    // Generate in memory
    // DataGen.generateXData(n, d, xFilePath, fs);
    // Generate partition files
    // Each file contains a data partition and a
    // weight partition (or multiple)
    DataGen.generatePartitionFiles(inputFolder,
      inputPrefix, weightPrefix, vPrefix,
      dataDirPath, fs, numMapTasks);
    return runWDAMDS(numMapTasks, dataDirPath,
      xFilePath, xOutFilePath, outDirPath,
      idsFile, labelsFile, threshold, d, alpha, n,
      cgIter, numThreads);
  }

  private boolean runWDAMDS(int numMapTasks,
    Path dataDirPath, Path xFilePath,
    Path xOutFilePath, Path outDirPath,
    String idsFile, String labelsFile,
    double threshold, int d, double alpha, int n,
    int cgIter, int numThreads)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    long jobStartTime =
      System.currentTimeMillis();
    Job mdsJob = prepareWDAMDSJob(numMapTasks,
      dataDirPath, xFilePath, xOutFilePath,
      outDirPath, idsFile, labelsFile, threshold,
      d, alpha, n, cgIter, numThreads);
    boolean jobSuccess =
      mdsJob.waitForCompletion(true);
    if (jobSuccess) {
      System.out.println("| Job finished in "
        + (System.currentTimeMillis()
          - jobStartTime) / 1000.0
        + " seconds |");
      return true;
    } else {
      System.out.println("WDAMDS Job failed.");
      return false;
    }
  }

  private Job prepareWDAMDSJob(int numMapTasks,
    Path dataDirPath, Path xFilePath,
    Path xOutFilePath, Path outDirPath,
    String idsFile, String labelsFile,
    double threshold, int d, double alpha, int n,
    int cgIter, int numThreads)
    throws IOException, URISyntaxException,
    InterruptedException, ClassNotFoundException {
    Job job = Job.getInstance(getConf(),
      "map-collective-wdamds");
    Configuration jobConfig =
      job.getConfiguration();
    FileInputFormat.setInputPaths(job,
      dataDirPath);
    FileOutputFormat.setOutputPath(job,
      outDirPath);
    jobConfig.setInt(MDSConstants.NUM_MAPS,
      numMapTasks);
    // Load from HDFS
    // Now we ignore and don't read x file from
    // HDFS
    jobConfig.set(MDSConstants.X_FILE_PATH,
      xFilePath.toString());
    // Output to HDFS
    jobConfig.set(MDSConstants.X_OUT_FILE_PATH,
      xOutFilePath.toString());
    // Load from shared file system
    jobConfig.set(MDSConstants.IDS_FILE, idsFile);
    // Load from shared file system
    jobConfig.set(MDSConstants.LABELS_FILE,
      labelsFile);
    jobConfig.setDouble(MDSConstants.THRESHOLD,
      threshold);
    jobConfig.setInt(MDSConstants.D, d);
    jobConfig.setDouble(MDSConstants.ALPHA,
      alpha);
    jobConfig.setInt(MDSConstants.N, n);
    jobConfig.setInt(MDSConstants.CG_ITER,
      cgIter);
    jobConfig.setInt(MDSConstants.NUM_THREADS,
      numThreads);
    // input class to file-based class
    job.setInputFormatClass(
      SingleFileInputFormat.class);
    job.setJarByClass(MDSLauncher.class);
    job.setMapperClass(WDAMDSMapper.class);
    // When use MultiFileInputFormat, remember to
    // set the number of map tasks
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    jobConf.set("mapreduce.framework.name",
      "map-collective");
    jobConf.setNumMapTasks(numMapTasks);
    job.setNumReduceTasks(0);
    return job;
  }
}
