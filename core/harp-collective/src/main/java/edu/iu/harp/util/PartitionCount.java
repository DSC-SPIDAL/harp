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

import edu.iu.harp.resource.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * PartitionCount records the number of partitions
 * on the worker
 ******************************************************/
public class PartitionCount extends Writable {
  private int workerID;
  private int partitionCount;

  public PartitionCount() {
  }

  /**
   * Write this to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(workerID);
    out.writeInt(partitionCount);
  }

  /**
   * Read this from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    this.workerID = in.readInt();
    this.partitionCount = in.readInt();
  }

  /**
   * Set the workerID
   * 
   * @param workerID
   *          the ID of the worker
   */
  public void setWorkerID(int workerID) {
    this.workerID = workerID;
  }

  /**
   * Get the ID of the worker
   * 
   * @return the ID of the worker
   */
  public int getWorkerID() {
    return workerID;
  }

  /**
   * Set the count of partitions
   * 
   * @param pCount
   *          the count of partitions
   */
  public void setPartitionCount(int pCount) {
    this.partitionCount = pCount;
  }

  /**
   * Get the count of partitions
   * 
   * @return the count of partitions
   */
  public int getPartitionCount() {
    return this.partitionCount;
  }

  /**
   * Get the number of bytes of the encoded data
   */
  @Override
  public int getNumWriteBytes() {
    return 8;
  }

  /**
   * Clear the data
   */
  @Override
  public void clear() {
  }
}
