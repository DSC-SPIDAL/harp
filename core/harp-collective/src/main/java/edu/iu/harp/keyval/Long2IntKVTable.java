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
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/*******************************************************
 * Long2IntKVPartitionCombiner defines how to
 * merge Long2IntKVPartition
 ******************************************************/
class Long2IntKVPartitionCombiner
  extends PartitionCombiner<Long2IntKVPartition> {

  private TypeIntCombiner valCombiner;

  Long2IntKVPartitionCombiner(
    TypeIntCombiner combiner) {
    this.valCombiner = combiner;
  }

  /**
   * Combine two partitions
   */
  @Override
  public PartitionStatus combine(
    Long2IntKVPartition op,
    Long2IntKVPartition np) {
    Long2IntOpenHashMap nMap = np.getKVMap();
    ObjectIterator<Long2IntMap.Entry> iterator =
      nMap.long2IntEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Long2IntMap.Entry entry = iterator.next();
      op.putKeyVal(entry.getLongKey(),
        entry.getIntValue(), valCombiner);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * A KVTable manages Long2IntKVPartition
 ******************************************************/
public class Long2IntKVTable
  extends KVTable<Long2IntKVPartition> {

  private final TypeIntCombiner valCombiner;

  public Long2IntKVTable(int tableID,
    TypeIntCombiner combiner) {
    super(tableID,
      new Long2IntKVPartitionCombiner(combiner));
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
  public void addKeyVal(long key, int val) {
    Long2IntKVPartition partition =
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
  public int getVal(long key) {
    Partition<Long2IntKVPartition> partition =
      getKVPartition(key);
    if (partition != null) {
      return partition.get().getVal(key);
    } else {
      return Long2IntKVPartition.defaultReturnVal;
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
  private Long2IntKVPartition
    getOrCreateKVPartition(long key) {
    int partitionID = getKVPartitionID(key);
    Partition<Long2IntKVPartition> partition =
      this.getPartition(partitionID);
    if (partition == null) {
      partition =
        new Partition<>(partitionID, Writable
          .create(Long2IntKVPartition.class));
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
  private Partition<Long2IntKVPartition>
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
    return (int) key;
  }
}
