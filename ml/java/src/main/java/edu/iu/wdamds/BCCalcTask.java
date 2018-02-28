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
import edu.iu.harp.resource.ShortArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.log4j.Logger;

import java.util.Arrays;

public class BCCalcTask implements
  Task<RowData, Partition<DoubleArray>> {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(BCCalcTask.class);

  private final Partition<DoubleArray>[] xPartitions;
  private final int d;
  private final double tCur;

  public BCCalcTask(
    Partition<DoubleArray>[] xPartitions, int d,
    double tCur) {
    this.xPartitions = xPartitions;
    this.d = d;
    this.tCur = tCur;
  }

  @Override
  public Partition<DoubleArray>
    run(RowData rowData) throws Exception {
    // Copy to local
    int d = this.d;
    double tCur = this.tCur;
    int bcSize = rowData.height * d;
    DoubleArray bcArray =
      DoubleArray.create(bcSize, true);
    // Matrix multiplication result array should
    // be initialized as 0
    // This array is small. Initialization should
    // be efficient.
    Arrays.fill(bcArray.get(), 0, bcSize, 0);
    calculateBC(rowData.distArray,
      rowData.weightArray, xPartitions, d,
      bcArray, rowData.row, rowData.rowOffset,
      rowData.height, rowData.width, tCur);
    return new Partition<DoubleArray>(rowData.row,
      bcArray);
  }

  void calculateBC(ShortArray distArray,
    DoubleArray weightArray,
    Partition<DoubleArray>[] xPartitions, int d,
    DoubleArray bcArray, int row, int rowOffset,
    int height, int width, double tCur)
    throws Exception {
    int rowEnd = rowOffset + height;
    int bOfZSize = width * height;
    // long time0 = System.currentTimeMillis();
    DoubleArray bOfZ =
      DoubleArray.create(bOfZSize, true);
    // long time1 = System.currentTimeMillis();
    calculateBofZ(xPartitions, d, distArray,
      weightArray, bOfZ, row, rowOffset, rowEnd,
      height, width, tCur);
    // long time2 = System.currentTimeMillis();
    // Next we can calculate the BofZ * preX.
    CalcUtil.matrixMultiply(bOfZ, xPartitions,
      bcArray, height, d, width);
    // long time3 = System.currentTimeMillis();
    // System.out.println(" " + (time3 - time2) +
    // " " + (time2 - time1) + " "
    // + (time1 - time0));
    bOfZ.release();
  }

  private void calculateBofZ(
    Partition<DoubleArray>[] xPartitions, int d,
    ShortArray distArray, DoubleArray weightArray,
    DoubleArray bofZ, int row, int rowOffset,
    int rowEnd, int height, int width,
    double tCur) {
    short[] dists = distArray.get();
    double[] weights = weightArray.get();
    double[] bOfZs = bofZ.get();
    double vBlockVal = -1;
    // Because tMax = maxOrigDistance /
    // Math.sqrt(2.0 * d);
    // and tCur = alpha * tMax
    // diff must < maxOrigDistance
    double diff = Math.sqrt(2.0 * d) * tCur;
    if (tCur <= 0.000000001) {
      diff = 0;
    }
    // LOG.info("bc diff: " + diff);
    // To store the summation of each row
    DoubleArray bOfZsIIArray =
      DoubleArray.create(height, true);
    double[] bOfZsIIArr = bOfZsIIArray.get();
    int tmpI = 0;
    int tmpII = 0;
    int tmpIJ = 0;
    double distance = 0;
    double[] xiArr = xPartitions[row].get().get();
    int xiArrSize = xPartitions[row].get().size();
    double[] xjArr = null;
    int xjArrSize = 0;
    double tmpWeight = 0;
    double tmpDist = 0;
    double bOfZsII = 0;
    double bOfZsIJ = 0;
    tmpII = rowOffset;
    for (int i = 0; i < xiArrSize; i += d) {
      for (int j =
        0; j < xPartitions.length; j++) {
        xjArr = xPartitions[j].get().get();
        xjArrSize = xPartitions[j].get().size();
        for (int k = 0; k < xjArrSize; k += d) {
          // II will be updated later
          if (tmpIJ != tmpII) {
            tmpWeight = weights[tmpIJ];
            if (tmpWeight != 0) {
              tmpDist = (double) dists[tmpIJ]
                / (double) Short.MAX_VALUE;
              bOfZsIJ = 0;
              distance =
                CalcUtil.calculateDistance(xiArr,
                  i, xjArr, k, d);
              if (distance >= 1.0E-10
                && diff < tmpDist) {
                bOfZsIJ = tmpWeight * vBlockVal
                  * (tmpDist - diff) / distance;
              }
              bOfZsII -= bOfZsIJ;
              bOfZs[tmpIJ] = bOfZsIJ;
            } else {
              bOfZs[tmpIJ] = 0;
            }
          }
          tmpIJ++;
        }
      }
      bOfZsIIArr[tmpI] = bOfZsII;
      bOfZsII = 0;
      tmpI++;
      tmpII += width + 1;
    }
    // Update II position
    tmpII = rowOffset;
    for (int i = 0; i < height; i++) {
      bOfZs[tmpII] = bOfZsIIArr[i];
      tmpII += width + 1;
    }
    bOfZsIIArray.release();
  }
}
