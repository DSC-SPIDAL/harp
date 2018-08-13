package edu.iu.harp.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DeserializerTest {
  @Test
  public void testReadBoolean() {
    byte vOut = (byte) (1);
    byte[] b = new byte[1];
    b[0] = vOut;
    Deserializer deserializer = new Deserializer(b, 0, 1);

    try {
      Assert.assertTrue(deserializer.readBoolean());
    } catch (IOException e) {
      Assert.fail();
    }

    vOut = (byte) (0);
    b[0] = vOut;
    deserializer = new Deserializer(b, 0, 1);

    try {
      Assert.assertFalse(deserializer.readBoolean());
    } catch (IOException e) {
      Assert.fail();
    }
  }

  @Test
  public void testReadBytes() {
    byte vOut = (byte) (1);
    byte[] b = new byte[1];
    b[0] = vOut;
    Deserializer deserializer = new Deserializer(b, 0, 1);

    try {
      Assert.assertEquals(deserializer.readByte(), 1);
    } catch (IOException e) {
      Assert.fail();
    }

    vOut = (byte) (0);
    b[0] = vOut;
    deserializer = new Deserializer(b, 0, 1);

    try {
      Assert.assertEquals(deserializer.readByte(), 0);
    } catch (IOException e) {
      Assert.fail();
    }
  }


}
