package edu.iu.harp.client;

import edu.iu.harp.util.Ack;
import org.junit.Assert;
import org.junit.Test;

public class EventTest {
  @Test
  public void testEvent() {
    Ack ack = new Ack();
    Event event = new Event(EventType.COLLECTIVE_EVENT, "test", 0, 1, ack);

    Assert.assertEquals(ack, event.getBody());
    Assert.assertEquals("test", event.getContextName());
    Assert.assertEquals(0, event.getSourceID());
    Assert.assertEquals(1, event.getTargetID());
  }
}
