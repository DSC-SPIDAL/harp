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

package edu.iu.harp.util;

import edu.iu.harp.io.Constant;
import edu.iu.harp.resource.Writable;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * PartitionSet is the set of partition IDs on the
 * worker
 ******************************************************/
public class PartitionSet extends Writable {
  private int workerID;
  private IntArrayList parIDs;

  public PartitionSet() {
    workerID = Constant.UNKNOWN_WORKER_ID;
    parIDs = null;
  }

  /**
   * Set the worker ID
   * 
   * @param id
   *          woker ID to set
   */
  public void setWorkerID(int id) {
    this.workerID = id;
  }

  /**
   * Get the worker ID
   * 
   * @return worker ID
   */
  public int getWorkerID() {
    return workerID;
  }

  /**
   * Set the partition ID set
   * 
   * @param partitionSet
   *          partition ID set
   */
  public void
    setParSet(IntArrayList partitionSet) {
    parIDs = partitionSet;
  }

  /**
   * Get the partition ID set
   * 
   * @return partition ID set
   */
  public IntArrayList getParSet() {
    return parIDs;
  }

  /**
   * Get the number of bytes of encoded data 4 for
   * storing worker ID, 4 for storing the size of
   * parIDs, the rest for storing the parIDs
   */
  @Override
  public int getNumWriteBytes() {
    // Worker ID + map size
    if (parIDs != null) {
      return 8 + parIDs.size() * 4;
    } else {
      return 8;
    }
  }

  /**
   * Write the PartitionSet to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(workerID);
    if (parIDs != null) {
      out.writeInt(parIDs.size());
      for (int i = 0; i < parIDs.size(); i++) {
        out.writeInt(parIDs.getInt(i));
      }
    } else {
      out.writeInt(0);
    }
  }

  /**
   * Read PartitionSet from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    this.workerID = in.readInt();
    int size = in.readInt();
    if (size > 0) {
      if (parIDs == null) {
        parIDs = new IntArrayList(size);
      }
      for (int i = 0; i < size; i++) {
        parIDs.add(in.readInt());
      }
    }
  }

  /**
   * Clear the parIDs
   */
  @Override
  public void clear() {
    parIDs = null;
  }
}
