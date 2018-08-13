package edu.iu.harp.worker;

import org.junit.Assert;
import org.junit.Test;

public class WorkerInfoTest {
  @Test
  public void testWorkerInfo() {
    WorkerInfo info = new WorkerInfo(0, "127.0.0.1", 1294, 100);

    Assert.assertEquals(info.getID(), 0);
    Assert.assertEquals(info.getNode(), "127.0.0.1");
    Assert.assertEquals(info.getPort(), 1294);
    Assert.assertEquals(info.getRack(), 100);
  }
}
