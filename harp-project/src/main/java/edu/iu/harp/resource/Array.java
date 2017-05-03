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

/*******************************************************
 * The abstract class of arrays. It's used for
 * managing real array data.
 ******************************************************/
public abstract class Array<T> extends Simple {
  /** The array */
  protected T array = null;
  /**
   * Start position in the array data, -1 by
   * default
   */
  protected int start = -1;
  /** Size of the array data, -1 by default */
  protected int size = -1;

  public Array(T arr, int start, int size) {
    this.array = arr;
    this.start = start;
    this.size = size;
  }

  /**
   * Get the array body.
   * 
   * @return the array
   */
  public T get() {
    return array;
  }

  /**
   * Get the start index of the array
   * 
   * @return start index
   */
  public int start() {
    return start;
  }

  /**
   * Get the size of the array.
   * 
   * @return array size
   */
  public int size() {
    return size;
  }

  /**
   * Reset the array
   */
  protected void reset() {
    array = null;
    start = -1;
    size = -1;
  }
}
