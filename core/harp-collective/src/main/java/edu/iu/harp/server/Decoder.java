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

package edu.iu.harp.server;

import edu.iu.harp.client.EventType;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;

import java.util.concurrent.RecursiveAction;

/*******************************************************
 * The recursive resultless Fork-Join task for
 * decoding the data.
 ******************************************************/
public class Decoder extends RecursiveAction {
  private static final long serialVersionUID = 1L;

  private final Data data;
  private final int selfID;
  private final EventType eventType;
  private final EventQueue eventQueue;
  private final DataMap dataMap;

  public Decoder(Data data, int selfID,
    EventType eventType, EventQueue eventQueue,
    DataMap dataMap) {
    this.data = data;
    this.selfID = selfID;
    this.eventType = eventType;
    this.eventQueue = eventQueue;
    this.dataMap = dataMap;
  }

  /**
   * The main computation performed by this task.
   */
  @Override
  public void compute() {
    // Decode data array
    data.decodeBodyArray();
    DataUtil.addDataToQueueOrMap(selfID,
      eventQueue, eventType, dataMap, data);
  }
}
