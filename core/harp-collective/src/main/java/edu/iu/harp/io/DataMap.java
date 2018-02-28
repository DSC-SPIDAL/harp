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

package edu.iu.harp.io;

import org.apache.log4j.Logger;

import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * A wrapper for operations on queues associated
 * with communication contextName and
 * operationName. The main data structure is a map
 * from contextName to a sub-map; The sub-map is a
 * map from operationName to a BlockingQueue. The
 * BlockingQueue is a queue for the Data. All
 * operations are conducted on this map.
 ******************************************************/
public class DataMap {

  @SuppressWarnings("unused")
  private static final Logger LOG =
    Logger.getLogger(DataMap.class);

  private ConcurrentMap<String, ConcurrentMap<String, BlockingQueue<Data>>> dataMap;
  private final int initialCapacity =
    Constant.NUM_THREADS;

  public DataMap() {
    dataMap =
      new ConcurrentHashMap<>(initialCapacity);
  }

  /**
   * Create a queue for the operation under the
   * context
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @return the queue of the data of the
   *         operation
   */
  private BlockingQueue<Data>
    createOperationDataQueue(String contextName,
      String operationName) {
    ConcurrentMap<String, BlockingQueue<Data>> opDataMap =
      dataMap.get(contextName);
    if (opDataMap == null) {
      opDataMap =
        new ConcurrentHashMap<>(initialCapacity);
      ConcurrentMap<String, BlockingQueue<Data>> oldOpDataMap =
        dataMap.putIfAbsent(contextName,
          opDataMap);
      if (oldOpDataMap != null) {
        opDataMap = oldOpDataMap;
      }
    }
    BlockingQueue<Data> opDataQueue =
      opDataMap.get(operationName);
    if (opDataQueue == null) {
      opDataQueue = new LinkedBlockingQueue<>();
      BlockingQueue<Data> oldOpDataQueue =
        opDataMap.putIfAbsent(operationName,
          opDataQueue);
      if (oldOpDataQueue != null) {
        opDataQueue = oldOpDataQueue;
      }
    }
    return opDataQueue;
  }

  /**
   * Wait and get the data from the queue of the
   * operation under the context
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @param maxWaitTime
   *          maximum waiting time
   * @return the data
   * @throws InterruptedException
   */
  public Data waitAndGetData(String contextName,
    String operationName, long maxWaitTime)
    throws InterruptedException {
    BlockingQueue<Data> opDataQueue =
      createOperationDataQueue(contextName,
        operationName);
    return opDataQueue.poll(maxWaitTime,
      TimeUnit.SECONDS);
  }

  /**
   * Put the Data to the DataMap
   * 
   * @param data
   *          the Data to be put
   */
  public void putData(Data data) {
    BlockingQueue<Data> opDataQueue =
      createOperationDataQueue(
        data.getContextName(),
        data.getOperationName());
    opDataQueue.add(data);
  }

  /**
   * Clean the data of the operation under the
   * context
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   */
  public void cleanOperationData(
    String contextName, String operationName) {
    ConcurrentMap<String, BlockingQueue<Data>> opDataMap =
      dataMap.get(contextName);
    if (opDataMap != null) {
      BlockingQueue<Data> opDataQueue =
        opDataMap.remove(operationName);
      if (opDataQueue != null) {
        for (Data data : opDataQueue) {
          data.release();
        }
        opDataQueue.clear();
      }
    }
  }

  /**
   * Clean the Data related to the context Invoke
   * this when the context is done.
   * 
   * @param contextName
   *          the name of the context
   */
  public void cleanData(String contextName) {
    ConcurrentMap<String, BlockingQueue<Data>> opDataMap =
      dataMap.remove(contextName);
    if (opDataMap != null) {
      for (Entry<String, BlockingQueue<Data>> entry : opDataMap
        .entrySet()) {
        BlockingQueue<Data> opDataQueue =
          entry.getValue();
        if (opDataQueue != null) {
          for (Data data : opDataQueue) {
            data.release();
          }
          opDataQueue.clear();
        }
      }
    }
  }

  /**
   * Clean the DataMap
   */
  public void clean() {
    ConcurrentMap<String, ConcurrentMap<String, BlockingQueue<Data>>> tmpDataMap =
      null;
    synchronized (this) {
      tmpDataMap = dataMap;
      dataMap =
        new ConcurrentHashMap<>(initialCapacity);
    }
    for (Entry<String, ConcurrentMap<String, BlockingQueue<Data>>> entry : tmpDataMap
      .entrySet()) {
      cleanData(entry.getKey());
    }
    dataMap.clear();
  }
}
