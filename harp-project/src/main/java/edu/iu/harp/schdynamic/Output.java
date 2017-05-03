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

package edu.iu.harp.schdynamic;

/*******************************************************
 * The output class for tasks
 ******************************************************/
public class Output<O> {

  private O obj;
  private boolean error;

  public Output(O obj, boolean error) {
    this.obj = obj;
    this.error = error;
  }

  /**
   * Get the output object
   * 
   * @return the output object
   */
  public O getOutput() {
    return obj;
  }

  /**
   * Check if it's an error or not
   * 
   * @return true if it's an error, false
   *         otherwise
   */
  public boolean isError() {
    return error;
  }
}
