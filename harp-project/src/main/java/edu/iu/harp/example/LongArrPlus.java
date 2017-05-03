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

package edu.iu.harp.example;

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.resource.LongArray;

/*******************************************************
 * Combiner for LongArray
 ******************************************************/
public class LongArrPlus
  extends PartitionCombiner<LongArray> {

  /**
   * Combine two LongArrays
   */
  @Override
  public PartitionStatus combine(LongArray curPar,
    LongArray newPar) {
    long[] longs1 = curPar.get();
    int size1 = curPar.size();
    long[] longs2 = newPar.get();
    int size2 = newPar.size();
    if (size1 != size2) {
      // throw new Exception("size1: " + size1
      // + ", size2: " + size2);
      return PartitionStatus.COMBINE_FAILED;
    }
    for (int i = 0; i < size2; i++) {
      longs1[i] += longs2[i];
    }
    return PartitionStatus.COMBINED;
  }
}
