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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.LinkedList;
import java.util.Random;

import edu.iu.harp.schdynamic.Task;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;

class DWSplit {
  LinkedList<DocWord> docWordList = null;
  int splitID = -1;

  DWSplit(int i) {
    docWordList = new LinkedList<>();
    splitID = i;
  }
}

public class DataInitTask implements
  Task<DWSplit, Object> {

  private final Int2ObjectOpenHashMap<DocWord>[] vDWMap;
  private final Int2ObjectOpenHashMap<Int2IntOpenHashMap> dMap;
  private final Table<TopicCount> wordTable;
  private final int numTopics;
  private final Random random;

  public DataInitTask(
    Int2ObjectOpenHashMap<DocWord>[] vDWMap,
    Int2ObjectOpenHashMap<Int2IntOpenHashMap> dMap,
    Table<TopicCount> wordTable, int numTopics) {
    this.vDWMap = vDWMap;
    this.dMap = dMap;
    this.wordTable = wordTable;
    this.numTopics = numTopics;
    this.random =
      new Random(System.currentTimeMillis());
  }

  @Override
  public Object run(DWSplit split)
    throws Exception {
    Int2ObjectOpenHashMap<DocWord> vWMap =
      vDWMap[split.splitID];
    for (DocWord docWord : split.docWordList) {
      for (int i = 0; i < docWord.numV; i++) {
        LDAUtil.addToData(vWMap, docWord.id2[i],
          docWord.id1, docWord.v[i]);
      }
    }
    split = null;
    ObjectIterator<Int2ObjectMap.Entry<DocWord>> iterator =
      vWMap.int2ObjectEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<DocWord> entry =
        iterator.next();
      DocWord docWord = entry.getValue();
      Partition<TopicCount> partition =
        wordTable.getPartition(docWord.id1);
      // Trim
      int[] v = new int[docWord.numV];
      System.arraycopy(docWord.v, 0, v, 0,
        docWord.numV);
      docWord.v = v;
      // Assign
      docWord.z = new int[docWord.numV][];
      docWord.m2 =
        new Int2IntOpenHashMap[docWord.numV];
      for (int j = 0; j < docWord.numV; j++) {
        // Each v is the number of z on
        // this word and this doc
        docWord.m2[j] = dMap.get(docWord.id2[j]);
        int numTokens = docWord.v[j];
        docWord.z[j] = new int[numTokens];
        for (int k = 0; k < numTokens; k++) {
          int topic = random.nextInt(numTopics);
          docWord.z[j][k] = topic;
          // Add topic to W model
          // partition.getPartition()
          // .getTopicCount().addTo(topic, 1);
          // Add topic to D model
          docWord.m2[j].addTo(topic, 1);
        }
      }
      // Add topic to W model
      Int2IntOpenHashMap wRow =
        partition.get().getTopicCount();
      synchronized (wRow) {
        for (int j = 0; j < docWord.numV; j++) {
          for (int k = 0; k < docWord.z[j].length; k++) {
            wRow.addTo(docWord.z[j][k], 1);
          }
        }
      }
      docWord.id2 = null;
    }
    vWMap.trim();
    return null;
  }

}
