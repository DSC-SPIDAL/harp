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

package org.apache.hadoop.mapreduce.v2.app.launcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.util.RackResolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/*******************************************************
 * This class is modified from
 * ContainerLauncherImpl. When launch event is
 * processed, record the location and generate
 * nodes file. Max mem is also reset for mapper in
 * map-collective job.
 *
 * The following is the original document.
 *
 * This class is responsible for launching of
 * containers.
 ******************************************************/
public class MapCollectiveContainerLauncherImpl
  extends ContainerLauncherImpl {

  static final Log LOG = LogFactory.getLog(
    MapCollectiveContainerLauncherImpl.class);

  static final String MAP_COLLECTIVE_JAVA_OPTS =
    "mapreduce.map.collective.java.opts";

  // The total number of map tasks should be
  // launched
  private int numMapTasks;
  // Record task locations, and prepare for
  // generating node list
  private Map<Integer, String> taskLocations;
  // Print flag
  private boolean isPrinted;

  public MapCollectiveContainerLauncherImpl(
    AppContext context) {
    super(context);
  }

  @Override
  protected void serviceInit(Configuration conf)
    throws Exception {
    super.serviceInit(conf);
    // Get total number of map tasks
    JobConf jobConf = (JobConf) conf;
    numMapTasks = jobConf.getNumMapTasks();
    // Initialize task location map
    taskLocations =
      new TreeMap<Integer, String>();
    isPrinted = false;
  }

  protected EventProcessor createEventProcessor(
    ContainerLauncherEvent event) {
    return new MapCollectiveEventProcessor(event);
  }

  /**
   * Setup and start the container on remote
   * nodemanager.
   */
  class MapCollectiveEventProcessor
    extends EventProcessor {
    private ContainerLauncherEvent event;

    MapCollectiveEventProcessor(
      ContainerLauncherEvent event) {
      super(event);
      // EventProcessor doesn't provide a method
      // to get the event in processing
      // add a reference in child class
      this.event = event;
    }

    @Override
    public void run() {
      LOG.info("Processing the event "
        + event.toString());
      if (event
        .getType() == ContainerLauncher.EventType.CONTAINER_REMOTE_LAUNCH
        && event.getTaskAttemptID().getTaskId()
          .getTaskType() == TaskType.MAP) {
        ContainerRemoteLaunchEvent launchEvent =
          (ContainerRemoteLaunchEvent) event;
        editMapCollectiveLaunchEvent(launchEvent);
      }
      super.run();
    }
  }

  /**
   * The event is generated in
   * TaskAttemptImpl.ContainerAssignedTransition.
   * The event is edited with the new map
   * collective java opts.
   * 
   * @param event
   */
  private void editMapCollectiveLaunchEvent(
    ContainerRemoteLaunchEvent launchEvent) {
    try {
      int taskID = launchEvent.getTaskAttemptID()
        .getTaskId().getId();
      String jobID =
        launchEvent.getTaskAttemptID().getTaskId()
          .getJobId().toString();
      // Get container information
      Container container =
        launchEvent.getAllocatedContainer();
      long conID =
        container.getId().getContainerId();
      int conMem =
        container.getResource().getMemory();
      int conCore =
        container.getResource().getVirtualCores();
      // Get host and rack
      // Use ContainerLauncherImpl's configuration
      String host =
        container.getNodeId().getHost();
      RackResolver.init(getConfig());
      String nodeRackName = RackResolver
        .resolve(host).getNetworkLocation();
      // Get task information
      Task task = launchEvent.getRemoteTask();
      // Here TaskAttemptID is different from
      // TaskAttemptId obtained in
      // launchEvent.getTaskAttemptID()
      TaskAttemptID attemptID = task.getTaskID();
      // Get original user defined java opts
      // string (userClasspath in
      // MapReduceChildJVM.getChildJavaOpts) which
      // should be same as the
      // commands currently set in commands
      // Note that all configurations are from app
      // master, but we follow the
      // original code to get the configuration
      // from task
      String javaOptsStr = task.getConf().get(
        JobConf.MAPRED_MAP_TASK_JAVA_OPTS,
        JobConf.DEFAULT_MAPRED_TASK_JAVA_OPTS);
      // Based on the code in
      // MapReduceChildJVM.getVMCommand to get
      // java opts
      javaOptsStr = javaOptsStr
        .replace("@taskid@", attemptID.toString())
        .trim();
      String[] javaOptsSplit =
        javaOptsStr.split("[\\s]+");
      List<String> javaOpts = new ArrayList<>();
      for (String javaOpt : javaOptsSplit) {
        if (!javaOpt.equals("")) {
          javaOpts.add(javaOpt);
        }
      }
      // Use a similar way to get map collective
      // java opts
      String mcJavaOptsStr = task.getConf()
        .get(MAP_COLLECTIVE_JAVA_OPTS, "");
      mcJavaOptsStr = mcJavaOptsStr
        .replace("@taskid@", attemptID.toString())
        .trim();
      // Record task locations
      LOG.info("Container launch info: "
        + "task_id = " + taskID + ", "
        + "task_type = Map" + ", " + "host = "
        + host + ", " + "rack = " + nodeRackName
        + ", " + "container ID = " + conID + ", "
        + "container mem = " + conMem + ", "
        + "container core = " + conCore + ", "
        + "java new opts = " + mcJavaOptsStr);
      recordTaskLocations(jobID, taskID, host,
        nodeRackName);
      // Update container launch context with the
      // new commands
      ContainerLaunchContext launchContext =
        launchEvent.getContainerLaunchContext();
      // There should be only one string in
      // commands
      String commands =
        launchContext.getCommands().get(0);
      // LOG.info("Original commands: " +
      // commands);
      String newCommands = commands;
      // If there are new Java opts
      if (!mcJavaOptsStr.equals("")) {
        // Remove original java opts from 1 to the
        // last one
        for (int i = 1; i < javaOpts
          .size(); i++) {
          newCommands = newCommands
            .replace(javaOpts.get(i), "");
        }
        // Replace the first one (0) to new java
        // opt string
        newCommands = newCommands.replace(
          javaOpts.get(0), mcJavaOptsStr);
      }
      // LOG.info("New commands: " + newCommands);
      List<String> finalNewCommands =
        new ArrayList<>();
      finalNewCommands.add(newCommands);
      launchContext.setCommands(finalNewCommands);
    } catch (Exception e) {
      LOG.error(
        "Fail to edit ContainerRemoteLaunchEvent",
        e);
    }
  }

  /**
   * All the information are from launchEvent,
   * which gets all the task information from
   * taskAttempt, should match with the task
   * information inside Yarn Child
   * 
   * @param jobID
   * @param taskID
   * @param host
   * @param rack
   */
  private void recordTaskLocations(String jobID,
    int taskID, String host, String rack) {
    synchronized (taskLocations) {
      taskLocations.put(taskID, host);
      LOG.info("RECORD TASK " + taskID + ", NODE "
        + host + ". CURRENT NUMBER OF TASKS: "
        + taskLocations.size());
      if (taskLocations.size() == numMapTasks
        && !isPrinted) {
        Map<String, List<Integer>> nodeTaskMap =
          new TreeMap<String, List<Integer>>();
        int task = 0;
        String node = null;
        List<Integer> tasks = null;
        for (Entry<Integer, String> entry : taskLocations
          .entrySet()) {
          task = entry.getKey();
          node = entry.getValue();
          tasks = nodeTaskMap.get(node);
          if (tasks == null) {
            tasks = new ArrayList<>();
            nodeTaskMap.put(node, tasks);
          }
          tasks.add(task);
        }
        LOG.info("PRINT TASK LOCATIONS");
        for (Entry<Integer, String> entry : taskLocations
          .entrySet()) {
          task = entry.getKey();
          node = entry.getValue();
          LOG.info("Task ID: " + task
            + ". Task Location: " + node);
        }
        LOG.info("PRINT NODE AND TASK MAPPING");
        // Write several files to HDFS
        String nodesFile = jobID + "/nodes";
        // Mapping between task IDs and worker IDs
        String tasksFile = jobID + "/tasks";
        String lockFile = jobID + "/lock";
        try {
          FileSystem fs =
            FileSystem.get(getConfig());
          // Write nodes file to HDFS
          Path nodesPath = new Path(
            fs.getHomeDirectory(), nodesFile);
          FSDataOutputStream out1 =
            fs.create(nodesPath, true);
          BufferedWriter bw1 = new BufferedWriter(
            new OutputStreamWriter(out1));
          bw1.write("#0");
          bw1.newLine();
          // Write tasks file
          Path tasksPath = new Path(
            fs.getHomeDirectory(), tasksFile);
          FSDataOutputStream out2 =
            fs.create(tasksPath, true);
          BufferedWriter bw2 = new BufferedWriter(
            new OutputStreamWriter(out2));
          // Worker ID starts from 0
          int workerID = 0;
          for (Entry<String, List<Integer>> entry : nodeTaskMap
            .entrySet()) {
            node = entry.getKey();
            tasks = entry.getValue();
            for (int i = 0; i < tasks
              .size(); i++) {
              // Write together
              // For each task, there is a line in
              // nodes file
              // There is also a task ID and
              // worker ID mapping in tasks file
              bw1.write(node + "\n");
              bw2.write(tasks.get(i) + "\t"
                + workerID + "\n");
              LOG.info("Node: " + node
                + ". Task: " + tasks.get(i)
                + ". Worker: " + workerID);
              workerID++;
            }
          }
          bw1.flush();
          out1.hflush();
          out1.hsync();
          bw1.close();
          bw2.flush();
          out2.hflush();
          out2.hsync();
          bw2.close();
          // Write Lock file
          Path lock = new Path(
            fs.getHomeDirectory(), lockFile);

          FSDataOutputStream lockOut =
            fs.create(lock, true);
          lockOut.hflush();
          lockOut.hsync();
          lockOut.close();
        } catch (IOException e) {
          LOG.info(
            "Error when writing nodes file to HDFS. ",
            e);
        }
        isPrinted = true;
      }
    }
  }
}
