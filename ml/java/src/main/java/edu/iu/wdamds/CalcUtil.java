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
import edu.iu.harp.resource.FloatArray;
import edu.iu.harp.resource.IntArray;
import org.apache.log4j.Logger;

public class CalcUtil {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(CalcUtil.class);

  public static double calculateDistance(
    Partition<DoubleArray>[] x, int d, int[] iID,
    int[] jID) {
    Partition<DoubleArray> xi = x[iID[0]];
    Partition<DoubleArray> xj = x[jID[0]];
    double[] xiArr = xi.get().get();
    double[] xjArr = xj.get().get();
    double dist = 0;
    double diff = 0;
    int iStart = iID[1] * d;
    int jStart = jID[1] * d;
    for (int k = 0; k < d; k++) {
      diff = xiArr[iStart++] - xjArr[jStart++];
      dist += diff * diff;
    }
    dist = Math.sqrt(dist);
    return dist;
  }

  public static double calculateDistance(
    double[] xiArr, int iStart, double[] xjArr,
    int jStart, int d) {
    double dist = 0;
    double diff = 0;
    for (int k = 0; k < d; k++) {
      diff = xiArr[iStart++] - xjArr[jStart++];
      dist += diff * diff;
    }
    dist = Math.sqrt(dist);
    return dist;
  }

  /**
   * Do matrix multiplication in a horizontal
   * direction. Assume C has been initialized with
   * 0
   * 
   * @param A
   * @param B
   * @param C
   * @param aHeight
   * @param bWidth
   * @param len
   * @throws Exception
   */
  public static void matrixMultiply(DoubleArray A,
    Partition<DoubleArray>[] B, DoubleArray C,
    int aHeight, int bWidth, int len)
    throws Exception {
    if (C.size() != aHeight * bWidth) {
      throw new Exception(
        "Cannot multiply matrices.");
    }
    double[] aArray = A.get();
    int aSize = A.size();
    int aRowStart = 0;
    double[] bParArr = null;
    int bParArrSize = 0;
    int bRowStart = 0;
    double[] cArray = C.get();
    int cRowStart = 0;
    int tmpCRowStart = 0;
    // Go through rows in A
    for (int i = 0; i < aSize; i += len) {
      // Go through rows in B
      for (int j = 0; j < B.length; j++) {
        bParArr = B[j].get().get();
        bParArrSize = B[j].get().size();
        bRowStart = 0;
        for (int k = 0; k < bParArrSize; k +=
          bWidth) {
          // Start calculation on row[i] in A and
          // row[j][k] in B.
          // Get current row start in C
          tmpCRowStart = cRowStart;
          for (int l = 0; l < bWidth; l++) {
            cArray[tmpCRowStart++] +=
              aArray[aRowStart]
                * bParArr[bRowStart++];
          }
          // Go to next element in A
          aRowStart++;
        }
      }
      cRowStart += bWidth;
    }
  }

  public static void matrixMultiply(IntArray A,
    Partition<DoubleArray>[] B, DoubleArray C,
    int aHeight, int bWidth, int len)
    throws Exception {
    if (C.size() != aHeight * bWidth) {
      throw new Exception(
        "Cannot multiply matrices.");
    }
    int[] aArray = A.get();
    int aSize = A.size();
    int aRowStart = 0;
    double[] bParArr = null;
    int bParArrSize = 0;
    int bRowStart = 0;
    double[] cArray = C.get();
    int cRowStart = 0;
    int tmpCRowStart = 0;
    // Go through rows in A
    for (int i = 0; i < aSize; i += len) {
      // Go through rows in B
      for (int j = 0; j < B.length; j++) {
        bParArr = B[j].get().get();
        bParArrSize = B[j].get().size();
        // This will go through each element
        bRowStart = 0;
        for (int k = 0; k < bParArrSize; k +=
          bWidth) {
          // Get current row start in C
          tmpCRowStart = cRowStart;
          for (int l = 0; l < bWidth; l++) {
            cArray[tmpCRowStart++] +=
              aArray[aRowStart]
                * bParArr[bRowStart++];
          }
          aRowStart++; // Go to next element in A
        }
      }
      cRowStart += bWidth;
    }
  }

  public static void matrixMultiply(FloatArray A,
    Partition<DoubleArray>[] B, DoubleArray C,
    int aHeight, int bWidth, int len)
    throws Exception {
    if (C.size() != aHeight * bWidth) {
      throw new Exception(
        "Cannot multiply matrices.");
    }
    float[] aArray = A.get();
    int aSize = A.size();
    int aRowStart = 0;
    double[] bParArr = null;
    int bParArrSize = 0;
    int bRowStart = 0;
    double[] cArray = C.get();
    int cRowStart = 0;
    int tmpCRowStart = 0;
    // Go through rows in A
    for (int i = 0; i < aSize; i += len) {
      // Go through rows in B
      for (int j = 0; j < B.length; j++) {
        bParArr = B[j].get().get();
        bParArrSize = B[j].get().size();
        bRowStart = 0;
        for (int k = 0; k < bParArrSize; k +=
          bWidth) {
          // Get current row start in C
          tmpCRowStart = cRowStart;
          for (int l = 0; l < bWidth; l++) {
            cArray[tmpCRowStart++] +=
              aArray[aRowStart]
                * bParArr[bRowStart++];
          }
          aRowStart++; // Go to next element in A
        }
      }
      cRowStart += bWidth;
    }
  }
}
