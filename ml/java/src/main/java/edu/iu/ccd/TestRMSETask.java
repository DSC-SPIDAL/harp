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

package edu.iu.ccd;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import edu.iu.sgd.VRowCol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;

public class TestRMSETask
  implements Task<VRowCol, Object> {

  protected static final Log LOG =
    LogFactory.getLog(TestRMSETask.class);

  private List<Partition<DoubleArray>> wPartitions;
  private List<Partition<DoubleArray>> hPartitions;

  private boolean init;
  private boolean getRMSE;
  private double rmse;

  public TestRMSETask() {
    init = false;
    getRMSE = false;
    rmse = 0.0;
  }

  public void setWHPartitionLists(
    List<Partition<DoubleArray>> wPartitions,
    List<Partition<DoubleArray>> hPartitions) {
    this.wPartitions = wPartitions;
    this.hPartitions = hPartitions;
  }

  public void setInit(boolean init) {
    this.init = init;
  }

  public void setRMSE(boolean getRMSE) {
    this.getRMSE = getRMSE;
  }

  public double getRMSE() {
    double val = rmse;
    rmse = 0.0;
    return val;
  }

  @Override
  public Object run(VRowCol vRowCol)
    throws Exception {
    doCol(vRowCol);
    if (getRMSE) {
      doRMSE(vRowCol);
    }
    return null;
  }

  private void doCol(VRowCol col) {
    if (init) {
      System.arraycopy(col.v, 0, col.m1, 0,
        col.numV);
    }
    Iterator<Partition<DoubleArray>> wIterator =
      wPartitions.iterator();
    Iterator<Partition<DoubleArray>> hIterator =
      hPartitions.iterator();
    while (wIterator.hasNext()
      && hIterator.hasNext()) {
      double[] wr = wIterator.next().get().get();
      double[] hr = hIterator.next().get().get();
      double ht = hr[col.id];
      for (int j = 0; j < col.numV; j++) {
        col.m1[j] -= (ht * wr[col.ids[j]]);
      }
    }
  }

  private void doRMSE(VRowCol col) {
    for (int i = 0; i < col.numV; i++) {
      rmse += (col.m1[i] * col.m1[i]);
    }
  }
}
