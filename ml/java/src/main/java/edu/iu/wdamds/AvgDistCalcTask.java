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

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.ShortArray;
import edu.iu.harp.schdynamic.Task;

public class AvgDistCalcTask
  implements Task<RowData, DoubleArray> {

  @Override
  public DoubleArray run(RowData rowData)
    throws Exception {
    ShortArray distArray = rowData.distArray;
    DoubleArray weightArray = rowData.weightArray;
    double average = 0;
    double avgSquare = 0;
    double maxDelta = 0;
    short[] distShorts = distArray.get();
    double[] weightDoubles = weightArray.get();
    // These two arrays should both start at 0
    // and have the same length.
    double distDouble = 0;
    int len = distArray.size();
    for (int i = 0; i < len; i++) {
      if (weightDoubles[i] != 0) {
        distDouble = (double) distShorts[i]
          / (double) Short.MAX_VALUE;
        average += distDouble;
        avgSquare += (distDouble * distDouble);
        if (maxDelta < distDouble) {
          maxDelta = distDouble;
        }
      }
    }
    // Get double[3];
    DoubleArray vals =
      DoubleArray.create(3, false);
    vals.get()[0] = average;
    vals.get()[1] = avgSquare;
    vals.get()[2] = maxDelta;
    return vals;
  }
}
