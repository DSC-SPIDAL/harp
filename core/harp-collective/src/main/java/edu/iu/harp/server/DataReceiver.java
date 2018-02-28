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
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.Deserializer;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.resource.ByteArray;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/*******************************************************
 * The actual receiver for receiving the data.
 ******************************************************/
public class DataReceiver extends Receiver {

  private static final Logger LOG =
    Logger.getLogger(DataReceiver.class);

  private final int selfID;

  public DataReceiver(int selfID, ServerConn conn,
    EventQueue queue, DataMap map,
    byte commandType) {
    super(conn, queue, map, commandType);
    this.selfID = selfID;
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
      .getCommandType() == Constant.SEND_DECODE) {
      (new Decoder(data, selfID,
        EventType.MESSAGE_EVENT,
        this.getEventQueue(), this.getDataMap()))
          .fork();
    } else {
      DataUtil.addDataToQueueOrMap(selfID,
        this.getEventQueue(),
        EventType.MESSAGE_EVENT,
        this.getDataMap(), data);
    }
  }

  /**
   * Receive the Data
   * 
   * @param in
   *          the InputStream
   * @return the Data received
   * @throws Exception
   */
  private Data receiveData(final InputStream in)
    throws Exception {
    // Read head array size and body array size
    int headArrSize = -1;
    ByteArray opArray = ByteArray.create(4, true);
    try {
      IOUtil.receiveBytes(in, opArray.get(),
        opArray.start(), opArray.size());
      Deserializer deserializer =
        new Deserializer(opArray);
      headArrSize = deserializer.readInt();
    } catch (IOException e) {
      // Finally is executed first, then the
      // exception is thrown
      LOG.error("Fail to receive op array", e);
      throw e;
    } finally {
      // Release
      opArray.release();
    }
    // Read head array
    ByteArray headArray =
      ByteArray.create(headArrSize, true);
    if (headArray != null) {
      try {
        IOUtil.receiveBytes(in, headArray.get(),
          headArray.start(), headArrSize);
      } catch (Exception e) {
        LOG.error("Fail to receive head array",
          e);
        headArray.release();
        throw e;
      }
    } else {
      throw new Exception("Null head array");
    }
    // Prepare bytes from resource pool
    // Sending or receiving null array is allowed
    Data data = new Data(headArray);
    data.decodeHeadArray();
    ByteArray bodyArray = data.getBodyArray();
    if (bodyArray != null) {
      try {
        IOUtil.receiveBytes(in, bodyArray.get(),
          bodyArray.start(), bodyArray.size());
      } catch (Exception e) {
        LOG.error("Fail to receive body array",
          e);
        headArray.release();
        bodyArray.release();
        throw e;
      }
    }
    return data;
  }
}
