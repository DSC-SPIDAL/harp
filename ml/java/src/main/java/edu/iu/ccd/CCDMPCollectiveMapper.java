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

import edu.iu.dymoro.RotationUtil;
import edu.iu.dymoro.Rotator;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.sgd.SGDUtil;
import edu.iu.sgd.VRowCol;
import edu.iu.sgd.VSet;
import edu.iu.sgd.VSetCombiner;
import edu.iu.sgd.VSetSplit;
import edu.iu.sgd.VStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class CCDMPCollectiveMapper extends
  CollectiveMapper<String, String, Object, Object> {
  private int r;
  private double lambda;
  private int numIterations;
  private int numThreads;
  private String modelDirPath;
  private int numModelSlices;
  private int rmseIteInterval;
  private boolean printRMSE;
  private double testRMSE;
  private String testFilePath;
  private long computeTime;
  private long prepareResTime;
  private long totalNumV;
  private long waitTime;

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
    numIterations = configuration
      .getInt(Constants.NUM_ITERATIONS, 100);
    numThreads = configuration
      .getInt(Constants.NUM_THREADS, 16);
    modelDirPath =
      configuration.get(Constants.MODEL_DIR, "");
    numModelSlices = configuration
      .getInt(Constants.NUM_MODEL_SLICES, 2);
    testFilePath = configuration
      .get(Constants.TEST_FILE_PATH, "");
    rmseIteInterval = 1;
    printRMSE = false;
    testRMSE = 0.0;
    computeTime = 0L;
    prepareResTime = 0L;
    totalNumV = 0L;
    waitTime = 0L;
    long endTime = System.currentTimeMillis();
    LOG.info(
      "config (ms): " + (endTime - startTime));
    LOG.info("R " + r);
    LOG.info("Lambda " + lambda);
    LOG.info("No. Iterations " + numIterations);
    LOG.info("No. Threads " + numThreads);
    LOG.info("Model Dir Path " + modelDirPath);
    LOG
      .info("No. Model Slices " + numModelSlices);
    LOG.info("TEST FILE PATH " + testFilePath);
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    List<String> vFiles = getVFiles(reader);
    try {
      runCCD(vFiles, context.getConfiguration(),
        context);
    } catch (Exception e) {
      LOG.error("Fail to run CCD.", e);
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

  private void runCCD(
    final List<String> vFilePaths,
    final Configuration configuration,
    final Context context) throws Exception {
    LOG.info("Use Model Parallelism");
    VStore vStore = new VStore(vFilePaths,
      numThreads, configuration);
    vStore.load(true, true);
    Int2ObjectOpenHashMap<VRowCol> vWMap =
      vStore.getVWMap();
    Int2ObjectOpenHashMap<VRowCol> vHMap =
      vStore.getVHMap();
    Int2ObjectOpenHashMap<VRowCol> testVHMap =
      SGDUtil.loadTestVHMap(testFilePath,
        configuration, numThreads);
    long totalNumTestV = 0L;
    for (VRowCol vRowCol : testVHMap.values()) {
      totalNumTestV += vRowCol.numV;
    }
    LOG.info(
      "Total num of test V: " + totalNumTestV);
    // ---------------------------------------------
    // Find the W ID range and H ID range
    // Create vHMap and W model
    final int numRowSplits = 10000;
    final int numColSplits = 10000;
    LOG.info("Split: " + numRowSplits + " "
      + numColSplits);
    Int2ObjectOpenHashMap<VRowCol>[] vWSplitMap =
      new Int2ObjectOpenHashMap[numRowSplits];
    Int2ObjectOpenHashMap<VRowCol>[] vHSplitMap =
      new Int2ObjectOpenHashMap[numColSplits];
    final long workerNumWV = createSplitMap(vWMap,
      vWSplitMap, numRowSplits, numThreads, "W");
    final long workerNumHV = createSplitMap(vHMap,
      vHSplitMap, numColSplits, numThreads, "H");
    LOG.info("workerNumWV: " + workerNumWV
      + ", workerNumHV: " + workerNumHV);
    vWMap = null;
    vHMap = null;
    System.gc();
    // Trim TestVHMap
    trimTestVHMap(testVHMap, vHSplitMap,
      numColSplits);
    List<VRowCol>[] vWSplitList =
      new List[numRowSplits];
    List<VRowCol>[] vHSplitList =
      new List[numRowSplits];
    convertMapToList(vWSplitMap, vWSplitList,
      numRowSplits);
    convertMapToList(vHSplitMap, vHSplitList,
      numColSplits);
    vWSplitMap = null;
    vHSplitMap = null;
    // Create W model
    // Create H model
    final double oneOverSqrtR =
      1.0 / Math.sqrt(r);
    final Random random =
      new Random(System.currentTimeMillis());
    final int numWorkers = this.getNumWorkers();
    final int selfID = this.getSelfID();
    final Table<DoubleArray>[] wTableMap =
      new Table[numModelSlices];
    final Table<DoubleArray>[] hTableMap =
      new Table[numModelSlices];
    final long totalNumRows = createModel(
      wTableMap, numModelSlices, vWSplitList, r,
      oneOverSqrtR, random, "W");
    final long totalNumCols = createModel(
      hTableMap, numModelSlices, vHSplitList, r,
      oneOverSqrtR, random, "H");
    for (Table<DoubleArray> table : wTableMap) {
      LOG.info("W Table Slice: "
        + table.getNumPartitions());
    }
    for (Table<DoubleArray> table : hTableMap) {
      LOG.info("H Table Slice: "
        + table.getNumPartitions());
    }
    this.freeMemory();
    this.freeConn();
    System.gc();
    // ----------------------------------------------
    // Create rotators
    int[] orders = RotationUtil
      .getRotationSequences(random, numWorkers,
        (numIterations + 1) * 4, this);
    Rotator<DoubleArray> wRotator = new Rotator<>(
      wTableMap, 1, false, this, orders, "ccdw");
    Rotator<DoubleArray> hRotator = new Rotator<>(
      hTableMap, 1, false, this, orders, "ccdh");
    wRotator.start();
    hRotator.start();
    List<CCDMPTask> ccdTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      ccdTasks.add(new CCDMPTask(lambda));
    }
    DynamicScheduler<List<VRowCol>, Object, CCDMPTask> ccdCompute =
      new DynamicScheduler<>(ccdTasks);
    List<ResTask> resTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      resTasks.add(new ResTask());
    }
    DynamicScheduler<List<VRowCol>, Object, ResTask> resCompute =
      new DynamicScheduler<>(resTasks);
    List<TestRMSETask> rmseTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      rmseTasks.add(new TestRMSETask());
    }
    DynamicScheduler<VRowCol, Object, TestRMSETask> rmseCompute =
      new DynamicScheduler<>(rmseTasks);
    ccdCompute.start();
    resCompute.start();
    rmseCompute.start();
    boolean useRow = false;
    if (totalNumRows < totalNumCols) {
      useRow = true;
      LOG.info("useRow " + useRow);
    }
    LOG.info("Calculate Res.");
    printRMSE(wRotator, hRotator, resCompute,
      vWSplitList, vHSplitList, numWorkers,
      numThreads, "0", totalNumV, useRow);
    printTestRMSE(wRotator, hRotator, rmseCompute,
      testVHMap, numWorkers, totalNumTestV, "0");
    LOG.info("Iteration Starts.");
    // -----------------------------------------
    // For iteration
    for (int i = 1; i <= numIterations; i++) {
      long iteStart = System.currentTimeMillis();
      // scheduler use row
      computeCCD(wRotator, hRotator, ccdCompute,
        resCompute, vWSplitList, vHSplitList,
        numWorkers, useRow);
      // scheduler use col
      computeCCD(wRotator, hRotator, ccdCompute,
        resCompute, vWSplitList, vHSplitList,
        numWorkers, !useRow);
      long iteEnd = System.currentTimeMillis();
      long iteTime = iteEnd - iteStart;
      LOG.info("Iteration " + i + ": " + iteTime
        + ", compute time: " + computeTime + " "
        + prepareResTime + ", misc: " + waitTime);
      computeTime = 0L;
      prepareResTime = 0L;
      waitTime = 0L;
      // Calculate RMSE
      printRMSE =
        (i == 1 || i % rmseIteInterval == 0
          || i == numIterations);
      if (printRMSE) {
        // this.logMemUsage();
        // this.logGCTime();
        printTestRMSE(wRotator, hRotator,
          rmseCompute, testVHMap, numWorkers,
          totalNumTestV, i + "-1");
      }
      context.progress();
    }
    wRotator.stop();
    hRotator.stop();
    ccdCompute.stop();
    resCompute.stop();
    rmseCompute.stop();
  }

  long createSplitMap(
    Int2ObjectOpenHashMap<VRowCol> vMap,
    Int2ObjectOpenHashMap<VRowCol>[] vSplitMap,
    int numSplits, int numThreads,
    String opName) {
    Table<VSet> vSetTable =
      new Table<>(0, new VSetCombiner());
    ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
      vMap.int2ObjectEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<VRowCol> entry =
        iterator.next();
      int id = entry.getIntKey();
      VRowCol vRowCol = entry.getValue();
      vSetTable.addPartition(
        new Partition<>(id, new VSet(vRowCol.id,
          vRowCol.ids, vRowCol.v, vRowCol.numV)));
    }
    vMap.clear();
    long start = System.currentTimeMillis();
    regroup("ccd", "regroup-v-" + opName,
      vSetTable,
      new Partitioner(getNumWorkers()));
    long end = System.currentTimeMillis();
    LOG.info("Regroup " + opName + " took: "
      + (end - start)
      + ", number of rows in local: "
      + vSetTable.getNumPartitions());
    this.freeMemory();
    VSetSplit[] vSetList =
      new VSetSplit[numSplits];
    for (int i = 0; i < numSplits; i++) {
      vSplitMap[i] =
        new Int2ObjectOpenHashMap<>();
      vSetList[i] = new VSetSplit(i);
    }
    long workerNumV = 0L;
    IntArray idArray = IntArray.create(
      vSetTable.getNumPartitions(), false);
    int[] ids = idArray.get();
    vSetTable.getPartitionIDs().toArray(ids);
    IntArrays.quickSort(ids, 0, idArray.size());
    for (int i = 0; i < idArray.size(); i++) {
      Partition<VSet> partition =
        vSetTable.getPartition(ids[i]);
      VSet vSet = partition.get();
      workerNumV += vSet.getNumV();
      int splitID = i % numSplits;
      vSetList[splitID].list.add(vSet);
    }
    idArray.release();
    idArray = null;
    List<DataInitTask> tasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new DataInitTask(vSplitMap));
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

  void trimTestVHMap(
    Int2ObjectOpenHashMap<VRowCol> testVHMap,
    Int2ObjectOpenHashMap<VRowCol>[] vHSplitMap,
    int numVHSplits) {
    // TODO filter the test points without row or
    // column in the training dataset
    ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
      testVHMap.int2ObjectEntrySet()
        .fastIterator();
    IntArrayList rmColIDs = new IntArrayList();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<VRowCol> entry =
        iterator.next();
      int colID = entry.getIntKey();
      VRowCol vCol = entry.getValue();
      boolean exist = false;
      for (int i = 0; i < numVHSplits; i++) {
        if (vHSplitMap[i].containsKey(colID)) {
          exist = true;
        }
      }
      if (exist) {
        int[] ids = new int[vCol.numV];
        double[] v = new double[vCol.numV];
        System.arraycopy(vCol.ids, 0, ids, 0,
          vCol.numV);
        System.arraycopy(vCol.v, 0, v, 0,
          vCol.numV);
        vCol.ids = ids;
        vCol.v = v;
        // m1 is used for storing residual
        vCol.m1 = new double[vCol.numV];
      } else {
        rmColIDs.add(colID);
      }
    }
    for (int colID : rmColIDs) {
      testVHMap.remove(colID);
    }
  }

  private void convertMapToList(
    Int2ObjectOpenHashMap<VRowCol>[] vSplitMap,
    List<VRowCol>[] vListMap, int numSplits) {
    for (int i = 0; i < numSplits; i++) {
      vListMap[i] = new LinkedList<>();
      vListMap[i].addAll(vSplitMap[i].values());
    }
  }

  private long createModel(
    Table<DoubleArray>[] mTableMap,
    int numModelSlices,
    List<VRowCol>[] vSplitList, int r,
    double oneOverSqrtR, Random random,
    String opName) throws Exception {
    for (int i = 0; i < numModelSlices; i++) {
      mTableMap[i] =
        new Table<>(i, new DoubleArrPlus());
    }
    Table<IntArray> vSumTable =
      new Table<>(0, new IntArrPlus());
    for (int i = 0; i < vSplitList.length; i++) {
      for (VRowCol vRowCol : vSplitList[i]) {
        IntArray array =
          IntArray.create(1, false);
        array.get()[0] = vRowCol.numV;
        PartitionStatus status = vSumTable
          .addPartition(new Partition<IntArray>(
            vRowCol.id, array));
        if (status != PartitionStatus.ADDED) {
          array.release();
        }
      }
    }
    int numWorkers = this.getNumWorkers();
    this.regroup("ccd", "regroup-vsum-" + opName,
      vSumTable, new Partitioner(numWorkers));
    this.allgather("ccd",
      "allgather-vsum-" + opName, vSumTable);
    totalNumV = 0L;
    int totalNumRowCols =
      vSumTable.getNumPartitions();
    int maxID = Integer.MIN_VALUE;
    for (Partition<IntArray> partition : vSumTable
      .getPartitions()) {
      int id = partition.id();
      totalNumV +=
        (long) partition.get().get()[0];
      if (id > maxID) {
        maxID = id;
      }
    }
    vSumTable.release();
    vSumTable = null;
    this.freeMemory();
    int len = maxID + 1;
    int sliceIndex = 0;
    int selfID = this.getSelfID();
    for (int i = selfID; i < r; i += numWorkers) {
      DoubleArray array =
        DoubleArray.create(len, false);
      SGDUtil.randomize(random, array.get(), len,
        oneOverSqrtR);
      mTableMap[sliceIndex % numModelSlices]
        .addPartition(new Partition<>(i, array));
      sliceIndex++;
    }
    LOG.info(opName + " model is created."
      + " n. training points " + totalNumV
      + " n. row/cols " + totalNumRowCols
      + " maxID " + len + " local n. dimensions "
      + sliceIndex);
    return totalNumRowCols;
  }

  private void computeCCD(
    Rotator<DoubleArray> wRotator,
    Rotator<DoubleArray> hRotator,
    DynamicScheduler<List<VRowCol>, Object, CCDMPTask> ccdCompute,
    DynamicScheduler<List<VRowCol>, Object, ResTask> resCompute,
    List<VRowCol>[] vWSplitList,
    List<VRowCol>[] vHSplitList, int numWorkers,
    boolean useRow) throws InterruptedException {
    for (int j = 0; j < numWorkers; j++) {
      for (int k = 0; k < numModelSlices; k++) {
        long t1 = System.currentTimeMillis();
        List<Partition<DoubleArray>> wList =
          wRotator.getSplitMap(k)[0];
        List<Partition<DoubleArray>> hList =
          hRotator.getSplitMap(k)[0];
        for (CCDMPTask task : ccdCompute
          .getTasks()) {
          task.useRow(useRow);
          task.setWHPartitionLists(wList, hList);
        }
        for (ResTask task : resCompute
          .getTasks()) {
          task.setInit(false);
          task.setRMSE(false);
          if (j == 0 && k == 0) {
            task.setInit(true);
          }
          task.useRow(!useRow);
          task.setWHPartitionLists(wList, hList);
        }
        long t2 = System.currentTimeMillis();
        if (useRow) {
          ccdCompute.submitAll(vWSplitList);
        } else {
          ccdCompute.submitAll(vHSplitList);
        }
        while (ccdCompute.hasOutput()) {
          ccdCompute.waitForOutput();
        }
        long t3 = System.currentTimeMillis();
        if (useRow) {
          resCompute.submitAll(vHSplitList);
        } else {
          resCompute.submitAll(vWSplitList);
        }
        while (resCompute.hasOutput()) {
          resCompute.waitForOutput();
        }
        long t4 = System.currentTimeMillis();
        wRotator.rotate(k);
        hRotator.rotate(k);
        waitTime += (t2 - t1);
        computeTime += (t3 - t2);
        prepareResTime += (t4 - t3);
      }
    }
  }

  private void printRMSE(
    Rotator<DoubleArray> wRotator,
    Rotator<DoubleArray> hRotator,
    DynamicScheduler<List<VRowCol>, Object, ResTask> resCompute,
    List<VRowCol>[] vWSplitList,
    List<VRowCol>[] vHSplitList, int numWorkers,
    int numThreads, String name, long totalNumV,
    boolean useRow) throws InterruptedException {
    // Reset
    for (ResTask task : resCompute.getTasks()) {
      task.getRowRMSE();
      task.getColRMSE();
    }
    calcRes(wRotator, hRotator, resCompute,
      vWSplitList, vHSplitList, numWorkers,
      useRow);
    double rowRMSE = 0.0;
    double colRMSE = 0.0;
    for (ResTask task : resCompute.getTasks()) {
      rowRMSE += task.getRowRMSE();
      colRMSE += task.getColRMSE();
    }
    DoubleArray array =
      DoubleArray.create(2, false);
    array.get()[0] = rowRMSE;
    array.get()[1] = colRMSE;
    Table<DoubleArray> rmseArrTable =
      new Table<>(0, new DoubleArrPlus());
    rmseArrTable.addPartition(
      new Partition<DoubleArray>(0, array));
    this.allreduce("ccd",
      "allreduce-rmse-" + name, rmseArrTable);
    rowRMSE =
      rmseArrTable.getPartition(0).get().get()[0];
    colRMSE =
      rmseArrTable.getPartition(0).get().get()[1];
    rmseArrTable.release();
    rowRMSE =
      Math.sqrt(rowRMSE / (double) totalNumV);
    colRMSE =
      Math.sqrt(colRMSE / (double) totalNumV);
    LOG.info("Row RMSE " + rowRMSE + ", Col RMSE "
      + colRMSE);
  }

  private void calcRes(
    Rotator<DoubleArray> wRotator,
    Rotator<DoubleArray> hRotator,
    DynamicScheduler<List<VRowCol>, Object, ResTask> resCompute,
    List<VRowCol>[] vWSplitList,
    List<VRowCol>[] vHSplitList, int numWorkers,
    boolean useRow) throws InterruptedException {
    for (int j = 0; j < numWorkers; j++) {
      for (int k = 0; k < numModelSlices; k++) {
        List<Partition<DoubleArray>> wList =
          wRotator.getSplitMap(k)[0];
        List<Partition<DoubleArray>> hList =
          hRotator.getSplitMap(k)[0];
        for (ResTask task : resCompute
          .getTasks()) {
          task.setInit(false);
          task.setRMSE(false);
          if (j == 0 && k == 0) {
            task.setInit(true);
          }
          if (j == (numWorkers - 1)
            && k == (numModelSlices - 1)) {
            task.setRMSE(true);
          }
          task.useRow(useRow);
          task.setWHPartitionLists(wList, hList);
        }
        if (useRow) {
          resCompute.submitAll(vWSplitList);
        } else {
          resCompute.submitAll(vHSplitList);
        }
        while (resCompute.hasOutput()) {
          resCompute.waitForOutput();
        }
        wRotator.rotate(k);
        hRotator.rotate(k);
      }
    }
  }

  private void printTestRMSE(
    Rotator<DoubleArray> wRotator,
    Rotator<DoubleArray> hRotator,
    DynamicScheduler<VRowCol, Object, TestRMSETask> rmseCompute,
    Int2ObjectOpenHashMap<VRowCol> testVHListMap,
    int numWorkers, long totalNumTestV,
    String name) throws InterruptedException {
    double result =
      computeTestRMSE(wRotator, hRotator,
        rmseCompute, testVHListMap, numWorkers);
    DoubleArray array =
      DoubleArray.create(1, false);
    array.get()[0] = result;
    Table<DoubleArray> rmseArrTable =
      new Table<>(0, new DoubleArrPlus());
    rmseArrTable.addPartition(
      new Partition<DoubleArray>(0, array));
    this.allreduce("ccd",
      "allreduce-test-rmse-" + name,
      rmseArrTable);
    testRMSE =
      rmseArrTable.getPartition(0).get().get()[0];
    rmseArrTable.release();
    testRMSE = Math
      .sqrt(testRMSE / (double) totalNumTestV);
    LOG.info("Test RMSE " + testRMSE);
  }

  private double computeTestRMSE(
    Rotator<DoubleArray> wRotator,
    Rotator<DoubleArray> hRotator,
    DynamicScheduler<VRowCol, Object, TestRMSETask> rmseCompute,
    Int2ObjectOpenHashMap<VRowCol> testVHListMap,
    int numWorkers) throws InterruptedException {
    double result = 0.0;
    for (int j = 0; j < numWorkers; j++) {
      for (int k = 0; k < numModelSlices; k++) {
        List<Partition<DoubleArray>> wList =
          wRotator.getSplitMap(k)[0];
        List<Partition<DoubleArray>> hList =
          hRotator.getSplitMap(k)[0];
        for (TestRMSETask task : rmseCompute
          .getTasks()) {
          task.setInit(false);
          task.setRMSE(false);
          if (j == 0 && k == 0) {
            task.setInit(true);
          }
          if (j == (numWorkers - 1)
            && k == (numModelSlices - 1)) {
            task.setRMSE(true);
          }
          task.setWHPartitionLists(wList, hList);
        }
        rmseCompute
          .submitAll(testVHListMap.values());
        while (rmseCompute.hasOutput()) {
          rmseCompute.waitForOutput();
        }
        wRotator.rotate(k);
        hRotator.rotate(k);
      }
    }
    for (TestRMSETask task : rmseCompute
      .getTasks()) {
      result += task.getRMSE();
    }
    return result;
  }
}