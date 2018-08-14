package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class LongsPoolTest {
  @Test
  public void testCreateNewArray() {
    LongsPool longsPool = new LongsPool();
    long[] array = longsPool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    LongsPool longsPool = new LongsPool();
    long[] array = longsPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    long[] secondArray = longsPool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    LongsPool longsPool = new LongsPool();
    long[] array = longsPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    longsPool.releaseArray(array);

    long[] newArray = longsPool.getArray(100, false);
    Assert.assertSame(array, newArray);

    long[] secondArray = longsPool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    longsPool.releaseArray(secondArray);
    long[] newSecondArray = longsPool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
