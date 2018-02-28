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

package edu.iu.harp.resource;

import edu.iu.harp.io.Constant;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;

/*******************************************************
 * The abstract class of pools. An ArrayPool is
 * used for caching arrays. The arrays, which were
 * allocated before and are no longer used, will
 * be cached for reuse.
 ******************************************************/
public abstract class ArrayPool<T> {

  private static final Logger LOG =
    Logger.getLogger(ArrayPool.class);
  /* A map from size to ArrayStore */
  private Int2ObjectOpenHashMap<ArrayStore> arrayMap;

  /**
   * ArrayStore is used for buffering Arrays.
   * freeQueue stores not-in-use Arrays, which can
   * be used as required to avoid reallocating
   * Arrays. inUseSet stores in-use arrays.
   */
  private class ArrayStore {
    private LinkedList<T> freeQueue;
    private HashSet<T> inUseSet;

    private ArrayStore() {
      freeQueue = new LinkedList<>();
      inUseSet = new HashSet<>();
    }
  }

  public ArrayPool() {
    arrayMap = new Int2ObjectOpenHashMap<>();
  }

  /**
   * If approximate is true, the return value is
   * the smallest power of 2 which is no less than
   * size, else, the return value is the minimum
   * of Constant.MAX_ARRAY_SIZE and size.
   * 
   * @param size
   *          the size of array required
   * @param approximate
   *          true or false
   * @return the adjusted size
   */
  private int getAdjustedArraySize(int size,
    boolean approximate) {
    if (approximate) {
      int shift = 32
        - Integer.numberOfLeadingZeros(size - 1);
      return shift == 31 ? Constant.MAX_ARRAY_SIZE
        : 1 << shift;
    } else {
      return Constant.MAX_ARRAY_SIZE < size
        ? Constant.MAX_ARRAY_SIZE : size;
    }
  }

  /**
   * Create a new array of the size.
   * 
   * @param size
   * @return a new array
   */
  protected abstract T createNewArray(int size);

  /**
   * Get the length of the array
   * 
   * @param array
   * @return length of the array
   */
  protected abstract int getLength(T array);

  /**
   * If approximate is false, get an array of
   * required size. else, get an array of adjusted
   * size. If a not-in-use array of adjusted size
   * is already cached, use this array directly.
   * Else, create a new array.
   * 
   * @param size
   * @param approximate
   * @return an array
   */
  synchronized T getArray(int size,
    boolean approximate) {
    int originSize = size;
    if (originSize <= 0) {
      return null;
    }
    int adjustSize = getAdjustedArraySize(
      originSize, approximate);
    if (adjustSize < originSize) {
      return null;
    }
    ArrayStore arrayStore =
      arrayMap.get(adjustSize);
    if (arrayStore == null) {
      arrayStore = new ArrayStore();
      arrayMap.put(adjustSize, arrayStore);
    }
    if (arrayStore.freeQueue.isEmpty()) {
      try {
        T array = createNewArray(adjustSize);
        arrayStore.inUseSet.add(array);
        // LOG
        // .info("Create a new array with original
        // size: "
        // + originSize
        // + ", adjusted size: "
        // + adjustSize
        // + ", with type: "
        // + array.getClass().getName());
        return array;
      } catch (Throwable t) {
        LOG.error(
          "Cannot create array with size "
            + adjustSize
            + ", current total memory: "
            + Runtime.getRuntime().totalMemory()
            + ", current free memory "
            + Runtime.getRuntime().freeMemory(),
          t);
        return null;
      }
    } else {
      T array =
        arrayStore.freeQueue.removeFirst();
      arrayStore.inUseSet.add(array);
      // LOG
      // .info("Get an existing array with
      // adjusted size: "
      // + adjustSize
      // + ", with type "
      // + array.getClass().getName());
      return array;
    }
  }

  /**
   * Release the array by moving the array from
   * inUseSet to freeQueue. The array can be used
   * as a new array later.
   * 
   * @param array
   *          the array to release
   * @return true if succeeded, false if failed.
   */
  synchronized boolean releaseArray(T array) {
    if (array == null) {
      return false;
    }
    int size = getLength(array);
    ArrayStore arrayStore = arrayMap.get(size);
    if (arrayStore == null) {
      // LOG
      // .info("Fail to release an array with
      // size: "
      // + size
      // + ", with type "
      // + array.getClass().getName()
      // + ". no such store.");
      return false;
    } else {
      if (arrayStore.inUseSet.remove(array)) {
        arrayStore.freeQueue.add(array);
        return true;
      } else {
        // LOG
        // .info("Fail to release an array with
        // size: "
        // + size
        // + ", with type "
        // + array.getClass().getName()
        // + ". no such an array.");
        return false;
      }
    }
  }

  /**
   * Free the array by removing it from the
   * inUseSet. It is no longer available.
   * 
   * @param array
   *          the array to be freed
   * @return true if succeeded, false if failed
   */
  synchronized boolean freeArray(T array) {
    int size = getLength(array);
    // LOG.info("Free an array with size: " + size
    // + ", with type "
    // + array.getClass().getName());
    ArrayStore arrayStore = arrayMap.get(size);
    if (arrayStore == null) {
      return false;
    } else {
      return arrayStore.inUseSet.remove(array);
    }
  }

  /**
   * Clean all arrays in freeQueue, namely remove
   * all not-in-use arrays.
   */
  synchronized void clean() {
    for (ArrayStore store : arrayMap.values()) {
      store.freeQueue.clear();
    }
  }

  /**
   * Logging the usage of the arrays.
   */
  synchronized void log() {
    ObjectIterator<Int2ObjectMap.Entry<ArrayStore>> iterator =
      arrayMap.int2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<ArrayStore> entry =
        iterator.next();
      LOG.info(this + ": size="
        + entry.getIntKey() + ", use="
        + entry.getValue().inUseSet.size()
        + ", released="
        + entry.getValue().freeQueue.size());
    }
  }
}
