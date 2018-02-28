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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/*******************************************************
 * ByteArray class for managing writable objects.
 ******************************************************/
public abstract class Writable extends Simple {

  /**
   * Get the number of Bytes of encoded data.
   */
  @Override
  public final int getNumEnocdeBytes() {
    return 1
      + this.getClass().getName().length() * 2 + 4
      + getNumWriteBytes();
  }

  /**
   * Encode the writable as DataOutput
   */
  @Override
  public final void encode(DataOutput out)
    throws IOException {
    out.writeByte(DataType.WRITABLE);
    out.writeUTF(this.getClass().getName());
    this.write(out);
  }

  /**
   * Get a new instance of the class
   * 
   * @return new instance
   */
  public final static <W extends Writable> W
    newInstance(Class<W> clazz) {
    try {
      Constructor<W> constructor =
        clazz.getConstructor();
      return constructor.newInstance();
    } catch (InstantiationException |
      IllegalAccessException |
      IllegalArgumentException |
      InvocationTargetException |
      NoSuchMethodException |
      SecurityException e) {
      return null;
    }
  }

  /**
   * Get a writable from ResourcePool
   * 
   * @return a writable
   */
  public final static <W extends Writable> W
    create(Class<W> clazz) {
    return ResourcePool.get().getWritablePool()
      .getWritable(clazz);
  }

  /**
   * Get the Class object associated with the
   * class or interface with the given string
   * name.
   * 
   * @param className
   * @return the class object
   */
  public final static <W extends Writable>
    Class<W> forClass(String className) {
    try {
      return (Class<W>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Release the writable from the ResourcePool
   */
  @Override
  public final void release() {
    ResourcePool.get().getWritablePool()
      .releaseWritable(this);
  }

  /**
   * Free the writable from the ResourcePool
   */
  @Override
  public final void free() {
    ResourcePool.get().getWritablePool()
      .freeWritable(this);
  }

  /**
   * Abstract method for writing Writable data to
   * DataOutPut
   * 
   * @param out
   * @throws IOException
   */
  public abstract void write(DataOutput out)
    throws IOException;

  /**
   * Abstract method for reading Writable data
   * from DataInput
   * 
   * @param in
   * @throws IOException
   */
  public abstract void read(DataInput in)
    throws IOException;

  /**
   * Abstract method for clearing the Writable
   * data
   */
  public abstract void clear();

  /**
   * Abstract method for getting the number of
   * bytes of the Writable data
   * 
   * @return number of Bytes
   */
  public abstract int getNumWriteBytes();
}
