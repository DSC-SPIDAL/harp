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

import edu.iu.harp.io.Connection;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataStatus;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

/*******************************************************
 * The abstract class for sending the data
 ******************************************************/
public abstract class Sender {

  private static final Logger LOG =
      Logger.getLogger(Sender.class);
  private final Data data;
  private final String host;
  private final int port;
  private final int destWorkerID;
  private final Workers workers;
  private final byte commandType;

  /**
   * To send data between workers, sender and
   * receiver must both have valid IDs.
   *
   * @param d       the data
   * @param destID  the ID of the destination
   * @param w       the workers
   * @param command the command
   */
  public Sender(Data d, int destID, Workers w,
                byte command) {
    WorkerInfo selfInfo = w.getSelfInfo();
    WorkerInfo destInfo = w.getWorkerInfo(destID);
    if (selfInfo == null || destInfo == null) {
      data = null;
      destWorkerID = Constant.UNKNOWN_WORKER_ID;
      workers = null;
      host = null;
      port = Constant.UNKNOWN_PORT;
      commandType = Constant.UNKNOWN_CMD;
    } else {
      data = d;
      destWorkerID = destID;
      workers = w;
      host = destInfo.getNode();
      port = destInfo.getPort();
      commandType = command;
    }
  }

  /**
   * Use host and port directly do send between
   * any to processes It can send data from a
   * process other than workers or send data from
   * a worker to a process outside.
   * <p>
   * If send data from outside, data's worker ID
   * should be unknown
   *
   * @param d
   * @param h
   * @param p
   * @param command
   */
  public Sender(Data d, String h, int p,
                byte command) {
    data = d;
    destWorkerID = Constant.UNKNOWN_WORKER_ID;
    workers = null;
    host = h;
    port = p;
    commandType = command;
  }

  /**
   * Send the data
   *
   * @return true if succeeded, false if not.
   */
  public boolean execute() {
    // Check if we can send
    if (data == null || host == null
        || port == Constant.UNKNOWN_PORT
        || commandType == Constant.UNKNOWN_CMD) {
      return false;
    }
    if (data
        .getHeadStatus() == DataStatus.ENCODE_FAILED_DECODED
        || data
        .getBodyStatus() == DataStatus.ENCODE_FAILED_DECODED
        || data
        .getHeadStatus() == DataStatus.DECODE_FAILED
        || data
        .getBodyStatus() == DataStatus.DECODE_FAILED) {
      return false;
    }
    if (data
        .getHeadStatus() == DataStatus.DECODED) {
      DataStatus headStatus = data.encodeHead();
      if (headStatus == DataStatus.ENCODE_FAILED_DECODED) {
        return false;
      }
    }
    // Encode body
    if (data
        .getBodyStatus() == DataStatus.DECODED) {
      DataStatus bodyStatus = data.encodeBody();
      if (bodyStatus == DataStatus.ENCODE_FAILED_DECODED) {
        // No generating encoded data
        return false;
      }
    }
    // Open connection
    Connection conn =
        Connection.create(host, port, true);
    if (conn == null) {
      // Do not release encoded arrays in data.
      return false;
    }
    // Send
    boolean isFailed = false;
    try {
      handleData(conn, data);
      conn.release();
    } catch (Exception e) {
      LOG.error("Error in sending data.", e);
      conn.free();
      isFailed = true;
    }
    // Do not release encoded arrays in data
    return !isFailed;
  }

  /**
   * Get the ID of the destination worker
   *
   * @return the ID of the destination worker
   */
  protected int getDestWorkerID() {
    return this.destWorkerID;
  }

  /**
   * Get the workers
   *
   * @return the workers
   */
  protected Workers getWorkers() {
    return this.workers;
  }

  /**
   * Get the command
   *
   * @return the command
   */
  protected byte getCommand() {
    return this.commandType;
  }

  /**
   * Abstract method for handling the data
   *
   * @param conn the connection object
   * @param data the Data
   * @throws Exception
   */
  protected abstract void handleData(
      final Connection conn, final Data data)
      throws Exception;
}
