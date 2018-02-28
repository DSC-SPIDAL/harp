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

import java.util.Arrays;

public class MMCalcTask implements
  Task<RowData, Partition<DoubleArray>> {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(MMCalcTask.class);

  private final Partition<DoubleArray>[] xPartitions;
  private final int d;

  public MMCalcTask(
    Partition<DoubleArray>[] xPartitions, int d) {
    this.xPartitions = xPartitions;
    this.d = d;
  }

  @Override
  public Partition<DoubleArray>
    run(RowData rowData) throws Exception {
    // Copy to local
    int d = this.d;
    int mmSize = rowData.height * d;
    DoubleArray mmArray =
      DoubleArray.create(mmSize, true);
    // mms array should be small, hope this won't
    // generate large overhead.
    Arrays.fill(mmArray.get(), 0, mmSize, 0);
    CalcUtil.matrixMultiply(rowData.vArray,
      xPartitions, mmArray, rowData.height, d,
      rowData.width);
    return new Partition<DoubleArray>(rowData.row,
      mmArray);
  }
}
