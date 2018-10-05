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

package edu.iu.harp.server;

import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/*******************************************************
 * The Acceptor for handling the messages received
 * from clients
 ******************************************************/
public class Acceptor implements Runnable {

  private static final Logger LOG =
          Logger.getLogger(Acceptor.class);

  private final ServerConn conn;
  private final EventQueue eventQueue;
  private final DataMap dataMap;
  private final Workers workers;
  private byte commandType;
  private final int selfID;

  private InputStream in;

  public Acceptor(ServerConn conn,
                  EventQueue queue, DataMap map, Workers w,
                  byte command) {
    this.conn = conn;
    this.eventQueue = queue;
    this.dataMap = map;
    this.workers = w;
    this.commandType = command;
    this.selfID = workers.getSelfID();
    this.in = conn.getInputDtream();
  }

  public void forceStop(){
    if(this.in!=null){
      try {
        this.in.close();
      } catch (IOException e) {
        LOG.log(Level.DEBUG,"Error occurred when closing input stream",e);
      }
    }
  }

  /**
   * The overridden run function for handling the
   * messages received from a client
   */
  @Override
  public void run() {
    // All commands should use positive byte
    // integer 0 ~ 127
    try {
      do {
        if (commandType == Constant.CONNECTION_END) {
          break;
        } else if (commandType == Constant.SEND) {
          DataReceiver receiver =
            new DataReceiver(selfID, conn,
              eventQueue, dataMap, Constant.SEND);
          receiver.run();
        } else if (commandType == Constant.SEND_DECODE) {
          DataReceiver receiver =
            new DataReceiver(selfID, conn,
              eventQueue, dataMap,
              Constant.SEND_DECODE);
          receiver.run();
        } else if (commandType == Constant.CHAIN_BCAST) {
          Receiver receiver =
            new DataChainBcastReceiver(selfID,
              conn, eventQueue, dataMap, workers,
              Constant.CHAIN_BCAST);
          receiver.run();
        } else if (commandType == Constant.CHAIN_BCAST_DECODE) {
          Receiver receiver =
            new DataChainBcastReceiver(selfID,
              conn, eventQueue, dataMap, workers,
              Constant.CHAIN_BCAST_DECODE);
          receiver.run();
        } else if (commandType == Constant.MST_BCAST) {
          Receiver receiver =
            new DataMSTBcastReceiver(selfID, conn,
              eventQueue, dataMap, workers,
              Constant.MST_BCAST);
          receiver.run();
        } else if (commandType == Constant.MST_BCAST_DECODE) {
          Receiver receiver =
            new DataMSTBcastReceiver(selfID, conn,
              eventQueue, dataMap, workers,
              Constant.MST_BCAST_DECODE);
          receiver.run();
        } else {
          LOG.info(
            "Unknown command: " + commandType);
          break;
        }
        commandType = (byte) in.read();
        // LOG.info("Read next command " +
        // commandType);
      } while (true);
    } catch (Exception e) {
      LOG.error("Exception on Acceptor.", e);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
  }
}
