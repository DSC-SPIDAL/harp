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

package edu.iu.harp.schstatic;

import edu.iu.harp.schdynamic.Input;

/*******************************************************
 * The Submitter is used for submitting tasks
 ******************************************************/
public class Submitter<I> {

  private TaskMonitor<I, ?, ? extends Task<I, ?>>[] taskMonitors;

  public Submitter(
    TaskMonitor<I, ?, ? extends Task<I, ?>>[] taskMonitors) {
    this.taskMonitors = taskMonitors;
  }

  /**
   * Submit the task
   * 
   * @param taskID
   *          the ID of the task
   * @param input
   *          the input
   */
  public void submit(int taskID, I input) {
    if (taskID < taskMonitors.length
      && input != null) {
      taskMonitors[taskID]
        .submit(new Input<>(input, false, false));
    }
  }
}