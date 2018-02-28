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

import java.util.Random;

public class XInitializeTask implements
  Task<RowData, Partition<DoubleArray>> {

  private final int d;

  public XInitializeTask(int d) {
    this.d = d;
  }

  @Override
  public Partition<DoubleArray>
    run(RowData rowData) throws Exception {
    // Copy to local
    int d = this.d;
    Random rand = new Random(rowData.rowOffset);
    int arrSize = rowData.height * d;
    DoubleArray doubleArray =
      DoubleArray.create(arrSize, true);
    double[] doubles = doubleArray.get();
    for (int i = 0; i < arrSize; i++) {
      if (rand.nextBoolean()) {
        doubles[i] = rand.nextDouble();
      } else {
        doubles[i] = 0 - rand.nextDouble();
      }
    }
    return new Partition<DoubleArray>(rowData.row,
      doubleArray);
  }
}
