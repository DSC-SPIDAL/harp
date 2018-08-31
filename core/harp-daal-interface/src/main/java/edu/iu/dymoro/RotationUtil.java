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

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.util.Random;

public class RotationUtil {
  protected static final Log LOG = LogFactory
      .getLog(RotationUtil.class);

  public static int[] getRotationSequences(
      Random random, int numWorkers,
      int numIterations,
      CollectiveMapper<?, ?, ?, ?> mapper) {
    // Create rotation sequence
    Table<IntArray> ordersTable =
        new Table<>(0, new IntArrPlus());
    if (mapper.isMaster()) {
      int[] orders =
          createRotationOrder(random,
              numIterations, numWorkers);
      ordersTable.addPartition(new Partition<>(0,
          new IntArray(orders, 0, orders.length)));
    }
    long t1 = System.currentTimeMillis();
    mapper.broadcast("sgd",
        "bcast-rotate-orders", ordersTable,
        mapper.getMasterID(), false);
    int[] orders =
        ordersTable.getPartition(0).get().get();
    long t2 = System.currentTimeMillis();
    LOG.info("Rotation order is bcasted. "
        + (t2 - t1));
    return orders;
  }

  static int[] createRotationOrder(Random random,
                                   int numIterations, int numWorkers) {
    int[] orders =
        new int[(numWorkers * 2 - 1)
            * numIterations];
    int orderIndex = 0;
    IntArrayList workerIDList =
        new IntArrayList();
    for (int i = 0; i < numIterations; i++) {
      if (i == 0) {
        for (int j = 0; j < numWorkers; j++) {
          orders[orderIndex++] = j;
        }
      } else {
        for (int j = 0; j < numWorkers; j++) {
          workerIDList.add(j);
        }
        while (!workerIDList.isEmpty()) {
          orders[orderIndex++] =
              workerIDList.removeInt(random
                  .nextInt(workerIDList.size()));
        }
      }
      for (int k = 1; k < numWorkers; k++) {
        workerIDList.add(k);
      }
      while (!workerIDList.isEmpty()) {
        orders[orderIndex++] =
            workerIDList.removeInt(random
                .nextInt(workerIDList.size()));
      }
    }
    return orders;
  }

  public static void printRotateOrder(
      int[] order, int numWorkers) {
    if (order != null) {
      int rowLen = 2 * numWorkers - 1;
      int numIterations = order.length / rowLen;
      StringBuffer sb = new StringBuffer();
      int orderIndex = 0;
      for (int i = 0; i < numIterations; i++) {
        for (int j = 0; j < rowLen; j++) {
          sb.append(order[orderIndex++] + " ");
        }
        LOG.info(sb);
        sb.delete(0, sb.length());
      }
    }
  }
}
