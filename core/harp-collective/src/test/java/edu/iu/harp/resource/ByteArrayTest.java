package edu.iu.harp.resource;

import edu.iu.harp.io.DataType;
import edu.iu.harp.io.Serializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteArrayTest {
  @Test
  public void testGetNumEncodedBytes() {
    ByteArray array = new ByteArray(new byte[4], 0, 4);
    Assert.assertEquals(4 * Double.BYTES + 5, array.getNumEnocdeBytes());
  }

  @Test
  public void testCreate() throws IOException {
    int length = 4;
    byte[] arr = new byte[length];

    for (int i = 0; i < length; i++) {
      arr[i] = (byte) i;
    }

    ByteArray array = new ByteArray(arr, 0, length);
    int byteLength = length * Double.BYTES + 5;
    byte[] bytes = new byte[byteLength];
    Serializer serializer = new Serializer(bytes, 0, byteLength);

    array.encode(serializer);

    ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, byteLength);
    Assert.assertEquals(DataType.BYTE_ARRAY, buffer.get());
    Assert.assertEquals(4, buffer.getInt());
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(i, buffer.get(), 0.00001);
    }
  }
}
