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
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/*******************************************************
 * Long2DoubleKVPartitionCombiner defines how to
 * merge Long2DoubleKVPartition
 ******************************************************/
class Long2DoubleKVPartitionCombiner extends
  PartitionCombiner<Long2DoubleKVPartition> {

  private TypeDoubleCombiner valCombiner;

  Long2DoubleKVPartitionCombiner(
    TypeDoubleCombiner combiner) {
    this.valCombiner = combiner;
  }

  /**
   * Combine two partitions
   */
  @Override
  public PartitionStatus combine(
    Long2DoubleKVPartition op,
    Long2DoubleKVPartition np) {
    Long2DoubleOpenHashMap nMap = np.getKVMap();
    ObjectIterator<Long2DoubleMap.Entry> iterator =
      nMap.long2DoubleEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Long2DoubleMap.Entry entry =
        iterator.next();
      op.putKeyVal(entry.getLongKey(),
        entry.getDoubleValue(), valCombiner);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * A KVTable manages Long2DoubleKVPartition
 ******************************************************/
public class Long2DoubleKVTable
  extends KVTable<Long2DoubleKVPartition> {

  private final TypeDoubleCombiner valCombiner;

  public Long2DoubleKVTable(int tableID,
    TypeDoubleCombiner combiner) {
    super(tableID,
      new Long2DoubleKVPartitionCombiner(
        combiner));
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
  public void addKeyVal(long key, double val) {
    Long2DoubleKVPartition partition =
      getOrCreateKVPartition(key);
    partition.putKeyVal(key, val, valCombiner);
  }

  /**
   * Get the value associated with the key
   * 
   * @param key
   *          the key
   * @return the value
   */
  public double getVal(long key) {
    Partition<Long2DoubleKVPartition> partition =
      getKVPartition(key);
    if (partition != null) {
      return partition.get().getVal(key);
    } else {
      return Long2DoubleKVPartition.defaultReturnVal;
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
  private Long2DoubleKVPartition
    getOrCreateKVPartition(long key) {
    int partitionID = getKVPartitionID(key);
    Partition<Long2DoubleKVPartition> partition =
      this.getPartition(partitionID);
    if (partition == null) {
      partition =
        new Partition<>(partitionID, Writable
          .create(Long2DoubleKVPartition.class));
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
  private Partition<Long2DoubleKVPartition>
    getKVPartition(long key) {
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
  private int getKVPartitionID(long key) {
    return (int) (key);
  }
}
