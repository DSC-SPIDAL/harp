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

import edu.iu.harp.resource.Writable;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TopicCountMap extends Writable {

  protected static final Log LOG =
    LogFactory.getLog(TopicCountMap.class);

  private final Int2IntOpenHashMap topicCount;

  public TopicCountMap() {
    topicCount = new Int2IntOpenHashMap();
  }

  public Int2IntOpenHashMap getTopicCount() {
    return topicCount;
  }

  @Override
  public int getNumWriteBytes() {
    // map size + map entries (
    // each is 4 + 4)
    return 4 + topicCount.size() * 8;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(topicCount.size());
    ObjectIterator<Int2IntMap.Entry> iterator =
      topicCount.int2IntEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2IntMap.Entry entry = iterator.next();
      out.writeInt(entry.getIntKey());
      out.writeInt(entry.getIntValue());
    }
  }

  @Override
  public void read(DataInput in)
    throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      topicCount.put(in.readInt(), in.readInt());
    }
    topicCount.trim();
  }

  @Override
  public void clear() {
    topicCount.clear();
  }
}
