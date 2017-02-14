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
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;

/**
 * A map between a topic id and the count on this
 * topic.
 * 
 * @author zhangbj
 *
 */
public class TopicCountCombiner extends
  PartitionCombiner<TopicCount> {

  @Override
  public PartitionStatus combine(
    TopicCount curPartition,
    TopicCount newPartition) {
    Int2IntOpenHashMap newTopicCount =
      newPartition.getTopicCount();
    if (!newTopicCount.isEmpty()) {
      Int2IntOpenHashMap curTopicCount =
        curPartition.getTopicCount();
      ObjectIterator<Int2IntMap.Entry> iterator =
        newTopicCount.int2IntEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2IntMap.Entry entry = iterator.next();
        curTopicCount.addTo(entry.getIntKey(),
          entry.getIntValue());
      }
    }
    return PartitionStatus.COMBINED;
  }
}
