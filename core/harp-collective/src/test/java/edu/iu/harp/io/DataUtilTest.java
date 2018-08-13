package edu.iu.harp.io;

import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.ShortArray;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class DataUtilTest {

  @Test
  public void testDeserializeByteArray() {
    int numBytes = 4;
    byte[] bytes = new byte[Integer.BYTES + numBytes];

    ByteBuffer b = ByteBuffer.wrap(bytes);
    b.putInt(4);
    for (int i = 0; i< numBytes; i++) {
      b.put((byte) i);
    }

    Deserializer deserializer = new Deserializer(bytes, 0, Integer.BYTES + numBytes);
    ByteArray byteArray = DataUtil.deserializeByteArray(deserializer);
    Assert.assertNotNull(byteArray);
    Assert.assertEquals(5 + 4, byteArray.getNumEnocdeBytes());

    byte[] ret = byteArray.get();
    for (int i = 0; i< numBytes; i++) {
      Assert.assertEquals((byte)i, ret[i]);
    }
  }

  @Test
  public void testDeserializeShortArray() {
    int numShorts = 4;
    int length = Integer.BYTES + numShorts * Short.BYTES;
    byte[] bytes = new byte[length];

    ByteBuffer b = ByteBuffer.wrap(bytes);
    b.putInt(numShorts);
    for (int i = 0; i< numShorts; i++) {
      b.putShort((short) i);
    }

    Deserializer deserializer = new Deserializer(bytes, 0, length);
    ShortArray byteArray = DataUtil.deserializeShortArray(deserializer);
    Assert.assertNotNull(byteArray);
    Assert.assertEquals(5 + numShorts * Short.BYTES, byteArray.getNumEnocdeBytes());

    short[] ret = byteArray.get();
    for (int i = 0; i< numShorts; i++) {
      Assert.assertEquals((short) i, ret[i]);
    }
  }

  @Test
  public void testDeserializeDoubleArray() {
    int numDoubles = 4;
    int length = Integer.BYTES + numDoubles * Double.BYTES;
    byte[] bytes = new byte[length];

    ByteBuffer b = ByteBuffer.wrap(bytes);
    b.putInt(numDoubles);
    for (int i = 0; i< numDoubles; i++) {
      b.putDouble((double) i);
    }

    Deserializer deserializer = new Deserializer(bytes, 0, length);
    DoubleArray byteArray = DataUtil.deserializeDoubleArray(deserializer);
    Assert.assertNotNull(byteArray);
    Assert.assertEquals(5 + numDoubles * Double.BYTES, byteArray.getNumEnocdeBytes());

    double[] ret = byteArray.get();
    for (int i = 0; i< numDoubles; i++) {
      Assert.assertEquals((double) i, ret[i], .0000001);
    }
  }
}
