/*
 * Copyright 2013-2017 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.harp.io;

import edu.iu.harp.client.Event;
import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*******************************************************
 * The queue for Events
 ******************************************************/
public class EventQueue {

  private static final Logger LOG =
    Logger.getLogger(EventQueue.class);

  private BlockingQueue<Event> eventQueue;

  public EventQueue() {
    eventQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Add a event to the queue
   * 
   * @param event
   *          the event to be added
   */
  public void addEvent(Event event) {
    this.eventQueue.add(event);
  }

  /**
   * Retrieves and removes the head of this queue,
   * waiting if necessary until an element becomes
   * available.
   * 
   * @return the next event
   */
  public Event waitEvent() {
    try {
      return eventQueue.take();
    } catch (InterruptedException e) {
      LOG.error(
        "Error when waiting and getting event.",
        e);
    }
    return null;
  }

  /**
   * Retrieves and removes the head of this queue,
   * or returns null if this queue is empty.
   */
  public Event getEvent() {
    return eventQueue.poll();
  }
}
