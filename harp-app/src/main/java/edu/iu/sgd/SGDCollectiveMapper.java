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

package edu.iu.sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.dymoro.RotationUtil;
import edu.iu.dymoro.Rotator;
import edu.iu.dymoro.Scheduler;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

public class SGDCollectiveMapper
  extends
  CollectiveMapper<String, String, Object, Object> {
  private int r;
  private double oneOverSqrtR;
  private double lambda;
  private double epsilon;
  private int numIterations;
  private int numThreads;
  private String modelDirPath;
  private long time;
  private int numModelSlices;
  private int rmseIteInterval;
  private int freeInterval;
  private double rmse;
  private double testRMSE;
  private String testFilePath;
  private boolean timerTuning;
  private long computeTime;
  private long numVTrained;
  private long waitTime;
  private Random random;

  private long totalNumV;
  private long totalNumCols;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context) {
    LOG
      .info("start setup: "
        + new SimpleDateFormat("yyyyMMdd_HHmmss")
          .format(Calendar.getInstance()
            .getTime()));
    long startTime = System.currentTimeMillis();
    Configuration configuration =
      context.getConfiguration();
    r = configuration.getInt(Constants.R, 100);
    lambda =
      configuration.getDouble(Constants.LAMBDA,
        0.001);
    epsilon =
      configuration.getDouble(Constants.EPSILON,
        0.001);
    numIterations =
      configuration.getInt(
        Constants.NUM_ITERATIONS, 100);
    numThreads =
      configuration.getInt(Constants.NUM_THREADS,
        16);
    modelDirPath =
      configuration.get(Constants.MODEL_DIR, "");
    time =
      configuration
        .getLong(Constants.Time, 1000L);
    numModelSlices =
      configuration.getInt(
        Constants.NUM_MODEL_SLICES, 2);
    testFilePath =
      configuration.get(Constants.TEST_FILE_PATH,
        "");
    String javaOpts =
      configuration.get(
        "mapreduce.map.collective.java.opts", "");
    rmseIteInterval = 5;
    freeInterval = 20;
    rmse = 0.0;
    testRMSE = 0.0;
    computeTime = 0L;
    numVTrained = 0L;
    waitTime = 0L;
    timerTuning =
      configuration.getBoolean(
        Constants.ENABLE_TUNING, true);
    totalNumV = 0L;
    totalNumCols = 0L;
    oneOverSqrtR = 1.0 / Math.sqrt(r);
    random =
      new Random(System.currentTimeMillis());
    long endTime = System.currentTimeMillis();
    LOG.info("config (ms): "
      + (endTime - startTime));
    LOG.info("R " + r);
    LOG.info("Lambda " + lambda);
    LOG.info("Epsilon " + epsilon);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Timer " + time);
    LOG.info("Model Slices " + numModelSlices);
    LOG.info("Model Dir Path " + modelDirPath);
    LOG.info("TEST FILE PATH " + testFilePath);
    LOG.info("JAVA OPTS " + javaOpts);
    LOG.info("Time Tuning " + timerTuning);
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

  private LinkedList<String> getVFiles(
    final KeyValReader reader)
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
      SGDUtil.loadVWMap(vFilePaths, Runtime
        .getRuntime().availableProcessors(),
        configuration);
    Int2ObjectOpenHashMap<VRowCol> testVColMap =
      SGDUtil.loadTestVHMap(testFilePath,
        configuration, Runtime.getRuntime()
          .availableProcessors());
    // ---------------------------------------------
    // Create vHMap and W model
    int numSplits =
      ((int) Math.round(numThreads / 20.0) + 1) * 20;
    final int numRowSplits = numSplits;
    final int numColSplits = numRowSplits;
    LOG.info("numRowSplits: " + numRowSplits
      + " " + " numColSplits: " + numColSplits);
    final Int2ObjectOpenHashMap<double[]> wMap =
      new Int2ObjectOpenHashMap<>();
    // vHMap grouped to splits
    // based on their row IDs
    final Int2ObjectOpenHashMap<VRowCol>[] vWHMap =
      new Int2ObjectOpenHashMap[numRowSplits];
    final long workerNumV =
      createVWHMapAndWModel(vWHMap, wMap,
        vRowMap, r, oneOverSqrtR, numThreads,
        random);
    vRowMap = null;
    // Create H model
    Table<IntArray> vHSumTable =
      new Table<>(0, new IntArrPlus());
    Table<DoubleArray>[] hTableMap =
      new Table[numModelSlices];
    createHModel(hTableMap, vHSumTable,
      numModelSlices, vWHMap, oneOverSqrtR,
      random);
    // Trim Test VHMap
    SGDUtil.trimTestVHMap(testVColMap, wMap,
      vHSumTable);
    int totalNumTestV =
      getTotalNumTestV(testVColMap);
    LOG.info("Total num of test V: "
      + totalNumTestV);
    vHSumTable.release();
    vHSumTable = null;
    this.freeMemory();
    this.freeConn();
    System.gc();
    // ----------------------------------------------
    // Create rotator
    final int numWorkers = this.getNumWorkers();
    int[] order =
      RotationUtil
        .getRotationSequences(random, numWorkers,
          (numIterations + 1) * 2, this);
    Rotator<DoubleArray> rotator =
      new Rotator<>(hTableMap, numColSplits,
        true, this, order, "sgd");
    rotator.start();
    // Initialize scheduler
    List<SGDMPTask> sgdTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      sgdTasks.add(new SGDMPTask(r, lambda,
        epsilon));
    }
    Scheduler<VRowCol, DoubleArray, SGDMPTask> scheduler =
      new Scheduler<>(numRowSplits, numColSplits,
        vWHMap, time, sgdTasks);
    // Initialize RMSE compute
    LinkedList<RMSETask> rmseTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      rmseTasks.add(new RMSETask(r, vWHMap,
        testVColMap));
    }
    DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute =
      new DynamicScheduler<>(rmseTasks);
    rmseCompute.start();
    printRMSE(rotator, rmseCompute, numWorkers,
      totalNumV, totalNumTestV, 0, configuration);
    LOG.info("Iteration Starts.");
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
          waitTime += (t2 - t1);
          scheduler.schedule(hMap);
          numVTrained +=
            scheduler.getNumVItemsTrained();
          long t3 = System.currentTimeMillis();
          computeTime += (t3 - t2);
          rotator.rotate(k);
        }
      }
      int percentage =
        (int) Math.round((double) numVTrained
          / (double) workerNumV * 100.0);
      if (i == 1 && timerTuning) {
        long newMiniBatch =
          adjustMiniBatch(this.getSelfID(),
            computeTime, numVTrained, i, time,
            numModelSlices, numWorkers);
        if (time != newMiniBatch) {
          time = newMiniBatch;
          scheduler.setTimer(time);
          LOG.info("Set miniBatch to " + time);
        }
      }
      long iteEnd = System.currentTimeMillis();
      long iteTime = iteEnd - iteStart;
      LOG.info("Iteration " + i + ": " + iteTime
        + ", num V: " + numVTrained
        + ", compute time: " + computeTime
        + ", misc: " + waitTime
        + ", percentage(%): " + percentage);
      computeTime = 0L;
      numVTrained = 0L;
      waitTime = 0L;
      // Calculate RMSE
      if (i == 1 || i % rmseIteInterval == 0) {
        // this.logMemUsage();
        // this.logGCTime();
        printRMSE(rotator, rmseCompute,
          numWorkers, totalNumV, totalNumTestV,
          i, configuration);
      }
      if (i % freeInterval == 0) {
        this.freeMemory();
        this.freeConn();
      }
      context.progress();
    }
    // Stop sgdCompute and rotation
    scheduler.stop();
    rmseCompute.stop();
    rotator.stop();
  }

  private long createVWHMapAndWModel(
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    Int2ObjectOpenHashMap<double[]> wMap,
    Int2ObjectOpenHashMap<VRowCol> vRowMap,
    int r, double oneOverSqrtR, int numThreads,
    Random random) {
    // Organize vWMap
    Table<VSet> vSetTable =
      new Table<>(0, new VSetCombiner());
    int maxRowID = Integer.MIN_VALUE;
    ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
      vRowMap.int2ObjectEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<VRowCol> entry =
        iterator.next();
      int rowID = entry.getIntKey();
      VRowCol vRowCol = entry.getValue();
      vSetTable.addPartition(new Partition<>(
        rowID, new VSet(vRowCol.id, vRowCol.ids,
          vRowCol.v, vRowCol.numV)));
      if (rowID > maxRowID) {
        maxRowID = rowID;
      }
    }
    // Clean the data
    vRowMap.clear();
    // Randomize the row distribution among
    // workers
    long start = System.currentTimeMillis();
    int oldNumRows = vSetTable.getNumPartitions();
    Table<LongArray> seedTable =
      new Table<>(0, new LongArrMax());
    long seed = System.currentTimeMillis();
    LongArray seedArray =
      LongArray.create(2, false);
    seedArray.get()[0] = (long) maxRowID;
    seedArray.get()[1] = seed;
    seedTable.addPartition(new Partition<>(0,
      seedArray));
    this.allreduce("sgd", "get-row-seed",
      seedTable);
    maxRowID =
      (int) seedTable.getPartition(0).get().get()[0];
    seed =
      seedTable.getPartition(0).get().get()[1];
    seedTable.release();
    seedTable = null;
    regroup(
      "sgd",
      "regroup-vw",
      vSetTable,
      new RandomPartitioner(maxRowID, seed, this
        .getNumWorkers())
    // new Partitioner(this.getNumWorkers())
    );
    long end = System.currentTimeMillis();
    LOG.info("Regroup data by rows took: "
      + (end - start)
      + ", number of rows in local(o/n): "
      + oldNumRows + " "
      + vSetTable.getNumPartitions()
      + ", maxRowID " + maxRowID + ", seed: "
      + seed + " modulo distribution");
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
    IntArray idArray =
      IntArray.create(
        vSetTable.getNumPartitions(), false);
    int[] ids = idArray.get();
    vSetTable.getPartitionIDs().toArray(ids);
    IntArrays.quickSort(ids, 0, idArray.size());
    for (int i = 0; i < idArray.size(); i++) {
      Partition<VSet> partition =
        vSetTable.getPartition(ids[i]);
      int rowID = partition.id();
      VSet vSet = partition.get();
      double[] rRow = wMap.get(rowID);
      if (rRow == null) {
        rRow = new double[r];
        SGDUtil.randomize(random, rRow, r,
          oneOverSqrtR);
        wMap.put(rowID, rRow);
      }
      // int splitID = i % vWHMap.length;
      // Randomize the row partitioning inside a
      // worker
      int splitID = random.nextInt(vWHMap.length);
      vSetList[splitID].list.add(vSet);
      workerNumV += vSet.getNumV();
    }
    idArray.release();
    idArray = null;
    LOG.info("Number of V on this worker "
      + workerNumV + ". W model is created.");
    // Trim vHMap in vWHMap
    List<DataInitTask> tasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new DataInitTask(vWHMap, wMap));
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
    wMap.trim();
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
          vHSumTable
            .addPartition(new Partition<>(
              partitionID, array));
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
    vHSumTable.getPartitionIDs().toArray(
      idArray.get());
    IntArrays.quickSort(idArray.get(), 0,
      idArray.size());
    // Get the seed
    // Randomize the H model distribution
    Table<LongArray> seedTable =
      new Table<>(0, new LongArrMax());
    long seed = System.currentTimeMillis();
    LongArray seedArray =
      LongArray.create(1, false);
    seedArray.get()[0] = seed;
    seedTable.addPartition(new Partition<>(0,
      seedArray));
    this.allreduce("sgd", "get-col-seed",
      seedTable);
    seed =
      seedTable.getPartition(0).get().get()[0];
    seedTable.release();
    seedTable = null;
    int selfID = this.getSelfID();
    // int workerIndex = 0;
    int sliceIndex = 0;
    int[] ids = idArray.get();
    Random idRandom = new Random(seed);
    for (int i = 0; i < idArray.size(); i++) {
      Partition<IntArray> partition =
        vHSumTable.getPartition(ids[i]);
      totalNumV +=
        (long) partition.get().get()[0];
      if (idRandom.nextInt(numWorkers) == selfID) {
        // if (workerIndex % numWorkers == selfID)
        // {
        // This h column
        // will be created by this worker
        int colID = partition.id();
        DoubleArray rCol =
          DoubleArray.create(r, false);
        SGDUtil.randomize(random, rCol.get(), r,
          oneOverSqrtR);
        // Also do randomization for slicing?
        // hTableMap[sliceIndex % numModelSlices]
        // .addPartition(new Partition<>(colID,
        // rCol));
        hTableMap[random.nextInt(numModelSlices)]
          .addPartition(new Partition<>(colID,
            rCol));
        sliceIndex++;
      }
      // workerIndex++;
    }
    idArray.release();
    idArray = null;
    this.freeMemory();
    long t2 = System.currentTimeMillis();
    LOG.info("H model is created. "
      + "Total number of training data "
      + totalNumV + " " + totalNumCols + " "
      + sliceIndex + " took " + (t2 - t1));
    return totalNumV;
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
    int numModelSlices, int numWorkers) {
    // Try to get worker ID
    // and related computation Time
    // and the percentage of
    // completion
    Table<IntArray> arrTable =
      new Table<>(0, new IntArrPlus());
    IntArray array = IntArray.create(2, false);
    array.get()[0] = (int) computeTime;
    array.get()[1] = (int) numVTrained;
    arrTable.addPartition(new Partition<>(selfID,
      array));
    this.allgather("sgd",
      "allgather-compute-status-" + iteration,
      arrTable);
    long totalComputeTime = 0L;
    long totalNumVTrained = 0L;
    for (Partition<IntArray> partition : arrTable
      .getPartitions()) {
      int[] recvArr = partition.get().get();
      totalComputeTime += (long) recvArr[0];
      totalNumVTrained += (long) recvArr[1];
    }
    arrTable.release();
    arrTable = null;
    // double ratio =
    // Constants.COMM_VS_COMPUTE
    // * (double) numWorkers
    // * (double) numThreads
    // * (double) totalNumCols
    // / (double) totalNumV / 2.0;
    // if (ratio < Constants.MIN_RATIO) {
    // double tmp = ratio;
    // double delta = 0.0;
    // while (tmp < Constants.MIN_RATIO) {
    // delta += 0.1;
    // tmp = (1.0 + delta) * ratio;
    // }
    // ratio = tmp;
    // }
    // if (ratio > Constants.MAX_RATIO) {
    // ratio = Constants.MAX_RATIO;
    // }
    double ratio = Constants.GOLDEN_RATIO;
    double percentage =
      (double) totalNumVTrained
        / (double) totalNumV;
    double avgComputeTime =
      (double) totalComputeTime
        / (double) numWorkers
        / (double) numModelSlices
        / (double) numWorkers;
    miniBatch =
      (long) Math.round(ratio
        / (double) percentage
        * (double) avgComputeTime / 10.0) * 10L;
    LOG.info("new miniBatch " + miniBatch + " "
      + ratio + " " + percentage);
    return miniBatch;
  }

  private
    void
    printRMSE(
      Rotator<DoubleArray> rotator,
      DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute,
      int numWorkers, long totalNumV,
      int totalNumTestV, int iteration,
      Configuration configuration)
      throws InterruptedException {
    computeRMSE(rotator, rmseCompute, numWorkers);
    DoubleArray array =
      DoubleArray.create(2, false);
    array.get()[0] = rmse;
    array.get()[1] = testRMSE;
    Table<DoubleArray> rmseTable =
      new Table<>(0, new DoubleArrPlus());
    rmseTable
      .addPartition(new Partition<DoubleArray>(0,
        array));
    this.allreduce("sgd", "allreduce-rmse-"
      + iteration, rmseTable);
    rmse =
      rmseTable.getPartition(0).get().get()[0];
    testRMSE =
      rmseTable.getPartition(0).get().get()[1];
    rmse = Math.sqrt(rmse / (double) totalNumV);
    testRMSE =
      Math
        .sqrt(testRMSE / (double) totalNumTestV);
    LOG.info("RMSE " + rmse + ", Test RMSE "
      + testRMSE);
    rmseTable.release();
    rmse = 0.0;
    testRMSE = 0.0;
  }

  private
    void
    computeRMSE(
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
}