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

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

class Points {

  public double[] pointArray;
  public int[][] cenIDs;
}

public class ExpTask
  implements Task<Points, Object> {

  protected static final Log LOG =
    LogFactory.getLog(ExpTask.class);

  private Table<DoubleArray> cenTable;
  private List<Partition<DoubleArray>> centroids;
  private final int cenVecSize;

  public ExpTask(Table<DoubleArray> cenTable,
    int cenVecSize) {
    this.cenTable = cenTable;
    this.centroids = new ArrayList<>();
    this.cenVecSize = cenVecSize;
  }

  public void update() {
    centroids.clear();
    centroids.addAll(cenTable.getPartitions());
  }

  @Override
  public Object run(Points points)
    throws Exception {
    double[] pointArray = points.pointArray;
    int[][] cenIDs = points.cenIDs;
    int pointIndex = 0;
    for (int i = 0; i < pointArray.length; i +=
      cenVecSize) {
      int[] cenID = cenIDs[pointIndex];
      double minDistance = pointArray[i];
      for (Partition<DoubleArray> partition : centroids) {
        int partitionID = partition.id();
        double[] cenArray = partition.get().get();
        int cenArrSize = partition.get().size();
        for (int k = 0; k < cenArrSize;) {
          int pStart = i + 1;
          k++;
          double distance = 0.0;
          for (int l = 1; l < cenVecSize; l++) {
            double diff = (pointArray[pStart++]
              - cenArray[k++]);
            distance += diff * diff;
          }
          if (distance < minDistance) {
            minDistance = distance;
            cenID[0] = partitionID;
            cenID[1] = k - cenVecSize;
          }
        }
      }
      pointArray[i] = minDistance;
      pointIndex++;
    }
    return null;
  }
}
