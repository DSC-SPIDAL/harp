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

import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.Transferable;
import org.apache.log4j.Logger;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/*******************************************************
 * The wrapper of data used in communication. The
 * fields in data must be consistent
 ******************************************************/
public class Data {

  private static final Logger LOG =
      Logger.getLogger(Data.class);
  /**
   * The type of body data, array or object
   */
  private byte bodyType =
      DataType.UNKNOWN_DATA_TYPE;
  /**
   * This usually is the name of event handler
   */
  private String contextName = null;
  /**
   * Source worker ID
   */
  private int workerID =
      Constant.UNKNOWN_WORKER_ID;
  /**
   * Only required in collective communication
   */
  private String operationName = null;
  /**
   * Only required in collective communication
   */
  private int partitionID =
      Constant.UNKNOWN_PARTITION_ID;
  /**
   * The size in bytes of the body
   */
  private int bodySize = 0;
  /**
   * Data object contained
   */
  private List<Transferable> body = null;

  private ByteArray headArray = null;
  private ByteArray bodyArray = null;

  private DataStatus headStatus;
  private DataStatus bodyStatus;

  /**
   * Construct a data
   */
  public Data(byte type, String conName, int wID,
              List<Transferable> object, int s) {
    bodyType = type;
    contextName = conName;
    workerID = wID;
    bodySize = s;
    body = object;
    headStatus = DataStatus.DECODED;
    bodyStatus = DataStatus.DECODED;
    if (!isData() || body == null) {
      resetData();
      headStatus = DataStatus.DECODE_FAILED;
      bodyStatus = DataStatus.DECODE_FAILED;
    }
  }

  /**
   * Construct an operation data
   */
  public Data(byte type, String conName, int wID,
              List<Transferable> object, int s,
              String opName) {
    bodyType = type;
    contextName = conName;
    workerID = wID;
    operationName = opName;
    bodySize = s;
    body = object;
    headStatus = DataStatus.DECODED;
    bodyStatus = DataStatus.DECODED;
    if (!isOperationData() || body == null) {
      resetData();
      headStatus = DataStatus.DECODE_FAILED;
      bodyStatus = DataStatus.DECODE_FAILED;
    }
  }

  /**
   * Construct a partition data in an operation
   */
  public Data(byte type, String conName, int wID,
              List<Transferable> object, int s,
              String opName, int parID) {
    bodyType = type;
    contextName = conName;
    workerID = wID;
    operationName = opName;
    partitionID = parID;
    bodySize = s;
    body = object;
    headStatus = DataStatus.DECODED;
    bodyStatus = DataStatus.DECODED;
    if (!isPartitionData() || body == null) {
      resetData();
      headStatus = DataStatus.DECODE_FAILED;
      bodyStatus = DataStatus.DECODE_FAILED;
    }
  }

  public Data(ByteArray headArr) {
    headArray = headArr;
    headStatus = DataStatus.ENCODED_ARRAY;
    if (headArray == null
        || headArray.get() == null) {
      headArray = null;
      headStatus = DataStatus.DECODE_FAILED;
    }
    bodyArray = null;
    bodyStatus = DataStatus.DECODE_FAILED;
  }

  public Data(ByteArray headArr,
              ByteArray bodyArr) {
    headArray = headArr;
    bodyArray = bodyArr;
    headStatus = DataStatus.ENCODED_ARRAY;
    bodyStatus = DataStatus.ENCODED_ARRAY;
    if (headArray == null
        || headArray.get() == null
        || bodyArray == null
        || bodyArray.get() == null) {
      headArray = null;
      headStatus = DataStatus.DECODE_FAILED;
      bodyArray = null;
      bodyStatus = DataStatus.DECODE_FAILED;
    }
  }

  /**
   * Check if this is Data Data > Operation Data >
   * Partition Data
   */
  public boolean isData() {
    if (bodyType == DataType.UNKNOWN_DATA_TYPE
        || contextName == null || bodySize == 0
      // We don't detect worker ID
        ) {
      return false;
    }
    return true;
  }

  /**
   * Check if this is an operation data
   *
   * @return true if this is an operation data
   */
  public boolean isOperationData() {
    if (operationName == null) {
      return false;
    } else {
      return isData();
    }
  }

  /**
   * Check if this is a partition data
   *
   * @return true if this is a partition data
   */
  public boolean isPartitionData() {
    if (partitionID == Constant.UNKNOWN_PARTITION_ID) {
      return false;
    } else {
      return isOperationData();
    }
  }

  /**
   * Reset the data
   */
  private void resetData() {
    bodyType = DataType.UNKNOWN_DATA_TYPE;
    contextName = null;
    workerID = Constant.UNKNOWN_WORKER_ID;
    operationName = null;
    partitionID = Constant.UNKNOWN_PARTITION_ID;
    bodySize = 0;
    body = null;
  }

  /**
   * Get the type of the body
   *
   * @return the type of the body
   */
  public byte getBodyType() {
    return bodyType;
  }

  /**
   * Get the name of the context
   *
   * @return the name of the context
   */
  public String getContextName() {
    return contextName;
  }

  /**
   * Get the ID of the Worker
   *
   * @return the ID of the Worker
   */
  public int getWorkerID() {
    return workerID;
  }

  /**
   * Get the name of the operation
   *
   * @return the name of the operation
   */
  public String getOperationName() {
    return operationName;
  }

  /**
   * Get the ID of the partition
   *
   * @return the ID of the partition
   */
  public int getPartitionID() {
    return partitionID;
  }

  /**
   * Get the size of the body
   *
   * @return the size of the body
   */
  public int getBodySize() {
    return bodySize;
  }

  /**
   * Get the body
   *
   * @return the body
   */
  public List<Transferable> getBody() {
    return body;
  }

  /**
   * Get the head array
   *
   * @return the head array
   */
  public ByteArray getHeadArray() {
    return headArray;
  }

  /**
   * Get the body array
   *
   * @return the body array
   */
  public ByteArray getBodyArray() {
    return bodyArray;
  }

  /**
   * Get the DataStatus of the head
   *
   * @return the DataStatus of the head
   */
  public DataStatus getHeadStatus() {
    return headStatus;
  }

  /**
   * Get the DataStatus of the body
   *
   * @return the DataStatus of the body
   */
  public DataStatus getBodyStatus() {
    return bodyStatus;
  }

  /**
   * Release the head array. If the array is
   * removed without encode/decode, make sure that
   * the data status is correct. Other states
   * shouldn't have encoded head array
   */
  public void releaseHeadArray() {
    if (headStatus == DataStatus.ENCODED_ARRAY_DECODED) {
      headArray.release();
      headArray = null;
      headStatus = DataStatus.DECODED;
    } else if (headStatus == DataStatus.ENCODED_ARRAY) {
      headArray.release();
      headArray = null;
      headStatus = DataStatus.DECODE_FAILED;
    } else if (headStatus == DataStatus.ENCODED_ARRAY_DECODE_FAILED) {
      headArray.release();
      headArray = null;
      headStatus = DataStatus.DECODE_FAILED;
    }
  }

  /**
   * Release the data body array. If the array is
   * removed without encode/decode, make sure that
   * the data status is correct
   */
  public void releaseBodyArray() {
    if (bodyStatus == DataStatus.ENCODED_ARRAY_DECODED) {
      bodyArray.release();
      bodyArray = null;
      bodyStatus = DataStatus.DECODED;
    } else if (bodyStatus == DataStatus.ENCODED_ARRAY) {
      bodyArray.release();
      bodyArray = null;
      bodyStatus = DataStatus.DECODE_FAILED;
    } else if (bodyStatus == DataStatus.ENCODED_ARRAY_DECODE_FAILED) {
      bodyArray.release();
      bodyArray = null;
      bodyStatus = DataStatus.DECODE_FAILED;
    }
  }

  /**
   * Release the Data
   */
  public void release() {
    releaseHeadArray();
    releaseBodyArray();
    // In these two cases
    // we need to release body object
    if (bodyStatus == DataStatus.DECODED
        || bodyStatus == DataStatus.ENCODE_FAILED_DECODED) {
      releaseBody(body);
    }
    resetData();
    headStatus = DataStatus.DECODE_FAILED;
    bodyStatus = DataStatus.DECODE_FAILED;
  }

  /**
   * Release the body object
   *
   * @param trans the body
   */
  private void
  releaseBody(List<Transferable> trans) {
    DataUtil.releaseTransList(trans);
  }

  /**
   * Decode the headArray as the head
   *
   * @return
   */
  public DataStatus decodeHeadArray() {
    if (headStatus == DataStatus.ENCODED_ARRAY) {
      // Decode head array to fields
      // If head array is null, the status cannot
      // be encoded array
      Deserializer deserializer =
          new Deserializer(headArray);
      boolean isFailed = false;
      try {
        bodyType = deserializer.readByte();
        // LOG.info("body type: " + bodyType);
        contextName = deserializer.readUTF();
        workerID = deserializer.readInt();
        bodySize = deserializer.readInt();
      } catch (IOException e) {
        LOG.error("Fail to decode head array", e);
        resetHead();
        isFailed = true;
      }
      if (!isFailed && (deserializer
          .getPos() < deserializer.getLength())) {
        try {
          operationName = deserializer.readUTF();
        } catch (IOException e) {
          LOG.error(
              "Fail to decode operation name", e);
          resetHead();
          isFailed = true;
        }
      }
      if (!isFailed && (deserializer
          .getPos() < deserializer.getLength())) {
        try {
          partitionID = deserializer.readInt();
        } catch (IOException e) {
          LOG.error("Fail to decode partition ID",
              e);
          resetHead();
          isFailed = true;
        }
      }
      if (isFailed) {
        headStatus =
            DataStatus.ENCODED_ARRAY_DECODE_FAILED;
      } else {
        headStatus =
            DataStatus.ENCODED_ARRAY_DECODED;
        if (bodyArray == null
            && bodyStatus == DataStatus.DECODE_FAILED) {
          // Prepare body array
          // if there is no such one
          bodyArray =
              ByteArray.create(bodySize, true);
          if (bodyArray != null) {
            bodyStatus = DataStatus.ENCODED_ARRAY;
          }
        }
      }
    }
    return headStatus;
  }

  /**
   * Reset the Head
   */
  private void resetHead() {
    bodyType = DataType.UNKNOWN_DATA_TYPE;
    contextName = null;
    workerID = Constant.UNKNOWN_WORKER_ID;
    bodySize = 0;
    operationName = null;
    partitionID = Constant.UNKNOWN_PARTITION_ID;
  }

  /**
   * Decode the bodyArray as the body
   *
   * @return the DataStatus
   */
  public DataStatus decodeBodyArray() {
    if ((headStatus == DataStatus.DECODED
        || headStatus == DataStatus.ENCODED_ARRAY_DECODED
        || headStatus == DataStatus.ENCODE_FAILED_DECODED)
        && bodyStatus == DataStatus.ENCODED_ARRAY) {
      // If body status is encoded array
      // body array cannot be null.
      // body object must be null;
      if (bodyType == DataType.SIMPLE_LIST) {
        body =
            DataUtil.decodeSimpleList(bodyArray);
      } else if (bodyType == DataType.PARTITION_LIST) {
        body =
            DataUtil.decodePartitionList(bodyArray);
      } else {
        LOG.error("Cannot decode unknown body: "
            + bodyType);
      }
      if (body == null) {
        bodyStatus =
            DataStatus.ENCODED_ARRAY_DECODE_FAILED;
      } else {
        bodyStatus =
            DataStatus.ENCODED_ARRAY_DECODED;
      }
    }
    return bodyStatus;
  }

  /**
   * Encode the head as a ByteArray
   *
   * @return the DataStatus
   */
  public DataStatus encodeHead() {
    if (headStatus == DataStatus.DECODED) {
      // Encode fields to head array
      boolean isOpData = isOperationData();
      boolean isParData = isPartitionData();
      // bodyType, contextName, workerID, bodySize
      int headArrSize =
          9 + (4 + 2 * contextName.length());
      if (isOpData) {
        headArrSize +=
            (4 + 2 * operationName.length());
      }
      if (isParData) {
        headArrSize += 4;
      }
      headArray =
          ByteArray.create(headArrSize, true);
      Serializer serializer =
          new Serializer(headArray);
      boolean isFailed = false;
      try {
        serializer.writeByte(bodyType);
        serializer.writeUTF(contextName);
        serializer.writeInt(workerID);
        serializer.writeInt(bodySize);
      } catch (Exception e) {
        LOG.error(
            "Fail to encode body type, context name"
                + "and worker ID.",
            e);
        isFailed = true;
      }
      if (!isFailed & isOpData) {
        try {
          serializer.writeUTF(operationName);
        } catch (Exception e) {
          LOG.error(
              "Fail to encode operation name.", e);
          isFailed = true;
        }
      }
      if (!isFailed & isParData) {
        try {
          serializer.writeInt(partitionID);
        } catch (Exception e) {
          LOG.error(
              "Fail to encode parititon ID.", e);
          isFailed = true;
        }
      }
      if (isFailed) {
        // Null is set for failed encoding
        // Notice that encoded part can be
        // released directly
        // or in encoding
        headArray.release();
        headArray = null;
        headStatus =
            DataStatus.ENCODE_FAILED_DECODED;
      } else {
        headStatus =
            DataStatus.ENCODED_ARRAY_DECODED;
      }
    }
    return headStatus;
  }

  /**
   * Encode the body as a ByteArray
   *
   * @return the DataStatus
   */
  public DataStatus encodeBody() {
    if (bodyStatus == DataStatus.DECODED) {
      if (headStatus == DataStatus.DECODED
          || headStatus == DataStatus.ENCODED_ARRAY_DECODED
          || headStatus == DataStatus.ENCODE_FAILED_DECODED) {
        // Use body size info for encoding
        bodyArray =
            ByteArray.create(bodySize, true);
      } else {
        int size =
            DataUtil.getNumTransListBytes(body);
        bodyArray = ByteArray.create(size, true);
      }
      DataOutput dataOut =
          new Serializer(bodyArray);
      if (bodyType == DataType.SIMPLE_LIST
          || bodyType == DataType.PARTITION_LIST) {
        try {
          DataUtil.encodeTransList(body, bodySize,
              dataOut);
        } catch (Exception e) {
          bodyArray.release();
          bodyArray = null;
        }
      } else {
        LOG.info(
            "Cannot encode unknown data type.");
      }
      if (bodyArray != null) {
        bodyStatus =
            DataStatus.ENCODED_ARRAY_DECODED;
      } else {
        bodyStatus =
            DataStatus.ENCODE_FAILED_DECODED;
      }
    }
    return bodyStatus;
  }
}
