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

package edu.iu.harp.client;

import edu.iu.harp.io.Constant;
import edu.iu.harp.schdynamic.ComputeUtil;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;

/*******************************************************
 * Synchronous client
 ******************************************************/
public class SyncClient implements Runnable {

  protected static final Logger LOG =
    Logger.getLogger(SyncClient.class);

  private final Workers workers;
  private final int selfID;
  /** Dest ID and context name mapping */
  private final ConcurrentMap<Integer, ConcurrentMap<String, SyncQueue>> eventQueueMap;
  private final BlockingQueue<SyncQueue> consumerQueue;
  private final Thread client;

  final static int ALL_WORKERS =
    Integer.MAX_VALUE;
  final static int NO_WORKERS = Integer.MIN_VALUE;
  private final int initCapacity =
    Constant.NUM_THREADS;

  public SyncClient(Workers workers) {
    this.workers = workers;
    this.selfID = workers.getSelfID();
    eventQueueMap =
      new ConcurrentHashMap<>(initCapacity);
    consumerQueue = new LinkedBlockingQueue<>();
    this.client = new Thread(this);
  }

  /*******************************************************
   * Task definition for sending
   ******************************************************/
  private class SendTask extends RecursiveAction {
    /** Generated serial ID */
    private static final long serialVersionUID =
      2147825101521531758L;

    private final SyncQueue queue;

    private SendTask(SyncQueue queue) {
      this.queue = queue;
    }

    /**
     * The main computation performed by this
     * task.
     */
    @Override
    public void compute() {
      try {
        queue.enterConsumeBarrier();
        queue.send();
      } catch (Exception e) {
        LOG.error("Send thread fails.", e);
      }
    }
  }

  /**
   * Submit a message event
   * 
   * @param event
   *          the event to submit
   * @return true if succeeded, false otherwise
   */
  public boolean submitMessageEvent(Event event) {
    // Add event to the queue
    // Message Event to separate worker queues
    // Add queue to the consumer blocking queue
    if (event
      .getEventType() == EventType.MESSAGE_EVENT
      && event.getBody() != null) {
      if (event.getTargetID() != selfID) {
        SyncQueue queue =
          getSyncQueue(event.getTargetID(),
            event.getContextName());
        queue.add(event.getBody());
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Submit a collective event (broadcast)
   * 
   * @param event
   * @param hasFuture
   * @return
   */
  public boolean
    submitCollectiveEvent(Event event) {
    // Add event to the queue
    // Collective event to a special queue
    // Add queue to the consumer blocking queue
    if (event
      .getEventType() == EventType.COLLECTIVE_EVENT
      && event.getBody() != null) {
      SyncQueue queue = getSyncQueue(ALL_WORKERS,
        event.getContextName());
      queue.add(event.getBody());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get or create a sync queue.
   * 
   * @param destID
   *          the ID of the destination
   * @param contextName
   *          the name of the context
   * @return the SyncQueue
   */
  private SyncQueue getSyncQueue(int destID,
    String contextName) {
    ConcurrentMap<String, SyncQueue> queueMap =
      eventQueueMap.get(destID);
    if (queueMap == null) {
      queueMap = new ConcurrentHashMap<>();
      ConcurrentMap<String, SyncQueue> oldQueueMap =
        eventQueueMap.putIfAbsent(destID,
          queueMap);
      if (oldQueueMap != null) {
        queueMap = oldQueueMap;
      }
    }
    SyncQueue queue = queueMap.get(contextName);
    if (queue == null) {
      queue = new SyncQueue(contextName, selfID,
        destID, workers, consumerQueue);
      SyncQueue oldQueue =
        queueMap.putIfAbsent(contextName, queue);
      if (oldQueue != null) {
        queue = oldQueue;
      }
    }
    return queue;
  }

  /**
   * Start the client
   */
  public void start() {
    client.start();
  }

  /**
   * Main process by this client Go through each
   * queue, drain the queue and send/broadcast
   */
  @Override
  public void run() {
    while (true) {
      SyncQueue queue = null;
      try {
        queue = consumerQueue.take();
      } catch (InterruptedException e) {
        LOG.error(
          "Error when taking from the queue", e);
        continue;
      }
      if (queue.getDestID() != NO_WORKERS) {
        SendTask sendTask = new SendTask(queue);
        sendTask.fork();
      } else {
        break;
      }
    }
  }

  /**
   * Send StopSign to the execution threads
   */
  public void stop() {
    SyncQueue queue =
      getSyncQueue(NO_WORKERS, "");
    queue.add(null);
    ComputeUtil.joinThread(client);
  }
}
