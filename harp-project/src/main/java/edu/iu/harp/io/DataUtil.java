/*
 * Copyright 2013-2016 Indiana University
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.iu.harp.client.Event;
import edu.iu.harp.client.EventType;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.FloatArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.resource.ShortArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;

/*******************************************************
 * The operations, including deserialization, decoding and encoding on various
 * type of data
 ******************************************************/
public class DataUtil {

    private static final Logger LOG = Logger.getLogger(DataUtil.class);

    /**
     * Deserialize the data from a Deserializer as a ByteArray
     * 
     * @param din
     *            the Deserializer
     * @return a ByteArray deserialized from the Deserializer
     */
    public static ByteArray deserializeByteArray(Deserializer din) {
	int bytesSize = 0;
	try {
	    bytesSize = din.readInt();
	} catch (Exception e) {
	    LOG.error("Fail to deserialize byte array", e);
	    return null;
	}
	ByteArray byteArray = ByteArray.create(bytesSize, false);
	if (byteArray != null) {
	    byte[] bytes = byteArray.get();
	    try {
		din.readFully(bytes, 0, bytesSize);
		return byteArray;
	    } catch (Exception e) {
		LOG.error("Fail to deserialize byte array", e);
		byteArray.release();
		return null;
	    }
	} else {
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a ShortArray
     * 
     * @param din
     *            the Deserializer
     * @return a ShortArray deserialized from the Deserializer
     */
    public static ShortArray deserializeShortArray(Deserializer din) {
	int shortsSize = 0;
	try {
	    shortsSize = din.readInt();
	} catch (Exception e) {
	    return null;
	}
	ShortArray shortArray = ShortArray.create(shortsSize, false);
	try {
	    short[] shorts = shortArray.get();
	    for (int i = 0; i < shortsSize; i++) {
		shorts[i] = din.readShort();
	    }
	    return shortArray;
	} catch (Exception e) {
	    shortArray.release();
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a IntArray
     * 
     * @param din
     *            the Deserializer
     * @return a IntArray deserialized from the Deserializer
     */
    public static IntArray deserializeIntArray(Deserializer din) {
	int intsSize = 0;
	try {
	    intsSize = din.readInt();
	} catch (Exception e) {
	    return null;
	}
	IntArray intAray = IntArray.create(intsSize, false);
	try {
	    int[] ints = intAray.get();
	    for (int i = 0; i < intsSize; i++) {
		ints[i] = din.readInt();
	    }
	    return intAray;
	} catch (Exception e) {
	    intAray.release();
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a FloatArray
     * 
     * @param din
     *            the Deserializer
     * @return a FloatArray deserialized from the Deserializer
     */
    public static FloatArray deserializeFloatArray(Deserializer din) {
	int floatsSize = 0;
	try {
	    floatsSize = din.readInt();
	} catch (Exception e) {
	    return null;
	}
	FloatArray floatArray = FloatArray.create(floatsSize, false);
	try {
	    float[] floats = floatArray.get();
	    for (int i = 0; i < floatsSize; i++) {
		floats[i] = din.readFloat();
	    }
	    return floatArray;
	} catch (Exception e) {
	    floatArray.release();
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a LongArray
     * 
     * @param din
     *            the Deserializer
     * @return a LongArray deserialized from the Deserializer
     */
    public static LongArray deserializeLongArray(Deserializer din) {
	int longsSize = 0;
	try {
	    longsSize = din.readInt();
	} catch (IOException e) {
	    LOG.error("Fail to deserialize long array", e);
	    return null;
	}
	LongArray longArray = LongArray.create(longsSize, false);
	try {
	    long[] longs = longArray.get();
	    for (int i = 0; i < longsSize; i++) {
		longs[i] = din.readLong();
	    }
	    return longArray;
	} catch (Exception e) {
	    LOG.error("Fail to deserialize long array", e);
	    longArray.release();
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a DoubleArray
     * 
     * @param din
     *            the Deserializer
     * @return a DoubleArray deserialized from the Deserializer
     */
    public static DoubleArray deserializeDoubleArray(Deserializer din) {
	int doublesSize = 0;
	try {
	    doublesSize = din.readInt();
	} catch (IOException e) {
	    LOG.error("Fail to deserialize double array", e);
	    return null;
	}
	DoubleArray doubleArray = DoubleArray.create(doublesSize, false);
	if (doubleArray == null) {
	    return null;
	}
	double[] doubles = doubleArray.get();
	try {
	    for (int i = 0; i < doublesSize; i++) {
		doubles[i] = din.readDouble();
	    }
	    return doubleArray;
	} catch (Exception e) {
	    LOG.error("Fail to deserialize double array", e);
	    doubleArray.release();
	    return null;
	}
    }

    /**
     * Deserialize the data from a Deserializer as a Writable
     * 
     * @param din
     *            the Deserializer
     * @return a Writable deserialized from the Deserializer
     */
    public static Writable deserializeWritable(DataInput din) {
	String className = null;
	try {
	    className = din.readUTF();
	} catch (Exception e) {
	    LOG.error("Fail to deserialize the class name", e);
	    return null;
	}
	Writable obj = Writable.create(Writable.forClass(className));
	if (obj == null) {
	    return null;
	}
	try {
	    obj.read(din);
	    return obj;
	} catch (Exception e) {
	    LOG.error("Fail to deserialize writable with class name " + className, e);
	    obj.release();
	    return null;
	}
    }

    /**
     * Decode the ByteArray as a list of Transferable objects
     * 
     * @param byteArray
     *            the ByteArray to be decoded
     * @return a list of Transferable objects
     */
    public static List<Transferable> decodeSimpleList(final ByteArray byteArray) {
	List<Transferable> objs = new LinkedList<>();
	Deserializer decoder = new Deserializer(byteArray);
	while (decoder.getPos() < decoder.getLength()) {
	    byte dataType = DataType.UNKNOWN_DATA_TYPE;
	    try {
		dataType = decoder.readByte();
	    } catch (Exception e) {
		releaseTransList(objs);
		return null;
	    }
	    Simple obj = null;
	    if (dataType == DataType.UNKNOWN_DATA_TYPE) {
		break;
	    } else if (dataType == DataType.BYTE_ARRAY) {
		obj = deserializeByteArray(decoder);
	    } else if (dataType == DataType.SHORT_ARRAY) {
		obj = deserializeShortArray(decoder);
	    } else if (dataType == DataType.INT_ARRAY) {
		obj = deserializeIntArray(decoder);
	    } else if (dataType == DataType.FLOAT_ARRAY) {
		obj = deserializeFloatArray(decoder);
	    } else if (dataType == DataType.LONG_ARRAY) {
		obj = deserializeLongArray(decoder);
	    } else if (dataType == DataType.DOUBLE_ARRAY) {
		obj = deserializeDoubleArray(decoder);
	    } else if (dataType == DataType.WRITABLE) {
		obj = deserializeWritable(decoder);
	    } else {
		LOG.info("Unkown data type.");
	    }
	    if (obj == null) {
		releaseTransList(objs);
		return null;
	    } else {
		objs.add(obj);
	    }
	}
	return objs;
    }

    /**
     * Decode the ByteArray as a list of Partitions
     * 
     * @param byteArray
     *            the ByteArray to be decoded
     * @return a list of Partitions
     */
    public static List<Transferable> decodePartitionList(final ByteArray byteArray) {
	List<Transferable> partitions = new LinkedList<>();
	Deserializer decoder = new Deserializer(byteArray);
	while (decoder.getPos() < decoder.getLength()) {
	    byte dataType = DataType.UNKNOWN_DATA_TYPE;
	    try {
		dataType = decoder.readByte();
	    } catch (Exception e) {
		LOG.error("Fail to decode partition list.", e);
		releaseTransList(partitions);
		return null;
	    }
	    Simple partition = null;
	    if (dataType == DataType.UNKNOWN_DATA_TYPE) {
		break;
	    } else if (dataType == DataType.BYTE_ARRAY) {
		partition = deserializeByteArray(decoder);
	    } else if (dataType == DataType.SHORT_ARRAY) {
		partition = deserializeShortArray(decoder);
	    } else if (dataType == DataType.INT_ARRAY) {
		partition = deserializeIntArray(decoder);
	    } else if (dataType == DataType.FLOAT_ARRAY) {
		partition = deserializeFloatArray(decoder);
	    } else if (dataType == DataType.LONG_ARRAY) {
		partition = deserializeLongArray(decoder);
	    } else if (dataType == DataType.DOUBLE_ARRAY) {
		partition = deserializeDoubleArray(decoder);
	    } else if (dataType == DataType.WRITABLE) {
		partition = deserializeWritable(decoder);
	    } else {
		LOG.info("Unkown data type.");
	    }
	    if (partition == null) {
		releaseTransList(partitions);
		return null;
	    } else {
		try {
		    int partitionID = decoder.readInt();
		    partitions.add(new Partition<Simple>(partitionID, partition));
		} catch (IOException e) {
		    releaseTransList(partitions);
		    return null;
		}
	    }
	}
	return partitions;
    }

    /**
     * Serialize multiple Transferable with different data types
     * 
     * @param objs
     *            the data to be serialized
     * @return the ByteArray resulted from serialization
     */
    public static ByteArray encodeTransList(List<Transferable> objs) {
	int size = getNumTransListBytes(objs);
	ByteArray byteArray = ByteArray.create(size, true);
	if (byteArray != null) {
	    Serializer serializer = new Serializer(byteArray);
	    try {
		encodeTransList(objs, size, serializer);
		return byteArray;
	    } catch (Exception e) {
		LOG.error("Fail to encode transferable list.", e);
		byteArray.release();
		return null;
	    }
	} else {
	    return null;
	}
    }

    /**
     * Get the size in bytes of the encoded data
     * 
     * @param objs
     *            the data
     * @return the size in bytes of the data
     */
    public static int getNumTransListBytes(List<Transferable> objs) {
	int size = 0;
	for (Transferable obj : objs) {
	    size += obj.getNumEnocdeBytes();
	}
	if (size == 0) {
	    size = 1;
	}
	return size;
    }

    /**
     * Encode the data to DataOutput
     * 
     * @param objs
     *            the objects to be encoded
     * @param size
     *            the size of the encoded data
     * @param dataOut
     *            the DataOutput
     * @throws Exception
     */
    public static void encodeTransList(List<Transferable> objs, int size, DataOutput dataOut) throws Exception {
	if (size == 1) {
	    dataOut.writeByte(DataType.UNKNOWN_DATA_TYPE);
	} else {
	    for (Transferable obj : objs) {
		obj.encode(dataOut);
	    }
	}
    }

    /**
     * Transferable List may include different types Identify each of them and
     * release
     * 
     * @param pool
     * @param trans
     */
    public static void releaseTransList(List<Transferable> transList) {
	for (Transferable trans : transList) {
	    trans.release();
	}
	transList.clear();
    }

    /**
     * Add the Data to EventQueue or DataMap. If the Data is operation data, add
     * it to the DataMap; else if the Data isData, and the bodyType of the Data
     * is DataType.SIMPLE_LIST, add it to the EventQueue
     * 
     * @param selfID
     *            the ID of current worker
     * @param eventQueue
     *            the queue for events
     * @param eventType
     *            the type of the event
     * @param dataMap
     *            the DataMap
     * @param data
     *            the Data
     */
    public static void addDataToQueueOrMap(int selfID, EventQueue eventQueue, EventType eventType, DataMap dataMap,
	    Data data) {
	// LOG.info("Add data - " + "context name: "
	// + data.getContextName() + ", worker ID: "
	// + data.getWorkerID() + ", operation name: "
	// + data.getOperationName() + ", body type: "
	// + data.getBodyType() + ", body size: "
	// + data.getBodySize()
	// + ", isOperationData: "
	// + data.isOperationData() + ", isData: "
	// + data.isData() + ", head status: "
	// + data.getHeadStatus() + ", body status: "
	// + data.getBodyStatus());
	if ((data.getHeadStatus() == DataStatus.ENCODED_ARRAY_DECODED)
		&& (data.getBodyStatus() == DataStatus.ENCODED_ARRAY
			|| data.getBodyStatus() == DataStatus.ENCODED_ARRAY_DECODED)) {
	    if (data.isOperationData()) {
		dataMap.putData(data);
	    } else if (data.isData()) {
		if (data.getBodyType() == DataType.SIMPLE_LIST) {
		    List<Transferable> objs = data.getBody();
		    for (Transferable obj : objs) {
			eventQueue.addEvent(
				new Event(eventType, data.getContextName(), data.getWorkerID(), selfID, (Simple) obj));
		    }
		}
	    }
	}
    }
}
