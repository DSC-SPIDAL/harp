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

package edu.iu.harp.resource;

import edu.iu.harp.io.DataType;

import java.io.DataOutput;
import java.io.IOException;

/*******************************************************
 * ByteArray class for managing byte[] data.
 ******************************************************/
public final class ByteArray
  extends Array<byte[]> {

  public ByteArray(byte[] arr, int start,
    int size) {
    super(arr, start, size);
  }

  /**
   * Get the number of Bytes of encoded data. One
   * byte for storing DataType, four bytes for
   * storing the size, size bytes for storing the
   * data.
   */
  @Override
  public int getNumEnocdeBytes() {
    // array type, array length, array body
    return size + 5;
  }

  /**
   * Encode the array as DataOutput
   */
  @Override
  public void encode(DataOutput out)
    throws IOException {
    out.writeByte(DataType.BYTE_ARRAY);
    out.writeInt(size);
    out.write(array, start, size);
  }

  /**
   * Create an array. Firstly try to get an array
   * from ResourcePool; if failed, new an array.
   * 
   * @param len
   * @param approximate
   * @return
   */
  public static ByteArray create(int len,
    boolean approximate) {
    if (len > 0) {
      byte[] bytes =
        ResourcePool.get().getBytesPool()
          .getArray(len, approximate);
      if (bytes != null) {
        return new ByteArray(bytes, 0, len);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Release the array from the ResourcePool
   */
  @Override
  public void release() {
    ResourcePool.get().getBytesPool()
      .releaseArray(array);
    this.reset();
  }

  /**
   * Free the array from the ResourcePool
   */
  @Override
  public void free() {
    ResourcePool.get().getBytesPool()
      .freeArray(array);
    this.reset();

  }
}