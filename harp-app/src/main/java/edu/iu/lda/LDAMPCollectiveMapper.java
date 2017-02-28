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

package edu.iu.lda;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import cc.mallet.types.Dirichlet;

import edu.iu.dymoro.Rotator;
import edu.iu.dymoro.Scheduler;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.LongArrPlus;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

public class LDAMPCollectiveMapper
  extends
  CollectiveMapper<String, String, Object, Object> {
  private int numTopics;
  private double alpha;
  private double beta;
  private int numIterations;
  private int numThreads;
  private long time;
  private int numModelSlices;
  private String modelDirPath;
  private boolean printModel;
  private int printInterval;
  private long computeTime;
  private long numVTrained;
  private long waitTime;
  private boolean enableTuning;

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
    numTopics =
      configuration.getInt(Constants.NUM_TOPICS,
        100);
    alpha =
      configuration.getDouble(Constants.ALPHA,
        0.1);
    beta =
      configuration.getDouble(Constants.BETA,
        0.001);
    numIterations =
      configuration.getInt(
        Constants.NUM_ITERATIONS, 100);
    numThreads =
      configuration.getInt(Constants.NUM_THREADS,
        16);
    time =
      configuration
        .getLong(Constants.TIME, 1000L);
    numModelSlices =
      configuration.getInt(
        Constants.NUM_MODEL_SLICES, 2);
    modelDirPath =
      configuration.get(Constants.MODEL_DIR, "");
    printModel =
      configuration.getBoolean(
        Constants.PRINT_MODEL, false);
    printInterval = 10;
    computeTime = 0L;
    numVTrained = 0L;
    waitTime = 0L;
    enableTuning =
      configuration.getBoolean(
        Constants.ENABLE_TUNING, true);
    long endTime = System.currentTimeMillis();
    LOG.info("config (ms): "
      + (endTime - startTime));
    LOG.info("Num Topics " + numTopics);
    LOG.info("Alpha " + alpha);
    LOG.info("Beta " + beta);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Mini Batch " + time);
    LOG.info("Model Slices " + numModelSlices);
    LOG.info("Model Dir Path " + modelDirPath);
    LOG.info("Print Model " + printModel);
    LOG.info("Use Timer Tuning " + enableTuning);
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    LinkedList<String> docFiles =
      getDocFiles(reader);
    try {
      runLDA(docFiles,
        context.getConfiguration(), context);
    } catch (Exception e) {
      LOG.error("Fail to run LDA.", e);
    }
    LOG.info("Total execution time: "
      + (System.currentTimeMillis() - startTime));
  }

  private LinkedList<String> getDocFiles(
    final KeyValReader reader)
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
    LOG.info("Use Model Parallelism");
    Int2ObjectOpenHashMap<DocWord> vDocMap =
      LDAUtil.load(vFilePaths, numThreads,
        configuration);
    // ---------------------------------------------
    // Create vHMap and W model
    int numSplits =
      ((int) Math.round(numThreads / 20.0) + 1) * 20;
    final int numRowSplits = numSplits;
    final int numColSplits = numRowSplits;
    LOG.info("numRowSplits: " + numRowSplits
      + " numColSplits: " + numColSplits);
    // D model and W model
    Int2ObjectOpenHashMap<Int2IntOpenHashMap> dMap =
      new Int2ObjectOpenHashMap<>();
    Table<TopicCount> wordTable =
      new Table<>(0, new TopicCountCombiner());
    // vDMap grouped to splits based on row IDs
    final Int2ObjectOpenHashMap<DocWord>[] vDWMap =
      new Int2ObjectOpenHashMap[numRowSplits];
    LDAUtil.createDWSplitAndModel(vDocMap, dMap,
      wordTable, vDWMap, numRowSplits, numTopics,
      numThreads);
    vDocMap = null;
    // Create global W model
    Table<TopicCount>[] wordTableMap =
      new Table[numModelSlices];
    final int vocabularySize =
      LDAUtil.createWordModel(wordTableMap,
        numModelSlices, wordTable, this);
    wordTable = null;
    printModelSize(wordTableMap);
    this.freeMemory();
    this.freeConn();
    System.gc();
    // ----------------------------------------------
    // Calculate topic sums
    final int[] zeroTopicSums =
      new int[numTopics];
    final int[] topicSums = new int[numTopics];
    final double[] commons =
      new double[numTopics];
    final double[] rCoeffDistr =
      new double[numTopics];
    getTopicSums("get-initial-topics-word-sum",
      topicSums, zeroTopicSums, wordTableMap);
    printNumTokens(topicSums);
    double rCoeffSum =
      calculateCommons(vocabularySize, topicSums,
        commons, rCoeffDistr);
    // Count the training points per worker
    final long numTokens = getNumTokens(vDWMap);
    final int selfID = this.getSelfID();
    final int numWorkers = this.getNumWorkers();
    final long totalTokens =
      countWorkerTokenSum(numTokens, selfID,
        numWorkers);
    LOG.info("Total number of tokens "
      + totalTokens);
    // Create scheduler and rotator
    Rotator<TopicCount> rotator =
      new Rotator<>(wordTableMap, numColSplits,
        false, this, null, "lda");
    rotator.start();
    List<LDAMPTask> ldaTasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      ldaTasks.add(new LDAMPTask(numTopics,
        alpha, beta, commons, rCoeffDistr));
    }
    Scheduler<DocWord, TopicCount, LDAMPTask> scheduler =
      new Scheduler<>(numRowSplits, numColSplits,
        vDWMap, time, ldaTasks);
      
      // ADD: pb
      // Initialize RMSE compute
      LinkedList<CalcLikelihoodTask> calcLHTasks=
        new LinkedList<>();
      for (int i = 0; i < numThreads; i++) {
    	  calcLHTasks.add(new CalcLikelihoodTask(numTopics, alpha, beta));
      }
      DynamicScheduler<List<Partition<TopicCount>>, Object, CalcLikelihoodTask> calcLHCompute =
        new DynamicScheduler<>(calcLHTasks);
        calcLHCompute.start();
      printLikelihood(rotator, calcLHCompute,numWorkers, 0, topicSums, vocabularySize);
      LOG.info("Iteration Starts.");
      
    // -----------------------------------------
    // For iteration
    for (int i = 1; i <= numIterations; i++) {
      long iteStart = System.currentTimeMillis();
      for (int j = 0; j < numWorkers; j++) {
        for (int k = 0; k < numModelSlices; k++) {
          long t1 = System.currentTimeMillis();
          List<Partition<TopicCount>>[] wMap =
            rotator.getSplitMap(k);
          long t2 = System.currentTimeMillis();
          waitTime += (t2 - t1);
          scheduler.schedule(wMap);
          numVTrained +=
            scheduler.getNumVItemsTrained();
          long t3 = System.currentTimeMillis();
          computeTime += (t3 - t2);
          rotator.rotate(k);
        }
      }
      rotator.pause();
      getTopicSums("get-topics-word-sum-" + i,
        topicSums, zeroTopicSums, wordTableMap);
      rCoeffSum =
        calculateCommons(vocabularySize,
          topicSums, commons, rCoeffDistr);
      for (LDAMPTask ldaTask : ldaTasks) {
        ldaTask.setRCoeffSum(rCoeffSum);
      }
      if (i % printInterval == 0 || i == 1
        || i == numIterations) {
        // this.logMemUsage();
        // this.logGCTime();
        printNumTokens(topicSums);
        printModelSize(wordTableMap);
        if (printModel) {
        	
        	
        	// ADD: pb
            // Initialize RMSE compute
        	printLikelihood(rotator, calcLHCompute,numWorkers, i, topicSums,vocabularySize);
        	
          try {
            printWordTableMap(wordTableMap,
              modelDirPath + "/tmp_model/" + i
                + "/", selfID, configuration);
          } catch (Exception e) {
            LOG.error("Fail to print model.", e);
          }
        }
      }
      int percentage =
        (int) Math.round((double) numVTrained
          / (double) numTokens * 100.0);
      if (enableTuning) {
        long newTime =
          adjustMiniBatch(selfID, computeTime,
            numVTrained, totalTokens, i, time,
            numModelSlices, numWorkers);
        if (time != newTime) {
          if (newTime < time) {
            // cached resources may not be
            // suitable for the shrunk model
            this.freeMemory();
          }
          time = newTime;
          scheduler.setTimer(time);
          LOG.info("Set next timer to " + time);
        }
      }
      long iteEnd = System.currentTimeMillis();
      LOG.info("Iteration " + i + ": "
        + (iteEnd - iteStart)
        + ", compute time: " + computeTime
        + ", misc: " + waitTime + ", numTokens: "
        + numVTrained + ", percentage(%): "
        + percentage);
      computeTime = 0L;
      numVTrained = 0L;
      waitTime = 0L;
      context.progress();
      rotator.start();
    }
    // Stop sgdCompute and rotation
    scheduler.stop();
    rotator.stop();
  }

  private long getNumTokens(
    Int2ObjectOpenHashMap<DocWord>[] vDWMap) {
    long countV = 0L;
    for (int i = 0; i < vDWMap.length; i++) {
      ObjectIterator<Int2ObjectMap.Entry<DocWord>> iterator =
        vDWMap[i].int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<DocWord> entry =
          iterator.next();
        DocWord docWord = entry.getValue();
        // Number of tokens
        for (int j = 0; j < docWord.numV; j++) {
          countV += docWord.v[j];
        }
      }
    }
    return countV;
  }

  private long countWorkerTokenSum(
    long numTokens, int selfID, int numWorkers) {
    Table<LongArray> workerVSumTable =
      new Table<>(0, new LongArrPlus());
    LongArray array = LongArray.create(1, false);
    array.get()[0] = numTokens;
    workerVSumTable.addPartition(new Partition<>(
      selfID, array));
    this.allgather("lda", "allgather-vsum",
      workerVSumTable);
    long totalNumToken = 0;
    for (Partition<LongArray> partition : workerVSumTable
      .getPartitions()) {
      int workerID = partition.id();
      long numV = partition.get().get()[0];
      totalNumToken += numV;
      LOG.info("Worker ID: " + workerID + " "
        + numV);
    }
    return totalNumToken;
  }

  private void getTopicSums(final String opName,
    final int[] topicSums,
    final int[] zeroTopicSums,
    final Table<TopicCount>[] wTableMap) {
    System.arraycopy(zeroTopicSums, 0, topicSums,
      0, numTopics);
    for (int i = 0; i < wTableMap.length; i++) {
      for (Partition<TopicCount> partition : wTableMap[i]
        .getPartitions()) {
        ObjectIterator<Int2IntMap.Entry> iterator =
          partition.get().getTopicCount()
            .int2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
          Int2IntMap.Entry entry =
            iterator.next();
          topicSums[entry.getIntKey()] +=
            entry.getIntValue();
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
      commons[i] =
        1.0 / (topicSums[i] + beta
          * vocabularySize);
      rCoeffDistr[i] = commons[i] * beta;
      rCoeffSum += rCoeffDistr[i];
    }
    return rCoeffSum;
  }

  private void printWordTableMap(
    Table<TopicCount>[] wordTableMap,
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
    for (Table<TopicCount> wTable : wordTableMap) {
      for (Partition<TopicCount> wPartition : wTable
        .getPartitions()) {
        int wordID = wPartition.id();
        Int2IntOpenHashMap wRow =
          wPartition.get().getTopicCount();
        // Print word
        writer.print(wordID);
        // Print topic count
        ObjectIterator<Int2IntMap.Entry> iterator =
          wRow.int2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
          Int2IntMap.Entry ent = iterator.next();
          writer.print(" " + ent.getIntKey()
            + ":" + ent.getIntValue());
        }
        writer.println();
      }
    }
    writer.flush();
    writer.close();
  }

  private void printModelSize(
    Table<TopicCount>[] wordTableMap) {
    long size = 0L;
    for (Table<TopicCount> wTable : wordTableMap) {
      for (Partition<TopicCount> partition : wTable
        .getPartitions()) {
        size += partition.getNumEnocdeBytes();
      }
    }
    LOG.info("W model size: " + size + " bytes");
  }

  private void printNumTokens(int[] topicSums) {
    long numTokens = 0L;
    for (int i = 0; i < topicSums.length; i++) {
      numTokens += topicSums[i];
    }
    LOG.info("Number of tokens " + numTokens);
  }

  private long adjustMiniBatch(int selfID,
    long computeTime, long numTokenTrained,
    long totalTokens, int iteration,
    long miniBatch, int numModelSlices,
    int numWorkers) {
    // Try to get worker ID
    // and related computation Time
    // and the percentage of
    // completion
    Table<IntArray> arrTable =
      new Table<>(0, new IntArrPlus());
    IntArray array = IntArray.create(2, false);
    array.get()[0] = (int) computeTime;
    array.get()[1] = (int) numTokenTrained;
    arrTable.addPartition(new Partition<>(selfID,
      array));
    this.allgather("lda",
      "allgather-compute-status-" + iteration,
      arrTable);
    // int totalComputeTime = 0;
    long totalTokensTrained = 0L;
    for (Partition<IntArray> partition : arrTable
      .getPartitions()) {
      int[] recvArr = partition.get().get();
      // totalComputeTime += recvArr[0];
      totalTokensTrained += (long) recvArr[1];
    }
    arrTable.release();
    int percentage =
      (int) (Math
        .round((double) totalTokensTrained
          / (double) totalTokens * 100.0));
    boolean overTrain = false;
    boolean underTrain = false;
    if (percentage >= Constants.TRAIN_MAX_THRESHOLD) {
      overTrain = true;
    }
    if (percentage <= Constants.TRAIN_MIN_THRESHOLD) {
      underTrain = true;
    }
    long newMinibatch = miniBatch;
    if (overTrain) {
      newMinibatch /= 2L;
    } else if (underTrain) {
      newMinibatch *= 2L;
    }
    if (newMinibatch != miniBatch) {
      LOG.info("Now total percentage "
        + percentage);
    }
    return newMinibatch;
  }
  
  // ADD: pb
  // Likelihood compute
  private void printLikelihood(
		  Rotator<TopicCount> rotator,
		  DynamicScheduler<List<Partition<TopicCount>>, Object, CalcLikelihoodTask> calcLHCompute,
		  int numWorkers,int iteration,int[] topicSums,
		  int vocabularySize){
	  
	  double likelihood = 0.;
	  long t1 = System.currentTimeMillis();

	  // compute local partial likelihood 
        for (int k = 0; k < numModelSlices; k++) {
	        List<Partition<TopicCount>>[] hMap =
	          rotator.getSplitMap(k);
	        calcLHCompute.submitAll(hMap);
	        while (calcLHCompute.hasOutput()) {
	        	calcLHCompute.waitForOutput();
	        }
	      }
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
	    lhTable
	      .addPartition(new Partition<DoubleArray>(0,
	        array));
	    this.allreduce("lda", "allreduce-likelihood-"
	      + iteration, lhTable);
	    likelihood =
	  	      lhTable.getPartition(0).get().get()[0];
	    
	    // the remain parts 
	    for (int topic=0; topic < numTopics; topic++) {
	    	likelihood -= 
					Dirichlet.logGammaStirling( (beta * vocabularySize) +
							topicSums[ topic ] );
		}
		
		// logGamma(|V|*beta) for every topic
	    likelihood += 
				Dirichlet.logGammaStirling(beta * vocabularySize) * numTopics;

	      long t2 = System.currentTimeMillis();
	      waitTime += (t2 - t1);
	    
	    // output
	    LOG.info("Iteration " + iteration + ": " + waitTime + ", logLikelihood: " + likelihood );
	    lhTable.release();
	}
  
}
  
  