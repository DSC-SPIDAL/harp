package edu.iu.harp.combiner;

import edu.iu.harp.resource.LongArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class LongArrCombinerTest {
  private Random random;

  @Before
  public void setUp() throws Exception {
    random = new Random();
  }

  @Test
  public void testCombineSUM() {
    LongArrCombiner combiner = new LongArrCombiner(Operation.SUM);

    int length = 128;
    LongArray a1 = new LongArray(createArr(length, (long) 1), 0, length);
    LongArray a2 = new LongArray(createArr(length, (long) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((long)1 + (long)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMUL() {
    LongArrCombiner combiner = new LongArrCombiner(Operation.MULTIPLY);

    int length = 128;
    LongArray a1 = new LongArray(createArr(length, (long) 1), 0, length);
    LongArray a2 = new LongArray(createArr(length, (long) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((long)1 * (long)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMIN() {
    LongArrCombiner combiner = new LongArrCombiner(Operation.MIN);

    int length = 128;
    LongArray a1 = new LongArray(createArr(length, (long) 1), 0, length);
    LongArray a2 = new LongArray(createArr(length, (long) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMAX() {
    LongArrCombiner combiner = new LongArrCombiner(Operation.MAX);

    int length = 128;
    LongArray a1 = new LongArray(createArr(length, (long) 1), 0, length);
    LongArray a2 = new LongArray(createArr(length, (long) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMINUS() {
    LongArrCombiner combiner = new LongArrCombiner(Operation.MINUS);

    int length = 128;
    LongArray a1 = new LongArray(createArr(length, (long) 1), 0, length);
    LongArray a2 = new LongArray(createArr(length, (long) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)-1, a1.get()[i], 0.00001);
    }
  }

  private long[] createArr(int length, long val) {
    long[] b = new long[length];
    for (int i = 0; i < length; i++) {
      b[i] = val;
    }
    return b;
  }
}
