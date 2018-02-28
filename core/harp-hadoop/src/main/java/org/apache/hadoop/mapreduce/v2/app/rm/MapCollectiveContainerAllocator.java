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

package org.apache.hadoop.mapreduce.v2.app.rm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.ResourceMgrDelegate;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.mapreduce.v2.app.client.ClientService;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/*******************************************************
 * This class is modified from
 * RMContainerAllocator.
 *
 * Allocates the container from the
 * ResourceManager scheduler.
 ******************************************************/
public class MapCollectiveContainerAllocator
  extends RMContainerAllocator {

  static final Log LOG = LogFactory.getLog(
    MapCollectiveContainerAllocator.class);

  static final String MAP_COLLECTIVE_MEMORY =
    "mapreduce.map.collective.memory.mb";

  private int memory;
  private int vCores;

  private String[] nodes;
  private int nextNode;

  public MapCollectiveContainerAllocator(
    ClientService clientService,
    AppContext context) {
    super(clientService, context);
    memory = 0;
    vCores = 1;
  }

  protected void serviceInit(Configuration conf)
    throws Exception {
    super.serviceInit(conf);
    // getAllNodesInfo(conf);
  }

  /**
   * Get all nodes information. This may not be
   * required if the container can be randomly
   * assigned by the resource manager to an empty
   * slot through removing hosts and racks
   * information in ContainerRequestEvent
   * 
   * @param conf
   * @throws YarnException
   * @throws IOException
   */
  private void getAllNodesInfo(Configuration conf)
    throws YarnException, IOException {
    long start = System.currentTimeMillis();
    ResourceMgrDelegate resMgrDelegate =
      new ResourceMgrDelegate(
        new YarnConfiguration(conf));
    List<NodeReport> reports =
      resMgrDelegate.getNodeReports();
    resMgrDelegate.close();
    long end = System.currentTimeMillis();
    LOG.info(
      "Get all nodes inforamtion from resource manager, use "
        + (end - start) + " milliseconds.");
    nodes = new String[reports.size()];
    int i = 0;
    for (NodeReport report : reports) {
      nodes[i] = report.getNodeId().getHost();
      i++;
    }
    Random ran =
      new Random(System.currentTimeMillis());
    nextNode = ran.nextInt(nodes.length);
  }

  /**
   * How the event is generated in
   * TaskAttemptImpl: RequestContainerTransition
   * taskAttempt.eventHandler.handle(new
   * ContainerRequestEvent( taskAttempt.attemptId,
   * taskAttempt.resourceCapability,
   * taskAttempt.dataLocalHosts.toArray( new
   * String[taskAttempt.dataLocalHosts.size()]),
   * taskAttempt.dataLocalRacks.toArray( new
   * String[taskAttempt.dataLocalRacks.size()])));
   */
  protected synchronized void
    handleEvent(ContainerAllocatorEvent event) {
    if (event
      .getType() == ContainerAllocator.EventType.CONTAINER_REQ) {
      // Modify original request information
      if (event.getAttemptID().getTaskId()
        .getTaskType().equals(TaskType.MAP)) {
        ContainerRequestEvent reqEvent =
          (ContainerRequestEvent) event;
        // Use map-collective memory settings,
        if (memory == 0) {
          memory = Integer.parseInt(getConfig()
            .get(MAP_COLLECTIVE_MEMORY, "0"));
        }
        // Remove hosts or rack information
        // Assume the nodes are highly connected
        // Ignore the comments above, still add
        // hosts and racks, but empty
        // this is to avoid earlier failure
        // message, if host and rack info are
        // not added
        if (reqEvent.getHosts() != null
          || reqEvent.getRacks() != null) {
          reqEvent = new ContainerRequestEvent(
            reqEvent.getAttemptID(),
            reqEvent.getCapability(),
            new String[0], new String[0]);
        }
        if (memory != 0) {
          reqEvent.getCapability()
            .setMemory(memory);
          reqEvent.getCapability()
            .setVirtualCores(vCores);
        }
        event = reqEvent;
      }
    }
    super.handleEvent(event);
  }
}
