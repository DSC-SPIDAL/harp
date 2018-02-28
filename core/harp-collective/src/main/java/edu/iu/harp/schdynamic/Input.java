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
 * The input class for tasks
 ******************************************************/
public class Input<I> {

  private I object;
  private boolean pause;
  private boolean stop;

  public Input(I object, boolean pause,
    boolean stop) {
    this.object = object;
    this.pause = pause;
    this.stop = stop;
  }

  /**
   * Get the input object
   * 
   * @return the input object
   */
  public I getInput() {
    return object;
  }

  /**
   * Check if the input is paused or not
   * 
   * @return true if paused, false otherwise
   */
  public boolean isPause() {
    return pause;
  }

  /**
   * Check if the input is stopped or not
   * 
   * @return true if the input is stopped, false
   *         otherwise
   */
  public boolean isStop() {
    return stop;
  }
}
