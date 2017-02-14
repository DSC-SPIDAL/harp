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

package edu.iu.sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.dymoro.MPTask;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;

public class SGDMPTask extends
  MPTask<VRowCol, DoubleArray> {

  protected static final Log LOG = LogFactory
    .getLog(SGDMPTask.class);
  private final int r;
  private final double lambda;
  private final double epsilon;

  public SGDMPTask(int r, double lambda,
    double epsilon) {
    this.r = r;
    this.lambda = lambda;
    this.epsilon = epsilon;
  }

  public long doRun(
    List<Partition<DoubleArray>> hPartitionList,
    Int2ObjectOpenHashMap<VRowCol> vRowColMap) {
    // Implement NZL2 loss function
    long numV = 0L;
    for (Partition<DoubleArray> hPartition : hPartitionList) {
      VRowCol vRowCol =
        vRowColMap.get(hPartition.id());
      if (vRowCol != null) {
        numV += vRowCol.numV;
        updateModel(vRowCol, hPartition.get()
          .get());
      }
    }
    return numV;
  }

  private void updateModel(VRowCol vCol,
    double[] hRow) {
    for (int i = 0; i < vCol.numV; i++) {
      double[] wRow = vCol.m2[i];
      double error = -vCol.v[i];
      for (int k = 0; k < r; k++) {
        error += wRow[k] * hRow[k];
      }
      // Update H
      // Update W
      for (int k = 0; k < r; k++) {
        double wk = wRow[k];
        double hk = hRow[k];
        wRow[k] =
          wk - epsilon
            * (error * hk + lambda * wk);
        hRow[k] =
          hk - epsilon
            * (error * wk + lambda * hk);
      }
    }
  }
}
