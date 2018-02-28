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

import edu.iu.dymoro.RotationUtil;
import edu.iu.dymoro.Rotator;
import edu.iu.dymoro.Scheduler;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.LongArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SGDCollectiveMapper extends
  CollectiveMapper<String, String, Object, Object> {
  private int r;
  private double oneOverSqrtR;
  private double lambda;
  private double epsilon;
  private int numIterations;
  private long time;
  private int trainRatio;
  private int numThreads;
  private double scheduleRatio;
  private boolean enableTuning;
  private String modelDirPath;
  private int numModelSlices;
  private int rmseIteInterval;
  private int freeInterval;
  private double rmse;
  private double testRMSE;
  private String testFilePath;

  private long computeTime;
  private long waitTime;
  private Random random;

  private long totalNumV;
  private long totalNumCols;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context) {
    LOG.info("start setup: "
      + new SimpleDateFormat("yyyyMMdd_HHmmss")
        .format(
          Calendar.getInstance().getTime()));
    long startTime = System.currentTimeMillis();
    Configuration configuration =
      context.getConfiguration();
    r = configuration.getInt(Constants.R, 100);
    lambda = configuration
      .getDouble(Constants.LAMBDA, 0.001);
    epsilon = configuration
      .getDouble(Constants.EPSILON, 0.001);
    numIterations = configuration
      .getInt(Constants.NUM_ITERATIONS, 100);
    trainRatio =
      configuration.getInt(Constants.TRAIN_RATIO,
        Constants.TARGET_BOUND);
    if (trainRatio <= 0 || trainRatio > 100) {
      trainRatio = Constants.TARGET_BOUND;
    }
    if (trainRatio == 100) {
      enableTuning = false;
    } else {
      enableTuning = true;
    }
    time = enableTuning ? 1000L : 1000000000L;
    numThreads = configuration
      .getInt(Constants.NUM_THREADS, 16);
    scheduleRatio = configuration
      .getDouble(Constants.SCHEDULE_RATIO, 2.0);
    modelDirPath =
      configuration.get(Constants.MODEL_DIR, "");
    testFilePath = configuration
      .get(Constants.TEST_FILE_PATH, "");
    numModelSlices = 2;
    rmseIteInterval = 5;
    freeInterval = 20;
    rmse = 0.0;
    testRMSE = 0.0;
    computeTime = 0L;
    waitTime = 0L;

    totalNumV = 0L;
    totalNumCols = 0L;
    oneOverSqrtR = 1.0 / Math.sqrt(r);
    random =
      new Random(System.currentTimeMillis());
    long endTime = System.currentTimeMillis();
    LOG.info(
      "config (ms): " + (endTime - startTime));
    LOG.info("R " + r);
    LOG.info("Lambda " + lambda);
    LOG.info("Epsilon " + epsilon);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Num Threads " + numThreads + " "
      + scheduleRatio);
    LOG.info(
      "enableTuning\\Time\\Bound " + enableTuning
        + "\\" + time + "\\" + trainRatio);
    LOG.info("Model Slices " + numModelSlices);
    LOG.info("Model Dir Path " + modelDirPath);
    LOG.info("TEST FILE PATH " + testFilePath);
    LOG.info("Container Memory " + configuration
      .get("mapreduce.map.collective.memory.mb"));
    LOG.info("Java Memory " + configuration
      .get("mapreduce.map.collective.java.opts"));
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    LinkedList<String> vFiles = getVFiles(reader);
    try {
      runSGD(vFiles, context.getConfiguration(),
        context);
    } catch (Exception e) {
      LOG.error("Fail to run SGD.", e);
    }
    LOG.info("Total execution time: "
      + (System.currentTimeMillis() - startTime));
  }

  private LinkedList<String>
    getVFiles(final KeyValReader reader)
      throws IOException, InterruptedException {
    final LinkedList<String> vFiles =
      new LinkedList<>();
    while (reader.nextKeyValue()) {
      final String value =
        reader.getCurrentValue();
      LOG.info("File: " + value);
      vFiles.add(value);
    }
    return vFiles;
  }

  private void runSGD(
    final LinkedList<String> vFilePaths,
    final Configuration configuration,
    final Context context) throws Exception {
    Int2ObjectOpenHashMap<VRowCol> vRowMap =
      SGDUtil
        .loadVWMap(vFilePaths,
          Runtime.getRuntime()
            .availableProcessors(),
          configuration);
    Int2ObjectOpenHashMap<VRowCol> testVColMap =
      SGDUtil.loadTestVHMap(testFilePath,
        configuration, Runtime.getRuntime()
          .availableProcessors());
    // ---------------------------------------------
    // Create vHMap and W model
    int numSplits = (int) Math
      .ceil(Math.sqrt((double) numThreads
        * (double) numThreads * scheduleRatio));
    final int numRowSplits = numSplits;
    final int numColSplits = numRowSplits;
    LOG.info("numRowSplits: " + numRowSplits + " "
      + " numColSplits: " + numColSplits);
    this.logMemUsage();
    this.logGCTime();
    double[][][] wMapRef = new double[1][][];
    // vHMap grouped to splits
    // based on their row IDs
    final Int2ObjectOpenHashMap<VRowCol>[] vWHMap =
      new Int2ObjectOpenHashMap[numRowSplits];
    final long workerNumV = createVWHMapAndWModel(
      vWHMap, wMapRef, vRowMap, r, oneOverSqrtR,
      numThreads, random);
    vRowMap = null;
    double[][] wMap = wMapRef[0];
    wMapRef = null;
    // Create H model
    Table<IntArray> vHSumTable =
      new Table<>(0, new IntArrPlus());
    Table<DoubleArray>[] hTableMap =
      new Table[numModelSlices];
    createHModel(hTableMap, vHSumTable,
      numModelSlices, vWHMap, oneOverSqrtR,
      random);
    // Trim Test VHMap
    trimTestVHMap(testVColMap, wMap, vHSumTable);
    int totalNumTestV =
      getTotalNumTestV(testVColMap);
    LOG.info(
      "Total num of test V: " + totalNumTestV);
    vHSumTable.release();
    vHSumTable = null;
    this.freeMemory();
    this.freeConn();
    System.gc();
    // ----------------------------------------------
    // Create rotator
    final int numWorkers = this.getNumWorkers();
    int[] order = RotationUtil
      .getRotationSequences(random, numWorkers,
        (numIterations + 1) * 2, this);
    boolean randomModelSplit = true;
    Rotator<DoubleArray> rotator =
      new Rotator<>(hTableMap, numColSplits,
        randomModelSplit, this, order, "sgd");
    rotator.start();
    // Initialize scheduler
    List<SGDMPTask> sgdTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      sgdTasks.add(
        new SGDMPTask(r, lambda, epsilon, wMap));
    }
    Scheduler<Int2ObjectOpenHashMap<VRowCol>, DoubleArray, SGDMPTask> scheduler =
      new Scheduler<>(numRowSplits, numColSplits,
        vWHMap, time, sgdTasks);
    printRMSE(rotator, numWorkers, vWHMap,
      totalNumV, testVColMap, totalNumTestV, wMap,
      0);
    // -----------------------------------------
    // For iteration
    for (int i = 1; i <= numIterations; i++) {
      long iteStart = System.currentTimeMillis();
      for (int j = 0; j < numWorkers; j++) {
        for (int k = 0; k < numModelSlices; k++) {
          long t1 = System.currentTimeMillis();
          List<Partition<DoubleArray>>[] hMap =
            rotator.getSplitMap(k);
          long t2 = System.currentTimeMillis();
          scheduler.schedule(hMap, false);
          long t3 = System.currentTimeMillis();
          waitTime += (t2 - t1);
          computeTime += (t3 - t2);
          rotator.rotate(k);
        }
      }
      long numVTrained =
        scheduler.getNumVItemsTrained();
      if (i == 1 && enableTuning) {
        long newMiniBatch = adjustMiniBatch(
          this.getSelfID(), computeTime,
          numVTrained, i, time, numModelSlices,
          numWorkers, trainRatio);
        if (time != newMiniBatch) {
          time = newMiniBatch;
          scheduler.setTimer(time);
        }
      }
      long iteEnd = System.currentTimeMillis();
      int percentage =
        (int) Math.ceil((double) numVTrained
          / (double) workerNumV * 100.0);
      LOG.info("Iteration " + i + ": "
        + (iteEnd - iteStart) + ", num V: "
        + numVTrained + ", compute time: "
        + computeTime + ", misc: " + waitTime
        + ", percentage(%): " + percentage);
      computeTime = 0L;
      waitTime = 0L;
      // Calculate RMSE
      if (i == 1 || i % rmseIteInterval == 0 ||
              i == numIterations) {
        // this.logMemUsage();
        // this.logGCTime();
        double retTestRMSE = 
        printRMSE(rotator, numWorkers, vWHMap,
          totalNumV, testVColMap, totalNumTestV,
          wMap, i);
        context.progress();

        if ( i == numIterations){
            saveModels(hTableMap, wMap, r, retTestRMSE,
              modelDirPath,
              this.getSelfID(), configuration);
        }

      }
      if (i % freeInterval == 0) {
        this.freeMemory();
        this.freeConn();
      }
    }
    scheduler.stop();
    rotator.stop();
  }

  private long createVWHMapAndWModel(
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    double[][][] wMapRef,
    Int2ObjectOpenHashMap<VRowCol> vRowMap, int r,
    double oneOverSqrtR, int numThreads,
    Random random) {
    LOG.info("Number of training data rows: "
      + vRowMap.size());
    // Organize vWMap
    int maxRowID = Integer.MIN_VALUE;
    VRowCol[] values =
      vRowMap.values().toArray(new VRowCol[0]);
    // Clean the data
    vRowMap.clear();
    vRowMap.trim();
    Table<VSet> vSetTable = new Table<>(0,
      new VSetCombiner(), values.length);
    for (int i = 0; i < values.length; i++) {
      VRowCol vRowCol = values[i];
      vSetTable.addPartition(new Partition<>(
        vRowCol.id, new VSet(vRowCol.id,
          vRowCol.ids, vRowCol.v, vRowCol.numV)));
      if ((i + 1) % 1000000 == 0) {
        LOG.info("Processed " + (i + 1));
      }
      if (vRowCol.id > maxRowID) {
        maxRowID = vRowCol.id;
      }
    }
    values = null;
    // Randomize the row distribution among
    // workers
    long start = System.currentTimeMillis();
    int oldNumRows = vSetTable.getNumPartitions();
    Table<LongArray> seedTable =
      new Table<>(0, new LongArrMax());
    long seed = System.currentTimeMillis();
    LOG.info("Generate seed" + ", maxRowID "
      + maxRowID + ", seed: " + seed);
    LongArray seedArray =
      LongArray.create(2, false);
    seedArray.get()[0] = (long) maxRowID;
    seedArray.get()[1] = seed;
    seedTable.addPartition(
      new Partition<>(0, seedArray));
    this.allreduce("sgd", "get-row-seed",
      seedTable);
    maxRowID = (int) seedTable.getPartition(0)
      .get().get()[0];
    seed =
      seedTable.getPartition(0).get().get()[1];
    seedTable.release();
    seedTable = null;
    LOG.info("Regroup data by rows " + oldNumRows
      + ", maxRowID " + maxRowID + ", seed: "
      + seed);
    regroup("sgd", "regroup-vw", vSetTable,
      new RandomPartitioner(maxRowID, seed,
        this.getNumWorkers())
    // new Partitioner(this.getNumWorkers())
    );
    long end = System.currentTimeMillis();
    LOG.info("Regroup data by rows took: "
      + (end - start)
      + ", number of rows in local(o/n): "
      + oldNumRows + " random distribution");
    this.freeMemory();
    // Create a local V Map indexed by H columns
    // Create W model
    VSetSplit[] vSetList =
      new VSetSplit[vWHMap.length];
    for (int i = 0; i < vWHMap.length; i++) {
      vWHMap[i] = new Int2ObjectOpenHashMap<>();
      vSetList[i] = new VSetSplit(i);
    }
    long workerNumV = 0L;
    IntArray idArray = IntArray.create(
      vSetTable.getNumPartitions(), false);
    int[] ids = idArray.get();
    vSetTable.getPartitionIDs().toArray(ids);
    IntArrays.quickSort(ids, 0, idArray.size());
    wMapRef[0] =
      new double[ids[idArray.size() - 1] + 1][];
    for (int i = 0; i < idArray.size(); i++) {
      Partition<VSet> partition =
        vSetTable.getPartition(ids[i]);
      int rowID = partition.id();
      VSet vSet = partition.get();
      double[] rRow = wMapRef[0][rowID];
      if (rRow == null) {
        rRow = new double[r];
        SGDUtil.randomize(random, rRow, r,
          oneOverSqrtR);
        wMapRef[0][rowID] = rRow;
      }
      int splitID = random.nextInt(vWHMap.length);
      vSetList[splitID].list.add(vSet);
      workerNumV += vSet.getNumV();
    }
    ids = null;
    idArray.release();
    idArray = null;
    LOG.info(
      "Number of V on this worker: " + workerNumV
        + ", W model size: " + wMapRef[0].length);
    // Trim vHMap in vWHMap
    List<DataInitTask> tasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new DataInitTask(vWHMap));
    }
    DynamicScheduler<VSetSplit, Object, DataInitTask> compute =
      new DynamicScheduler<>(tasks);
    compute.submitAll(vSetList);
    compute.start();
    compute.stop();
    while (compute.hasOutput()) {
      compute.waitForOutput();
    }
    vSetList = null;
    vSetTable.release();
    vSetTable = null;
    this.freeMemory();
    return workerNumV;
  }

  private long createHModel(
    Table<DoubleArray>[] hTableMap,
    Table<IntArray> vHSumTable,
    int numModelSlices,
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    double oneOverSqrtR, Random random)
    throws Exception {
    LOG.info("Start creating H model.");
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < numModelSlices; i++) {
      hTableMap[i] =
        new Table<>(i, new DoubleArrPlus());
    }
    for (int i = 0; i < vWHMap.length; i++) {
      ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
        vWHMap[i].int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<VRowCol> entry =
          iterator.next();
        IntArray array =
          IntArray.create(1, false);
        int partitionID = entry.getIntKey();
        array.get()[0] = entry.getValue().numV;
        PartitionStatus status =
          vHSumTable.addPartition(
            new Partition<>(partitionID, array));
        if (status != PartitionStatus.ADDED) {
          array.release();
        }
      }
    }
    // Aggregate column index and the element
    // count on each column
    int numWorkers = this.getNumWorkers();
    this.regroup("sgd", "regroup-vhsum",
      vHSumTable, new Partitioner(numWorkers));
    this.allgather("sgd", "allgather-vhsum",
      vHSumTable);
    totalNumCols = vHSumTable.getNumPartitions();
    IntArray idArray =
      IntArray.create((int) totalNumCols, false);
    vHSumTable.getPartitionIDs()
      .toArray(idArray.get());
    IntArrays.quickSort(idArray.get(), 0,
      idArray.size());
    // Get the seed
    // Randomize the H model distribution
    // Table<LongArray> seedTable =
    // new Table<>(0, new LongArrMax());
    // long seed = System.currentTimeMillis();
    // LongArray seedArray =
    // LongArray.create(1, false);
    // seedArray.get()[0] = seed;
    // seedTable.addPartition(
    // new Partition<>(0, seedArray));
    // this.allreduce("sgd", "get-col-seed",
    // seedTable);
    // seed =
    // seedTable.getPartition(0).get().get()[0];
    // seedTable.release();
    // seedTable = null;
    int selfID = this.getSelfID();
    int sliceIndex = 0;
    int[] ids = idArray.get();
    // Random idRandom = new Random(seed);
    for (int i = 0; i < idArray.size(); i++) {
      Partition<IntArray> partition =
        vHSumTable.getPartition(ids[i]);
      totalNumV +=
        (long) partition.get().get()[0];
      // if (idRandom
      // .nextInt(numWorkers) == selfID) {
      if (i % numWorkers == selfID) {
        // This h column
        // will be created by this worker
        int colID = partition.id();
        DoubleArray rCol =
          DoubleArray.create(r, false);
        SGDUtil.randomize(random, rCol.get(), r,
          oneOverSqrtR);
        hTableMap[sliceIndex % numModelSlices]
          .addPartition(
            new Partition<>(colID, rCol));
        sliceIndex++;
      }
    }
    ids = null;
    idArray.release();
    idArray = null;
    this.freeMemory();
    long t2 = System.currentTimeMillis();
    LOG.info("H model is created. "
      + "Total number of training data: "
      + totalNumV + " Total number of columns: "
      + totalNumCols
      + " Local number of columns: " + sliceIndex
      + " took " + (t2 - t1));
    return totalNumV;
  }

  static void trimTestVHMap(
    Int2ObjectOpenHashMap<VRowCol> testVHMap,
    double[][] wMap, Table<IntArray> vHSumTable) {
    // Trim testVHMap
    LOG.info("Total Number of H partitions: "
      + vHSumTable.getNumPartitions());
    ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
      testVHMap.int2ObjectEntrySet()
        .fastIterator();
    IntArrayList rmColIDs = new IntArrayList();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<VRowCol> entry =
        iterator.next();
      int colID = entry.getIntKey();
      VRowCol vRowCol = entry.getValue();
      if (vHSumTable
        .getPartition(colID) == null) {
        // LOG.info("remove col ID " + colID);
        rmColIDs.add(colID);
        continue;
      }
      // Only record test V
      // related to the local W model
      int[] ids = new int[vRowCol.numV];
      double[] v = new double[vRowCol.numV];
      int index = 0;
      for (int i = 0; i < vRowCol.numV; i++) {
        if (vRowCol.ids[i] < wMap.length
          && wMap[vRowCol.ids[i]] != null) {
          ids[index] = vRowCol.ids[i];
          v[index] = vRowCol.v[i];
          index++;
        }
      }
      int[] newids = new int[index];
      double[] newV = new double[index];
      System.arraycopy(ids, 0, newids, 0, index);
      System.arraycopy(v, 0, newV, 0, index);
      vRowCol.numV = index;
      vRowCol.ids = newids;
      vRowCol.v = newV;
      vRowCol.m1 = null;
      vRowCol.m2 = null;
    }
    for (int colID : rmColIDs) {
      testVHMap.remove(colID);
    }
    testVHMap.trim();
  }

  private int getTotalNumTestV(
    Int2ObjectOpenHashMap<VRowCol> testVColMap) {
    int totalNumTestV = 0;
    for (VRowCol vRowCol : testVColMap.values()) {
      totalNumTestV += vRowCol.numV;
    }
    Table<IntArray> table =
      new Table<>(0, new IntArrPlus());
    IntArray array = IntArray.create(1, false);
    array.get()[0] = totalNumTestV;
    table.addPartition(new Partition<>(0, array));
    this.allreduce("sgd", "get-num-testv", table);
    totalNumTestV =
      table.getPartition(0).get().get()[0];
    table.release();
    table = null;
    return totalNumTestV;
  }

  private long adjustMiniBatch(int selfID,
    long computeTime, long numVTrained,
    int iteration, long miniBatch,
    int numModelSlices, int numWorkers,
    int trainRatio) {
    // Try to get worker ID
    // and related computation Time
    // and the percentage of
    // completion
    Table<LongArray> arrTable =
      new Table<>(0, new LongArrPlus());
    LongArray array = LongArray.create(2, false);
    array.get()[0] = computeTime;
    array.get()[1] = numVTrained;
    arrTable.addPartition(
      new Partition<>(selfID, array));
    array = null;
    this.allgather("sgd",
      "allgather-compute-status-" + iteration,
      arrTable);
    long totalComputeTime = 0L;
    long totalNumVTrained = 0L;
    for (Partition<LongArray> partition : arrTable
      .getPartitions()) {
      long[] recvArr = partition.get().get();
      totalComputeTime += recvArr[0];
      totalNumVTrained += recvArr[1];
    }
    arrTable.release();
    arrTable = null;
    double percentage = (double) totalNumVTrained
      / (double) totalNumV;
    double avgComputeTime =
      (double) totalComputeTime
        / (double) numWorkers
        / (double) numModelSlices
        / (double) numWorkers;
    long newMinibatch = (long) Math
      .ceil(trainRatio / (double) percentage
        * (double) avgComputeTime);
    if (newMinibatch != miniBatch) {
      LOG.info("percentage: " + percentage
        + " new timer: " + newMinibatch);
    }
    return newMinibatch;
  }

  private double printRMSE(
    Rotator<DoubleArray> rotator, int numWorkers,
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    long totalNumV,
    Int2ObjectOpenHashMap<VRowCol> testVColMap,
    int totalNumTestV, double[][] wMap,
    int iteration) {
    // Initialize RMSE compute
    LinkedList<RMSETask> rmseTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      rmseTasks.add(new RMSETask(r, vWHMap,
        testVColMap, wMap));
    }
    DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute =
      new DynamicScheduler<>(rmseTasks);
    rmseCompute.start();
    computeRMSE(rotator, rmseCompute, numWorkers);
    rmseCompute.stop();
    DoubleArray array =
      DoubleArray.create(2, false);
    array.get()[0] = rmse;
    array.get()[1] = testRMSE;
    Table<DoubleArray> rmseTable =
      new Table<>(0, new DoubleArrPlus());
    rmseTable.addPartition(
      new Partition<DoubleArray>(0, array));
    this.allreduce("sgd",
      "allreduce-rmse-" + iteration, rmseTable);
    rmse =
      rmseTable.getPartition(0).get().get()[0];
    testRMSE =
      rmseTable.getPartition(0).get().get()[1];
    rmse = Math.sqrt(rmse / (double) totalNumV);
    testRMSE = Math
      .sqrt(testRMSE / (double) totalNumTestV);
    LOG.info(
      "RMSE " + rmse + ", Test RMSE " + testRMSE);
    rmseTable.release();
    double ret = testRMSE;
    rmse = 0.0;
    testRMSE = 0.0;
    return ret;
  }

  private void computeRMSE(
    Rotator<DoubleArray> rotator,
    DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute,
    int numWorkers) {
    for (int j = 0; j < numWorkers; j++) {
      for (int k = 0; k < numModelSlices; k++) {
        List<Partition<DoubleArray>>[] hMap =
          rotator.getSplitMap(k);
        rmseCompute.submitAll(hMap);
        while (rmseCompute.hasOutput()) {
          rmseCompute.waitForOutput();
        }
        rotator.rotate(k);
      }
    }
    for (RMSETask rmseTask : rmseCompute
      .getTasks()) {
      rmse += rmseTask.getRMSE();
      testRMSE += rmseTask.getTestRMSE();
    }
  }

  private void saveModels(
    Table<DoubleArray>[] hTableMap,
    double[][] wMap,
    int K, double testRMSE,
    String folderPath, int selfID,
    Configuration congfiguration)
    throws IOException {
    FileSystem fs =
      FileSystem.get(congfiguration);
    Path folder = new Path(folderPath);
    if (!fs.exists(folder)) {
      fs.mkdirs(folder);
    }
 
    //evaluation result
    if (this.isMaster()){
      Path file =
        new Path(folderPath + "/evaluation");
      PrintWriter writer =
        new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(fs.create(file))));
      writer.print(testRMSE);
      writer.println();
      writer.flush();
      writer.close();
    }

    //H model
    {
      Path file =
        new Path(folderPath + "/H-" + selfID);
      PrintWriter writer =
        new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(fs.create(file))));
      for (Table<DoubleArray> hTable : hTableMap) {
        if (hTable == null) continue;
        for (Partition<DoubleArray> hPartition : hTable
          .getPartitions()) {
          int colID = hPartition.id();
          if (hPartition.get() != null){
            double[] hRow =
              hPartition.get().get();
            // Print word
            writer.print(colID +" :");
            // Print topic count
            for (int i = 0; i < K; i++) {
              writer.print(" " + hRow[i]);
            }
            writer.println();
          }
        }
      }
      writer.flush();
      writer.close();
    }

    //W model
    {
      LOG.info("wMap length:" + wMap.length + " K:" + K);
      Path file =
        new Path(folderPath + "/W-" + selfID);
      PrintWriter writer =
        new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(fs.create(file))));
      for (int i=0; i< wMap.length; i++) {
          double[] wRow = wMap[i];
          if (wRow != null){
            // Print word
            writer.print(i + " :");
            // Print topic count
            for (int j = 0; j < K; j++) {
              writer.print(" " + wRow[j]);
            }
            writer.println();
          }
      }
      writer.flush();
      writer.close();
    }

 }

}
