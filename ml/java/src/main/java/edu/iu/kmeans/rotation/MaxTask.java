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

package edu.iu.kmeans.rotation;

import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

import java.util.concurrent.BlockingQueue;

class CenPair {

  public int cenParID;
  public int offset;

  CenPair(int id, int offset) {
    this.cenParID = id;
    this.offset = offset;
  }
}

public class MaxTask
  implements Task<CenPair, Object> {
  private final BlockingQueue<Points> pointsList;
  private final Table<DoubleArray> cenTable;
  private final int cenVecSize;

  public MaxTask(BlockingQueue<Points> pointsList,
    Table<DoubleArray> cenTable, int cenVecSize) {
    this.pointsList = pointsList;
    this.cenTable = cenTable;
    this.cenVecSize = cenVecSize;
  }

  @Override
  public Object run(CenPair pair)
    throws Exception {
    double[] cenArray = cenTable
      .getPartition(pair.cenParID).get().get();
    for (Points points : pointsList) {
      double[] pointArray = points.pointArray;
      int[][] cenIDs = points.cenIDs;
      for (int i = 0; i < cenIDs.length; i++) {
        if (cenIDs[i][0] == pair.cenParID
          && cenIDs[i][1] == pair.offset) {
          // Add point to centroid
          int a = pair.offset;
          int b = i * cenVecSize;
          cenArray[a++]++;
          pointArray[b++] = Double.MAX_VALUE;
          for (int j = 1; j < cenVecSize; j++) {
            cenArray[a++] += pointArray[b++];
          }
        }
      }
    }
    return null;
  }
}
