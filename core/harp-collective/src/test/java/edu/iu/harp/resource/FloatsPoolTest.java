package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class FloatsPoolTest {
  @Test
  public void testCreateNewArray() {
    FloatsPool pool = new FloatsPool();
    float[] array = pool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    FloatsPool pool = new FloatsPool();
    float[] array = pool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    float[] secondArray = pool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    FloatsPool pool = new FloatsPool();
    float[] array = pool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    pool.releaseArray(array);

    float[] newArray = pool.getArray(100, false);
    Assert.assertSame(array, newArray);

    float[] secondArray = pool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    pool.releaseArray(secondArray);
    float[] newSecondArray = pool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
