package edu.iu.harp.example;

import edu.iu.harp.resource.LongArray;
import org.junit.Assert;
import org.junit.Test;

public class LongArrPlusTest {
  public static final int SIZE = 100;

  @Test
  public void testCombine() {
    long[] first = new long[SIZE];
    long[] second = new long[SIZE];

    LongArrPlus doubleArrPlus = new LongArrPlus();
    doubleArrPlus.combine(new LongArray(first, 0, SIZE), new LongArray(second, 0, SIZE));

    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], 0, 0.00001);
    }

    for (int i = 0; i < SIZE; i++) {
      first[i] = i;
      second[i] = i;
    }
    doubleArrPlus.combine(new LongArray(first, 0, SIZE), new LongArray(second, 0, SIZE));
    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], i + i, 0.00001);
    }
  }
}
