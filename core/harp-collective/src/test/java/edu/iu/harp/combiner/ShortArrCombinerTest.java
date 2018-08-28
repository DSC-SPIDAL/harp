package edu.iu.harp.combiner;

import edu.iu.harp.resource.ShortArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class ShortArrCombinerTest {
  private Random random;

  @Before
  public void setUp() throws Exception {
    random = new Random();
  }

  @Test
  public void testCombineSUM() {
    ShortArrCombiner combiner = new ShortArrCombiner(Operation.SUM);

    int length = 128;
    ShortArray a1 = new ShortArray(createArr(length, (short) 1), 0, length);
    ShortArray a2 = new ShortArray(createArr(length, (short) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((short)1 + (short)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMUL() {
    ShortArrCombiner combiner = new ShortArrCombiner(Operation.MULTIPLY);

    int length = 128;
    ShortArray a1 = new ShortArray(createArr(length, (short) 1), 0, length);
    ShortArray a2 = new ShortArray(createArr(length, (short) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((short)1 * (short)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMIN() {
    ShortArrCombiner combiner = new ShortArrCombiner(Operation.MIN);

    int length = 128;
    ShortArray a1 = new ShortArray(createArr(length, (short) 1), 0, length);
    ShortArray a2 = new ShortArray(createArr(length, (short) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMAX() {
    ShortArrCombiner combiner = new ShortArrCombiner(Operation.MAX);

    int length = 128;
    ShortArray a1 = new ShortArray(createArr(length, (short) 1), 0, length);
    ShortArray a2 = new ShortArray(createArr(length, (short) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMINUS() {
    ShortArrCombiner combiner = new ShortArrCombiner(Operation.MINUS);

    int length = 128;
    ShortArray a1 = new ShortArray(createArr(length, (short) 1), 0, length);
    ShortArray a2 = new ShortArray(createArr(length, (short) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)-1, a1.get()[i], 0.00001);
    }
  }

  private short[] createArr(int length, short val) {
    short[] b = new short[length];
    for (int i = 0; i < length; i++) {
      b[i] = val;
    }
    return b;
  }
}
