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

package edu.iu.wdamds;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.log4j.Logger;

public class StressCalcTask
  implements Task<RowData, DoubleArray> {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(StressCalcTask.class);

  private final Partition<DoubleArray>[] xPartitions;
  private final int d;
  private final double tCur;

  public StressCalcTask(
    Partition<DoubleArray>[] xPartitions, int d,
    double tCur) {
    this.xPartitions = xPartitions;
    this.d = d;
    this.tCur = tCur;
  }

  @Override
  public DoubleArray run(RowData rowData)
    throws Exception {
    // Copy to local
    int d = this.d;
    double tCur = this.tCur;
    // int rowEnd =
    // rowData.rowOffset + rowData.height;
    short[] dists = rowData.distArray.get();
    double[] weights = rowData.weightArray.get();
    double diff = Math.sqrt(2.0 * d) * tCur;
    if (tCur <= 0) {
      diff = 0;
    }
    // LOG.info("stress diff: " + diff);
    int tmpII = 0;
    int tmpIJ = 0;
    double distance;
    double dd;
    double sigma = 0;
    double[] xiArr =
      xPartitions[rowData.row].get().get();
    int xiArrSize =
      xPartitions[rowData.row].get().size();
    double[] xjArr = null;
    int xjArrSize = 0;
    double tmpWeight = 0;
    double tmpDist = 0;
    tmpII = rowData.rowOffset;
    for (int i = 0; i < xiArrSize; i += d) {
      for (int j =
        0; j < xPartitions.length; j++) {
        xjArr = xPartitions[j].get().get();
        xjArrSize = xPartitions[j].get().size();
        for (int k = 0; k < xjArrSize; k += d) {
          tmpDist = (double) dists[tmpIJ]
            / (double) Short.MAX_VALUE;
          tmpWeight = weights[tmpIJ];
          if (tmpWeight != 0 && tmpDist >= diff) {
            distance = 0;
            if (tmpIJ != tmpII) {
              distance =
                CalcUtil.calculateDistance(xiArr,
                  i, xjArr, k, d);
            }
            dd = tmpDist - diff - distance;
            sigma += tmpWeight * dd * dd;
          }
          tmpIJ++;
        }
      }
      // Get next II position
      tmpII += rowData.width + 1;
    }
    // Get double[1];
    DoubleArray vals =
      DoubleArray.create(1, false);
    vals.get()[0] = sigma;
    return vals;
  }
}
