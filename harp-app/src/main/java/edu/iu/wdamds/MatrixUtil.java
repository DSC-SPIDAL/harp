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

public class MatrixUtil {

  public static void matrixMultiply(DoubleArray A,
    DoubleArray B, DoubleArray C, int aHeight,
    int bWidth, int len) {
    double[] aArray = A.get();
    double[] bArray = B.get();
    double[] cArray = C.get();
    if (C.size() != aHeight * bWidth) {
      return;
    }
    double tmp = 0;
    for (int i = 0; i < aHeight; i++) {
      for (int j = 0; j < bWidth; j++) {
        tmp = 0;
        for (int k = 0; k < len; k++) {
          tmp = tmp + aArray[len * i + k]
            * bArray[bWidth * k + j];
        }
        cArray[i * bWidth + j] = tmp;
      }
    }
  }

  public static void matrixMultiply(DoubleArray A,
    Partition<DoubleArray>[] B, DoubleArray C,
    int aHeight, int bWidth, int len) {
    // we need to cross the partitions for each
    // column!
    double[] aArray = A.get();
    double[] cArray = C.get();
    if (C.size() != aHeight * bWidth) {
      return;
    }
    double tmp = 0;
    double[] bParArr;
    double bParHeight = 0;
    for (int i = 0; i < aHeight; i++) {
      for (int j = 0; j < bWidth; j++) {
        tmp = 0;
        for (int k = 0; k < len; k++) {
          for (int l = 0; l < B.length; l++) {
            bParArr = B[l].get().get();
            bParHeight =
              B[l].get().size() / bWidth;
            for (int m = 0; m < bParHeight; m++) {
              tmp = tmp + aArray[len * i + k]
                * bParArr[bWidth * m + j];
            }
          }
        }
        cArray[i * bWidth + j] = tmp;
      }
    }
  }
}
