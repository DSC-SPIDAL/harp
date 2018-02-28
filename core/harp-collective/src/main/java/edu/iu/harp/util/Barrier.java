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
 * The Barrier class
 ******************************************************/
public class Barrier extends Writable {

  private boolean status;

  public Barrier() {
    status = false;
  }

  /**
   * Set the status
   * 
   * @param st
   *          the status
   */
  public void setStatus(boolean st) {
    status = st;
  }

  /**
   * Get the status
   * 
   * @return the status
   */
  public boolean getStatus() {
    return status;
  }

  /**
   * Write the status to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeBoolean(status);
  }

  /**
   * Read the status from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    status = in.readBoolean();
  }

  /**
   * Get the number of bytes of the encoded data
   */
  @Override
  public int getNumWriteBytes() {
    return 1;
  }

  /**
   * Clear the data
   */
  @Override
  public void clear() {
  }
}
