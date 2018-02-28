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

package edu.iu.kmeans.rotation;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.kmeans.regroupallgather.Constants;
import edu.iu.kmeans.regroupallgather.KMUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class KMeansCollectiveMapper extends
  CollectiveMapper<String, String, Object, Object> {

  private int pointsPerFile;
  private int numCentroids;
  private int vectorSize;
  private int numCenPars;
  private int cenVecSize;
  private int numMappers;
  private int numThreads;
  private int numIterations;
  private String cenDir;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    Configuration configuration =
      context.getConfiguration();
    pointsPerFile = configuration
      .getInt(Constants.POINTS_PER_FILE, 20);
    numCentroids = configuration
      .getInt(Constants.NUM_CENTROIDS, 20);
    vectorSize = configuration
      .getInt(Constants.VECTOR_SIZE, 20);
    numMappers = configuration
      .getInt(Constants.NUM_MAPPERS, 10);
    numCenPars = numMappers;
    cenVecSize = vectorSize + 1;
    numThreads = configuration
      .getInt(Constants.NUM_THREADS, 10);
    numIterations = configuration
      .getInt(Constants.NUM_ITERATIONS, 10);
    cenDir = configuration.get(Constants.CEN_DIR);
    LOG.info("Points Per File " + pointsPerFile);
    LOG.info("Num Centroids " + numCentroids);
    LOG.info("Vector Size " + vectorSize);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Cen Dir " + cenDir);
    long endTime = System.currentTimeMillis();
    LOG.info(
      "config (ms) :" + (endTime - startTime));
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    List<String> pointFiles =
      new LinkedList<String>();
    while (reader.nextKeyValue()) {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info(
        "Key: " + key + ", Value: " + value);
      pointFiles.add(value);
    }
    Configuration conf =
      context.getConfiguration();
    runKmeans(pointFiles, conf, context);
    LOG.info("Total iterations in master view: "
      + (System.currentTimeMillis() - startTime));
  }

  private void runKmeans(List<String> fileNames,
    Configuration conf, Context context)
    throws IOException {
    // Load data points
    List<double[]> pointArrays =
      KMUtil.loadPoints(fileNames, pointsPerFile,
        cenVecSize, conf, numThreads);
    // Create related data structure for holding
    // the nearest centroid partition+offset and
    // the distance
    BlockingQueue<Points> pointsList =
      new LinkedBlockingQueue<>();
    pointArrays.parallelStream().forEach(e -> {
      Points points = new Points();
      int numPoints = e.length / cenVecSize;
      points.pointArray = e;
      points.cenIDs = new int[numPoints][2];
      pointsList.add(points);
    });
    // Generate centroids
    Table<DoubleArray> cenTable =
      new Table<>(0, new DoubleArrPlus());
    generateCenTable(cenTable, numCentroids,
      numCenPars, cenVecSize);
    // Initialize tasks
    List<ExpTask> expTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      expTasks
        .add(new ExpTask(cenTable, cenVecSize));
    }
    DynamicScheduler<Points, Object, ExpTask> expCompute =
      new DynamicScheduler<>(expTasks);
    List<MaxTask> maxTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      maxTasks.add(new MaxTask(pointsList,
        cenTable, cenVecSize));
    }
    DynamicScheduler<CenPair, Object, MaxTask> maxCompute =
      new DynamicScheduler<>(maxTasks);
    expCompute.start();
    maxCompute.start();
    // ----------------------------------------------------
    // For iterations
    for (int i = 0; i < numIterations; i++) {
      // LOG.info("Iteration: " + i);
      // Expectation
      long t1 = System.currentTimeMillis();
      for (int j = 0; j < this
        .getNumWorkers(); j++) {
        // LOG.info("Expectation Round: " + j);
        for (ExpTask task : expCompute
          .getTasks()) {
          task.update();
        }
        expCompute.submitAll(pointsList);
        while (expCompute.hasOutput()) {
          expCompute.waitForOutput();
        }
        this.rotate("kmeans",
          "exp-rotate-" + i + "-" + j, cenTable,
          null);
      }
      long t2 = System.currentTimeMillis();
      // Clean centroids
      cenTable.getPartitions().parallelStream()
        .forEach(e -> {
          Arrays.fill(e.get().get(), 0.0);
        });
      // Maximization
      for (int j = 0; j < this
        .getNumWorkers(); j++) {
        // LOG.info("Maximization Round: " + j);
        for (Partition<DoubleArray> partition : cenTable
          .getPartitions()) {
          int id = partition.id();
          int size = partition.get().size();
          for (int k = 0; k < size; k +=
            cenVecSize) {
            maxCompute.submit(new CenPair(id, k));
          }
        }
        while (maxCompute.hasOutput()) {
          maxCompute.waitForOutput();
        }
        this.rotate("kmeans",
          "max-rotate-" + i + "-" + j, cenTable,
          null);
      }
      long t3 = System.currentTimeMillis();
      for (Partition<DoubleArray> partition : cenTable
        .getPartitions()) {
        double[] doubles = partition.get().get();
        int size = partition.get().size();
        for (int j = 0; j < size; j +=
          cenVecSize) {
          for (int k = 1; k < cenVecSize; k++) {
            // Calculate avg
            if (doubles[j] != 0) {
              doubles[j + k] /= doubles[j];
            }
          }
        }
      }
      long t4 = System.currentTimeMillis();
      LOG.info("Expectation: " + (t2 - t1)
        + ", Maximization: " + (t3 - t2)
        + ", Averging: " + (t4 - t3));
      logMemUsage();
      logGCTime();
      context.progress();
    }
    expCompute.stop();
    maxCompute.stop();
    // Write out centroids
    LOG.info("Start to write out centroids.");
    long startTime = System.currentTimeMillis();
    KMUtil.storeCentroids(conf, cenDir, cenTable,
      cenVecSize, this.getSelfID() + "");
    long endTime = System.currentTimeMillis();
    LOG.info("Store centroids time (ms): "
      + (endTime - startTime));
    cenTable.release();
  }

  private void generateCenTable(
    Table<DoubleArray> cenTable, int numCentroids,
    int numCenPartitions, int cenVecSize) {
    int selfID = this.getSelfID();
    int numWorkers = this.getNumWorkers();
    Random random =
      new Random(System.currentTimeMillis());
    int cenParSize =
      numCentroids / numCenPartitions;
    int cenRest = numCentroids % numCenPartitions;
    for (int i = 0; i < numCenPartitions; i++) {
      if (cenRest > 0) {
        int size = (cenParSize + 1) * cenVecSize;
        cenRest--;
        if (i % numWorkers == selfID) {
          DoubleArray array =
            DoubleArray.create(size, false);
          double[] doubles = array.get();
          for (int j = 0; j < size; j++) {
            doubles[j] =
              random.nextDouble() * 1000;
          }
          cenTable.addPartition(
            new Partition<>(i, array));
        }
      } else if (cenParSize > 0) {
        int size = cenParSize * cenVecSize;
        if (i % numWorkers == selfID) {
          DoubleArray array =
            DoubleArray.create(size, false);
          double[] doubles = array.get();
          for (int j = 0; j < size; j++) {
            doubles[j] =
              random.nextDouble() * 1000;
          }
          cenTable.addPartition(
            new Partition<>(i, array));
        }
      }
    }
  }
}
