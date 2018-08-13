package edu.iu.harp.resource;

import edu.iu.harp.io.DataType;
import edu.iu.harp.io.Serializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FloatsArrayTest {
  @Test
  public void testGetNumEncodedBytes() {
    FloatArray array = new FloatArray(new float[4], 0, 4);
    Assert.assertEquals(4 * Float.BYTES + 5, array.getNumEnocdeBytes());
  }

  @Test
  public void testCreate() throws IOException {
    int length = 4;
    float[] arr = new float[length];

    for (int i = 0; i < length; i++) {
      arr[i] = i;
    }

    FloatArray array = new FloatArray(arr, 0, length);
    int byteLength = length * Float.BYTES + 5;
    byte[] bytes = new byte[byteLength];
    Serializer serializer = new Serializer(bytes, 0, byteLength);

    array.encode(serializer);

    ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, byteLength);
    Assert.assertEquals(DataType.FLOAT_ARRAY, buffer.get());
    Assert.assertEquals(4, buffer.getInt());
    for (int i = 0; i < length; i++) {
      Assert.assertEquals(i, buffer.getFloat(), 0.00001);
    }
  }
}
