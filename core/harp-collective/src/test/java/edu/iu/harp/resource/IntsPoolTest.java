package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class IntsPoolTest {
  @Test
  public void testCreateNewArray() {
    IntsPool pool = new IntsPool();
    int[] array = pool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    IntsPool pool = new IntsPool();
    int[] array = pool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    int[] secondArray = pool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    IntsPool pool = new IntsPool();
    int[] array = pool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    pool.releaseArray(array);

    int[] newArray = pool.getArray(100, false);
    Assert.assertSame(array, newArray);

    int[] secondArray = pool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    pool.releaseArray(secondArray);
    int[] newSecondArray = pool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
