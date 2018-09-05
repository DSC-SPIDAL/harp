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

import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.Serializer;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

/*******************************************************
 * The actual sender for broadcasting the data
 * using chain method We don't allow the worker
 * broadcasts to itself.
 ******************************************************/
public class DataChainBcastSender
    extends DataSender {
  /**
   * Class logger
   */
  @SuppressWarnings("unused")
  private static final Logger LOG =
      Logger.getLogger(DataChainBcastSender.class);

  public DataChainBcastSender(Data data,
                              Workers workers, byte command) {
    super(data, getDestID(workers.getSelfID(),
        workers.getNextID()), workers, command);
  }

  /**
   * Get ID of the destination
   *
   * @param selfID the self
   * @param nextID the ID of the next worker
   * @return the ID of the destination
   */
  private static int getDestID(int selfID,
                               int nextID) {
    if (selfID == nextID) {
      return Constant.UNKNOWN_WORKER_ID;
    } else {
      return nextID;
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
  @Override
  protected ByteArray
  getOPByteArray(int headArrSize) {
    ByteArray opArray = ByteArray.create(8, true);
    if (opArray != null) {
      try {
        Serializer serializer =
            new Serializer(opArray);
        serializer.writeInt(headArrSize);
        serializer
            .writeInt(getWorkers().getSelfID());
        return opArray;
      } catch (Exception e) {
        opArray.release();
        return null;
      }
    } else {
      return null;
    }
  }
}
