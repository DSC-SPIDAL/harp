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

import edu.iu.harp.keyval.Value;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * Int-type Value
 ******************************************************/
public class IntVal extends Value {

  private int val;

  public IntVal() {
  }

  public IntVal(int val) {
    this.val = val;
  }

  /**
   * Set the value
   * 
   * @param val
   *          the value
   */
  public void setInt(int val) {
    this.val = val;
  }

  /**
   * Get the value
   * 
   * @return the value
   */
  public int getInt() {
    return this.val;
  }

  /**
   * Write the value to DataOutput
   */
  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(val);
  }

  /**
   * Read the value from DataInput
   */
  @Override
  public void read(DataInput in)
    throws IOException {
    this.val = in.readInt();
  }

  /**
   * Get the number of bytes of the data
   */
  @Override
  public int getNumWriteBytes() {
    return 4;
  }

  /**
   * Clear the value
   */
  @Override
  public void clear() {
  }
}
