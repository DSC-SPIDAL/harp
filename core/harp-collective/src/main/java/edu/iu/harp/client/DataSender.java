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
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataStatus;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.io.Serializer;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/*******************************************************
 * The actual sender for sending the data.
 * Different from DataReceiver, we don't release
 * the encoded bytes in failures.
 ******************************************************/
public class DataSender extends Sender {

  @SuppressWarnings("unused")
  private static final Logger LOG =
      Logger.getLogger(DataSender.class);

  public DataSender(Data data, int destWorkerID,
                    Workers workers, byte command) {
    super(data, destWorkerID, workers, command);
  }

  public DataSender(Data data, String host,
                    int port, byte command) {
    super(data, host, port, command);
  }

  /**
   * This method defines how to handle the data
   */
  @Override
  protected void handleData(final Connection conn,
                            final Data data) throws Exception {
    // Get head size and body size
    int headArrSize = getHeadSize(data);
    ByteArray opArray =
        getOPByteArray(headArrSize);
    if (opArray == null) {
      throw new IOException(
          "Cannot get op array.");
    }
    try {
      sendDataBytes(conn, opArray, data);
    } catch (IOException e) {
      throw e;
    } finally {
      opArray.release();
    }
  }

  /**
   * Get the size of the head array
   *
   * @param data the Data
   * @return the size of the head array of the
   * data
   */
  protected int getHeadSize(Data data) {
    return data.getHeadArray().size();
  }

  /**
   * Get the ByteArray storing the size of the
   * head array
   *
   * @param headArrSize the size of the head array
   * @return the ByteArray storing the size of the
   * head array
   */
  protected ByteArray
  getOPByteArray(int headArrSize) {
    ByteArray opArray = ByteArray.create(4, true);
    if (opArray != null) {
      try {
        Serializer serializer =
            new Serializer(opArray);
        serializer.writeInt(headArrSize);
        return opArray;
      } catch (Exception e) {
        opArray.release();
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Send the data
   *
   * @param conn    the Connection object
   * @param opArray the ByteArray storing the size of
   *                the head array
   * @param data    the Data to be sent
   * @throws IOException
   */
  protected void sendDataBytes(Connection conn,
                               final ByteArray opArray, final Data data)
      throws IOException {
    // Get op bytes and size
    OutputStream out = conn.getOutputStream();
    byte[] opBytes = opArray.get();
    int opArrSize = opArray.size();
    // Get head size and body size
    ByteArray headArray = data.getHeadArray();
    byte[] headBytes = headArray.get();
    int headArrSize = headArray.size();
    try {
      out.write(getCommand());
      IOUtil.sendBytes(out, opBytes, 0,
          opArrSize);
      out.flush();
      // Send head bytes
      if (headArrSize > 0) {
        IOUtil.sendBytes(out, headBytes, 0,
            headArrSize);
      }
      sendBodyBytes(out, data);
    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Send the data body
   *
   * @param out  the OutputStream
   * @param data the Data
   * @throws IOException
   */
  private void sendBodyBytes(
      final OutputStream out, final Data data)
      throws IOException {
    // Send content data, check the array size
    // first. Sending or receiving null array is
    // allowed
    DataStatus bodyStatus = data.getBodyStatus();
    if (bodyStatus == DataStatus.ENCODED_ARRAY_DECODED
        || bodyStatus == DataStatus.ENCODED_ARRAY
        || bodyStatus == DataStatus.ENCODED_ARRAY_DECODE_FAILED) {
      ByteArray bodyArray = data.getBodyArray();
      IOUtil.sendBytes(out, bodyArray.get(),
          bodyArray.start(), bodyArray.size());
    }
  }
}
