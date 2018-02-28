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

package edu.iu.kmeans.regroupallgather;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CenCalcTask
  implements Task<double[], Object> {

  protected static final Log LOG =
    LogFactory.getLog(CenCalcTask.class);

  private double[][] centroids;
  private double[][] local;
  private final int cenVecSize;

  public CenCalcTask(Table<DoubleArray> cenTable,
    int cenVecSize) {
    centroids =
      new double[cenTable.getNumPartitions()][];
    local = new double[centroids.length][];
    for (Partition<DoubleArray> partition : cenTable
      .getPartitions()) {
      int partitionID = partition.id();
      DoubleArray array = partition.get();
      centroids[partitionID] = array.get();
      local[partitionID] =
        new double[array.size()];
    }
    this.cenVecSize = cenVecSize;
  }

  public void
    update(Table<DoubleArray> cenTable) {
    for (Partition<DoubleArray> partition : cenTable
      .getPartitions()) {
      int partitionID = partition.id();
      DoubleArray array = partition.get();
      centroids[partitionID] = array.get();
    }
  }

  public double[][] getLocal() {
    return local;
  }

  @Override
  public Object run(double[] points)
    throws Exception {
    for (int i = 0; i < points.length;) {
      i++;
      double minDistance = Double.MAX_VALUE;
      int minCenParID = 0;
      int minOffset = 0;
      for (int j = 0; j < centroids.length; j++) {
        for (int k = 0; k < local[j].length;) {
          int pStart = i;
          k++;
          double distance = 0.0;
          for (int l = 1; l < cenVecSize; l++) {
            double diff = (points[pStart++]
              - centroids[j][k++]);
            distance += diff * diff;
          }
          if (distance < minDistance) {
            minDistance = distance;
            minCenParID = j;
            minOffset = k - cenVecSize;
          }
        }
      }
      // Count + 1
      local[minCenParID][minOffset++]++;
      // Add the point
      for (int j = 1; j < cenVecSize; j++) {
        local[minCenParID][minOffset++] +=
          points[i++];
      }
    }
    return null;
  }
}
