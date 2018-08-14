package edu.iu.harp.example;

import edu.iu.harp.resource.DoubleArray;
import org.junit.Assert;
import org.junit.Test;

public class DoubleArrPlusTest {
  public static final int SIZE = 100;

  @Test
  public void testCombine() {
    double[] first = new double[SIZE];
    double[] second = new double[SIZE];

    DoubleArrPlus doubleArrPlus = new DoubleArrPlus();
    doubleArrPlus.combine(new DoubleArray(first, 0, SIZE), new DoubleArray(second, 0, SIZE));

    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], 0, 0.00001);
    }

    for (int i = 0; i < SIZE; i++) {
      first[i] = i;
      second[i] = i;
    }
    doubleArrPlus.combine(new DoubleArray(first, 0, SIZE), new DoubleArray(second, 0, SIZE));
    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], i + i, 0.00001);
    }
  }
}
