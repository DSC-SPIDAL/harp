package edu.iu.harp.example;

import edu.iu.harp.resource.IntArray;
import org.junit.Assert;
import org.junit.Test;

public class IntArrPlusTest {

  public static final int SIZE = 100;

  @Test
  public void testCombine() {
    int[] first = new int[SIZE];
    int[] second = new int[SIZE];

    IntArrPlus doubleArrPlus = new IntArrPlus();
    doubleArrPlus.combine(new IntArray(first, 0, SIZE), new IntArray(second, 0, SIZE));

    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], 0, 0.00001);
    }

    for (int i = 0; i < SIZE; i++) {
      first[i] = i;
      second[i] = i;
    }
    doubleArrPlus.combine(new IntArray(first, 0, SIZE), new IntArray(second, 0, SIZE));
    for (int i = 0; i < SIZE; i++) {
      Assert.assertEquals(first[i], i + i, 0.00001);
    }
  }
}
