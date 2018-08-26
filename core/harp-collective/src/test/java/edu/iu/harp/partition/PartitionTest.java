package edu.iu.harp.partition;

import edu.iu.harp.resource.IntArray;
import org.junit.Assert;
import org.junit.Test;

public class PartitionTest {
  @Test
  public void testGet() {
    int[] arr = new int[10];
    IntArray array = new IntArray(arr, 0, 10);
    Partition<IntArray> partition = new Partition<>(0, array);

    Assert.assertNotNull(partition.get());
    Assert.assertSame(partition.get(), array);
    Assert.assertSame(partition.get().get(), arr);
  }

  @Test
  public void testNumEncodedBytes() {
    int[] arr = new int[10];
    IntArray array = new IntArray(arr, 0, 10);
    Partition<IntArray> partition = new Partition<>(0, array);

    Assert.assertEquals(49, partition.getNumEnocdeBytes());
  }

  @Test
  public void testId() {
    int[] arr = new int[10];
    IntArray array = new IntArray(arr, 0, 10);
    Partition<IntArray> partition = new Partition<>(0, array);

    Assert.assertEquals(0, partition.id());
  }

  @Test
  public void testRelease() {
    int[] arr = new int[10];
    IntArray array = new IntArray(arr, 0, 10);
    Partition<IntArray> partition = new Partition<>(0, array);

    Assert.assertEquals(0, partition.id());

    partition.release();
    Assert.assertNull(partition.get());
    Assert.assertNull(array.get());
  }
}
