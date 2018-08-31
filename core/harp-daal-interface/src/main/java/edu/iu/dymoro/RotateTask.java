/*
 * Copyright 2013-2016 Indiana University
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

package edu.iu.dymoro;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.schstatic.Task;

public class RotateTask<P extends Simple> extends
    Task<Integer, List<Partition<P>>[]> {

  protected static final Log LOG = LogFactory
      .getLog(RotateTask.class);

  private final CollectiveMapper<?, ?, ?, ?> mapper;
  private final Table<P> table;
  private final List<Partition<P>>[] splitMap;
  private final String contextName;
  private int operationID;

  private final int[] orders;
  private final int orderRowLen;
  private int curOrderID;
  private final int numWorkers;
  private Int2IntOpenHashMap dataWorkerMap;
  private Int2IntOpenHashMap rotationMap;

  private long commTime;

  public RotateTask(Table<P> table,
                    CollectiveMapper<?, ?, ?, ?> mapper,
                    int[] orders, String contextName) {
    this.table = table;
    // this.numColSplits = numColSplits;
    // this.randomSplit = randomSplit;
    // random =
    // new Random(System.currentTimeMillis());

    this.splitMap = null;
    this.mapper = mapper;
    this.contextName = contextName;
    operationID = 0;
    numWorkers = mapper.getNumWorkers();

    if (orders != null) {
      this.orders = orders;
      orderRowLen = numWorkers * 2 - 1;
      curOrderID = 0;
      dataWorkerMap = new Int2IntOpenHashMap();
      // Initialize partition positions
      for (int i = 0; i < numWorkers; i++) {
        dataWorkerMap
            .put(i, orders[curOrderID++]);
      }
      rotationMap = new Int2IntOpenHashMap();
    } else {
      this.orders = null;
      orderRowLen = 0;
      curOrderID = 0;
      dataWorkerMap = null;
      rotationMap = null;
    }
    commTime = 0L;
  }

  @Override
  public List<Partition<P>>[] run(Integer cmd)
      throws Exception {
    long t1 = System.currentTimeMillis();

    updateRotationMap();
    mapper.rotate(contextName,
        "rotate-" + table.getTableID() + "-"
            + operationID, table, rotationMap);

    operationID++;
    long t2 = System.currentTimeMillis();
    commTime += (t2 - t1);
    return null;
  }

  private void updateRotationMap() {
    if (orders != null
        && curOrderID < orders.length) {
      if (curOrderID % orderRowLen == 0) {
        // The start of row
        int lastShift = orders[curOrderID - 1];
        for (int i = 0; i < numWorkers; i++) {
          // Get data i, calculate the new
          // location
          int originWorkerID =
              dataWorkerMap.get(i);
          int curWorkerID =
              (originWorkerID + lastShift)
                  % numWorkers;
          int newWorkerID = orders[curOrderID];
          dataWorkerMap.put(i, newWorkerID);
          rotationMap.put(curWorkerID,
              newWorkerID);
          curOrderID++;
        }
      } else if (curOrderID % orderRowLen == numWorkers) {
        // The first shift
        for (int i = 0; i < numWorkers; i++) {
          // Get data i, calculate the new
          // location
          int curWorkerID = dataWorkerMap.get(i);
          int newWorkerID =
              (curWorkerID + orders[curOrderID])
                  % numWorkers;
          rotationMap.put(curWorkerID,
              newWorkerID);
        }
        curOrderID++;
      } else {
        for (int i = 0; i < numWorkers; i++) {
          // Get data i, calculate the new
          // location
          int originWorkerID =
              dataWorkerMap.get(i);
          int curWorkerID =
              (originWorkerID + orders[curOrderID - 1])
                  % numWorkers;
          int newWorkerID =
              (originWorkerID + orders[curOrderID])
                  % numWorkers;
          rotationMap.put(curWorkerID,
              newWorkerID);
        }
        curOrderID++;
      }
    }
  }

  public long resetCommTime() {
    long time = commTime;
    commTime = 0L;
    return time;
  }
}
