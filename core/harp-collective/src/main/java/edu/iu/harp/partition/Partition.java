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

package edu.iu.harp.partition;

import edu.iu.harp.io.Constant;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;

import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * Partition provides a wrapper on an array or an
 * object. Partitions have partition IDs which is
 * used to identify themselves in the distributed
 * dataset.
 ******************************************************/
public final class Partition<P extends Simple>
  extends Transferable {
  private int partitionID;
  private P partition;

  public Partition(int partitionID, P partition) {
    this.partition = partition;
    this.partitionID = partitionID;
  }

  /**
   * Get the partition ID
   * 
   * @return ID
   */
  public int id() {
    return partitionID;
  }

  /**
   * Get the partition body
   * 
   * @return partition body
   */
  public P get() {
    return partition;
  }

  /**
   * Get the number bytes of encoded data. Four
   * for storing the partitionID, plus the number
   * bytes of encoded partition body data
   */
  @Override
  public int getNumEnocdeBytes() {
    return 4 + partition.getNumEnocdeBytes();
  }

  /**
   * Encode the partition data as DataOutput
   */
  @Override
  public void encode(DataOutput out)
    throws IOException {
    partition.encode(out);
    out.writeInt(partitionID);
  }

  /**
   * Release and reset the partition
   */
  @Override
  public void release() {
    partition.release();
    this.reset();
  }

  /**
   * Free and reset the partition
   */
  @Override
  public void free() {
    partition.free();
    this.reset();
  }

  /**
   * Reset the partition
   */
  private void reset() {
    partitionID = Constant.UNKNOWN_PARTITION_ID;
    partition = null;
  }
}
