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

import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import org.apache.log4j.Logger;

/*******************************************************
 * The abstract class for receiving the data
 ******************************************************/
public abstract class Receiver {

  private static final Logger LOG =
    Logger.getLogger(Receiver.class);

  private final ServerConn conn;
  private final EventQueue eventQueue;
  private final DataMap dataMap;
  private final byte commandType;

  public Receiver(ServerConn conn,
    EventQueue queue, DataMap map, byte command) {
    this.conn = conn;
    this.eventQueue = queue;
    this.dataMap = map;
    this.commandType = command;
  }

  /**
   * The execution method
   * 
   * @throws Exception
   */
  public void run() throws Exception {
    try {
      handleData(conn);
    } catch (Exception e) {
      LOG.error("Exception in handling data", e);
      throw e;
    }
  }

  /**
   * Get the type of the command
   * 
   * @return the type of the command
   */
  protected byte getCommandType() {
    return this.commandType;
  }

  /**
   * Get the EventQueue
   * 
   * @return the EventQueue
   */
  protected EventQueue getEventQueue() {
    return this.eventQueue;
  }

  /**
   * Get the DataMap
   * 
   * @return the DataMap
   */
  protected DataMap getDataMap() {
    return this.dataMap;
  }

  /**
   * Abstract method for handling the data
   * 
   * @param conn
   *          the Connection object
   * @throws Exception
   */
  protected abstract void handleData(
    final ServerConn conn) throws Exception;
}
