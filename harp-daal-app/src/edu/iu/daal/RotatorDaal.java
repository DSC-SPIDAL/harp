/*
 * Copyright 2013-2016 Indiana University
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

package edu.iu.daal;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Array;
import edu.iu.harp.schstatic.StaticScheduler;
import edu.iu.dymoro.*;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.NumericTable;

/**
 * @brief A Rotator that also includes the data conversion between Harp and DAAL
 */
public class RotatorDaal<I, P extends Array<I> > {

  protected static final Log LOG = LogFactory
    .getLog(RotatorDaal.class);

  private NumericTable daal_table;                      // daal_table to hold H Table 
  private List<RotateTaskDaal<I, P> > rotateTasks;
  private StaticScheduler<Integer, NumericTable, RotateTaskDaal<I, P> > rotation;

  public RotatorDaal(Table<P>[] tableMap,
    int rdim, int numThreads, CollectiveMapper<?, ?, ?, ?> mapper,
    int[] orders, String contextName) {

    this.daal_table = null;
    this.rotateTasks = new LinkedList<>();

    for (int i = 0; i < tableMap.length; i++) {
        //tableMap.length == sliceNumber of H model
      rotateTasks.add(new RotateTaskDaal<>(
        tableMap[i], rdim, numThreads, mapper, orders, contextName));
    }

    rotation = new StaticScheduler<>(rotateTasks);

  }

  /**
   * @brief Accessor of daal_table
   *
   * @return 
   */
  public NumericTable daal_table() {
      return this.daal_table;
  }

  /**
   * @brief get a daal table from rotator
   *
   * @param taskID
   *
   * @return 
   */
  public NumericTable getDaal_Table(
    int taskID) {

    //taskID is the id of H model slices
    if (rotation.hasOutput(taskID)) {
      daal_table = rotation.waitForOutput(taskID);
    } else {
      LOG.info("No task rotation output !!!");
      daal_table = rotation.getTask(taskID).daal_table();
    }

    return daal_table;
  }

  /**
   * @brief submit a rotation task
   *
   * @param taskID
   *
   * @return 
   */
  public void rotate(int taskID) {
    rotation.submit(taskID, 1);
  }

  public void start() {
    rotation.start();
  }

  public void pause() {
    rotation.pause();
  }

  public void stop() {
    rotation.stop();
  }
}
