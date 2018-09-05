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

package edu.iu.harp.client;

import edu.iu.harp.resource.Simple;

/*******************************************************
 * An event includes the event type, the context
 * name, the source ID, the target ID and the
 * event body.
 *
 * Note that no matter what source ID is assigned,
 * it dosen't make a mistake of the real worker
 * sender. no matter what target ID is set, if the
 * event type is LOCAL_EVENT or COLLECTIVE_EVENT,
 * the event is still sent to the worker itself
 * (LOCAL_EVENT) or sent to the rest workers
 * (COLLECTIVE_EVENT)
 *
 ******************************************************/
public class Event {
  private final EventType eventType;
  private final String contextName;
  private final int sourceID;
  private final int targetID;
  private final Simple body;

  public Event(EventType type, String name,
               int sourceID, int targetID, Simple b) {
    eventType = type;
    contextName = name;
    this.sourceID = sourceID;
    this.targetID = targetID;
    body = b;
  }

  /**
   * Get the type of the event
   *
   * @return the EventType
   */
  public EventType getEventType() {
    return eventType;
  }

  /**
   * Get the name of the context
   *
   * @return
   */
  public String getContextName() {
    return contextName;
  }

  /**
   * Get the ID of the source
   *
   * @return the ID of the source
   */
  public int getSourceID() {
    return sourceID;
  }

  /**
   * Get the ID of the target
   *
   * @return the ID of the target
   */
  public int getTargetID() {
    return targetID;
  }

  /**
   * Get the body of the event
   *
   * @return the body of the event
   */
  public Simple getBody() {
    return body;
  }
}
