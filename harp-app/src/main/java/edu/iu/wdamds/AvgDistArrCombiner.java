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

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.resource.DoubleArray;

public class AvgDistArrCombiner
  extends PartitionCombiner<DoubleArray> {

  @Override
  public PartitionStatus combine(
    DoubleArray curPar, DoubleArray newPar) {
    double[] doubles1 = curPar.get();
    int start1 = curPar.start();
    int size1 = curPar.size();
    double[] doubles2 = newPar.get();
    int start2 = newPar.start();
    int size2 = newPar.size();
    if (size1 != 3 || size2 != 3 || start1 != 0
      || start2 != 0) {
      return PartitionStatus.COMBINE_FAILED;
    }
    for (int i = 0; i < 2; i++) {
      doubles1[i] = doubles1[i] + doubles2[i];
    }
    if (doubles1[2] < doubles2[2]) {
      doubles1[2] = doubles2[2];
    }
    return PartitionStatus.COMBINED;
  }
}
