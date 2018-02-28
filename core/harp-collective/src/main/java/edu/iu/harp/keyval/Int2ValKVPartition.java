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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/*******************************************************
 * Int2ValKVPartition manages key-value pairs in
 * which the key is int-type and the value is
 * object
 ******************************************************/
public abstract class Int2ValKVPartition<V extends Value>
  extends KVPartition {

  /** Class logger */
  private static final Logger LOG =
    Logger.getLogger(Int2ValKVPartition.class);

  private Int2ObjectOpenHashMap<V> kvMap;
  /** The class of the values */
  private Class<V> vClass;
  /** Value objects for reuse */
  private LinkedList<V> freeVals;

  public Int2ValKVPartition() {
    kvMap = null;
    vClass = null;
    freeVals = new LinkedList<>();
  }

  /**
   * Initialization
   */
  public void initialize(Class<V> vClass) {
    if (this.kvMap == null) {
      this.kvMap = new Int2ObjectOpenHashMap<>();
      this.kvMap.defaultReturnValue(null);
    }
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
  public ValStatus putKeyVal(int key, V val,
    ValCombiner<V> combiner) {
    if (val == null) {
      return ValStatus.ADD_FAILED;
    } else {
      V curVal = this.kvMap.putIfAbsent(key, val);
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
   * @return the associated value
   */
  public V removeVal(int key) {
    return this.kvMap.remove(key);
  }

  /**
   * Get the associated value of the key
   * 
   * @param key
   *          the key
   * @return the associated value
   */
  public V getVal(int key) {
    return this.kvMap.get(key);
  }

  /**
   * Get the Int2ObjectOpenHashMap
   * 
   * @return the Int2ObjectOpenHashMap
   */
  public Int2ObjectOpenHashMap<V> getKVMap() {
    return kvMap;
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
   * Get the available Value objects to reuse
   * 
   * @return Value objects for reuse
   */
  List<V> getFreeVals() {
    return freeVals;
  }

  /**
   * Clear the partition. All Value objects will
   * be cached in and freeVales for later reuse
   */
  @Override
  public void clear() {
    if (!this.kvMap.isEmpty()) {
      ObjectIterator<Int2ObjectMap.Entry<V>> iterator =
        this.kvMap.int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<V> entry =
          iterator.next();
        entry.getValue().clear();
        freeVals.add(entry.getValue());
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
    // vClass name
    size += (vClass.getName().length() * 2 + 4);
    size += (kvMap.size() * 4);
    // Key + each array size
    ObjectIterator<Int2ObjectMap.Entry<V>> iterator =
      this.kvMap.int2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<V> entry =
        iterator.next();
      size += entry.getValue().getNumWriteBytes();
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
    out.writeUTF(this.vClass.getName());
    ObjectIterator<Int2ObjectMap.Entry<V>> iterator =
      this.kvMap.int2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<V> entry =
        iterator.next();
      out.writeInt(entry.getIntKey());
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
    // No matter how much the size is
    // we still need to initialize a map
    if (this.kvMap == null) {
      this.kvMap =
        new Int2ObjectOpenHashMap<>(size);
      this.kvMap.defaultReturnValue(null);
    }
    try {
      this.vClass =
        Writable.forClass(in.readUTF());
      for (int i = 0; i < size; i++) {
        V val = null;
        if (freeVals.isEmpty()) {
          val = Writable.newInstance(vClass);
        } else {
          val = freeVals.removeFirst();
        }
        int key = in.readInt();
        val.read(in);
        this.kvMap.put(key, val);
      }
    } catch (Exception e) {
      LOG.error("Fail to initialize keyvals.", e);
      throw new IOException(e);
    }
  }
}
