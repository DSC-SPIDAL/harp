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

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.schdynamic.DynamicScheduler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class WDAMDSMapper extends
  CollectiveMapper<String, String, Object, Object> {
  // private int numMapTasks;
  // private String xFile;
  private String xOutFile;
  private String idsFile;
  private String labelsFile;
  private double threshold;
  private int d;
  private double alpha;
  private int n;
  private int cgIter;
  private int cgRealIter;
  private int maxNumThreads;
  private Configuration conf;
  private int totalPartitions;
  // Metrics;
  private long totalInitXTime;
  private long totalInitXCount;
  private long totalBCTime;
  private long totalBCCount;
  private long totalMMTime;
  private long totalMMCount;
  private long totalStressTime;
  private long totalStressCount;
  private long totalInnerProductTime;
  private long totalInnerProductCount;
  private long totalAllgatherTime;
  private long totalAllgatherSyncTime;
  private long totalAllgatherCount;
  private long totalAllgatherValTime;
  private long totalAllgatherValSyncTime;
  private long totalAllgatherValCount;

  public void setup(Context context) {
    conf = context.getConfiguration();
    // numMapTasks =
    // conf.getInt(MDSConstants.NUM_MAPS, 1);
    // xFile =
    // conf.get(MDSConstants.X_FILE_PATH, "");
    xOutFile =
      conf.get(MDSConstants.X_OUT_FILE_PATH, "");
    idsFile = conf.get(MDSConstants.IDS_FILE, "");
    labelsFile =
      conf.get(MDSConstants.LABELS_FILE, "");
    threshold =
      conf.getDouble(MDSConstants.THRESHOLD, 1);
    d = conf.getInt(MDSConstants.D, 1);
    alpha =
      conf.getDouble(MDSConstants.ALPHA, 0.1);
    n = conf.getInt(MDSConstants.N, 1);
    cgIter = conf.getInt(MDSConstants.CG_ITER, 1);
    maxNumThreads =
      conf.getInt(MDSConstants.NUM_THREADS, 8);
    LOG.info(
      "Max number of threads: " + maxNumThreads);
    cgRealIter = 0;

    totalInitXTime = 0;
    totalInitXCount = 0;
    totalBCTime = 0;
    totalBCCount = 0;
    totalMMTime = 0;
    totalMMCount = 0;
    totalStressTime = 0;
    totalStressCount = 0;
    totalInnerProductTime = 0;
    totalInnerProductCount = 0;
    totalAllgatherTime = 0;
    totalAllgatherSyncTime = 0;
    totalAllgatherCount = 0;
    totalAllgatherValTime = 0;
    totalAllgatherValSyncTime = 0;
    totalAllgatherValCount = 0;
  }

  @Override
  public void mapCollective(KeyValReader reader,
    Context context)
    throws IOException, InterruptedException {
    LOG.info("Start collective mapper.");
    LinkedList<String> partitionFiles =
      new LinkedList<>();
    // There should be only one file
    while (reader.nextKeyValue()) {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info(
        "Key: " + key + ", Value: " + value);
      partitionFiles.add(value);
    }
    context.progress();
    try {
      runMDS(partitionFiles.getFirst(), context);
    } catch (Exception e) {
      LOG.error("Fail to run MDS.", e);
      throw new IOException(e);
    }
  }

  public void runMDS(String partitionFile,
    Context context) throws Exception {
    double startTime = System.currentTimeMillis();
    Map<Integer, RowData> rowDataMap =
      loadData(partitionFile, context);
    double endTime = System.currentTimeMillis();
    LOG.info("Load data took ="
      + (endTime - startTime) / 1000
      + " Seconds.");
    logMemUsage();
    logGCTime();
    context.progress();
    // --------------------------------------------------
    // Perform Initial Calculations
    // Average distance calculation
    double[] avgs =
      calculateAvgDistances(rowDataMap);
    // Print average distances
    double div =
      (double) n * (double) n - (double) n;
    double avgOrigDistance = avgs[0] / div;
    double sumOrigDistanceSquare = avgs[1];
    // double avgOrigDistanceSquare = avgs[1] /
    // div;
    double maxOrigDistance = avgs[2];
    LOG.info("N: " + n + ", AvgOrgDistance: "
      + avgOrigDistance
      + ", SumSquareOrgDistance: "
      + sumOrigDistanceSquare
      + ", MaxOrigDistance: " + maxOrigDistance);
    // ------------------------------------------------
    // Create X and allgather X
    Table<DoubleArray> xTable =
      initializeX(d, rowDataMap);
    // Create array partitions
    Partition<DoubleArray>[] xPartitions =
      getPartitionArrayFromXTable(xTable);
    LOG.info("Initial X is generated.");
    // -----------------------------------------------
    // Calculate initial stress value
    double tCur = 0.0;
    double preStress = calculateStress(
      "calc-initial-stress", rowDataMap,
      xPartitions, tCur, sumOrigDistanceSquare);
    LOG.info("Initial Stress: " + preStress);
    // -----------------------------------------------
    // Starting value
    double diffStress = 10.0 * threshold;
    double stress = 0.0;
    double qoR1 = 0.0;
    double qoR2 = 0.0;
    double tMax =
      maxOrigDistance / Math.sqrt(2.0 * d);
    double tMin =
      (0.01 * tMax < 0.01) ? 0.01 * tMax : 0.01;
    tCur = alpha * tMax;
    endTime = System.currentTimeMillis();
    LOG.info("Upto the loop took ="
      + (endTime - startTime) / 1000
      + " Seconds.");
    // -----------------------------------------------
    int iter = 0;
    int smacofRealIter = 0;
    while (tCur > tMin) {
      preStress = calculateStress(
        "calc-pre-stress" + smacofRealIter,
        rowDataMap, xPartitions, tCur,
        sumOrigDistanceSquare);
      diffStress = threshold + 1.0;
      LOG.info("###############################");
      LOG.info("# T_Cur = " + tCur);
      LOG.info("###############################");
      while (diffStress >= threshold) {
        Table<DoubleArray> bcTable = calculateBC(
          rowDataMap, xPartitions, tCur);
        // Calculate new X
        conjugateGradient(rowDataMap, bcTable,
          xPartitions);
        // Release bc
        bcTable.release();
        bcTable = null;
        stress = calculateStress(
          "calc-stress" + smacofRealIter,
          rowDataMap, xPartitions, tCur,
          sumOrigDistanceSquare);
        diffStress = preStress - stress;
        iter++;
        smacofRealIter++;
        if (iter == 1) {
          LOG.info("Iteration ## " + iter
            + " completed. " + threshold + " "
            + diffStress + " " + stress);
          context.progress();
        }
        if (iter % 10 == 0) {
          LOG.info("Iteration ## " + iter
            + " completed. " + threshold
            + " preStress " + preStress
            + " diffStress " + diffStress
            + " stress " + stress);
        }
        if (iter % 100 == 0) {
          logMemUsage();
          logGCTime();
        }
        if (iter >= MDSConstants.MAX_ITER) {
          break;
        }
        preStress = stress;
      }
      LOG.info("ITERATION ## " + iter
        + " completed. " + " diffStress "
        + diffStress + " stress " + stress);
      context.progress();
      tCur *= alpha;
      iter = 0;
    }
    // T == 0
    tCur = 0.0;
    iter = 0;
    preStress = calculateStress(
      "calc-pre-stress" + smacofRealIter,
      rowDataMap, xPartitions, tCur,
      sumOrigDistanceSquare);
    diffStress = threshold + 1.0;
    LOG.info("%%%%%%%%%%%%%%%%%%%%%%");
    LOG.info("% T_Cur = " + tCur);
    LOG.info("%%%%%%%%%%%%%%%%%%%%%%");
    while (diffStress > threshold) {
      Table<DoubleArray> bcTable = calculateBC(
        rowDataMap, xPartitions, tCur);
      // LOG.info("BC end");
      conjugateGradient(rowDataMap, bcTable,
        xPartitions);
      // LOG.info("CG end");
      // Release bc
      bcTable.release();
      bcTable = null;
      stress = calculateStress(
        "calc-stress" + smacofRealIter,
        rowDataMap, xPartitions, tCur,
        sumOrigDistanceSquare);
      // LOG.info("Stress end");
      diffStress = preStress - stress;
      iter++;
      smacofRealIter++;
      if (iter % 10 == 0) {
        LOG.info("Iteration ## " + iter
          + " completed. " + threshold + " "
          + diffStress + " " + stress);
        context.progress();
      }
      if (iter % 100 == 0) {
        logMemUsage();
        logGCTime();
      }
      // Probably I need to break here
      if (iter >= MDSConstants.MAX_ITER) {
        break;
      }
      preStress = stress;
    }
    LOG.info("ITERATION ## " + iter
      + " completed. " + threshold + " "
      + diffStress + " " + stress);
    qoR1 = stress / (n * (n - 1) / 2);
    qoR2 =
      qoR1 / (avgOrigDistance * avgOrigDistance);
    LOG.info("Normalize1 = " + qoR1
      + " Normalize2 = " + qoR2);
    LOG.info(
      "Average of Delta(original distance) = "
        + avgOrigDistance);
    endTime = System.currentTimeMillis();
    double finalStress = calculateStress(
      "calc-final-stress" + smacofRealIter,
      rowDataMap, xPartitions, tCur,
      sumOrigDistanceSquare);
    // Store X file
    if (labelsFile.endsWith("NoLabel")) {
      XFileUtil.storeXOnMaster(conf, xTable, d,
        xOutFile, this.isMaster());
    } else {
      XFileUtil.storeXOnMaster(conf, xTable, d,
        xOutFile, this.isMaster(), labelsFile);
    }
    xTable.release();
    xTable = null;
    xPartitions = null;
    LOG.info(
      "===================================================");
    LOG.info("CG REAL ITER:" + cgRealIter);
    LOG.info(
      "SMACOF REAL ITER: " + smacofRealIter);
    LOG.info("Init X Time " + totalInitXTime);
    LOG.info("Init X  Count " + totalInitXCount);
    LOG.info("Stress Time " + totalStressTime);
    LOG.info("Stress Count " + totalStressCount);
    LOG.info("BC Time " + totalBCTime);
    LOG.info("BC Count " + totalBCCount);
    LOG.info("MM Time " + totalMMTime);
    LOG.info("MM Count " + totalMMCount);
    LOG.info("Inner Product Time "
      + totalInnerProductTime);
    LOG.info("Inner Product Count "
      + totalInnerProductCount);
    LOG.info(
      "Allgather Time " + totalAllgatherTime);
    LOG.info("Allgather Sync Time "
      + totalAllgatherSyncTime);
    LOG.info(
      "Allgather Count " + totalAllgatherCount);
    LOG.info("Allgather Val Time "
      + totalAllgatherValTime);
    LOG.info("Allgather Val Sync Time "
      + totalAllgatherValSyncTime);
    LOG.info("Allgather Val Count "
      + totalAllgatherValCount);
    LOG.info("For CG iter: "
      + ((double) cgRealIter
        / (double) smacofRealIter)
      + "\tFinal Result is:\t" + finalStress
      + "\t" + (endTime - startTime) / 1000
      + " seconds.");
    LOG.info(
      "===================================================");
  }

  private Map<Integer, RowData> loadData(
    String partitionFile, Context context)
    throws IOException {
    // Read partition file
    // check DataGen.generatePartitionFiles
    // to see how the partition files are
    // generated
    LOG.info("Reading the partition file: "
      + partitionFile);
    HashMap<Integer, RowData> rowDataMap =
      new HashMap<>();
    totalPartitions =
      DataFileUtil.readPartitionFile(
        partitionFile, idsFile, conf, rowDataMap);
    LOG.info(
      "Total partitions: " + totalPartitions);
    // Load data
    // doTasks(rowDataList, "data-load-task",
    // new DataLoadTask(context), maxNumThreads);
    LinkedList<DataLoadTask> dataLoadTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      dataLoadTasks
        .add(new DataLoadTask(context));
    }
    DynamicScheduler<RowData, Object, DataLoadTask> compute =
      new DynamicScheduler<>(dataLoadTasks);
    for (RowData rowData : rowDataMap.values()) {
      LOG.info(rowData.height + " "
        + rowData.width + " " + rowData.row + " "
        + rowData.rowOffset);
      LOG.info(rowData.distPath);
      LOG.info(rowData.weightPath);
      LOG.info(rowData.vPath);
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    while (compute.hasOutput()) {
      compute.waitForOutput();
    }
    return rowDataMap;
  }

  private double[] calculateAvgDistances(
    Map<Integer, RowData> rowDataMap)
    throws Exception {
    LinkedList<AvgDistCalcTask> avgDistCalcTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      avgDistCalcTasks.add(new AvgDistCalcTask());
    }
    DynamicScheduler<RowData, DoubleArray, AvgDistCalcTask> compute =
      new DynamicScheduler<>(avgDistCalcTasks);
    for (RowData rowData : rowDataMap.values()) {
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    // Create table
    Table<DoubleArray> arrTable =
      new Table<>(0, new AvgDistArrCombiner());
    while (compute.hasOutput()) {
      DoubleArray output =
        compute.waitForOutput();
      if (output != null) {
        PartitionStatus status =
          arrTable.addPartition(
            new Partition<DoubleArray>(0,
              output));
        if (status != PartitionStatus.ADDED) {
          output.release();
        }
      }
    }
    // avgDist is all-gathered
    allreduceVal("wda-mds",
      "calculateAvgDistances", arrTable);
    DoubleArray array =
      arrTable.getPartition(0).get();
    double[] avgs = new double[array.size()];
    System.arraycopy(array.get(), array.start(),
      avgs, 0, avgs.length);
    arrTable.release();
    return avgs;
  }

  private Table<DoubleArray> initializeX(int d,
    Map<Integer, RowData> rowDataMap)
    throws Exception {
    long startTime = System.currentTimeMillis();
    LinkedList<XInitializeTask> xInitializeTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      xInitializeTasks
        .add(new XInitializeTask(d));
    }
    DynamicScheduler<RowData, Partition<DoubleArray>, XInitializeTask> compute =
      new DynamicScheduler<>(xInitializeTasks);
    for (RowData rowData : rowDataMap.values()) {
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    Table<DoubleArray> xTable =
      new Table<>(0, new DoubleArrPlus());
    while (compute.hasOutput()) {
      Partition<DoubleArray> xPartition =
        compute.waitForOutput();
      if (xPartition != null) {
        PartitionStatus status =
          xTable.addPartition(xPartition);
        if (status != PartitionStatus.ADDED) {
          xPartition.release();
        }
      }
    }
    long endTime = System.currentTimeMillis();
    totalInitXTime += (endTime - startTime);
    totalInitXCount += 1;
    startTime = System.nanoTime();
    allgather("wda-mds", "initialize-x", xTable);
    endTime = System.nanoTime();
    totalAllgatherTime += (endTime - startTime);
    totalAllgatherCount += 1;
    if (xTable
      .getNumPartitions() != totalPartitions) {
      throw new Exception(
        "Fail to initialize X.");
    }
    return xTable;
  }

  private double calculateStress(String opName,
    Map<Integer, RowData> rowDataMap,
    Partition<DoubleArray>[] xPartitions,
    double tCur, double sumOrigDistanceSquare)
    throws Exception {
    long startTime = System.currentTimeMillis();
    LinkedList<StressCalcTask> stressCalcTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      stressCalcTasks.add(
        new StressCalcTask(xPartitions, d, tCur));
    }
    DynamicScheduler<RowData, DoubleArray, StressCalcTask> compute =
      new DynamicScheduler<>(stressCalcTasks);
    for (RowData rowData : rowDataMap.values()) {
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    Table<DoubleArray> arrTable =
      new Table<>(0, new DoubleArrPlus());
    while (compute.hasOutput()) {
      DoubleArray output =
        compute.waitForOutput();
      if (output != null) {
        PartitionStatus status =
          arrTable.addPartition(
            new Partition<DoubleArray>(0,
              output));
        if (status != PartitionStatus.ADDED) {
          output.release();
        }
      }
    }
    long endTime = System.currentTimeMillis();
    totalStressTime += (endTime - startTime);
    totalStressCount += 1;
    allreduceVal("wda-mds", opName, arrTable);
    double stressVal =
      arrTable.getPartition(0).get().get()[0]
        / sumOrigDistanceSquare;
    arrTable.release();
    return stressVal;
  }

  private <P extends Simple> void allreduceVal(
    String contextName, String opName,
    Table<P> arrTable) throws Exception {
    long startTime = System.nanoTime();
    this.allreduce(contextName, opName, arrTable);
    long endTime = System.nanoTime();
    totalAllgatherValTime +=
      (endTime - startTime);
    totalAllgatherValCount += 1;
  }

  private Table<DoubleArray> calculateBC(
    Map<Integer, RowData> rowDataMap,
    Partition<DoubleArray>[] xPartitions,
    double tCur) {
    long startTime = System.currentTimeMillis();
    LinkedList<BCCalcTask> calcBCTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      calcBCTasks.add(
        new BCCalcTask(xPartitions, d, tCur));
    }
    DynamicScheduler<RowData, Partition<DoubleArray>, BCCalcTask> compute =
      new DynamicScheduler<>(calcBCTasks);
    for (RowData rowData : rowDataMap.values()) {
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    Table<DoubleArray> bcTable =
      new Table<>(0, new DoubleArrPlus());
    while (compute.hasOutput()) {
      Partition<DoubleArray> output =
        compute.waitForOutput();
      if (output != null) {
        bcTable.addPartition(output);
      }
    }
    long endTime = System.currentTimeMillis();
    totalBCTime += (endTime - startTime);
    totalBCCount += 1;
    return bcTable;
  }

  private void conjugateGradient(
    Map<Integer, RowData> rowDataMap,
    Table<DoubleArray> pTable,
    Partition<DoubleArray>[] xPartitions)
    throws Exception {
    // pList was called bcList in calculateBC
    // Multiply X with V data, get r
    // r is distributed, create rTable
    Table<DoubleArray> rTable =
      calculateMM(rowDataMap, xPartitions);
    // Update p and r
    // r and p both should be the same as x in
    // size and partitioning
    for (Partition<DoubleArray> p : pTable
      .getPartitions()) {
      double[] ps = p.get().get();
      double[] rs =
        rTable.getPartition(p.id()).get().get();
      for (int i = 0; i < p.get().size(); i++) {
        ps[i] -= rs[i];
        rs[i] = ps[i];
      }
    }
    // r is distributed
    // calculate rTr needs allgather partial rTr
    double rTr = calculateInnerProduct(
      "inner-product-0-" + cgRealIter, rTable,
      null);
    // LOG.info("rTr " + rTr);
    int cgCount = 0;
    while (cgCount < cgIter) {
      // p is distributed, to do matrix
      // multiplication, we need to allgather p.
      // p should be partitioned the same as x.
      // Note that before allgather pTable holds
      // the local partitions, after that, pTable
      // holds all the partitions.
      long startTime = System.nanoTime();
      allgather("wda-mds", "cg-" + cgRealIter,
        pTable);
      long endTime = System.nanoTime();
      totalAllgatherTime += (endTime - startTime);
      totalAllgatherCount += 1;
      Partition<DoubleArray>[] pPartitions =
        getPartitionArrayFromXTable(pTable);
      Table<DoubleArray> apTable =
        calculateMM(rowDataMap, pPartitions);
      // LOG.info("mm apTable");
      // Calculate alpha
      // ap is distributed, we use ap to find
      // related p (based on row ID)
      double ip = calculateInnerProduct(
        "inner-product-1-" + cgRealIter, apTable,
        pTable);
      // LOG.info("ip for alpha " + ip);
      double alpha = rTr / ip;
      // LOG.info("alpha " + alpha);
      // Now X is duplicated on all workers
      // do normalizedValue directly
      double sum1 =
        calculateNormalizedVal(xPartitions);
      // update X_i to X_i+1
      // p is duplicated on all the workers as x
      // update X directly
      updateX(xPartitions, pPartitions, alpha);
      double sum2 =
        calculateNormalizedVal(xPartitions);
      // LOG.info("sum1 " + sum1);
      // LOG.info("sum2 " + sum2);
      // Release p in pTable not in local
      Iterator<Partition<DoubleArray>> pTableIterator =
        pTable.getPartitions().iterator();
      while (pTableIterator.hasNext()) {
        Partition<DoubleArray> p =
          pTableIterator.next();
        if (!rowDataMap.containsKey(p.id())) {
          pTableIterator.remove();
          p.release();
        }
      }
      pPartitions = null;
      // Temporarily disable this
      if (Math.abs(sum2 - sum1) < 10E-3) {
        // The final iteration
        // Release before exiting from the loop
        // Release ap
        apTable.release();
        apTable = null;
        cgCount++;
        cgRealIter++;
        break;
      }
      // update r_i to r_i+1
      // r is distributed, ap is distributed
      // but the distribution of r should match
      // with the distribution of ap
      // (both are based on V's distribution)
      for (Partition<DoubleArray> ap : apTable
        .getPartitions()) {
        double[] aps = ap.get().get();
        double[] rs = rTable.getPartition(ap.id())
          .get().get();
        for (int i = 0; i < ap.get()
          .size(); i++) {
          rs[i] = rs[i] - alpha * aps[i];
        }
      }
      // Calculate beta
      double rTr1 = calculateInnerProduct(
        "inner-product-2-" + cgRealIter, rTable,
        null);
      // LOG.info("rTr1 " + rTr1);
      double beta = rTr1 / rTr;
      // LOG.info("beta " + beta);
      rTr = rTr1;
      // Update p_i to p_i+1
      // We only update p which are the
      // local partitions
      for (Partition<DoubleArray> p : pTable
        .getPartitions()) {
        double[] ps = p.get().get();
        double[] rs =
          rTable.getPartition(p.id()).get().get();
        for (int i = 0; i < p.get().size(); i++) {
          ps[i] = rs[i] + beta * ps[i];
        }
      }
      // Release ap
      apTable.release();
      apTable = null;
      cgCount++;
      cgRealIter++;
    }
    // Release rTable
    rTable.release();
    rTable = null;
    // Release pTable
    // Because pTable is a parameter, arrays are
    // released outside
    pTable = null;
  }

  private Table<DoubleArray> calculateMM(
    Map<Integer, RowData> rowDataMap,
    Partition<DoubleArray>[] xPartitions) {
    // Do matrix multiplication
    // The result is same as X in size
    long startTime = System.currentTimeMillis();
    LinkedList<MMCalcTask> mmCalcTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      mmCalcTasks
        .add(new MMCalcTask(xPartitions, d));
    }
    DynamicScheduler<RowData, Partition<DoubleArray>, MMCalcTask> compute =
      new DynamicScheduler<>(mmCalcTasks);
    for (RowData rowData : rowDataMap.values()) {
      compute.submit(rowData);
    }
    compute.start();
    compute.stop();
    Table<DoubleArray> mmTable =
      new Table<>(0, new DoubleArrPlus());
    while (compute.hasOutput()) {
      Partition<DoubleArray> output =
        compute.waitForOutput();
      if (output != null) {
        mmTable.addPartition(output);
      }
    }
    long endTime = System.currentTimeMillis();
    totalMMTime += (endTime - startTime);
    totalMMCount += 1;
    return mmTable;
  }

  private double calculateInnerProduct(
    String opName, Table<DoubleArray> table,
    Table<DoubleArray> refTable)
    throws Exception {
    long startTime = System.currentTimeMillis();
    LinkedList<InnerProductCalcTask> innerProductTasks =
      new LinkedList<>();
    for (int i = 0; i < maxNumThreads; i++) {
      innerProductTasks
        .add(new InnerProductCalcTask(refTable));
    }
    DynamicScheduler<Partition<DoubleArray>, DoubleArray, InnerProductCalcTask> compute =
      new DynamicScheduler<>(innerProductTasks);
    for (Partition<DoubleArray> partition : table
      .getPartitions()) {
      compute.submit(partition);
    }
    compute.start();
    compute.stop();
    Table<DoubleArray> outputTable =
      new Table<>(0, new DoubleArrPlus());
    while (compute.hasOutput()) {
      DoubleArray output =
        compute.waitForOutput();
      outputTable.addPartition(
        new Partition<DoubleArray>(0, output));
    }
    long endTime = System.currentTimeMillis();
    totalInnerProductTime +=
      (endTime - startTime);
    totalInnerProductCount += 1;
    allreduceVal("wda-mds", opName, outputTable);
    double productVal =
      outputTable.getPartition(0).get().get()[0];
    outputTable.release();
    return productVal;
  }

  private Partition<DoubleArray>[]
    getPartitionArrayFromXTable(
      Table<DoubleArray> xTable)
      throws Exception {
    Partition<DoubleArray>[] xPartitions =
      new Partition[xTable.getNumPartitions()];
    for (Partition<DoubleArray> xPartition : xTable
      .getPartitions()) {
      xPartitions[xPartition.id()] = xPartition;
    }
    return xPartitions;
  }

  private static double calculateNormalizedVal(
    Partition<DoubleArray>[] xPartitions) {
    double sum = 0;
    DoubleArray array = null;
    double[] doubles = null;
    for (Partition<DoubleArray> xPartition : xPartitions) {
      array = xPartition.get();
      doubles = array.get();
      for (int i = 0; i < array.size(); i++)
        sum += doubles[i] * doubles[i];
    }
    return Math.sqrt(sum);
  }

  private void updateX(
    Partition<DoubleArray>[] xPartitions,
    Partition<DoubleArray>[] pPartitions,
    double alpha) {
    DoubleArray xArray;
    double[] xDoubles;
    DoubleArray pArray;
    double[] pDoubles;
    for (int i = 0; i < xPartitions.length; i++) {
      xArray = xPartitions[i].get();
      xDoubles = xArray.get();
      pArray = pPartitions[i].get();
      pDoubles = pArray.get();
      for (int j = 0; j < xArray.size(); j++)
        xDoubles[j] += alpha * pDoubles[j];
    }
  }
}
