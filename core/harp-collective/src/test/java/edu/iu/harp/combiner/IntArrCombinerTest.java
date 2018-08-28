package edu.iu.harp.combiner;

import edu.iu.harp.resource.IntArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class IntArrCombinerTest {
  private Random random;

  @Before
  public void setUp() throws Exception {
    random = new Random();
  }

  @Test
  public void testCombineSUM() {
    IntArrCombiner combiner = new IntArrCombiner(Operation.SUM);

    int length = 128;
    IntArray a1 = new IntArray(createArr(length, (int) 1), 0, length);
    IntArray a2 = new IntArray(createArr(length, (int) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((int)1 + (int)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMUL() {
    IntArrCombiner combiner = new IntArrCombiner(Operation.MULTIPLY);

    int length = 128;
    IntArray a1 = new IntArray(createArr(length, (int) 1), 0, length);
    IntArray a2 = new IntArray(createArr(length, (int) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((int)1 * (int)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMIN() {
    IntArrCombiner combiner = new IntArrCombiner(Operation.MIN);

    int length = 128;
    IntArray a1 = new IntArray(createArr(length, (int) 1), 0, length);
    IntArray a2 = new IntArray(createArr(length, (int) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMAX() {
    IntArrCombiner combiner = new IntArrCombiner(Operation.MAX);

    int length = 128;
    IntArray a1 = new IntArray(createArr(length, (int) 1), 0, length);
    IntArray a2 = new IntArray(createArr(length, (int) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMINUS() {
    IntArrCombiner combiner = new IntArrCombiner(Operation.MINUS);

    int length = 128;
    IntArray a1 = new IntArray(createArr(length, (int) 1), 0, length);
    IntArray a2 = new IntArray(createArr(length, (int) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)-1, a1.get()[i], 0.00001);
    }
  }

  private int[] createArr(int length, int val) {
    int[] b = new int[length];
    for (int i = 0; i < length; i++) {
      b[i] = val;
    }
    return b;
  }
}
