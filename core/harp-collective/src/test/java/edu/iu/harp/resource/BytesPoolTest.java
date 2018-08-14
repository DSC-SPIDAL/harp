package edu.iu.harp.resource;

import org.junit.Assert;
import org.junit.Test;

public class BytesPoolTest {
  @Test
  public void testCreateNewArray() {
    BytesPool bytesPool = new BytesPool();
    byte[] array = bytesPool.createNewArray(100);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);
  }

  @Test
  public void testGetArray() {
    BytesPool bytesPool = new BytesPool();
    byte[] array = bytesPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    byte[] secondArray = bytesPool.getArray(100, true);
    Assert.assertEquals(128, secondArray.length);
  }

  @Test
  public void testReleaseArray() {
    BytesPool bytesPool = new BytesPool();
    byte[] array = bytesPool.getArray(100, false);

    Assert.assertNotNull(array);
    Assert.assertEquals(100, array.length);

    bytesPool.releaseArray(array);

    byte[] newArray = bytesPool.getArray(100, false);
    Assert.assertSame(array, newArray);

    byte[] secondArray = bytesPool.getArray(100, true);
    Assert.assertNotNull(secondArray);
    Assert.assertEquals(128, secondArray.length);
    bytesPool.releaseArray(secondArray);
    byte[] newSecondArray = bytesPool.getArray(125, true);
    Assert.assertSame(secondArray, newSecondArray);
  }
}
