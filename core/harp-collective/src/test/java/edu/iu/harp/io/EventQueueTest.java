package edu.iu.harp.io;

import edu.iu.harp.client.Event;
import edu.iu.harp.client.EventType;
import org.junit.Assert;
import org.junit.Test;

public class EventQueueTest {
  @Test
  public void addEventTest() {
    EventQueue eventQueue = new EventQueue();

    eventQueue.addEvent(new Event(EventType.COLLECTIVE_EVENT, "test", 0, 1, null));
    Event e = eventQueue.waitEvent();

    Assert.assertNotNull(e);
  }

  @Test
  public void waitEventTest() {
    EventQueue eventQueue = new EventQueue();

    eventQueue.addEvent(new Event(EventType.COLLECTIVE_EVENT, "test", 0, 1, null));
    Event e = eventQueue.waitEvent();

    Assert.assertNotNull(e);
  }

  @Test
  public void getEventTest() {
    EventQueue eventQueue = new EventQueue();

    eventQueue.addEvent(new Event(EventType.COLLECTIVE_EVENT, "test", 0, 1, null));
    Event e = eventQueue.getEvent();

    Assert.assertNotNull(e);

    e = eventQueue.getEvent();
    Assert.assertNull(e);
  }
}
