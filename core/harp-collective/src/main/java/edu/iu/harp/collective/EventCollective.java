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

package edu.iu.harp.collective;

import edu.iu.harp.client.Event;
import edu.iu.harp.client.EventType;
import edu.iu.harp.client.SyncClient;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.server.Server;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class EventCollective {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(EventCollective.class);

  public static void main(String args[])
    throws Exception {
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    int workerID = Integer.parseInt(args[2]);
    long jobID = Long.parseLong(args[3]);
    // Initialize log
    Driver.initLogger(workerID);
    LOG.info(
      "args[] " + driverHost + " " + driverPort
        + " " + workerID + " " + jobID);
    // ---------------------------------------------------
    // Worker initialize
    EventQueue eventQueue = new EventQueue();
    DataMap dataMap = new DataMap();
    Workers workers = new Workers(workerID);
    SyncClient client = new SyncClient(workers);
    Server server =
      new Server(workers.getSelfInfo().getNode(),
        workers.getSelfInfo().getPort(),
        eventQueue, dataMap, workers);
    client.start();
    server.start();
    String contextName = jobID + "";
    // Barrier guarantees the living workers get
    // the same view of the barrier result
    LOG.info("Start Barrier");
    boolean isSuccess = Communication.barrier(
      contextName, "barrier", dataMap, workers);
    LOG.info("Barrier: " + isSuccess);
    // -----------------------------------------------
    try {
      LOG.info("Start sending message event.");
      int eventCount = 0;
      // Send message event
      for (int i = 0; i < 10; i++) {
        int destID = i % workers.getNumWorkers();
        if (destID != workers.getMasterID()) {
          if (workers.isMaster()) {
            DoubleArray doubleArray =
              BcastCollective
                .generateDoubleArray(100);
            client.submitMessageEvent(
              new Event(EventType.MESSAGE_EVENT,
                contextName, workers.getSelfID(),
                destID, doubleArray));
          } else {
            if (destID == workers.getSelfID()) {
              eventCount++;
            }
          }
        }
      }
      // Send collective event
      LOG.info("Start sending collective event.");
      for (int i = 0; i < 10; i++) {
        if (workers.isMaster()) {
          DoubleArray doubleArray =
            BcastCollective
              .generateDoubleArray(100);
          client.submitCollectiveEvent(new Event(
            EventType.COLLECTIVE_EVENT,
            contextName, workers.getSelfID(),
            workers.getMaxID() + 1, doubleArray));
        } else {
          eventCount++;
        }
      }
      // Receive event
      LOG.info("Start receiving. Event count: "
        + eventCount);
      for (int i = 0; i < eventCount; i++) {
        Event event = eventQueue.waitEvent();
        DoubleArray doubleArray =
          (DoubleArray) event.getBody();
        int start = doubleArray.start();
        int size = doubleArray.size();
        LOG.info("Receive event: "
          + event.getEventType()
          + ". Double Array: First double: "
          + doubleArray.get()[start]
          + ", last double: "
          + doubleArray.get()[start + size - 1]);
        doubleArray.release();
      }
    } catch (Exception e) {
      LOG.error("Error in send/receive events.",
        e);
    }
    // -------------------------------------------------------
    Driver.reportToDriver(contextName,
      "report-to-driver", workers.getSelfID(),
      driverHost, driverPort);
    client.stop();
    ConnPool.get().clean();
    server.stop();
    ForkJoinPool.commonPool().awaitQuiescence(
      Constant.TERMINATION_TIMEOUT,
      TimeUnit.SECONDS);
    System.exit(0);
  }
}
