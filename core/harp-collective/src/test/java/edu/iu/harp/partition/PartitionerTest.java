package edu.iu.harp.partition;

import org.junit.Assert;
import org.junit.Test;

public class PartitionerTest {
  @Test
  public void testGetWorkerId() {
    Partitioner partitioner = new Partitioner(100);
    Assert.assertEquals(0, partitioner.getWorkerID(0));
    Assert.assertEquals(1, partitioner.getWorkerID(1));
    Assert.assertEquals(0, partitioner.getWorkerID(1000));
    Assert.assertEquals(9, partitioner.getWorkerID(1009));
  }
}
