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

package edu.iu.harp.collective;

import edu.iu.harp.client.DataSender;
import edu.iu.harp.client.Sender;
import edu.iu.harp.depl.Depl;
import edu.iu.harp.depl.Output;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.server.Server;
import edu.iu.harp.util.Ack;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import edu.iu.harp.worker.Workers.WorkerInfoList;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Test collective communication
 ******************************************************/
public class Driver {
  /** Class logger */
  private static final Logger LOG =
    Logger.getLogger(Driver.class);
  private static final String bcast_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/bcast.sh";
  private static final String regroup_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/regroup.sh";
  private static final String wordcount_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/wordcount.sh";
  private static final String allgather_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/allgather.sh";
  private static final String allreduce_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/allreduce.sh";
  private static final String reduce_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/reduce.sh";
  private static final String graph_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/graph.sh";
  private static final String event_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/event.sh";
  private static final String lgs_script =
    Depl.getProjectHomePath()
      + Depl.getBinDirectory()
      + "collective/lgs.sh";

  public static void main(String args[])
    throws Exception {
    // args [driver host][driver port][task name]
    // Get driver host and port
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    String task = args[2];
    long jobID = 0;
    // Initialize
    LOG.info("Initialize driver.");
    EventQueue eventQueue = new EventQueue();
    DataMap dataMap = new DataMap();
    Workers workers = new Workers();
    Server server = new Server(driverHost,
      driverPort, eventQueue, dataMap, workers);
    server.start();
    // Start workers
    LOG.info("Start all workers...");
    LOG.info("Number of workers: "
      + workers.getNumWorkers());
    boolean isRunning = false;
    if (task.equals("bcast")) {
      // args[3]: totalByteData
      // args[4]: numLoops
      isRunning = startAllWorkers(workers,
        bcast_script, driverHost, driverPort,
        jobID, args[3], args[4]);
    } else if (task.equals("regroup")) {
      // args[3]: partitionByteData
      // args[4]: numPartitions
      isRunning = startAllWorkers(workers,
        regroup_script, driverHost, driverPort,
        jobID, args[3], args[4]);
    } else if (task.equals("lgs")) {
      // args[3]: partitionByteData
      // args[4]: numPartitions
      isRunning = startAllWorkers(workers,
        lgs_script, driverHost, driverPort, jobID,
        args[3], args[4]);
    } else if (task.equals("wordcount")) {
      isRunning =
        startAllWorkers(workers, wordcount_script,
          driverHost, driverPort, jobID);
    } else if (task.equals("allgather")) {
      // args[3]: partitionByteData
      // args[4]: numPartitions
      isRunning = startAllWorkers(workers,
        allgather_script, driverHost, driverPort,
        jobID, args[3], args[4]);
    } else if (task.equals("allreduce")) {
      // args[3]: partitionByteData
      // args[4]: numPartitions
      isRunning = startAllWorkers(workers,
        allreduce_script, driverHost, driverPort,
        jobID, args[3], args[4]);
    } else if (task.equals("reduce")) {
      // args[3]: partitionByteData
      // args[4]: numPartitions
      isRunning = startAllWorkers(workers,
        reduce_script, driverHost, driverPort,
        jobID, args[3], args[4]);
    } else if (task.equals("graph")) {
      isRunning =
        startAllWorkers(workers, graph_script,
          driverHost, driverPort, jobID);
    } else if (task.equals("event")) {
      isRunning =
        startAllWorkers(workers, event_script,
          driverHost, driverPort, jobID);
    } else {
      LOG.info("Inccorect task command... ");
    }
    if (isRunning) {
      waitForAllWorkers(jobID + "",
        "report-to-driver", dataMap, workers);
    }
    ConnPool.get().clean();
    server.stop();
    ForkJoinPool.commonPool().awaitQuiescence(
      Constant.TERMINATION_TIMEOUT,
      TimeUnit.SECONDS);
    System.exit(0);
  }

  /**
   * It seems that the logger you get in static
   * field can be re-initialized later.
   * 
   * @param workerID
   */
  static void initLogger(int workerID) {
    String fileName =
      "harp-worker-" + workerID + ".log";
    File file = new File(fileName);
    if (file.exists()) {
      file.delete();
    }
    FileAppender fileAppender =
      new FileAppender();
    fileAppender.setName("FileLogger");
    fileAppender.setFile(fileName);
    fileAppender.setLayout(new PatternLayout(
      "%d{dd MMM yyyy HH:mm:ss,SSS} %-4r [%t] %-5p %c %x - %m%n"));
    fileAppender.setAppend(true);
    fileAppender.activateOptions();
    // Add appender to any Logger (here is root)
    Logger.getRootLogger()
      .addAppender(fileAppender);
  }

  private static boolean startAllWorkers(
    Workers workers, String script,
    String driverHost, int driverPort, long jobID,
    String... otherArgs) {
    WorkerInfoList workerInfoList =
      workers.getWorkerInfoList();
    for (WorkerInfo workerInfo : workerInfoList) {
      String workerNode = workerInfo.getNode();
      int workerID = workerInfo.getID();
      boolean isSuccess = startWorker(workerNode,
        script, driverHost, driverPort, workerID,
        jobID, otherArgs);
      LOG.info(
        "Start worker ID: " + workerInfo.getID()
          + " Node: " + workerInfo.getNode()
          + " Result: " + isSuccess);
      if (!isSuccess) {
        return false;
      }
    }
    return true;
  }

  private static boolean startWorker(
    String workerHost, String script,
    String driverHost, int driverPort,
    int workerID, long jobID,
    String... otherArgs) {
    int otherArgsLen = otherArgs.length;
    String cmdstr[] =
      new String[8 + otherArgsLen];
    cmdstr[0] = "ssh";
    cmdstr[1] = workerHost;
    cmdstr[2] = script;
    cmdstr[3] = driverHost;
    cmdstr[4] = driverPort + "";
    cmdstr[5] = workerID + "";
    cmdstr[6] = jobID + "";
    for (int i = 0; i < otherArgsLen; i++) {
      cmdstr[7 + i] = otherArgs[i];
    }
    cmdstr[cmdstr.length - 1] = "&";
    Output cmdOutput =
      Depl.executeCMDandNoWait(cmdstr);
    return cmdOutput.getExeStatus();
  }

  private static boolean waitForAllWorkers(
    String contextName, String opName,
    DataMap dataMap, Workers workers) {
    int count = 0;
    while (count < workers.getNumWorkers()) {
      Data data = IOUtil.waitAndGet(dataMap,
        contextName, opName);
      if (data != null) {
        LOG.info(
          "Worker Status: " + data.getWorkerID());
        data.release();
        data = null;
        count++;
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Report to driver that worker
   */
  public static boolean reportToDriver(
    String contextName, String operationName,
    int workerID, String driverHost,
    int driverPort) {
    LOG.info("Worker " + workerID + " reports. ");
    // Send ack to report worker status
    Ack ack = Writable.create(Ack.class);
    LinkedList<Transferable> transList =
      new LinkedList<>();
    transList.add(ack);
    Data ackData = new Data(DataType.SIMPLE_LIST,
      contextName, workerID, transList,
      DataUtil.getNumTransListBytes(transList),
      operationName);
    Sender sender =
      new DataSender(ackData, driverHost,
        driverPort, Constant.SEND_DECODE);
    boolean isSuccess = sender.execute();
    ackData.release();
    ackData = null;
    ack = null;
    return isSuccess;
  }
}
