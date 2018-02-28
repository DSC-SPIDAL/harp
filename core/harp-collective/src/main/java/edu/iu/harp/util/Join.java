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

package edu.iu.harp.util;

import edu.iu.harp.resource.Writable;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * This class records the destinations of each
 * partition. Each partition could have
 * multi-destinations.
 ******************************************************/

public class Join extends Writable {
  /** Which workers this partition goes */
  private Int2ObjectOpenHashMap<IntArrayList> parToWorkerMap;
  /**
   * How many Data arrays this worker needs to
   * receive
   */
  private Int2IntOpenHashMap workerParCountMap;

  public Join() {
    parToWorkerMap = null;
    workerParCountMap = null;
  }

  /**
   * Set the parToWorkerMap
   * 
   * @param partitionToWorkerMap
   *          the parToWorkerMap
   */
  public void setParToWorkerMap(
    Int2ObjectOpenHashMap<IntArrayList> partitionToWorkerMap) {
    this.parToWorkerMap = partitionToWorkerMap;
  }

  /**
   * Get the parToWorkerMap
   * 
   * @return the parToWorkerMap
   */
  public Int2ObjectOpenHashMap<IntArrayList>
    getParToWorkerMap() {
    return parToWorkerMap;
  }

  /**
   * Set the workerPartitionCountMap
   * 
   * @param workerPartitionCountMap
   *          the workerPartitionCountMap
   */
  public void setWorkerParCountMap(
    Int2IntOpenHashMap workerPartitionCountMap) {
    this.workerParCountMap =
      workerPartitionCountMap;
  }

  /**
   * Get the workerParCountMap
   * 
   * @return the workerParCountMap
   */
  public Int2IntOpenHashMap
    getWorkerParCountMap() {
    return workerParCountMap;
  }

  /**
   * Get the number of bytes of encoded data
   */
  @Override
  public int getNumWriteBytes() {
    int size = 4;
    if (parToWorkerMap != null) {
      ObjectIterator<Int2ObjectMap.Entry<IntArrayList>> iterator =
        parToWorkerMap.int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<IntArrayList> entry =
          iterator.next();
        size += (8 + entry.getValue().size() * 4);
      }
    }
    size += 4;
    if (workerParCountMap != null) {
      size += (workerParCountMap.size() * 8);
    }
    return size;
  }

  /**
   * Write this to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    if (parToWorkerMap != null) {
      out.writeInt(parToWorkerMap.size());
      ObjectIterator<Int2ObjectMap.Entry<IntArrayList>> iterator =
        parToWorkerMap.int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<IntArrayList> entry =
          iterator.next();
        out.writeInt(entry.getIntKey());
        out.writeInt(entry.getValue().size());
        for (int workerID : entry.getValue()) {
          out.writeInt(workerID);
        }
      }
    } else {
      out.writeInt(0);
    }
    if (workerParCountMap != null) {
      out.writeInt(this.workerParCountMap.size());
      for (Int2IntMap.Entry entry : this.workerParCountMap
        .int2IntEntrySet()) {
        out.writeInt(entry.getIntKey());
        out.writeInt(entry.getIntValue());
      }
    } else {
      out.writeInt(0);
    }
  }

  /**
   * Read this from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    int mapSize = in.readInt();
    if (mapSize > 0) {
      if (parToWorkerMap == null) {
        parToWorkerMap =
          new Int2ObjectOpenHashMap<IntArrayList>(
            mapSize);
      }
    }
    IntArrayList list = null;
    for (int i = 0; i < mapSize; i++) {
      int key = in.readInt();
      int listSize = in.readInt();
      list = new IntArrayList(listSize);
      for (int j = 0; j < listSize; j++) {
        list.add(in.readInt());
      }
      parToWorkerMap.put(key, list);
    }
    mapSize = in.readInt();
    if (mapSize > 0) {
      if (workerParCountMap == null) {
        workerParCountMap =
          new Int2IntOpenHashMap(mapSize);
      }
    }
    for (int i = 0; i < mapSize; i++) {
      workerParCountMap.put(in.readInt(),
        in.readInt());
    }
  }

  /**
   * Clear the data
   */
  @Override
  public void clear() {
    if (parToWorkerMap != null) {
      parToWorkerMap.clear();
    }
    if (workerParCountMap != null) {
      workerParCountMap.clear();
    }
  }
}
