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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

/*******************************************************
 * Int2ValKVPartitionCombiner defines how to merge
 * Int2ValKVPartition
 ******************************************************/
class Int2ValKVPartitionCombiner<V extends Value, P extends Int2ValKVPartition<V>>
  extends PartitionCombiner<P> {

  private ValCombiner<V> valCombiner;

  Int2ValKVPartitionCombiner(
    ValCombiner<V> combiner) {
    this.valCombiner = combiner;
  }

  /**
   * Combine two partitions
   */
  @Override
  public PartitionStatus combine(P op, P np) {
    // remove method in iterator has a bug
    // the rest keys may not be traversed if
    // the last key was removed because the
    // position of keys could be shifted.
    Int2ObjectOpenHashMap<V> nMap = np.getKVMap();
    ObjectIterator<Int2ObjectMap.Entry<V>> iterator =
      nMap.int2ObjectEntrySet().fastIterator();
    IntArrayList rmKeys = new IntArrayList();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<V> entry =
        iterator.next();
      int key = entry.getIntKey();
      ValStatus status = op.putKeyVal(key,
        entry.getValue(), valCombiner);
      if (status == ValStatus.ADDED) {
        rmKeys.add(key);
        if (!op.getFreeVals().isEmpty()) {
          np.getFreeVals()
            .add(op.getFreeVals().remove(0));
        }
      }
    }
    for (int rmKey : rmKeys) {
      nMap.remove(rmKey);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * A KVTable manages Int2ValKVPartitions
 ******************************************************/
public abstract class Int2ValKVTable<V extends Value, P extends Int2ValKVPartition<V>>
  extends KVTable<P> {

  @SuppressWarnings("unused")
  private static final Logger LOG =
    Logger.getLogger(Int2ValKVTable.class);

  private final Class<V> vClass;
  private final Class<P> pClass;
  private final ValCombiner<V> combiner;

  public Int2ValKVTable(int tableID,
    ValCombiner<V> combiner, Class<V> vClass,
    Class<P> pClass) {
    super(tableID,
      new Int2ValKVPartitionCombiner<V, P>(
        combiner));
    this.vClass = vClass;
    this.pClass = pClass;
    this.combiner = combiner;
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
  public ValStatus addKeyVal(int key, V val) {
    P partition = getOrCreateKVPartition(key);
    return addKVInPartition(partition, key, val);
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
  private ValStatus addKVInPartition(P partition,
    int key, V val) {
    return partition.putKeyVal(key, val,
      combiner);
  }

  /**
   * Get the value associated with the key
   * 
   * @param key
   *          the key
   * @return the value
   */
  public V getVal(int key) {
    Partition<P> partition = getKVPartition(key);
    if (partition != null) {
      return partition.get().getVal(key);
    } else {
      return null;
    }
  }

  /**
   * Remove the value associated with the key
   * 
   * @param key
   *          the key
   * @return the value, or null if not exists
   */
  public V removeVal(int key) {
    Partition<P> partition = getKVPartition(key);
    if (partition != null) {
      return partition.get().removeVal(key);
    } else {
      return null;
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
  private P getOrCreateKVPartition(int key) {
    int partitionID = getKVPartitionID(key);
    Partition<P> partition =
      this.getPartition(partitionID);
    if (partition == null) {
      partition = new Partition<>(partitionID,
        Writable.create(pClass));
      partition.get().initialize(vClass);
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
  private Partition<P> getKVPartition(int key) {
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
