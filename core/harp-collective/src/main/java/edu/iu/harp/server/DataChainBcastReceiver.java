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
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*******************************************************
 * The actual receiver for receiving the
 * broadcasted data using chain method.
 ******************************************************/
public class DataChainBcastReceiver
  extends Receiver {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger
    .getLogger(DataChainBcastReceiver.class);

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
  public DataChainBcastReceiver(int selfID,
    ServerConn conn, EventQueue queue,
    DataMap map, Workers w, byte commandType)
    throws Exception {
    super(conn, queue, map, commandType);
    this.selfID = selfID;
    this.workers = w;
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
    InputStream in = conn.getInputDtream();
    // Receive data
    Data data = receiveData(in);
    if (this
      .getCommandType() == Constant.CHAIN_BCAST_DECODE) {
      // here only body array is decoded
      (new Decoder(data, selfID,
        EventType.COLLECTIVE_EVENT,
        this.getEventQueue(), this.getDataMap()))
          .fork();
    } else {
      // If the data is not for operation,
      // put it to the queue
      // with collective event type
      DataUtil.addDataToQueueOrMap(selfID,
        this.getEventQueue(),
        EventType.COLLECTIVE_EVENT,
        this.getDataMap(), data);
    }
  }

  /**
   * Receive the Data: 1. command 2. head size and
   * source ID 3. head array 4. body array
   * 
   * @param in
   *          the InputStream
   * @return the Data received
   * @throws Exception
   */
  private Data receiveData(final InputStream in)
    throws Exception {
    // Get next worker
    final WorkerInfo next = workers.getNextInfo();
    final int nextID = next.getID();
    final String nextHost = next.getNode();
    final int nextPort = next.getPort();
    // Read head array size and body array size
    int headArrSize = -1;
    int sourceID = -1;
    ByteArray opArray = ByteArray.create(8, true);
    try {
      IOUtil.receiveBytes(in, opArray.get(),
        opArray.start(), opArray.size());
      Deserializer deserializer =
        new Deserializer(opArray);
      headArrSize = deserializer.readInt();
      sourceID = deserializer.readInt();
    } catch (Exception e) {
      opArray.release();
      throw e;
    }
    // Get the next connection
    Connection nextConn = null;
    if (sourceID != nextID) {
      nextConn = Connection.create(nextHost,
        nextPort, true);
      if (nextConn == null) {
        opArray.release();
        throw new IOException(
          "Cannot create the next connection.");
      }
    }
    OutputStream out = null;
    if (nextConn != null) {
      out = nextConn.getOutputStream();
    }
    // Prepare and read head array
    ByteArray headArray =
      ByteArray.create(headArrSize, true);
    if (headArray != null) {
      try {
        IOUtil.receiveBytes(in, headArray.get(),
          headArray.start(), headArray.size());
        // Forward op bytes and head bytes
        if (out != null) {
          out.write(getCommandType());
          IOUtil.sendBytes(out, opArray.get(),
            opArray.start(), opArray.size());
          IOUtil.sendBytes(out, headArray.get(),
            headArray.start(), headArray.size());
        }
      } catch (Exception e) {
        headArray.release();
        if (nextConn != null) {
          nextConn.free();
          nextConn = null;
        }
        throw e;
      } finally {
        opArray.release();
      }
    } else {
      throw new Exception("Null head array.");
    }
    // Prepare body array
    Data data = new Data(headArray);
    data.decodeHeadArray();
    ByteArray bodyArray = data.getBodyArray();
    // Receive and forward body array
    if (bodyArray != null) {
      try {
        receiveBytes(in, out, bodyArray.get(),
          bodyArray.start(), bodyArray.size());
      } catch (Exception e) {
        headArray.release();
        bodyArray.release();
        if (nextConn != null) {
          nextConn.free();
          nextConn = null;
        }
        throw e;
      }
    }
    // Close connection to the next worker
    if (nextConn != null) {
      nextConn.release();
    }
    return data;
  }

  /**
   * Receive bytes data and process
   * 
   * @param in
   *          the InputStream
   * @param out
   *          the OutputStream
   * @param bytes
   *          the byte[] to put data received
   * @param start
   *          the offset index
   * @param size
   *          the size of the data
   * @throws IOException
   */
  private void receiveBytes(InputStream in,
    OutputStream out, byte[] bytes, int start,
    int size) throws IOException {
    if (out != null) {
      while (size > Constant.PIPELINE_SIZE) {
        int len = in.read(bytes, start,
          Constant.PIPELINE_SIZE);
        if (len > 0) {
          out.write(bytes, start, len);
          out.flush();
        }
        start += len;
        size -= len;
      }
      while (size > 0) {
        int len = in.read(bytes, start, size);
        if (len > 0) {
          out.write(bytes, start, len);
          out.flush();
        }
        size -= len;
        start += len;
      }
    } else {
      IOUtil.receiveBytes(in, bytes, start, size);
    }
  }
}
