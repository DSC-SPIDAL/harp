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

package edu.iu.kmeans.regroupallgather;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class KMUtil {

  protected static final Log LOG =
    LogFactory.getLog(KMUtil.class);

  /**
   * Generate centroids and upload to the cDir
   * 
   * @param numCentroids
   * @param vectorSize
   * @param configuration
   * @param random
   * @param cenDir
   * @param fs
   * @throws IOException
   */
  static void generateCentroids(int numCentroids,
    int vectorSize, Configuration configuration,
    Path cenDir, FileSystem fs)
    throws IOException {
    Random random = new Random();
    double[] data = null;
    if (fs.exists(cenDir))
      fs.delete(cenDir, true);
    if (!fs.mkdirs(cenDir)) {
      throw new IOException(
        "Mkdirs failed to create "
          + cenDir.toString());
    }
    data = new double[numCentroids * vectorSize];
    for (int i = 0; i < data.length; i++) {
      // data[i] = 1000;
      data[i] = random.nextDouble() * 1000;
    }
    Path initClustersFile = new Path(cenDir,
      Constants.CENTROID_FILE_NAME);
    System.out.println("Generate centroid data."
      + initClustersFile.toString());
    FSDataOutputStream out =
      fs.create(initClustersFile, true);
    BufferedWriter bw = new BufferedWriter(
      new OutputStreamWriter(out));
    for (int i = 0; i < data.length; i++) {
      if ((i % vectorSize) == (vectorSize - 1)) {
        bw.write(data[i] + "");
        bw.newLine();
      } else {
        bw.write(data[i] + " ");
      }
    }
    bw.flush();
    bw.close();
    System.out
      .println("Wrote centroids data to file");
  }

  /**
   * Generate data and upload to the data dir.
   * 
   * @param numOfDataPoints
   * @param vectorSize
   * @param numPointFiles
   * @param localInputDir
   * @param fs
   * @param dataDir
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  static void generatePoints(int numOfDataPoints,
    int vectorSize, int numPointFiles,
    String localInputDir, FileSystem fs,
    Path dataDir) throws IOException,
    InterruptedException, ExecutionException {
    int pointsPerFile =
      numOfDataPoints / numPointFiles;
    System.out.println("Writing " + pointsPerFile
      + " vectors to a file");
    // Check data directory
    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }
    // Check local directory
    File localDir = new File(localInputDir);
    // If existed, regenerate data
    if (localDir.exists()
      && localDir.isDirectory()) {
      for (File file : localDir.listFiles()) {
        file.delete();
      }
      localDir.delete();
    }
    boolean success = localDir.mkdir();
    if (success) {
      System.out.println("Directory: "
        + localInputDir + " created");
    }
    if (pointsPerFile == 0) {
      throw new IOException("No point to write.");
    }
    // Create random data points
    int poolSize =
      Runtime.getRuntime().availableProcessors();
    ExecutorService service =
      Executors.newFixedThreadPool(poolSize);
    List<Future<?>> futures =
      new LinkedList<Future<?>>();
    for (int k = 0; k < numPointFiles; k++) {
      Future<?> f =
        service.submit(new DataGenRunnable(
          pointsPerFile, localInputDir,
          Integer.toString(k), vectorSize));
      futures.add(f); // add a new thread
    }
    for (Future<?> f : futures) {
      f.get();
    }
    // Shut down the executor service so that this
    // thread can exit
    service.shutdownNow();
    // Wrap to path object
    Path localInput = new Path(localInputDir);
    fs.copyFromLocalFile(localInput, dataDir);
  }

  public static void generateData(
    int numDataPoints, int numCentroids,
    int vectorSize, int numPointFiles,
    Configuration configuration, FileSystem fs,
    Path dataDir, Path cenDir, String localDir)
    throws IOException, InterruptedException,
    ExecutionException {
    System.out.println("Generating data..... ");
    generatePoints(numDataPoints, vectorSize,
      numPointFiles, localDir, fs, dataDir);
  }

  public static List<double[]> loadPoints(
    List<String> fileNames, int pointsPerFile,
    int cenVecSize, Configuration conf,
    int numThreads) {
    long startTime = System.currentTimeMillis();
    List<PointLoadTask> tasks =
      new LinkedList<>();
    List<double[]> arrays = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new PointLoadTask(pointsPerFile,
        cenVecSize, conf));
    }
    DynamicScheduler<String, double[], PointLoadTask> compute =
      new DynamicScheduler<>(tasks);
    for (String fileName : fileNames) {
      compute.submit(fileName);
    }
    compute.start();
    compute.stop();
    while (compute.hasOutput()) {
      double[] output = compute.waitForOutput();
      if (output != null) {
        arrays.add(output);
      }
    }
    long endTime = System.currentTimeMillis();
    LOG.info(
      "File read (ms): " + (endTime - startTime)
        + ", number of point arrays: "
        + arrays.size());
    return arrays;
  }

  public static void storeCentroids(
    Configuration configuration, String cenDir,
    Table<DoubleArray> cenTable, int cenVecSize,
    String name) throws IOException {
    String cFile = cenDir + File.separator + "out"
      + File.separator + name;
    Path cPath = new Path(cFile);
    LOG.info(
      "centroids path: " + cPath.toString());
    FileSystem fs = FileSystem.get(configuration);
    fs.delete(cPath, true);
    FSDataOutputStream out = fs.create(cPath);
    BufferedWriter bw = new BufferedWriter(
      new OutputStreamWriter(out));
    int linePos = 0;
    int[] idArray = cenTable.getPartitionIDs()
      .toArray(new int[0]);
    IntArrays.quickSort(idArray);
    for (int i = 0; i < idArray.length; i++) {
      Partition<DoubleArray> partition =
        cenTable.getPartition(idArray[i]);
      for (int j = 0; j < partition.get()
        .size(); j++) {
        linePos = j % cenVecSize;
        if (linePos == (cenVecSize - 1)) {
          bw.write(
            partition.get().get()[j] + "\n");
        } else if (linePos > 0) {
          // Every row with vectorSize + 1 length,
          // the first one is a count,
          // ignore it in output
          bw.write(
            partition.get().get()[j] + " ");
        }
      }
    }
    bw.flush();
    bw.close();
  }
}
