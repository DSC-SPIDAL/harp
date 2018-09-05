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
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/*******************************************************
 * Int2IntKVPartitionCombiner defines how to merge
 * Int2IntKVPartition
 ******************************************************/
class Int2IntKVPartitionCombiner
    extends PartitionCombiner<Int2IntKVPartition> {

  private TypeIntCombiner valCombiner;

  Int2IntKVPartitionCombiner(
      TypeIntCombiner combiner) {
    this.valCombiner = combiner;
  }

  /**
   * Combine two partitions
   */
  @Override
  public PartitionStatus combine(
      Int2IntKVPartition op,
      Int2IntKVPartition np) {
    Int2IntOpenHashMap nMap = np.getKVMap();
    ObjectIterator<Int2IntMap.Entry> iterator =
        nMap.int2IntEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2IntMap.Entry entry = iterator.next();
      op.putKeyVal(entry.getIntKey(),
          entry.getIntValue(), valCombiner);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * A KVTable manages Int2IntKVPartitions
 ******************************************************/
public class Int2IntKVTable
    extends KVTable<Int2IntKVPartition> {

  private final TypeIntCombiner valCombiner;

  public Int2IntKVTable(int tableID,
                        TypeIntCombiner combiner) {
    super(tableID,
        new Int2IntKVPartitionCombiner(combiner));
    this.valCombiner = combiner;
  }

  /**
   * Add a new key-value pair to the table. If the
   * key exists, combine the old one and the new
   * one, else, create a new partition and then
   * add the new key-value pair to it.
   *
   * @param key the key
   * @param val the value
   */
  public void addKeyVal(int key, int val) {
    Int2IntKVPartition partition =
        getOrCreateKVPartition(key);
    partition.putKeyVal(key, val, valCombiner);
  }

  /**
   * Get the value associated with the key
   *
   * @param key the key
   * @return the value
   */
  public int getVal(int key) {
    Partition<Int2IntKVPartition> partition =
        getKVPartition(key);
    if (partition != null) {
      return partition.get().getVal(key);
    } else {
      return Int2IntKVPartition.defaultReturnVal;
    }
  }

  /**
   * Get a partition by key if exists, or create a
   * new partition if not.
   *
   * @param key the key
   * @return the partition
   */
  private Int2IntKVPartition
  getOrCreateKVPartition(int key) {
    int partitionID = getKVPartitionID(key);
    Partition<Int2IntKVPartition> partition =
        this.getPartition(partitionID);
    if (partition == null) {
      partition =
          new Partition<>(partitionID, Writable
              .create(Int2IntKVPartition.class));
      partition.get().initialize();
      this.insertPartition(partition);
    }
    return partition.get();
  }

  /**
   * Get the partition by key
   *
   * @param key the key
   * @return the partition
   */
  private Partition<Int2IntKVPartition>
  getKVPartition(int key) {
    int partitionID = getKVPartitionID(key);
    return this.getPartition(partitionID);
  }

  /**
   * Get the partition Id by key
   *
   * @param key the key
   * @return the partition id
   */
  protected int getKVPartitionID(int key) {
    return key;
  }
}
