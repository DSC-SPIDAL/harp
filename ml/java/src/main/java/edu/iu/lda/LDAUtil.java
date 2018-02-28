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

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.schdynamic.DynamicScheduler;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.util.LinkedList;
import java.util.List;

public class LDAUtil {
  protected static final Log LOG =
    LogFactory.getLog(LDAUtil.class);

  static int load(LinkedList<String> vFilePaths,
    int numThreads, Configuration configuration,
    Int2ObjectOpenHashMap<DocWord> vDocMap,
    Int2ObjectOpenHashMap<String> docIDMap) {
    DocStore vStore = new DocStore(vFilePaths,
      numThreads, configuration);
    vStore.load(vDocMap, docIDMap);
    return vStore.getMaxDocID();
  }

  static void createDWSplitAndModel(
    Int2ObjectOpenHashMap<DocWord> vDocMap,
    LongArrayList[] dMap,
    Table<TopicCountMap> wordTable,
    Int2ObjectOpenHashMap<DocWord>[] vDWMap,
    int numSplits, int numTopics,
    int numThreads) {
    // Create a local V Map indexed by W columns
    // Create D model and local W model
    DWSplit[] dwSplits = new DWSplit[numSplits];
    for (int i = 0; i < numSplits; i++) {
      dwSplits[i] = new DWSplit(i);
      vDWMap[i] = new Int2ObjectOpenHashMap<>();
    }
    IntArray idArray =
      IntArray.create(vDocMap.size(), false);
    int[] ids = idArray.get();
    vDocMap.keySet().toArray(ids);
    IntArrays.quickSort(ids, 0, idArray.size());
    int rowIndex = 0;
    // Num of topics and then a topic and a count
    for (int i = 0; i < idArray.size(); i++) {
      int docID = ids[i];
      DocWord docWord = vDocMap.get(docID);
      // Create doc model
      if (dMap[docID] == null) {
        dMap[docID] = new LongArrayList();
      }
      // Create word model
      for (int j = 0; j < docWord.numV; j++) {
        Partition<TopicCountMap> partition =
          wordTable.getPartition(docWord.id2[j]);
        if (partition == null) {
          TopicCountMap topicCount =
            Writable.create(TopicCountMap.class);
          wordTable.addPartition(
            new Partition<TopicCountMap>(
              docWord.id2[j], topicCount));
        }
      }
      int splitID = i % numSplits;
      dwSplits[splitID].docWordList.add(docWord);
      rowIndex++;
      if (rowIndex % 1000000 == 0) {
        LOG.info("Processed docs " + rowIndex);
      }
    }
    idArray.release();
    vDocMap.clear();
    LOG.info("D & local W model are created.");
    List<DataInitTask> tasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new DataInitTask(vDWMap, dMap,
        wordTable, numTopics));
    }
    DynamicScheduler<DWSplit, Object, DataInitTask> compute =
      new DynamicScheduler<>(tasks);
    compute.submitAll(dwSplits);
    dwSplits = null;
    compute.start();
    compute.stop();
    LOG.info("D & local W model are initialized");
  }

  static void addToData(
    Int2ObjectOpenHashMap<DocWord> map, int id1,
    int id2, int val) {
    DocWord docWord = map.get(id1);
    if (docWord == null) {
      docWord = new DocWord();
      docWord.id1 = id1;
      docWord.id2 = new int[Constants.ARR_LEN];
      docWord.v = new int[Constants.ARR_LEN];
      map.put(id1, docWord);
    }
    // Search in ids2 for the current id2
    int pos = IntArrays.binarySearch(docWord.id2,
      0, docWord.numV, id2);
    if (pos >= 0) {
      docWord.v[pos] += val;
    } else {
      if (docWord.id2.length == docWord.numV) {
        int[] ids2 =
          new int[docWord.id2.length << 1];
        int[] v =
          new int[docWord.id2.length << 1];
        System.arraycopy(docWord.id2, 0, ids2, 0,
          docWord.numV);
        System.arraycopy(docWord.v, 0, v, 0,
          docWord.numV);
        docWord.id2 = ids2;
        docWord.v = v;
      }
      int insertPos = -pos - 1;
      System.arraycopy(docWord.id2, insertPos,
        docWord.id2, insertPos + 1,
        docWord.numV - insertPos);
      System.arraycopy(docWord.v, insertPos,
        docWord.v, insertPos + 1,
        docWord.numV - insertPos);
      docWord.id2[insertPos] = id2;
      docWord.v[insertPos] = val;
      docWord.numV++;
    }
  }

  static int createWordModel(
    Table<TopicCountList>[] wordTableMap,
    int numModelSlices,
    Table<TopicCountMap> wordTable,
    CollectiveMapper<?, ?, ?, ?> mapper) {
    // allreduce column index and the element
    // count on each column
    long t1 = System.currentTimeMillis();
    mapper.regroup("lda", "regroup-word-model",
      wordTable,
      new Partitioner(mapper.getNumWorkers()));
    for (int i = 0; i < numModelSlices; i++) {
      wordTableMap[i] = new Table<>(i,
        new TopicCountListCombiner());
    }
    IntArray idArray = IntArray.create(
      wordTable.getNumPartitions(), false);
    int[] ids = idArray.get();
    wordTable.getPartitionIDs().toArray(ids);
    int size = idArray.size();
    IntArrays.quickSort(ids, 0, size);
    for (int i = 0; i < size; i++) {
      int sliceID = i % numModelSlices;
      TopicCountMap map =
        wordTable.getPartition(ids[i]).get();
      ObjectIterator<Int2IntMap.Entry> iterator =
        map.getTopicCount().int2IntEntrySet()
          .fastIterator();
      TopicCountList list =
        Writable.create(TopicCountList.class);
      LongArrayList array = list.getTopicCount();
      while (iterator.hasNext()) {
        Int2IntMap.Entry entry = iterator.next();
        long topicID = entry.getIntKey();
        long topicCount = entry.getIntValue();
        array.add((topicCount << 32) + topicID);
      }
      wordTableMap[sliceID].addPartition(
        new Partition<TopicCountList>(ids[i],
          list));
    }
    idArray.release();
    wordTable.release();
    wordTable = null;
    Table<IntArray> wordSumTable =
      new Table<>(0, new IntArrPlus());
    IntArray array = IntArray.create(1, false);
    array.get()[0] = size;
    wordSumTable
      .addPartition(new Partition<>(0, array));
    mapper.allreduce("lda", "allreduce-wordsum",
      wordSumTable);
    int vocabularySize =
      wordSumTable.getPartition(0).get().get()[0];
    wordSumTable.release();
    long t2 = System.currentTimeMillis();
    LOG.info("W model is created, vocabulary: "
      + vocabularySize + ", took: " + (t2 - t1));
    return vocabularySize;
  }
}
