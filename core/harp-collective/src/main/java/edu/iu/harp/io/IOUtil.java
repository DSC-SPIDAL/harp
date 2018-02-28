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

package edu.iu.harp.io;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/*******************************************************
 * Some utils for IO operations
 ******************************************************/
public class IOUtil {

  private static final Logger LOG =
    Logger.getLogger(IOUtil.class);

  /**
   * Sent the byte[] to OutputStream
   * 
   * @param out
   *          the OutputStream
   * @param bytes
   *          the byte[]
   * @param start
   *          the start index
   * @param size
   *          the size to send
   * @throws IOException
   */
  public static void sendBytes(
    final OutputStream out, final byte[] bytes,
    int start, int size) throws IOException {
    out.write(bytes, start, size);
    out.flush();
  }

  /**
   * Get byte[] from the InputStream
   */
  public static void receiveBytes(InputStream in,
    byte[] bytes, int start, int size)
    throws IOException {
    while (size > 0) {
      int len = in.read(bytes, start, size);
      size -= len;
      start += len;
    }
  }

  /**
   * Set the options for the Socket
   * 
   * @param socket
   *          the Socket
   * @throws SocketException
   */
  public static void setSocketOptions(
    Socket socket) throws SocketException {
    socket.setKeepAlive(true);
    socket.setReuseAddress(true);
    // socket.setTcpNoDelay(true);
    // int sendSize1 = socket.getSendBufferSize();
    // int recvSize1 =
    // socket.getReceiveBufferSize();
    socket
      .setSendBufferSize(Constant.BUFFER_SIZE);
    socket
      .setReceiveBufferSize(Constant.BUFFER_SIZE);
    // int sendSize2 = socket.getSendBufferSize();
    // int recvSize2 =
    // socket.getReceiveBufferSize();
    // LOG.info("send buffer " + sendSize1 + " "
    // + sendSize2 + ", recv buffer " + recvSize1
    // + " " + recvSize2);
  }

  /**
   * Set the options for the ServerSocket
   * 
   * @param socket
   *          the ServerSocket
   * @throws SocketException
   */
  public static void setServerSocketOptions(
    ServerSocket socket) throws SocketException {
    // int recvSize1 =
    // socket.getReceiveBufferSize();
    socket.setReuseAddress(true);
    socket
      .setReceiveBufferSize(Constant.BUFFER_SIZE);
    // int recvSize2 =
    // socket.getReceiveBufferSize();
    // LOG.info("Server recv buffer " + recvSize1
    // + " " + recvSize2);
  }

  /**
   * Wait and get a data from DataMap for
   * collective communication
   * 
   * @param dataMap
   *          the DataMap
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @return the Data got from the DataMap
   */
  public static Data waitAndGet(DataMap dataMap,
    String contextName, String operationName) {
    int count = 0;
    do {
      try {
        return dataMap.waitAndGetData(contextName,
          operationName,
          Constant.DATA_MAX_WAIT_TIME);
      } catch (InterruptedException e) {
        if (count == Constant.SMALL_RETRY_COUNT) {
          return null;
        }
        count++;
        LOG.info("Retry. context name: "
          + contextName + ", operationName: "
          + operationName);
      }
    } while (true);
  }
}
