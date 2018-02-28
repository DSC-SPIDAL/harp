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

import edu.iu.harp.client.EventType;
import edu.iu.harp.io.Connection;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.Deserializer;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.io.Serializer;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*******************************************************
 * The actual receiver for receiving the
 * broadcasted data using MST method.
 ******************************************************/
public class DataMSTBcastReceiver
  extends Receiver {

  @SuppressWarnings("unused")
  private static final Logger LOG =
    Logger.getLogger(DataMSTBcastReceiver.class);

  private final Workers workers;
  private final int selfID;

  /**
   * Throw exception when failing to initialize
   * 
   * @param selfID
   * @param conn
   * @param queue
   * @param map
   * @param w
   * @param commandType
   * @throws Exception
   */
  public DataMSTBcastReceiver(int selfID,
    ServerConn conn, EventQueue queue,
    DataMap map, Workers w, byte commandType)
    throws Exception {
    super(conn, queue, map, commandType);
    this.selfID = selfID;
    workers = w;
    if (selfID == Constant.UNKNOWN_WORKER_ID) {
      throw new Exception(
        "Fail to initialize receiver.");
    }
  }

  /**
   * Defines how to handle the Data
   */
  @Override
  protected void handleData(final ServerConn conn)
    throws Exception {
    // Receive data
    Data data = receiveData(conn);
    if (this
      .getCommandType() == Constant.MST_BCAST_DECODE) {
      (new Decoder(data, selfID,
        EventType.COLLECTIVE_EVENT,
        this.getEventQueue(), this.getDataMap()))
          .fork();
    } else {
      // If the data is not for operation,
      // put it to the queue with collective event
      // type
      DataUtil.addDataToQueueOrMap(selfID,
        this.getEventQueue(),
        EventType.COLLECTIVE_EVENT,
        this.getDataMap(), data);
    }
  }

  /**
   * Receive the Data
   * 
   * @param conn
   *          the connnection object
   * @return the Data received
   * @throws Exception
   */
  private Data receiveData(final ServerConn conn)
    throws Exception {
    InputStream in = conn.getInputDtream();
    // Read head array size and body array size
    int headArrSize = -1;
    int left = -1;
    int right = -1;
    ByteArray opArray =
      ByteArray.create(12, true);
    try {
      IOUtil.receiveBytes(in, opArray.get(),
        opArray.start(), opArray.size());
      Deserializer deserializer =
        new Deserializer(opArray);
      headArrSize = deserializer.readInt();
      left = deserializer.readInt();
      right = deserializer.readInt();
    } catch (Exception e) {
      opArray.release();
      throw e;
    }
    // Prepare and receive head array
    ByteArray headArray =
      ByteArray.create(headArrSize, true);
    if (headArray != null) {
      try {
        IOUtil.receiveBytes(in, headArray.get(),
          headArray.start(), headArrSize);
      } catch (Exception e) {
        opArray.release();
        headArray.release();
        throw e;
      }
    } else {
      throw new Exception("Null head array.");
    }
    // Prepare and receive body array
    Data data = new Data(headArray);
    data.decodeHeadArray();
    ByteArray bodyArray = data.getBodyArray();
    if (bodyArray != null) {
      try {
        IOUtil.receiveBytes(in, bodyArray.get(),
          bodyArray.start(), bodyArray.size());
      } catch (Exception e) {
        opArray.release();
        headArray.release();
        bodyArray.release();
        throw e;
      }
    }
    if (left < right) {
      // Try to send out,
      // Be careful about the exceptions
      try {
        sendDataInMST(opArray.get(),
          opArray.size(), headArray.get(),
          headArray.size(), bodyArray.get(),
          bodyArray.size(), left, right);
      } catch (Exception e) {
        opArray.release();
        headArray.release();
        bodyArray.release();
        throw e;
      }
    }
    // Release op bytes
    opArray.release();
    return data;
  }

  /**
   * Send the data in MST method
   * 
   * @param opBytes
   *          the operation array
   * @param opArrSize
   *          the size of the operation array
   * @param headBytes
   *          the head array
   * @param headArrSize
   *          the size of the head array
   * @param bodyBytes
   *          the body array
   * @param bodyArrSize
   *          the size of body array
   * @param left
   *          the left worker
   * @param right
   *          the right worker
   * @throws IOException
   */
  private void sendDataInMST(byte[] opBytes,
    int opArrSize, byte[] headBytes,
    int headArrSize, byte[] bodyBytes,
    int bodyArrSize, int left, int right)
    throws IOException {
    // Send data to other nodes
    int middle = (left + right) / 2;
    int half = middle - left + 1;
    int destID = -1;
    int destLeft = -1;
    int destRight = -1;
    while (left < right) {
      // Update destination and the new range
      if (selfID <= middle) {
        destID = selfID + half;
        if (destID > right) {
          destID = right;
        }
        destLeft = middle + 1;
        destRight = right;
        right = middle;
      } else {
        destID = selfID - half;
        destLeft = left;
        destRight = middle;
        left = middle + 1;
      }
      // Update middle and half for the new range
      middle = (left + right) / 2;
      half = middle - left + 1;
      // LOG.info("MST Dest ID " + destID + " "
      // + destLeft + " " + destRight);
      Serializer serializer = new Serializer(
        new ByteArray(opBytes, 4, 8));
      serializer.writeInt(destLeft);
      serializer.writeInt(destRight);
      // Send data to dest
      WorkerInfo destWorker =
        workers.getWorkerInfo(destID);
      if (destWorker != null) {
        Connection destConn =
          Connection.create(destWorker.getNode(),
            destWorker.getPort(), true);
        if (destConn != null) {
          OutputStream out =
            destConn.getOutputStream();
          // Send head and body array
          if (out != null) {
            try {
              out.write(getCommandType());
              IOUtil.sendBytes(out, opBytes, 0,
                opArrSize);
              if (headArrSize > 0) {
                IOUtil.sendBytes(out, headBytes,
                  0, headArrSize);
              }
              IOUtil.sendBytes(out, bodyBytes, 0,
                bodyArrSize);
              destConn.release();
            } catch (Exception e) {
              destConn.free();
              throw e;
            }
          }
        }
      }
    }
  }
}
