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
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

public class InnerProductCalcTask implements
  Task<Partition<DoubleArray>, DoubleArray> {

  private final Table<DoubleArray> refTable;

  public InnerProductCalcTask(
    Table<DoubleArray> refTable) {
    this.refTable = refTable;
  }

  @Override
  public DoubleArray
    run(Partition<DoubleArray> partition)
      throws Exception {
    double[] xrs = partition.get().get();
    int xRowSize = partition.get().size();
    double sum = 0;
    if (refTable == null) {
      for (int i = 0; i < xRowSize; i++) {
        sum += xrs[i] * xrs[i];
      }
    } else {
      // It is safe to get value from a shared map
      // in parallel
      double[] rrs = refTable
        .getPartition(partition.id()).get().get();
      for (int i = 0; i < xRowSize; i++) {
        sum += xrs[i] * rrs[i];
      }
    }
    // Get double[1];
    DoubleArray array =
      DoubleArray.create(1, false);
    array.get()[0] = sum;
    return array;
  }
}