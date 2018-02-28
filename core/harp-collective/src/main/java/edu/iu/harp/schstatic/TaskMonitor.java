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
import edu.iu.harp.schdynamic.Output;
import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/*******************************************************
 * Monitor and manage tasks
 ******************************************************/
public class TaskMonitor<I, O, T extends Task<I, O>>
  implements Runnable {
  protected static final Logger LOG =
    Logger.getLogger(TaskMonitor.class);
  /** the task object */
  private final T taskObject;
  /** the input queue */
  private final BlockingQueue<Input<I>> inputQueue;
  /** the output queue */
  private final BlockingQueue<Output<O>> outputQueue;
  private int inputCount;
  private int outputCount;
  private int errorCount;
  private boolean isRunning;
  private final Semaphore barrier1;
  private final Semaphore barrier2;

  TaskMonitor(int taskID, T task,
    Submitter<I> submitter, int numTasks,
    Semaphore barrier1) {
    this.taskObject = task;
    this.taskObject.setTaskID(taskID);
    this.taskObject.setNumTasks(numTasks);
    this.taskObject.setSubmitter(submitter);
    this.inputQueue = new LinkedBlockingQueue<>();
    this.outputQueue =
      new LinkedBlockingQueue<>();
    this.inputCount = 0;
    this.outputCount = 0;
    this.errorCount = 0;
    this.barrier1 = barrier1;
    this.barrier2 = new Semaphore(0);
  }

  /**
   * Get the task object
   * 
   * @return the task object
   */
  T getTask() {
    return taskObject;
  }

  /**
   * Start the task
   */
  synchronized void start() {
    if (!isRunning) {
      isRunning = true;
      inputCount += inputQueue.size();
    }
  }

  /**
   * Submit the input to the task
   * 
   * @param input
   *          the input
   */
  synchronized void submit(Input<I> input) {
    inputQueue.add(input);
    if (isRunning && !input.isPause()
      && !input.isStop()) {
      inputCount++;
    }
  }

  /**
   * Pause or stop the task
   */
  private synchronized void pauseOrStop() {
    if (isRunning) {
      isRunning = false;
      inputCount -= inputQueue.size();
    }
  }

  /**
   * Clean the input queue
   */
  synchronized void cleanInputQueue() {
    if (!isRunning) {
      inputQueue.clear();
    }
  }

  /**
   * Check if has output
   * 
   * @return true if has output, false otherwise
   */
  synchronized boolean hasOutput() {
    return inputCount > outputCount;
  }

  /**
   * Blocked and wait for output
   * 
   * @return the output
   */
  O waitForOutput() {
    // If no output is available, wait for one
    if (hasOutput()) {
      boolean isFailed = false;
      Output<O> output = null;
      do {
        try {
          output = outputQueue.take();
          isFailed = false;
        } catch (Exception e) {
          output = null;
          isFailed = true;
          LOG.error("Error when waiting output",
            e);
        }
      } while (isFailed);
      outputCount++;
      if (output.isError()) {
        errorCount++;
      }
      return output.getOutput();
    } else {
      return null;
    }
  }

  /**
   * Check if has errors or not
   * 
   * @return true if has erros, false otherwise
   */
  boolean hasError() {
    int count = errorCount;
    errorCount = 0;
    return count > 0;
  }

  /**
   * Release the barrier
   */
  void release() {
    barrier2.release();
  }

  /**
   * The main process of monitoring and managing
   * tasks
   */
  @Override
  public void run() {
    while (true) {
      try {
        Input<I> input = inputQueue.take();
        if (input != null) {
          if (input.isStop()) {
            pauseOrStop();
            break;
          } else if (input.isPause()) {
            // Pause
            pauseOrStop();
            barrier1.release();
            ComputeUtil.acquire(barrier2);
          } else {
            O output = null;
            boolean isFailed = false;
            try {
              output =
                taskObject.run(input.getInput());
            } catch (Exception e) {
              output = null;
              isFailed = true;
              LOG.error(
                "Error when processing input", e);
            }
            if (isFailed) {
              outputQueue
                .add(new Output<>(null, true));
            } else {
              outputQueue
                .add(new Output<>(output, false));
            }
          }
        }
      } catch (Throwable t) {
        LOG.error("Fail to run input", t);
      }
    }
  }
}
