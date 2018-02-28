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

package edu.iu.dymoro;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public abstract class MPTask<D, S extends Simple>
  implements
  Task<RowColSplit<D, S>, RowColSplit<D, S>> {

  protected static final Log LOG =
    LogFactory.getLog(MPTask.class);

  private long numItems = 0L;

  private long startTime = 0L;
  private long endTime = 0L;
  private long itemsRecored = 0L;
  private boolean record = false;

  @Override
  public RowColSplit<D, S> run(
    RowColSplit<D, S> split) throws Exception {
    long n = doRun(split.cData, split.rData);
    numItems += n;
    // ----------------------------------
    // Code for recording
    if (record) {
      itemsRecored += n;
      endTime = System.currentTimeMillis();
    }
    // ----------------------------------
    return split;
  }

  public abstract long
    doRun(List<Partition<S>> cData, D rData);

  public long getNumItemsProcessed() {
    long n = numItems;
    numItems = 0L;
    return n;
  }

  public void startRecord(boolean r) {
    record = r;
    if (record) {
      startTime = System.currentTimeMillis();
      itemsRecored = 0L;
    }
  }

  public long getRecordDuration() {
    return endTime - startTime;
  }

  public long getItemsRecorded() {
    return itemsRecored;
  }
}
