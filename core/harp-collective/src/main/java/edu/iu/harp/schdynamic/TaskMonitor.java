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

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/*******************************************************
 * Monitor and manage tasks
 ******************************************************/
public class TaskMonitor<I, O, T extends Task<I, O>>
  implements Runnable {

  protected static final Logger LOG =
    Logger.getLogger(TaskMonitor.class);

  private final BlockingDeque<Input<I>> inputQueue;
  private final BlockingQueue<Output<O>> outputQueue;
  private final T taskObject;
  private final Semaphore barrier1;
  private final Semaphore barrier2;

  TaskMonitor(BlockingDeque<Input<I>> inQueue,
    BlockingQueue<Output<O>> outQueue, T task,
    Semaphore barrier1) {
    inputQueue = inQueue;
    outputQueue = outQueue;
    taskObject = task;
    this.barrier1 = barrier1;
    this.barrier2 = new Semaphore(0);
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
            break;
          } else if (input.isPause()) {
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
      } catch (InterruptedException e) {
        LOG.error("Fail to run input", e);
        continue;
      }
    }
  }
}
