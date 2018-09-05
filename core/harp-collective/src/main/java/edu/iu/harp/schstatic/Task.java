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

/*******************************************************
 * The abstract class for task definition
 ******************************************************/
public abstract class Task<I, O> {

  private int taskID;
  private int numTasks;
  private Submitter<I> submitter;

  /**
   * The main computation of the task
   *
   * @param input the input
   * @return the output
   * @throws Exception
   */
  public abstract O run(I input) throws Exception;

  /**
   * Get the ID of the task
   *
   * @return
   */
  public int getTaskID() {
    return taskID;
  }

  /**
   * Set the ID of the task
   *
   * @param taskID
   */
  void setTaskID(int taskID) {
    this.taskID = taskID;
  }

  /**
   * Get the number of the task
   *
   * @return
   */
  public int getNumTasks() {
    return this.numTasks;
  }

  /**
   * Set the number of the task
   *
   * @param numTasks
   */
  void setNumTasks(int numTasks) {
    this.numTasks = numTasks;
  }

  /**
   * Get the Submitter of this task
   *
   * @return the Submitter
   */
  public Submitter<I> getSubmitter() {
    return submitter;
  }

  /**
   * Set the Submitter for this task
   *
   * @param submitter the Submitter
   */
  void setSubmitter(Submitter<I> submitter) {
    this.submitter = submitter;
  }
}