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

package edu.iu.harp.keyval;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * Int2IntKVPartition manages key-value pairs in
 * which the key and the value are int-type
 ******************************************************/
public class Int2IntKVPartition
  extends KVPartition {

  private Int2IntOpenHashMap kvMap;
  final static int defaultReturnVal =
    Integer.MIN_VALUE;

  public Int2IntKVPartition() {
    super();
    kvMap = null;
  }

  /**
   * Initialization
   */
  public void initialize() {
    if (kvMap != null) {
      kvMap.clear();
    } else {
      kvMap = new Int2IntOpenHashMap();
      kvMap.defaultReturnValue(defaultReturnVal);
    }
  }

  /**
   * Put the new key-value pair to the partition.
   * If the key already exists in the partition,
   * combine the original value with the new
   * value; else, add the new key-value pair to
   * the partition
   * 
   * @param key
   *          the new key
   * @param val
   *          the new value
   * @param combiner
   *          the combiner
   * @return the ValStatus
   */
  public ValStatus putKeyVal(int key, int val,
    TypeIntCombiner combiner) {
    int curVal = kvMap.put(key, val);
    if (curVal == defaultReturnVal) {
      return ValStatus.ADDED;
    } else {
      kvMap.put(key,
        combiner.combine(curVal, val));
      return ValStatus.COMBINED;
    }
  }

  /**
   * Get the associated value of the key
   * 
   * @param key
   *          the key
   * @return the associated value
   */
  public int getVal(int key) {
    return this.kvMap.get(key);
  }

  /**
   * Get the Int2IntOpenHashMap
   * 
   * @return the Int2IntOpenHashMap
   */
  public Int2IntOpenHashMap getKVMap() {
    return kvMap;
  }

  /**
   * Get the number of key-value pairs
   * 
   * @return the number of key-value pairs
   */
  public int size() {
    return this.kvMap.size();
  }

  /**
   * Indicates if the partition is empty or not
   * 
   * @return true if empty, false if not
   */
  public boolean isEmpty() {
    return this.kvMap.isEmpty();
  }

  /**
   * Clear the partition.
   */
  @Override
  public void clear() {
    this.kvMap.clear();
  }

  /**
   * Get the number of bytes of encoded data
   */
  @Override
  public int getNumWriteBytes() {
    return 4 + kvMap.size() * 8;
  }

  /**
   * Write this to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(kvMap.size());
    ObjectIterator<Int2IntMap.Entry> iterator =
      kvMap.int2IntEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2IntMap.Entry entry = iterator.next();
      out.writeInt(entry.getIntKey());
      out.writeInt(entry.getIntValue());
    }
  }

  /**
   * Read this from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    int size = in.readInt();
    if (kvMap != null) {
      kvMap.clear();
    } else {
      kvMap = new Int2IntOpenHashMap(size);
      this.kvMap
        .defaultReturnValue(defaultReturnVal);
    }
    for (int i = 0; i < size; i++) {
      kvMap.put(in.readInt(), in.readInt());
    }
  }
}
