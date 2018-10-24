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
import edu.iu.harp.io.Serializer;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * The actual sender for broadcasting the data
 * using MST method We don't allow the worker
 * broadcasts to itself.
 */
public class DataMSTBcastSender extends DataSender {

  private static final Logger LOG =
          Logger.getLogger(DataMSTBcastSender.class);

  public DataMSTBcastSender(Data data,
                            Workers workers, byte command) {
    super(data,
            getDestID(workers.getSelfID(),
                    workers.getMinID(), workers.getMiddleID(),
                    workers.getMaxID()),
            workers, command);
  }

  /**
   * Get the ID of the destination
   *
   * @param selfID the self
   * @param left   the ID of the left worker
   * @param middle the ID of the middle worker
   * @param right  the ID of the right worker
   * @return the ID of the destination
   */
  private static int getDestID(int selfID,
                               int left, int middle, int right) {
    int half = middle - left + 1;
    int destID = (selfID <= middle ? (selfID + half) : (selfID - half));
    // destID may be greater than right
    // but won't be less than left
    if (destID > right) {
      destID = right;
    }
    if (selfID == destID) {
      return Constant.UNKNOWN_WORKER_ID;
    } else {
      return destID;
    }
  }

  /**
   * Get the ByteArray storing the size of the
   * head array
   *
   * @param headArrSize the size of the head array
   * @return the ByteArray storing the size of the
   * head array
   */
  protected ByteArray getOPByteArray(int headArrSize) {
    ByteArray opArray = ByteArray.create(12, true);
    if (opArray != null) {
      try {
        Serializer serializer = new Serializer(opArray);
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
   * Broadcast the data
   *
   * @param conn    the Connection object
   * @param opArray the ByteArray storing the size of
   *                the head array
   * @param data    the Data to be broadcast
   * @throws IOException
   */
  protected void sendDataBytes(Connection conn, final ByteArray opArray, final Data data)
          throws IOException {
    // Send data to other workers
    int selfID = getWorkers().getSelfID();
    int left = getWorkers().getMinID();
    int middle = getWorkers().getMiddleID();
    int right = getWorkers().getMaxID();
    int destID = getDestWorkerID();
    int destLeft = left;
    int destRight = right;
    // Get dest's left and right
    // and self's new left and right
    if (selfID > middle) {
      // Destination ID is at the left side
      destRight = middle;
      left = middle + 1;
    } else {
      // Destination ID is at the right side
      destLeft = middle + 1;
      right = middle;
    }

    Serializer serializer = new Serializer(
            new ByteArray(opArray.get(), 4, 8)
    );

    try {
      serializer.writeInt(destLeft);
      serializer.writeInt(destRight);
    } catch (IOException e) {
      LOG.error("Fail to serialize op array.", e);
      throw e;
    }
    try {
      super.sendDataBytes(conn, opArray, data);
    } catch (IOException e) {
      LOG.error("Fail to send data bytes.", e);
      throw e;
    }
    // Send to other workers in MST
    while (left < right) {
      middle = (left + right) / 2;
      int half = middle - left + 1;
      // Update new destination
      if (selfID <= middle) {
        destID = selfID + half;
        if (destID > right) {
          destID = right;
        }
        destLeft = middle + 1;
        destRight = right;
        right = middle;
      } else {
        // destID won't be less than left
        destID = selfID - half;
        destLeft = left;
        destRight = middle;
        left = middle + 1;
      }
      // LOG.info("MST Dest ID " + destID + " "
      // + destLeft + " " + destRight);
      serializer = new Serializer(
              new ByteArray(opArray.get(), 4, 8)
      );

      try {
        serializer.writeInt(destLeft);
        serializer.writeInt(destRight);
      } catch (IOException e) {
        LOG.error(
                "Fail to serialize to op array.", e);
        continue;
      }
      // Send data to destination
      WorkerInfo destWorker =
              getWorkers().getWorkerInfo(destID);
      if (destWorker != null) {
        Connection destConn =
                Connection.create(destWorker.getNode(),
                        destWorker.getPort(), true);
        if (destConn != null) {
          try {
            super.sendDataBytes(destConn, opArray,
                    data);
            destConn.release();
          } catch (IOException e) {
            LOG.error("Fail to send data bytes.",
                    e);
            destConn.free();
            continue;
          }
        }
      }
    }
  }
}
