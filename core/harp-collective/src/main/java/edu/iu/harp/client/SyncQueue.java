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
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;

/*******************************************************
 * Synchronous queue
 ******************************************************/
public class SyncQueue {

  protected static final Logger LOG =
    Logger.getLogger(SyncQueue.class);

  private final String contextName;
  private final int sourceID;
  private final int destID;
  private final Workers workers;
  private LinkedList<Transferable> queue;
  private LinkedList<Transferable> localList;
  private final BlockingQueue<SyncQueue> consumerQueue;
  private boolean inConsumerQueue;
  private boolean isConsuming;

  SyncQueue(String contextName, int sourceID,
    int destID, Workers workers,
    BlockingQueue<SyncQueue> consumerQueue) {
    this.contextName = contextName;
    this.sourceID = sourceID;
    this.destID = destID;
    this.workers = workers;
    this.queue = new LinkedList<>();
    this.localList = new LinkedList<>();
    this.consumerQueue = consumerQueue;
    this.inConsumerQueue = false;
    this.isConsuming = false;
  }

  /**
   * Get the context name
   * 
   * @return the context name
   */
  String getContextName() {
    return contextName;
  }

  /**
   * Get the ID of the destination
   * 
   * @return the ID of the destination
   */
  int getDestID() {
    return destID;
  }

  /**
   * Add an element
   * 
   * @param trans
   */
  synchronized void add(Simple trans) {
    // Only add to the consumer queue if it is
    // taken from the queue
    if (inConsumerQueue) {
      // LOG.info("this queue is in consumer
      // queue");
      if (trans != null) {
        queue.addLast(trans);
      }
    } else if (isConsuming) {
      // Add to local list
      // LOG
      // .info("this queue is consuming, add to
      // local list");
      if (trans != null) {
        localList.addLast(trans);
      }
    } else {
      // LOG
      // .info("this queue is not consuming, add
      // to consumer queue.");
      if (trans != null) {
        queue.addLast(trans);
      }
      consumerQueue.add(this);
      inConsumerQueue = true;
    }
  }

  /**
   * Enter to the consuming state. If it's in the
   * consumer queue, then it's only allowed to
   * take, not sending.
   */
  synchronized void enterConsumeBarrier() {
    if (inConsumerQueue) {
      inConsumerQueue = false;
      if (!isConsuming) {
        isConsuming = true;
      }
    }
  }

  /**
   * Send data. It should be in the consuming
   * state
   */
  void send() {
    if (!inConsumeBarrier()) {
      return;
    }
    LinkedList<Transferable> transList = queue;
    if (transList.isEmpty()) {
      return;
    }
    Data data = new Data(DataType.SIMPLE_LIST,
      contextName, sourceID, transList,
      DataUtil.getNumTransListBytes(transList));
    if (destID == SyncClient.ALL_WORKERS) {
      // Broadcast events
      Sender sender = new DataMSTBcastSender(data,
        workers, Constant.MST_BCAST_DECODE);
      sender.execute();
    } else {
      // Send events to worker ID
      Sender sender = new DataSender(data, destID,
        workers, Constant.SEND_DECODE);
      sender.execute();
    }
    // Release the data
    data.release();
    leaveConsumeBarrier();
  }

  /**
   * Check if it's in consuming state
   * 
   * @return true if it's in consuming state,
   *         false otherwise
   */
  private synchronized boolean
    inConsumeBarrier() {
    if (!inConsumerQueue && isConsuming) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Exit the consuming state
   */
  private synchronized void
    leaveConsumeBarrier() {
    if (!inConsumerQueue && isConsuming) {
      queue.clear();
      isConsuming = false;
      if (!localList.isEmpty()) {
        // Exchange the local list and the queue
        LinkedList<Transferable> tmpList = null;
        tmpList = queue;
        queue = localList;
        localList = tmpList;
        consumerQueue.add(this);
        inConsumerQueue = true;
      }
    }
  }
}
