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

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.resource.Writable;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/*******************************************************
 * Int2LongKVPartitionCombiner defines how to
 * merge Int2LongKVPartition
 ******************************************************/
class Int2LongKVPartitionCombiner
  extends PartitionCombiner<Int2LongKVPartition> {

  private TypeLongCombiner valCombiner;

  Int2LongKVPartitionCombiner(
    TypeLongCombiner combiner) {
    this.valCombiner = combiner;
  }

  /**
   * Combine two partitions
   */
  @Override
  public PartitionStatus combine(
    Int2LongKVPartition op,
    Int2LongKVPartition np) {
    Int2LongOpenHashMap nMap = np.getKVMap();
    ObjectIterator<Int2LongMap.Entry> iterator =
      nMap.int2LongEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2LongMap.Entry entry = iterator.next();
      op.putKeyVal(entry.getIntKey(),
        entry.getLongValue(), valCombiner);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * A KVTable manages Int2LongKVPartition
 ******************************************************/
public class Int2LongKVTable
  extends KVTable<Int2LongKVPartition> {

  private final TypeLongCombiner valCombiner;

  public Int2LongKVTable(int tableID,
    TypeLongCombiner combiner) {
    super(tableID,
      new Int2LongKVPartitionCombiner(combiner));
    this.valCombiner = combiner;
  }

  /**
   * Add a new key-value pair to the table. If the
   * key exists, combine the old one and the new
   * one, else, create a new partition and then
   * add the new key-value pair to it.
   * 
   * @param key
   *          the key
   * @param val
   *          the value
   */
  public void addKeyVal(int key, long val) {
    Int2LongKVPartition partition =
      getOrCreateKVPartition(key);
    addKVInPartition(partition, key, val);
  }

  /**
   * Add the key-value pair to the partition
   * 
   * @param partition
   *          the partition
   * @param key
   *          the key
   * @param val
   *          the value
   * @return the ValStatus
   */
  private ValStatus addKVInPartition(
    Int2LongKVPartition partition, int key,
    long val) {
    return partition.putKeyVal(key, val,
      valCombiner);
  }

  /**
   * Get the value associated with the key
   * 
   * @param key
   *          the key
   * @return the value
   */
  public long getVal(int key) {
    Partition<Int2LongKVPartition> partition =
      getKVPartition(key);
    if (partition != null) {
      return partition.get().getVal(key);
    } else {
      return Int2LongKVPartition.defaultReturnVal;
    }
  }

  /**
   * Get a partition by key if exists, or create a
   * new partition if not.
   * 
   * @param key
   *          the key
   * @return the partition
   */
  private Int2LongKVPartition
    getOrCreateKVPartition(int key) {
    int partitionID = getKVPartitionID(key);
    Partition<Int2LongKVPartition> partition =
      this.getPartition(partitionID);
    if (partition == null) {
      partition =
        new Partition<>(partitionID, Writable
          .create(Int2LongKVPartition.class));
      partition.get().initialize();
      this.insertPartition(partition);
    }
    return partition.get();
  }

  /**
   * Get the partition by key
   * 
   * @param key
   *          the key
   * @return the partition
   */
  private Partition<Int2LongKVPartition>
    getKVPartition(int key) {
    int partitionID = getKVPartitionID(key);
    return this.getPartition(partitionID);
  }

  /**
   * Get the partition Id by key
   * 
   * @param key
   *          the key
   * @return the partition id
   */
  protected int getKVPartitionID(int key) {
    return key;
  }
}
