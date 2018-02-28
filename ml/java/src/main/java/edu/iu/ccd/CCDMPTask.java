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

public class CCDMPTask
  implements Task<List<VRowCol>, Object> {

  protected static final Log LOG =
    LogFactory.getLog(CCDMPTask.class);
  private final double lambda;
  private List<Partition<DoubleArray>> wPartitions;
  private List<Partition<DoubleArray>> hPartitions;

  private boolean useRow;

  public CCDMPTask(double lambda) {
    this.lambda = lambda;
    useRow = false;
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

  @Override
  public Object run(List<VRowCol> vList)
    throws Exception {
    if (useRow) {
      doRowCCD(vList);
    } else {
      doColCCD(vList);
    }
    return null;
  }

  private void doRowCCD(List<VRowCol> vList) {
    for (VRowCol row : vList) {
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
        double up = 0.0;
        double down = lambda * row.numV;
        for (int j = 0; j < row.numV; j++) {
          double ht = hr[row.ids[j]];
          up += ((row.m1[j] + wt * ht) * ht);
          down += (ht * ht);
        }
        double zStar = up / down;
        double delta = zStar - wt;
        for (int j = 0; j < row.numV; j++) {
          row.m1[j] -= (delta * hr[row.ids[j]]);
        }
        wr[row.id] = zStar;
      }
    }
  }

  private void doColCCD(List<VRowCol> vList) {
    for (VRowCol col : vList) {
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
        double up = 0.0;
        double down = lambda * col.numV;
        for (int j = 0; j < col.numV; j++) {
          double wt = wr[col.ids[j]];
          up += ((col.m1[j] + ht * wt) * wt);
          down += (wt * wt);
        }
        double sStar = up / down;
        double delta = sStar - ht;
        for (int j = 0; j < col.numV; j++) {
          col.m1[j] -= (delta * wr[col.ids[j]]);
        }
        hr[col.id] = sStar;
      }
    }
  }
}
