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

import edu.iu.harp.schdynamic.ComputeUtil;
import edu.iu.harp.schdynamic.Input;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.Semaphore;

/*******************************************************
 * The static scheduler
 ******************************************************/
public class StaticScheduler<I, O, T extends Task<I, O>> {
  protected static final Logger LOG =
    Logger.getLogger(StaticScheduler.class);

  private Thread[] threads;
  private boolean isRunning;
  private boolean isPausing;
  private final TaskMonitor<I, O, T>[] taskMonitors;
  private final int numTaskMonitors;
  private final Submitter<I> submitter;
  private final Semaphore barrier1;

  @SuppressWarnings("unchecked")
  public StaticScheduler(List<T> tasks) {
    threads = null;
    barrier1 = new Semaphore(0);
    numTaskMonitors = tasks.size();
    taskMonitors =
      new TaskMonitor[numTaskMonitors];
    submitter = new Submitter<>(taskMonitors);
    int i = 0;
    for (T task : tasks) {
      taskMonitors[i] = new TaskMonitor<>(i, task,
        submitter, numTaskMonitors, barrier1);
      i++;
    }
  }

  /**
   * Get the Task by its ID
   * 
   * @param taskID
   *          the ID of the task
   * @return the Task
   */
  public T getTask(int taskID) {
    return taskMonitors[taskID].getTask();
  }

  /**
   * Submit the input to task
   * 
   * @param taskID
   *          the ID of the task
   * @param input
   *          the input
   */
  public synchronized void submit(int taskID,
    I input) {
    submitter.submit(taskID, input);
  }

  /**
   * Start to schedule tasks
   */
  public synchronized void start() {
    if (!isRunning) {
      isRunning = true;
      if (isPausing) {
        isPausing = false;
        for (TaskMonitor<I, O, T> monitor : taskMonitors) {
          monitor.start();
          monitor.release();
        }
      } else {
        threads = new Thread[numTaskMonitors];
        int i = 0;
        for (TaskMonitor<I, O, T> monitor : taskMonitors) {
          threads[i] = new Thread(monitor);
          monitor.start();
          threads[i].start();
          i++;
        }
      }
    }
  }

  /**
   * Pause the scheduling
   */
  public synchronized void pause() {
    if (isRunning && !isPausing) {
      isRunning = false;
      isPausing = true;
      for (TaskMonitor<I, O, T> taskMonitor : taskMonitors) {
        taskMonitor.submit(
          new Input<I>(null, true, false));
      }
      ComputeUtil.acquire(barrier1,
        numTaskMonitors);
    }
  }

  /**
   * Clean the queue of input
   */
  public synchronized void cleanInputQueue() {
    if (isPausing || !isRunning) {
      for (TaskMonitor<I, O, T> taskMonitor : taskMonitors) {
        taskMonitor.cleanInputQueue();
      }
    }
  }

  /**
   * Stop scheduling
   */
  public synchronized void stop() {
    if (isPausing) {
      start();
    }
    if (isRunning) {
      isRunning = false;
      for (TaskMonitor<I, O, T> taskMonitor : taskMonitors) {
        taskMonitor.submit(
          new Input<I>(null, false, true));
      }
      for (int i = 0; i < numTaskMonitors; i++) {
        ComputeUtil.joinThread(threads[i]);
      }
      threads = null;
    }
  }

  /**
   * 
   * Blocked and wait for output. Invoke as
   * while(hasOutput()) { waitForOutput(); }
   * 
   * @return the output
   */
  public O waitForOutput(int taskID) {
    if (taskID < taskMonitors.length) {
      return taskMonitors[taskID].waitForOutput();
    } else {
      return null;
    }
  }

  /**
   * Check if has a new output
   * 
   * @param taskID
   *          the ID of the task
   * @return true if has a new output, false
   *         otherwise
   */
  public boolean hasOutput(int taskID) {
    if (taskID < taskMonitors.length) {
      return taskMonitors[taskID].hasOutput();
    } else {
      return false;
    }
  }

  /**
   * Check if has errors on this task
   * 
   * @param taskID
   *          the ID of the task
   * @return true if has errors, false otherwise
   */
  public boolean hasError(int taskID) {
    if (taskID < taskMonitors.length) {
      return taskMonitors[taskID].hasError();
    } else {
      return false;
    }
  }
}
