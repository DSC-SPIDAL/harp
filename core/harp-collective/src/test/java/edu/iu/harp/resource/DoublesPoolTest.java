package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class DoublesPoolTest {
  @Test
  public void testCreateNewArray() {
    DoublesPool bytesPool = new DoublesPool();
    double[] array = bytesPool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    DoublesPool bytesPool = new DoublesPool();
    double[] array = bytesPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    double[] secondArray = bytesPool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    DoublesPool bytesPool = new DoublesPool();
    double[] array = bytesPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    bytesPool.releaseArray(array);

    double[] newArray = bytesPool.getArray(100, false);
    Assert.assertSame(array, newArray);

    double[] secondArray = bytesPool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    bytesPool.releaseArray(secondArray);
    double[] newSecondArray = bytesPool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
