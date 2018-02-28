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

public class ResTask
  implements Task<List<VRowCol>, Object> {

  protected static final Log LOG =
    LogFactory.getLog(ResTask.class);

  private List<Partition<DoubleArray>> wPartitions;
  private List<Partition<DoubleArray>> hPartitions;

  private boolean useRow;
  private boolean init;
  private boolean getRMSE;

  private double rowRMSE;
  private double colRMSE;

  public ResTask() {
    useRow = false;
    init = false;
    getRMSE = false;

    rowRMSE = 0.0;
    colRMSE = 0.0;
  }

  public void setWHPartitionLists(
    List<Partition<DoubleArray>> wPartitions,
    List<Partition<DoubleArray>> hPartitions) {
    this.wPartitions = wPartitions;
    this.hPartitions = hPartitions;
  }

  public void useRow(boolean useRow) {
    this.useRow = useRow;
  }

  public void setInit(boolean init) {
    this.init = init;
  }

  public void setRMSE(boolean getRMSE) {
    this.getRMSE = getRMSE;
  }

  public double getRowRMSE() {
    double val = rowRMSE;
    rowRMSE = 0.0;
    return val;
  }

  public double getColRMSE() {
    double val = colRMSE;
    colRMSE = 0.0;
    return val;
  }

  @Override
  public Object run(List<VRowCol> vRowColList)
    throws Exception {
    if (useRow) {
      doRow(vRowColList);
      if (getRMSE) {
        doRowRMSE(vRowColList);
      }
    } else {
      doCol(vRowColList);
      if (getRMSE) {
        doColRMSE(vRowColList);
      }
    }
    return null;
  }

  private void doRow(List<VRowCol> vList) {
    for (VRowCol row : vList) {
      if (init) {
        System.arraycopy(row.v, 0, row.m1, 0,
          row.numV);
      }
      Iterator<Partition<DoubleArray>> wIterator =
        wPartitions.iterator();
      Iterator<Partition<DoubleArray>> hIterator =
        hPartitions.iterator();
      while (wIterator.hasNext()
        && hIterator.hasNext()) {
        double[] wr =
          wIterator.next().get().get();
        double[] hr =
          hIterator.next().get().get();
        double wt = wr[row.id];
        for (int j = 0; j < row.numV; j++) {
          row.m1[j] -= (wt * hr[row.ids[j]]);
        }
      }
    }
  }

  private void doCol(List<VRowCol> vList) {
    for (VRowCol col : vList) {
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
        double[] wr =
          wIterator.next().get().get();
        double[] hr =
          hIterator.next().get().get();
        double ht = hr[col.id];
        for (int j = 0; j < col.numV; j++) {
          col.m1[j] -= (ht * wr[col.ids[j]]);
        }
      }
    }
  }

  private void doRowRMSE(List<VRowCol> vList) {
    for (VRowCol row : vList) {
      for (int i = 0; i < row.numV; i++) {
        rowRMSE += (row.m1[i] * row.m1[i]);
      }
    }
  }

  private void doColRMSE(List<VRowCol> vList) {
    for (VRowCol col : vList) {
      for (int i = 0; i < col.numV; i++) {
        colRMSE += (col.m1[i] * col.m1[i]);
      }
    }
  }
}
