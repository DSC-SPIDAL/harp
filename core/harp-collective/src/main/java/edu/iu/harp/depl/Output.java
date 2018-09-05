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

package edu.iu.harp.depl;

import java.util.ArrayList;
import java.util.List;

/*******************************************************
 * The wrapper class of the outputs from command
 * execution
 ******************************************************/
public class Output {
  private boolean status;
  private List<String> output;

  Output() {
    status = true;
    output = new ArrayList<>();
  }

  /**
   * Set the status of the execution
   *
   * @param isSuccess the status
   */
  void setExeStatus(boolean isSuccess) {
    status = isSuccess;
  }

  /**
   * Get the status of the execution
   *
   * @return true if the execution succeeded,
   * false other wise
   */
  public boolean getExeStatus() {
    return status;
  }

  /**
   * Get the output from the command execution
   *
   * @return
   */
  public List<String> getExeOutput() {
    return output;
  }
}