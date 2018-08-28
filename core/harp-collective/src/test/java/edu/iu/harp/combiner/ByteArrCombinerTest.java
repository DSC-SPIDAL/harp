package edu.iu.harp.combiner;

import edu.iu.harp.resource.ByteArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class ByteArrCombinerTest {
  private Random random;

  @Before
  public void setUp() throws Exception {
    random = new Random();
  }

  @Test
  public void testCombineSUM() {
    ByteArrCombiner combiner = new ByteArrCombiner(Operation.SUM);

    int length = 128;
    ByteArray a1 = new ByteArray(createBytes(length, (byte) 1), 0, length);
    ByteArray a2 = new ByteArray(createBytes(length, (byte) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1 + (byte)2, a1.get()[i]);
    }
  }

  @Test
  public void testCombineMUL() {
    ByteArrCombiner combiner = new ByteArrCombiner(Operation.MULTIPLY);

    int length = 128;
    ByteArray a1 = new ByteArray(createBytes(length, (byte) 1), 0, length);
    ByteArray a2 = new ByteArray(createBytes(length, (byte) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1 * (byte)2, a1.get()[i]);
    }
  }

  @Test
  public void testCombineMIN() {
    ByteArrCombiner combiner = new ByteArrCombiner(Operation.MIN);

    int length = 128;
    ByteArray a1 = new ByteArray(createBytes(length, (byte) 1), 0, length);
    ByteArray a2 = new ByteArray(createBytes(length, (byte) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1, a1.get()[i]);
    }
  }

  @Test
  public void testCombineMAX() {
    ByteArrCombiner combiner = new ByteArrCombiner(Operation.MAX);

    int length = 128;
    ByteArray a1 = new ByteArray(createBytes(length, (byte) 1), 0, length);
    ByteArray a2 = new ByteArray(createBytes(length, (byte) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)2, a1.get()[i]);
    }
  }

  @Test
  public void testCombineMINUS() {
    ByteArrCombiner combiner = new ByteArrCombiner(Operation.MINUS);

    int length = 128;
    ByteArray a1 = new ByteArray(createBytes(length, (byte) 1), 0, length);
    ByteArray a2 = new ByteArray(createBytes(length, (byte) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)-1, a1.get()[i]);
    }
  }

  private byte[] createBytes(int length, byte val) {
    byte[] b = new byte[length];
    for (int i = 0; i < length; i++) {
      b[i] = val;
    }
    return b;
  }
}
