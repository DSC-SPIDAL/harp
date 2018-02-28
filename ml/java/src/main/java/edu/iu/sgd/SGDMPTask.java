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

package edu.iu.sgd;

import edu.iu.dymoro.MPTask;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class SGDMPTask extends
  MPTask<Int2ObjectOpenHashMap<VRowCol>, DoubleArray> {

  protected static final Log LOG =
    LogFactory.getLog(SGDMPTask.class);
  private final int r;
  private final double lambda;
  private final double epsilon;
  private final double[][] wMap;

  public SGDMPTask(int r, double lambda,
    double epsilon, double[][] wMap) {
    this.r = r;
    this.lambda = lambda;
    this.epsilon = epsilon;
    this.wMap = wMap;
  }

  public long doRun(
    List<Partition<DoubleArray>> partitionList,
    Int2ObjectOpenHashMap<VRowCol> vRowColMap) {
    // Implement NZL2 loss function
    long numV = 0L;
    for (Partition<DoubleArray> partition : partitionList) {
      VRowCol vCol =
        vRowColMap.get(partition.id());
      if (vCol != null) {
        double[] hRow = partition.get().get();
        for (int i = 0; i < vCol.numV; i++) {
          double[] wRow = wMap[vCol.ids[i]];
          double error = -vCol.v[i];
          for (int k = 0; k < r; k++) {
            error += wRow[k] * hRow[k];
          }
          // Update H
          // Update W
          for (int k = 0; k < r; k++) {
            double wk = wRow[k];
            double hk = hRow[k];
            wRow[k] = wk - epsilon
              * (error * hk + lambda * wk);
            hRow[k] = hk - epsilon
              * (error * wk + lambda * hk);
          }
        }
        numV += vCol.numV;
      }
    }
    return numV;
  }
}
