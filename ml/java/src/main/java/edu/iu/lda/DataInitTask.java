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

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.schdynamic.Task;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.LinkedList;
import java.util.Random;

class DWSplit {
  LinkedList<DocWord> docWordList = null;
  int splitID = -1;

  DWSplit(int i) {
    docWordList = new LinkedList<>();
    splitID = i;
  }
}

public class DataInitTask
  implements Task<DWSplit, Object> {

  private final Int2ObjectOpenHashMap<DocWord>[] vDWMap;
  private final LongArrayList[] dMap;
  private final Table<TopicCountMap> wordTable;
  private final int numTopics;
  private final Random random;

  public DataInitTask(
    Int2ObjectOpenHashMap<DocWord>[] vDWMap,
    LongArrayList[] dMap,
    Table<TopicCountMap> wordTable,
    int numTopics) {
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
    ObjectIterator<Int2ObjectMap.Entry<DocWord>> iterator =
      vWMap.int2ObjectEntrySet().fastIterator();
    for (; iterator.hasNext();) {
      DocWord docWord =
        iterator.next().getValue();
      // Assign
      int[] id2 = new int[docWord.numV];
      System.arraycopy(docWord.id2, 0, id2, 0,
        docWord.numV);
      docWord.id2 = id2;
      docWord.z = new int[docWord.numV][];
      for (int j = 0; j < docWord.numV; j++) {
        // Each v is the number of z on
        // this word and this doc
        docWord.z[j] = new int[docWord.v[j]];
        for (int k = 0; k < docWord.v[j]; k++) {
          int topic = random.nextInt(numTopics);
          docWord.z[j][k] = topic;
          // Add topic to D model
          LongArrayList dRow =
            dMap[docWord.id2[j]];
          boolean isFound = false;
          for (int l = 0; l < dRow.size(); l++) {
            if (topic == (int) dRow.getLong(l)) {
              dRow.set(l, dRow.getLong(l)
                + Constants.TOPIC_DELTA);
              isFound = true;
              break;
            }
          }
          if (!isFound) {
            dRow.add((long) topic
              + Constants.TOPIC_DELTA);
          }
        }
      }
      // Add topic to W model
      Partition<TopicCountMap> partition =
        wordTable.getPartition(docWord.id1);
      Int2IntOpenHashMap wRow =
        partition.get().getTopicCount();
      synchronized (wRow) {
        for (int j = 0; j < docWord.numV; j++) {
          for (int k =
            0; k < docWord.z[j].length; k++) {
            wRow.addTo(docWord.z[j][k], 1);
          }
        }
      }
      docWord.v = null;
    }
    vWMap.trim();
    return null;
  }
}
