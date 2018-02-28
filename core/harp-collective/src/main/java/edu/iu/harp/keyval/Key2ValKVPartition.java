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

import edu.iu.harp.resource.Writable;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/*******************************************************
 * Key2ValKVPartition manages key-value pairs in
 * which the key and the value are objects
 ******************************************************/
public abstract class Key2ValKVPartition<K extends Key, V extends Value>
  extends KVPartition {

  private static final Logger LOG =
    Logger.getLogger(Key2ValKVPartition.class);

  /** A hashmap storing key-value pairs */
  private Object2ObjectOpenHashMap<K, V> kvMap;
  /** The class of the keys */
  private Class<K> kClass;
  /** The class of the values */
  private Class<V> vClass;
  /** Key objects for reuse */
  private LinkedList<K> freeKeys;
  /** Value objects for reuse */
  private LinkedList<V> freeVals;

  public Key2ValKVPartition() {
    super();
    kvMap = null;
    kClass = null;
    vClass = null;
    freeKeys = new LinkedList<>();
    freeVals = new LinkedList<>();
  }

  /**
   * Initialize the partition
   * 
   * @param kClass
   *          the class of Key
   * @param vClass
   *          the class of Value
   */
  public void initialize(Class<K> kClass,
    Class<V> vClass) {
    if (this.kvMap == null) {
      this.kvMap =
        new Object2ObjectOpenHashMap<>();
      this.kvMap.defaultReturnValue(null);
    }
    this.kClass = kClass;
    this.vClass = vClass;
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
  public ValStatus putKeyVal(K key, V val,
    ValCombiner<V> combiner) {
    if (key == null || val == null) {
      return ValStatus.ADD_FAILED;
    } else {
      V curVal = kvMap.putIfAbsent(key, val);
      if (curVal == null) {
        return ValStatus.ADDED;
      } else {
        return combiner.combine(curVal, val);
      }
    }
  }

  /**
   * Removes this key and the associated value
   * from this function if it is present.
   * 
   * @param key
   *          the key
   * @return the associated value, or null if no
   *         value was present for the given key.
   */
  public V removeVal(K key) {
    return this.kvMap.remove(key);
  }

  /**
   * Get the class of the keys
   * 
   * @return the class of the keys
   */
  public Class<K> getKeyClass() {
    return this.kClass;
  }

  /**
   * Get the class of the values
   * 
   * @return the class of the values
   */
  public Class<V> getVClass() {
    return this.vClass;
  }

  /**
   * Get the associated value of the key
   * 
   * @param key
   *          the key
   * @return the associated value
   */
  public V getVal(K key) {
    return this.kvMap.get(key);
  }

  /**
   * Get the Object2ObjectOpenHashMap
   * 
   * @return the Object2ObjectOpenHashMap
   */
  public Object2ObjectOpenHashMap<K, V>
    getKVMap() {
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
   * Get the available Key objects to reuse
   * 
   * @return Key objects for reuse
   */
  List<K> getFreeKeys() {
    return this.freeKeys;
  }

  /**
   * Get the available Value objects to reuse
   * 
   * @return Value objects for reuse
   */
  List<V> getFreeVals() {
    return freeVals;
  }

  /**
   * Clear the partition. All Key objects and
   * Value objects will be cached in freeKeys and
   * freeVales respectively for later reuse
   */
  @Override
  public void clear() {
    if (!this.kvMap.isEmpty()) {
      for (Object2ObjectMap.Entry<K, V> entry : this.kvMap
        .object2ObjectEntrySet()) {
        entry.getKey().clear();
        entry.getValue().clear();
        this.freeKeys.add(entry.getKey());
        this.freeVals.add(entry.getValue());
      }
      this.kvMap.clear();
    }
  }

  /**
   * Get the number of bytes of encoded data
   */
  @Override
  public int getNumWriteBytes() {
    // mapSize
    int size = 4;
    // kClass name
    size +=
      (this.kClass.getName().length() * 2 + 4);
    // vClass name
    size +=
      (this.vClass.getName().length() * 2 + 4);
    // Key + each array size
    ObjectIterator<Object2ObjectMap.Entry<K, V>> iterator =
      this.kvMap.object2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Object2ObjectMap.Entry<K, V> entry =
        iterator.next();
      size += (entry.getKey().getNumWriteBytes()
        + entry.getValue().getNumWriteBytes());
    }
    return size;
  }

  /**
   * Write this to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(this.kvMap.size());
    out.writeUTF(this.kClass.getName());
    out.writeUTF(this.vClass.getName());
    ObjectIterator<Object2ObjectMap.Entry<K, V>> iterator =
      this.kvMap.object2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Object2ObjectMap.Entry<K, V> entry =
        iterator.next();
      entry.getKey().write(out);
      entry.getValue().write(out);
    }
  }

  /**
   * Read this from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    int size = in.readInt();
    if (this.kvMap == null) {
      this.kvMap =
        new Object2ObjectOpenHashMap<>(size);
      this.kvMap.defaultReturnValue(null);
    }
    try {
      this.kClass =
        Writable.forClass(in.readUTF());
      this.vClass =
        Writable.forClass(in.readUTF());
      for (int i = 0; i < size; i++) {
        K key = null;
        V val = null;
        if (freeKeys.isEmpty()
          && freeVals.isEmpty()) {
          key = Writable.newInstance(kClass);
          val = Writable.newInstance(vClass);
        } else {
          key = freeKeys.removeFirst();
          val = freeVals.removeFirst();
        }
        key.read(in);
        val.read(in);
        this.kvMap.put(key, val);
      }
    } catch (Exception e) {
      LOG.error("Fail to initialize keyvals.", e);
      throw new IOException(e);
    }
  }
}
