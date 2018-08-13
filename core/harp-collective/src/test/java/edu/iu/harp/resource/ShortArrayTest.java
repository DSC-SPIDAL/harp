package edu.iu.harp.resource;

import edu.iu.harp.io.DataType;
import edu.iu.harp.io.Serializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ShortArrayTest {
  @Test
  public void testGetNumEncodedBytes() {
    ShortArray array = new ShortArray(new short[4], 0, 4);
    Assert.assertEquals(4 * Short.BYTES + 5, array.getNumEnocdeBytes());
  }

  @Test
  public void testCreate() throws IOException {
    int length = 4;
    short[] arr = new short[length];

    for (int i = 0; i < length; i++) {
      arr[i] = (short) i;
    }

    ShortArray array = new ShortArray(arr, 0, length);
    int byteLength = length * Short.BYTES + 5;
    byte[] bytes = new byte[byteLength];
    Serializer serializer = new Serializer(bytes, 0, byteLength);

    array.encode(serializer);

    ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, byteLength);
    Assert.assertEquals(DataType.SHORT_ARRAY, buffer.get());
    Assert.assertEquals(4, buffer.getInt());
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(i, buffer.getShort());
    }
  }
}
