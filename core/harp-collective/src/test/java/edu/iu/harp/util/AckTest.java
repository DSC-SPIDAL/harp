package edu.iu.harp.util;

import org.junit.Assert;
import org.junit.Test;

public class AckTest {
  @Test
  public void testAck() {
    Ack ack = new Ack();

    Assert.assertEquals(ack.getNumWriteBytes(), 0);
  }
}
