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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/*******************************************************
 * Key2ValKVPartitionCombiner defines how to merge
 * Key2ValKVPartitions
 ******************************************************/
class Key2ValKVPartitionCombiner<K extends Key, V extends Value, P extends Key2ValKVPartition<K, V>>
  extends PartitionCombiner<P> {

  private ValCombiner<V> valCombiner;

  Key2ValKVPartitionCombiner(
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
    Object2ObjectOpenHashMap<K, V> nMap =
      np.getKVMap();
    ObjectIterator<Object2ObjectMap.Entry<K, V>> iterator =
      nMap.object2ObjectEntrySet().fastIterator();
    List<K> rmKeys = new LinkedList<>();
    while (iterator.hasNext()) {
      Object2ObjectMap.Entry<K, V> entry =
        iterator.next();
      K key = entry.getKey();
      V val = entry.getValue();
      ValStatus status =
        op.putKeyVal(key, val, valCombiner);
      if (status == ValStatus.ADDED) {
        rmKeys.add(key);
        if (!op.getFreeKeys().isEmpty()) {
          np.getFreeKeys()
            .add(op.getFreeKeys().remove(0));
        }
        if (!op.getFreeVals().isEmpty()) {
          np.getFreeVals()
            .add(op.getFreeVals().remove(0));
        }
      }
    }
    for (K rmKey : rmKeys) {
      nMap.remove(rmKey);
    }
    return PartitionStatus.COMBINED;
  }
}

/*******************************************************
 * An abstract class of Key-Vale tables
 ******************************************************/
public abstract class Key2ValKVTable<K extends Key, V extends Value, P extends Key2ValKVPartition<K, V>>
  extends KVTable<P> {

  @SuppressWarnings("unused")
  private static final Logger LOG =
    Logger.getLogger(Key2ValKVTable.class);

  private final Class<K> kClass;
  private final Class<V> vClass;
  private final Class<P> pClass;
  private final ValCombiner<V> valCombiner;

  public Key2ValKVTable(int tableID,
    ValCombiner<V> combiner, Class<K> kClass,
    Class<V> vClass, Class<P> pClass) {
    super(tableID,
      new Key2ValKVPartitionCombiner<K, V, P>(
        combiner));
    this.kClass = kClass;
    this.vClass = vClass;
    this.pClass = pClass;
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
   * @return the ValStatus
   */
  public ValStatus addKeyVal(K key, V val) {
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
    K key, V val) {
    return partition.putKeyVal(key, val,
      valCombiner);
  }

  /**
   * Get the value associated with the key
   * 
   * @param key
   *          the key
   * @return the value, or null if not exists
   */
  public V getVal(K key) {
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
  public V removeVal(K key) {
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
  private P getOrCreateKVPartition(K key) {
    int partitionID = getKVPartitionID(key);
    Partition<P> partition =
      this.getPartition(partitionID);
    if (partition == null) {
      partition = new Partition<>(partitionID,
        Writable.create(pClass));
      partition.get().initialize(kClass, vClass);
      this.insertPartition(partition);
    }
    return partition.get();
  }

  /**
   * Get the partition by key
   * 
   * @param key
   *          the key
   * @return the partition, or null if it doesn't
   *         exist
   */
  private Partition<P> getKVPartition(K key) {
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
  protected int getKVPartitionID(K key) {
    return key.hashCode();
  }
}
