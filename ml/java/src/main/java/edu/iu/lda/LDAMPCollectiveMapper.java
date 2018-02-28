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

package edu.iu.lda;

import cc.mallet.types.Dirichlet;
import edu.iu.dymoro.Rotator;
import edu.iu.dymoro.Scheduler;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.LongArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class LDAMPCollectiveMapper extends
  CollectiveMapper<String, String, Object, Object> {
  private int numTopics;
  private double alpha;
  private double beta;
  private int numIterations;
  private int numThreads;
  private double scheduleRatio;
  private boolean enableTuning;
  private int minBound;
  private int maxBound;
  private long time;
  private boolean hasOverTrained;
  private int lastUnderTrainIte;
  private int breakPeriod;
  private String modelDirPath;
  private boolean printModel;
  private int printInterval;
  private int freeInterval;
  private int numModelSlices;
  private long computeTime;
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
    numTopics = configuration
      .getInt(Constants.NUM_TOPICS, 100);
    alpha = configuration
      .getDouble(Constants.ALPHA, 0.1);
    beta = configuration.getDouble(Constants.BETA,
      0.001);
    numIterations = configuration
      .getInt(Constants.NUM_ITERATIONS, 100);
    numThreads = configuration
      .getInt(Constants.NUM_THREADS, 16);
    scheduleRatio = configuration
      .getDouble(Constants.SCHEDULE_RATIO, 2.0);
    minBound =
      configuration.getInt(Constants.MIN_BOUND,
        Constants.TRAIN_MIN_THRESHOLD);
    maxBound =
      configuration.getInt(Constants.MAX_BOUND,
        Constants.TRAIN_MAX_THRESHOLD);
    if (minBound <= 0 || minBound > 100) {
      minBound = Constants.TRAIN_MIN_THRESHOLD;
    }
    if (maxBound <= 0 || maxBound > 100) {
      maxBound = Constants.TRAIN_MAX_THRESHOLD;
    }
    if (maxBound < minBound) {
      maxBound = minBound;
    }
    if (maxBound == 100) {
      minBound = 100;
      enableTuning = false;
    } else {
      enableTuning = true;
    }
    time = enableTuning ? 1000L : 1000000000L;
    hasOverTrained = false;
    lastUnderTrainIte = 0;
    breakPeriod = 0;
    modelDirPath =
      configuration.get(Constants.MODEL_DIR, "");
    printModel = configuration
      .getBoolean(Constants.PRINT_MODEL, false);
    printInterval = 10;
    freeInterval = 10;
    numModelSlices = 2;
    computeTime = 0L;
    waitTime = 0L;
    long endTime = System.currentTimeMillis();
    LOG.info(
      "config (ms): " + (endTime - startTime));
    LOG.info("Num Topics " + numTopics);
    LOG.info("Alpha " + alpha);
    LOG.info("Beta " + beta);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("numThreads\\scheduleRaito "
      + numThreads + "\\" + scheduleRatio);
    LOG.info("enableTuning\\Time\\Bounds "
      + enableTuning + "\\" + time + "\\"
      + minBound + "\\" + maxBound);
    LOG.info("Model Dir Path " + modelDirPath);
    LOG.info("Print Model " + printModel);
    LOG.info("Model Slices " + numModelSlices);
    LOG.info("Container Memory " + configuration
      .get("mapreduce.map.collective.memory.mb"));
    LOG.info("Java Memory " + configuration
      .get("mapreduce.map.collective.java.opts"));
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    LinkedList<String> docFiles =
      getDocFiles(reader);
    try {
      runLDA(docFiles, context.getConfiguration(),
        context);
    } catch (Exception e) {
      LOG.error("Fail to run LDA.", e);
    }
    LOG.info("Total execution time: "
      + (System.currentTimeMillis() - startTime));
  }

  private LinkedList<String>
    getDocFiles(final KeyValReader reader)
      throws IOException, InterruptedException {
    final LinkedList<String> docFiles =
      new LinkedList<>();
    while (reader.nextKeyValue()) {
      final String value =
        reader.getCurrentValue();
      LOG.info("File: " + value);
      docFiles.add(value);
    }
    return docFiles;
  }

  private void runLDA(
    final LinkedList<String> vFilePaths,
    final Configuration configuration,
    final Context context) throws Exception {
    // Load training data
    Int2ObjectOpenHashMap<DocWord> vDocMap =
      new Int2ObjectOpenHashMap<>();
    final Int2ObjectOpenHashMap<String> docIDMap =
      new Int2ObjectOpenHashMap<>();
    final int maxDocIDw =
      LDAUtil.load(vFilePaths, numThreads,
        configuration, vDocMap, docIDMap);
    LOG.info("Max Doc ID on Worker " + maxDocIDw);
    // ---------------------------------------------
    // Create vHMap and W model
    int numSplits = (int) Math
      .ceil(Math.sqrt((double) numThreads
        * (double) numThreads * scheduleRatio));
    final int numRowSplits = numSplits;
    final int numColSplits = numSplits;
    LOG.info("numRowSplits: " + numRowSplits
      + " numColSplits: " + numColSplits);
    // D model and W model
    final LongArrayList[] dMap =
      new LongArrayList[maxDocIDw + 1];
    Table<TopicCountMap> wordTable =
      new Table<>(0, new TopicCountMapCombiner());
    // vDMap grouped to splits based on row IDs
    final Int2ObjectOpenHashMap<DocWord>[] vDWMap =
      new Int2ObjectOpenHashMap[numRowSplits];
    LDAUtil.createDWSplitAndModel(vDocMap, dMap,
      wordTable, vDWMap, numRowSplits, numTopics,
      numThreads);
    vDocMap = null;
    this.freeMemory();
    this.freeConn();
    System.gc();
    // Create global W model
    Table<TopicCountList>[] wordTableMap =
      new Table[numModelSlices];
    final int vocabularySize =
      LDAUtil.createWordModel(wordTableMap,
        numModelSlices, wordTable, this);
    wordTable = null;
    printWordModelSize(wordTableMap);
    printDocModelSize(dMap);
    sortTopicCounts(wordTableMap, dMap);
    // Count the training points per worker
    final long numTokens = getNumTokens(vDWMap);
    final int selfID = this.getSelfID();
    final int numWorkers = this.getNumWorkers();
    final long totalTokens = countWorkerTokenSum(
      numTokens, selfID, numWorkers);
    this.freeMemory();
    this.freeConn();
    System.gc();
    // ----------------------------------------------
    // Calculate topic sums
    final int[] topicSums = new int[numTopics];
    getTopicSums("get-initial-topics-word-sum",
      topicSums, wordTableMap);
    printNumTokens(topicSums);
    printLikelihood(wordTableMap, numWorkers, 0,
      topicSums, vocabularySize);
    final double[] commons =
      new double[numTopics];
    final double[] rCoeffDistr =
      new double[numTopics];
    double rCoeffSum =
      calculateCommons(vocabularySize, topicSums,
        commons, rCoeffDistr);
    // Create scheduler and rotator
    boolean randomModelSplit = false;
    int[] order = null;
    Rotator<TopicCountList> rotator =
      new Rotator<>(wordTableMap, numColSplits,
        randomModelSplit, this, order, "lda");
    rotator.start();
    List<LDAMPTask> ldaTasks =
      new ObjectArrayList<>();
    for (int i = 0; i < numThreads; i++) {
      LDAMPTask task =
        new LDAMPTask(dMap, numTopics, alpha,
          beta, commons, rCoeffDistr);
      task.setRCoeffSum(rCoeffSum);
      ldaTasks.add(task);
    }
    Scheduler<Int2ObjectOpenHashMap<DocWord>, TopicCountList, LDAMPTask> scheduler =
      new Scheduler<>(numRowSplits, numColSplits,
        vDWMap, time, ldaTasks);
    // -----------------------------------------
    // For iteration
    for (int i = 1; i <= numIterations; i++) {
      long iteStart = System.currentTimeMillis();
      for (int j = 0; j < numWorkers; j++) {
        for (int k = 0; k < numModelSlices; k++) {
          long t1 = System.currentTimeMillis();
          List<Partition<TopicCountList>>[] wMap =
            rotator.getSplitMap(k);
          long t2 = System.currentTimeMillis();
          scheduler.schedule(wMap, false);
          long t3 = System.currentTimeMillis();
          rotator.rotate(k);
          waitTime += (t2 - t1);
          computeTime += (t3 - t2);
        }
      }
      long itePause1 = System.currentTimeMillis();
      rotator.pause();
      long itePause2 = System.currentTimeMillis();
      long numVTrained =
        scheduler.getNumVItemsTrained();
      if (enableTuning) {
        long newTime = time;
        // Stop adjust the timer,
        // if recently there was a under train
        if (lastUnderTrainIte == 0 || i
          - lastUnderTrainIte >= breakPeriod) {
          newTime = adjustMiniBatch(selfID,
            computeTime, numVTrained, totalTokens,
            i, time, minBound, maxBound,
            numWorkers, numModelSlices);
        }
        if (time != newTime) {
          // if (newTime < time) {
          // this.freeMemory();
          // }
          time = newTime;
          scheduler.setTimer(time);
          sortTopicCounts(wordTableMap, dMap);
        }
      }
      long itePause3 = System.currentTimeMillis();
      // Update topic sums, rCoeff distr and sum
      getTopicSums("get-topics-word-sum-" + i,
        topicSums, wordTableMap);
      rCoeffSum = calculateCommons(vocabularySize,
        topicSums, commons, rCoeffDistr);
      for (LDAMPTask ldaTask : ldaTasks) {
        ldaTask.setRCoeffSum(rCoeffSum);
      }
      long iteEnd = System.currentTimeMillis();
      int percentage =
        (int) Math.ceil((double) numVTrained
          / (double) numTokens * 100.0);
      LOG.info("Iteration " + i + ": "
        + (iteEnd - iteStart) + ", compute time: "
        + computeTime + ", misc: " + waitTime
        + "\\" + (itePause2 - itePause1) + "\\"
        + (itePause3 - itePause2) + "\\"
        + (iteEnd - itePause3) + ", numTokens: "
        + numVTrained + ", percentage(%): "
        + percentage);
      computeTime = 0L;
      waitTime = 0L;
      if (i % freeInterval == 0) {
        this.freeMemory();
      }
      if (i % printInterval == 0 || i == 1
        || i == numIterations) {
        // this.logMemUsage();
        // this.logGCTime();
        printNumTokens(topicSums);
        printWordModelSize(wordTableMap);
        printDocModelSize(dMap);
        double likelihood = 
          printLikelihood(wordTableMap, numWorkers,
          i, topicSums, vocabularySize);
        if (printModel
          && i % (printInterval * 10) == 0) {
          LOG.info("Start to print word model.");
          try {
            printWordTableMap(
              wordTableMap, modelDirPath
                + "/tmp_word_model/" + i + "/",
              selfID, configuration);
            // printDocMap(
            // dMap, docIDMap, modelDirPath
            // + "/tmp_doc_model/" + i + "/",
            // selfID, configuration);
            
            if (isMaster() && (i == numIterations)){
              saveLikelihood(likelihood, modelDirPath,
                selfID, configuration);
            }
          } catch (Exception e) {
            LOG.error("Fail to print model.", e);
          }
        }
        context.progress();
      }
      rotator.start();
    }
    scheduler.stop();
    rotator.stop();
  }

  private long getNumTokens(
    Int2ObjectOpenHashMap<DocWord>[] vDWMap) {
    long countV = 0L;
    for (int i = 0; i < vDWMap.length; i++) {
      Int2ObjectOpenHashMap<DocWord> vWMap =
        vDWMap[i];
      ObjectIterator<Int2ObjectMap.Entry<DocWord>> iterator =
        vWMap.int2ObjectEntrySet().fastIterator();
      for (; iterator.hasNext();) {
        DocWord docWord =
          iterator.next().getValue();
        // Number of tokens
        for (int k = 0; k < docWord.numV; k++) {
          countV += docWord.z[k].length;
        }
      }
    }
    return countV;
  }

  private long countWorkerTokenSum(long numTokens,
    int selfID, int numWorkers) {
    Table<LongArray> workerVSumTable =
      new Table<>(0, new LongArrPlus());
    LongArray array = LongArray.create(1, false);
    array.get()[0] = numTokens;
    workerVSumTable.addPartition(
      new Partition<>(selfID, array));
    this.allgather("lda", "allgather-vsum",
      workerVSumTable);
    long totalNumToken = 0;
    for (Partition<LongArray> partition : workerVSumTable
      .getPartitions()) {
      int workerID = partition.id();
      long numV = partition.get().get()[0];
      totalNumToken += numV;
      LOG.info(
        "Worker ID: " + workerID + " " + numV);
    }
    LOG.info(
      "Total number of tokens " + totalNumToken);
    return totalNumToken;
  }

  private void getTopicSums(String opName,
    int[] topicSums, LongArrayList[] dMap) {
    Arrays.fill(topicSums, 0);
    for (int i = 0; i < dMap.length; i++) {
      if (dMap[i] != null) {
        for (int j = 0; j < dMap[i].size(); j++) {
          long t = dMap[i].getLong(j);
          topicSums[(int) t] += (int) (t >>> 32);
        }
      }
    }
    Table<IntArray> table =
      new Table<>(0, new IntArrPlus());
    table.addPartition(new Partition<IntArray>(0,
      new IntArray(topicSums, 0, numTopics)));
    this.allreduce("lda", opName, table);
  }

  private void getTopicSums(String opName,
    int[] topicSums,
    Table<TopicCountList>[] wTableMap) {
    Arrays.fill(topicSums, 0);
    for (int i = 0; i < wTableMap.length; i++) {
      for (Partition<TopicCountList> partition : wTableMap[i]
        .getPartitions()) {
        LongArrayList list =
          partition.get().getTopicCount();
        for (int j = 0; j < list.size(); j++) {
          long t = list.getLong(j);
          topicSums[(int) t] += (int) (t >>> 32);
        }
      }
    }
    Table<IntArray> table =
      new Table<>(0, new IntArrPlus());
    table.addPartition(new Partition<IntArray>(0,
      new IntArray(topicSums, 0, numTopics)));
    this.allreduce("lda", opName, table);
  }

  private double calculateCommons(
    int vocabularySize, int[] topicSums,
    double[] commons, double[] rCoeffDistr) {
    double rCoeffSum = 0.0;
    for (int i = 0; i < numTopics; i++) {
      commons[i] = 1.0
        / (topicSums[i] + beta * vocabularySize);
      rCoeffDistr[i] = commons[i] * beta;
      rCoeffSum += rCoeffDistr[i];
    }
    return rCoeffSum;
  }

  private long adjustMiniBatch(int selfID,
    long computeTime, long numTokenTrained,
    long totalTokens, int iteration,
    long miniBatch, int minBound, int maxBound,
    int numWorkers, int modelSlices) {
    // Try to get worker ID
    // and related computation Time
    // and the percentage of
    // completion
    Table<LongArray> arrTable =
      new Table<>(0, new LongArrPlus());
    LongArray array = LongArray.create(2, false);
    array.get()[0] = computeTime;
    array.get()[1] = numTokenTrained;
    arrTable.addPartition(
      new Partition<>(selfID, array));
    array = null;
    this.allgather("lda",
      "allgather-compute-status-" + iteration,
      arrTable);
    long totalComputeTime = 0L;
    long totalTokensTrained = 0L;
    for (Partition<LongArray> partition : arrTable
      .getPartitions()) {
      long[] recvArr = partition.get().get();
      totalComputeTime += recvArr[0];
      totalTokensTrained += recvArr[1];
    }
    arrTable.release();
    arrTable = null;
    // must been the ceiling
    // make sure the minimum is not 0
    long predictedComputeTime =
      miniBatch * (long) numWorkers
        * (long) modelSlices * (long) numWorkers;
    // adjust the percentage
    int percentage =
      (int) Math.ceil(((double) totalTokensTrained
        * 100.0 / (double) totalTokens)
        * ((double) predictedComputeTime
          / (double) totalComputeTime));
    // real percentage is the percentage got in
    // execution
    int realPercentage =
      (int) Math.ceil((double) totalTokensTrained
        * 100.0 / (double) totalTokens);
    boolean overTrain = false;
    boolean underTrain = false;
    if (percentage > maxBound) {
      overTrain = true;
    }
    if (percentage < minBound) {
      underTrain = true;
    }
    long newMinibatch = miniBatch;
    if (overTrain) {
      if (!hasOverTrained) {
        hasOverTrained = true;
      }
      newMinibatch /= 2L;
    } else if (underTrain) {
      if (hasOverTrained) {
        lastUnderTrainIte = iteration;
        if (breakPeriod == 0) {
          breakPeriod = 1;
        } else {
          breakPeriod *= 2;
        }
      }
      newMinibatch *= 2L;
      int potential = percentage * 2;
      while (potential < minBound) {
        potential *= 2;
        newMinibatch *= 2L;
      }
    }
    if (newMinibatch != miniBatch) {
      LOG.info("Total trained tokens: "
        + totalTokensTrained + " percentage: "
        + percentage + " " + realPercentage
        + " new timer: " + newMinibatch);
    }
    return newMinibatch;
  }

  private void sortTopicCounts(
    Table<TopicCountList>[] wTableMap,
    LongArrayList[] docMap) {
    // long t1 = System.currentTimeMillis();
    LinkedList<SortTask> sortTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      sortTasks.add(new SortTask(numTopics));
    }
    DynamicScheduler<LongArrayList, Object, SortTask> sortCompute =
      new DynamicScheduler<>(sortTasks);
    // compute local partial likelihood
    for (int k = 0; k < numModelSlices; k++) {
      for (Partition<TopicCountList> partition : wTableMap[k]
        .getPartitions()) {
        sortCompute.submit(
          partition.get().getTopicCount());
      }
    }
    for (int i = 0; i < docMap.length; i++) {
      if (docMap[i] != null) {
        sortCompute.submit(docMap[i]);
      }
    }
    sortCompute.start();
    sortCompute.stop();
    // long t2 = System.currentTimeMillis();
    // LOG.info(
    // "Sort topic counts, took " + (t2 - t1));
  }

  private void printWordTableMap(
    Table<TopicCountList>[] wordTableMap,
    String folderPath, int selfID,
    Configuration congfiguration)
    throws IOException {
    FileSystem fs =
      FileSystem.get(congfiguration);
    Path folder = new Path(folderPath);
    if (!fs.exists(folder)) {
      fs.mkdirs(folder);
    }
    Path file =
      new Path(folderPath + "/" + selfID);
    PrintWriter writer =
      new PrintWriter(new BufferedWriter(
        new OutputStreamWriter(fs.create(file))));
    for (Table<TopicCountList> wTable : wordTableMap) {
      for (Partition<TopicCountList> wPartition : wTable
        .getPartitions()) {
        int wordID = wPartition.id();
        LongArrayList wRow =
          wPartition.get().getTopicCount();
        // Print word
        writer.print(wordID);
        // Print topic count
        for (int i = 0; i < wRow.size(); i++) {
          long t = wRow.getLong(i);
          writer.print(" " + (int) t + ":"
            + (int) (t >>> 32));
        }
        writer.println();
      }
    }
    writer.flush();
    writer.close();
  }

  private void printDocMap(LongArrayList[] docMap,
    Int2ObjectOpenHashMap<String> docIDMap,
    String folderPath, int selfID,
    Configuration congfiguration)
    throws IOException {
    FileSystem fs =
      FileSystem.get(congfiguration);
    Path folder = new Path(folderPath);
    if (!fs.exists(folder)) {
      fs.mkdirs(folder);
    }
    Path file =
      new Path(folderPath + "/" + selfID);
    PrintWriter writer =
      new PrintWriter(new BufferedWriter(
        new OutputStreamWriter(fs.create(file))));
    for (int i = 0; i < docMap.length; i++) {
      if (docMap[i] != null) {
        LongArrayList dRow = docMap[i];
        // Print real doc ID
        writer.print(docIDMap.get(i));
        // Print topic count
        for (int j = 0; j < dRow.size(); j++) {
          long t = dRow.getLong(j);
          writer.print(" " + (int) t + ":"
            + (int) (t >>> 32));
        }
        writer.println();
      }
    }
    writer.flush();
    writer.close();
  }

  private void printWordModelSize(
    Table<TopicCountList>[] wordTableMap) {
    long size = 0L;
    for (Table<TopicCountList> wTable : wordTableMap) {
      for (Partition<TopicCountList> partition : wTable
        .getPartitions()) {
        size +=
          partition.get().getTopicCount().size()
            * 8;
      }
    }
    LOG.info(
      "Word model size: " + size + " bytes");
  }

  private void
    printDocModelSize(LongArrayList[] docMap) {
    long size = 0L;
    for (int i = 0; i < docMap.length; i++) {
      if (docMap[i] != null) {
        size += (long) docMap[i].size() * 8L;
      }
    }
    LOG
      .info("Doc model size: " + size + " bytes");
  }

  private void printNumTokens(int[] topicSums) {
    long numTokens = 0L;
    for (int i = 0; i < topicSums.length; i++) {
      numTokens += topicSums[i];
    }
    LOG.info("Total Topic Sum " + numTokens);
  }

  private double printLikelihood(
    Table<TopicCountList>[] wTableMap,
    int numWorkers, int iteration,
    int[] topicSums, int vocabularySize) {
    LinkedList<CalcLikelihoodTask> calcLHTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      calcLHTasks.add(new CalcLikelihoodTask(
        numTopics, alpha, beta));
    }
    DynamicScheduler<Partition<TopicCountList>, Object, CalcLikelihoodTask> calcLHCompute =
      new DynamicScheduler<>(calcLHTasks);
    // compute local partial likelihood
    for (int k = 0; k < numModelSlices; k++) {
      calcLHCompute
        .submitAll(wTableMap[k].getPartitions());
    }
    calcLHCompute.start();
    calcLHCompute.stop();
    double likelihood = 0.0;
    for (CalcLikelihoodTask calcTask : calcLHCompute
      .getTasks()) {
      likelihood += calcTask.getLikelihood();
    }
    // all reduce to get the sum
    DoubleArray array =
      DoubleArray.create(1, false);
    array.get()[0] = likelihood;
    Table<DoubleArray> lhTable =
      new Table<>(0, new DoubleArrPlus());
    lhTable.addPartition(
      new Partition<DoubleArray>(0, array));
    this.allreduce("lda",
      "allreduce-likelihood-" + iteration,
      lhTable);
    likelihood =
      lhTable.getPartition(0).get().get()[0];
    lhTable.release();
    // the remain parts
    for (int topic =
      0; topic < numTopics; topic++) {
      likelihood -= Dirichlet
        .logGammaStirling((beta * vocabularySize)
          + topicSums[topic]);
    }
    // logGamma(|V|*beta) for every topic
    likelihood += Dirichlet.logGammaStirling(
      beta * vocabularySize) * numTopics;
    // output
    LOG.info("Iteration " + iteration
      + ", logLikelihood: " + likelihood);

    return likelihood;
  }

  private void saveLikelihood(
    double likelihood,
    String folderPath, int selfID,
    Configuration congfiguration)
    throws IOException {
    FileSystem fs =
      FileSystem.get(congfiguration);
    Path folder = new Path(folderPath);
    if (!fs.exists(folder)) {
      fs.mkdirs(folder);
    }
    Path file =
      new Path(folderPath + "/evaluation");
    PrintWriter writer =
      new PrintWriter(new BufferedWriter(
        new OutputStreamWriter(fs.create(file))));
    writer.print(likelihood);
    writer.println();
    writer.flush();
    writer.close();
  }


}
