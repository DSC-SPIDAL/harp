package edu.iu.harp.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SerializerTest {
  @Test
  public void testSerializeBytes() {
    byte[] bytes = new byte[64];
    Serializer serializer = new Serializer(bytes, 0,64);
    byte[] b = new byte[]{0, 1, 2};
    try {
      serializer.write(b, 0, 3);
    } catch (IOException e) {
      Assert.fail();
    }
    Assert.assertEquals(bytes[0], b[0]);
    Assert.assertEquals(bytes[1], b[1]);
    Assert.assertEquals(bytes[2], b[2]);
  }

  @Test
  public void testSerializeInts() {
    byte[] bytes = new byte[64];
    Serializer serializer = new Serializer(bytes, 0,64);
    try {
      serializer.writeInt(0);
      serializer.writeInt(1);
      serializer.writeInt(2);
    } catch (IOException e) {
      Assert.fail();
    }

    Deserializer deserializer = new Deserializer(bytes, 0, 64);

    try {
      Assert.assertEquals(deserializer.readInt(), 0, .0001);
      Assert.assertEquals(deserializer.readInt(), 1, .0001);
      Assert.assertEquals(deserializer.readInt(), 2, .0001);
    } catch (IOException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSerializeDouble() {
    byte[] bytes = new byte[64];
    Serializer serializer = new Serializer(bytes, 0,64);
    try {
      serializer.writeDouble(0);
      serializer.writeDouble(1);
      serializer.writeDouble(2);
    } catch (IOException e) {
      Assert.fail();
    }

    Deserializer deserializer = new Deserializer(bytes, 0, 64);

    try {
      Assert.assertEquals(deserializer.readDouble(), 0, .0001);
      Assert.assertEquals(deserializer.readDouble(), 1, .0001);
      Assert.assertEquals(deserializer.readDouble(), 2, .0001);
    } catch (IOException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSerializeLong() {
    byte[] bytes = new byte[64];
    Serializer serializer = new Serializer(bytes, 0,64);
    try {
      serializer.writeLong(0);
      serializer.writeLong(1);
      serializer.writeLong(2);
    } catch (IOException e) {
      Assert.fail();
    }

    Deserializer deserializer = new Deserializer(bytes, 0, 64);

    try {
      Assert.assertEquals(deserializer.readLong(), 0, .0001);
      Assert.assertEquals(deserializer.readLong(), 1, .0001);
      Assert.assertEquals(deserializer.readLong(), 2, .0001);
    } catch (IOException e) {
      Assert.fail();
    }
  }

  @Test
  public void testSerializeChar() {
    byte[] bytes = new byte[64];
    Serializer serializer = new Serializer(bytes, 0,64);
    try {
      serializer.writeChar(0);
      serializer.writeChar(1);
      serializer.writeChar(2);
    } catch (IOException e) {
      Assert.fail();
    }

    Deserializer deserializer = new Deserializer(bytes, 0, 64);

    try {
      Assert.assertEquals(deserializer.readChar(), 0, .0001);
      Assert.assertEquals(deserializer.readChar(), 1, .0001);
      Assert.assertEquals(deserializer.readChar(), 2, .0001);
    } catch (IOException e) {
      Assert.fail();
    }
  }
}
