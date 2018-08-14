package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class ShortsPoolTest {
  @Test
  public void testCreateNewArray() {
    ShortsPool shortsPool = new ShortsPool();
    short[] array = shortsPool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    ShortsPool shortsPool = new ShortsPool();
    short[] array = shortsPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    short[] secondArray = shortsPool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    ShortsPool shortsPool = new ShortsPool();
    short[] array = shortsPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    shortsPool.releaseArray(array);

    short[] newArray = shortsPool.getArray(100, false);
    Assert.assertSame(array, newArray);

    short[] secondArray = shortsPool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    shortsPool.releaseArray(secondArray);
    short[] newSecondArray = shortsPool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
