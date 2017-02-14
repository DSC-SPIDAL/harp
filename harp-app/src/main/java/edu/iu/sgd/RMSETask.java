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

import edu.iu.harp.schdynamic.Task;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;

public class RMSETask implements
  Task<List<Partition<DoubleArray>>, Object> {

  protected static final Log LOG = LogFactory
    .getLog(RMSETask.class);

  private final int r;
  private double rmse;
  private double testRMSE;

  private Int2ObjectOpenHashMap<VRowCol>[] vWHMap;
  private Int2ObjectOpenHashMap<VRowCol> testVColMap;

  public RMSETask(int r,
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    Int2ObjectOpenHashMap<VRowCol> testVColMap) {
    this.r = r;
    rmse = 0.0;
    this.testRMSE = 0.0;
    this.vWHMap = vWHMap;
    this.testVColMap = testVColMap;
  }

  public double getRMSE() {
    double result = rmse;
    rmse = 0.0;
    return result;
  }

  public double getTestRMSE() {
    double result = testRMSE;
    testRMSE = 0.0;
    return result;
  }

  @Override
  public Object run(
    List<Partition<DoubleArray>> hPartitions)
    throws Exception {
    for (Partition<DoubleArray> partition : hPartitions) {
      int partitionID = partition.id();
      double[] doubles = partition.get().get();
      // for (Int2ObjectOpenHashMap<VRowCol> map :
      // vWHMap) {
      // VRowCol vRowCol = map.get(partitionID);
      // if (vRowCol != null) {
      // rmse +=
      // calculateRMSE(vRowCol, doubles, r);
      // }
      // }
      VRowCol vRowCol =
        testVColMap.get(partitionID);
      if (vRowCol != null) {
        testRMSE +=
          calculateRMSE(vRowCol, doubles, r);
      }
    }
    return null;
  }

  private double calculateRMSE(VRowCol vRowCol,
    double[] hRow, int r) {
    double rmse = 0.0;
    for (int i = 0; i < vRowCol.numV; i++) {
      double[] wRow = vRowCol.m2[i];
      double error = vRowCol.v[i];
      for (int k = 0; k < r; k++) {
        error -= wRow[k] * hRow[k];
      }
      rmse += (error * error);
    }
    return rmse;
  }
}
