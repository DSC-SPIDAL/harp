package edu.iu.harp.combiner;

import edu.iu.harp.resource.DoubleArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class DoubleArrCombinerTest {
  private Random random;

  @Before
  public void setUp() throws Exception {
    random = new Random();
  }

  @Test
  public void testCombineSUM() {
    DoubleArrCombiner combiner = new DoubleArrCombiner(Operation.SUM);

    int length = 128;
    DoubleArray a1 = new DoubleArray(createArr(length, (double) 1), 0, length);
    DoubleArray a2 = new DoubleArray(createArr(length, (double) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((double)1 + (double)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMUL() {
    DoubleArrCombiner combiner = new DoubleArrCombiner(Operation.MULTIPLY);

    int length = 128;
    DoubleArray a1 = new DoubleArray(createArr(length, (double) 1), 0, length);
    DoubleArray a2 = new DoubleArray(createArr(length, (double) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((double)1 * (double)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMIN() {
    DoubleArrCombiner combiner = new DoubleArrCombiner(Operation.MIN);

    int length = 128;
    DoubleArray a1 = new DoubleArray(createArr(length, (double) 1), 0, length);
    DoubleArray a2 = new DoubleArray(createArr(length, (double) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)1, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMAX() {
    DoubleArrCombiner combiner = new DoubleArrCombiner(Operation.MAX);

    int length = 128;
    DoubleArray a1 = new DoubleArray(createArr(length, (double) 1), 0, length);
    DoubleArray a2 = new DoubleArray(createArr(length, (double) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)2, a1.get()[i], 0.00001);
    }
  }

  @Test
  public void testCombineMINUS() {
    DoubleArrCombiner combiner = new DoubleArrCombiner(Operation.MINUS);

    int length = 128;
    DoubleArray a1 = new DoubleArray(createArr(length, (double) 1), 0, length);
    DoubleArray a2 = new DoubleArray(createArr(length, (double) 2), 0, length);

    combiner.combine(a1, a2);

    for (int i = 0; i < length; i++) {
      Assert.assertEquals((byte)-1, a1.get()[i], 0.00001);
    }
  }

  private double[] createArr(int length, double val) {
    double[] b = new double[length];
    for (int i = 0; i < length; i++) {
      b[i] = val;
    }
    return b;
  }
}
