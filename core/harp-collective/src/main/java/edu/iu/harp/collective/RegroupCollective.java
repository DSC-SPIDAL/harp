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

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionFunction;
import edu.iu.harp.partition.PartitionUtil;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.server.Server;
import edu.iu.harp.util.PartitionCount;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Regroup Collective communication
 ******************************************************/
public class RegroupCollective {

  protected static final Logger LOG =
    Logger.getLogger(RegroupCollective.class);

  public static void main(String args[])
    throws Exception {
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    int workerID = Integer.parseInt(args[2]);
    long jobID = Long.parseLong(args[3]);
    int partitionByteSize =
      Integer.parseInt(args[4]);
    int numPartitions = Integer.parseInt(args[5]);
    Driver.initLogger(workerID);
    LOG.info("args[] " + driverHost + " "
      + driverPort + " " + workerID + " " + jobID
      + " " + partitionByteSize + " "
      + numPartitions);
    // ---------------------------------------------------
    // Worker initialize
    EventQueue eventQueue = new EventQueue();
    DataMap dataMap = new DataMap();
    Workers workers = new Workers(workerID);
    Server server =
      new Server(workers.getSelfInfo().getNode(),
        workers.getSelfInfo().getPort(),
        eventQueue, dataMap, workers);
    server.start();
    String contextName = jobID + "";
    // Barrier guarantees the living workers get
    // the same view of the barrier result
    boolean isSuccess = Communication.barrier(
      contextName, "barrier", dataMap, workers);
    LOG.info("Barrier: " + isSuccess);
    // ----------------------------------------------------
    // Generate data partition
    Table<DoubleArray> table =
      new Table<DoubleArray>(0,
        new DoubleArrPlus());
    int doublesSize = partitionByteSize / 8;
    if (doublesSize < 2) {
      doublesSize = 2;
    }
    LOG.info("Double size: " + doublesSize);
    // Generate partition data
    // Assuming count is 1
    for (int i = 0; i < numPartitions; i++) {
      DoubleArray doubleArray =
        DoubleArray.create(doublesSize, false);
      double[] doubles = doubleArray.get();
      doubles[0] = 1; // count is 1
      double num = Math.random() * 100;
      doubles[doublesSize - 1] = num;
      Partition<DoubleArray> partition =
        new Partition<DoubleArray>(i,
          doubleArray);
      table.addPartition(partition);
      LOG.info("Data Generate, WorkerID: "
        + workerID + " Partition: "
        + partition.id() + " Count: " + doubles[0]
        + " First element: " + doubles[1]
        + " Last element: "
        + doubles[doublesSize - 1]);
    }
    // -------------------------------------------------
    // Regroup
    long startTime = System.currentTimeMillis();
    regroupCombine(contextName, "regroup", table,
      new Partitioner(workers.getNumWorkers()),
      dataMap, workers);
    long endTime = System.currentTimeMillis();
    LOG.info(
      "Regroup time: " + (endTime - startTime));
    for (Partition<DoubleArray> partition : table
      .getPartitions()) {
      double[] doubles = partition.get().get();
      int size = partition.get().size();
      LOG.info(" Partition: " + partition.id()
        + " Count: " + doubles[0]
        + " First element: " + doubles[1]
        + " Last element: " + doubles[size - 1]);
    }
    // ------------------------------------------------
    Driver.reportToDriver(contextName,
      "report-to-driver", workers.getSelfID(),
      driverHost, driverPort);
    ConnPool.get().clean();
    server.stop();
    ForkJoinPool.commonPool().awaitQuiescence(
      Constant.TERMINATION_TIMEOUT,
      TimeUnit.SECONDS);
    System.exit(0);
  }

  /**
   * The regroup communication operation
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @param table
   *          the Table
   * @param partitioner
   *          the Partitioner
   * @param dataMap
   *          the DataMap
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean
    regroupCombine(final String contextName,
      String operationName, Table<P> table,
      Partitioner partitioner, DataMap dataMap,
      Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    final int selfID = workers.getSelfID();
    final int numWorkers =
      workers.getNumWorkers();
    // -----------------------------------------
    // A partition to worker map
    Int2IntOpenHashMap partitionMap =
      new Int2IntOpenHashMap(
        table.getNumPartitions());
    if (partitioner == null) {
      partitioner = new Partitioner(numWorkers);
    }
    List<Transferable> recvPCounts =
      new LinkedList<>();
    boolean isSuccess = PartitionUtil
      .regroupPartitionCount(contextName,
        operationName + ".regroup.meta", table,
        recvPCounts, partitionMap, partitioner,
        dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    int numRecvPartitions = 0;
    for (Transferable trans : recvPCounts) {
      PartitionCount pCount =
        (PartitionCount) trans;
      if (pCount.getWorkerID() != selfID) {
        numRecvPartitions +=
          pCount.getPartitionCount();
      }
    }
    DataUtil.releaseTransList(recvPCounts);
    recvPCounts = null;
    // ----------------------------------------
    // Send partition
    @SuppressWarnings("unchecked")
    List<Partition<P>>[] sendPartitionMap =
      new LinkedList[numWorkers];
    int numSendWorkers = 0;
    IntArrayList rmPartitionIDs =
      new IntArrayList();
    for (Partition<P> partition : table
      .getPartitions()) {
      int partitionID = partition.id();
      int workerID =
        partitionMap.get(partitionID);
      if (workerID != selfID
        && workerID != Constant.UNKNOWN_WORKER_ID) {
        if (sendPartitionMap[workerID] == null) {
          sendPartitionMap[workerID] =
            new LinkedList<>();
          numSendWorkers++;
        }
        sendPartitionMap[workerID].add(partition);
        rmPartitionIDs.add(partitionID);
      }
    }
    if (numSendWorkers > 0) {
      LocalGlobalSyncCollective.dispatch(
        contextName, operationName,
        sendPartitionMap, workers);
    }
    return PartitionUtil.receivePartitions(
      contextName, operationName, table,
      numRecvPartitions, rmPartitionIDs, dataMap);
  }

  /**
   * Apply the function the each partition in the
   * Table
   * 
   * @param table
   *          the Table
   * @param function
   *          the function to be applied
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple, PF extends PartitionFunction<P>>
    boolean applyPartitionFunction(Table<P> table,
      PF function) {
    if (function != null) {
      for (Partition<P> partition : table
        .getPartitions()) {
        try {
          function.apply(partition.get());
        } catch (Exception e) {
          return false;
        }
      }
    }
    return true;
  }

  public static <P extends Simple, PF extends PartitionFunction<P>, PT extends Partitioner>
    boolean regroupAggregate(String contextName,
      String operationName, Table<P> table,
      PT partitioner, PF function,
      DataMap dataMap, Workers workers) {
    boolean isSuccess =
      regroupCombine(contextName, operationName,
        table, partitioner, dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    isSuccess =
      applyPartitionFunction(table, function);
    return isSuccess;
  }

  public static <P extends Simple, PF extends PartitionFunction<P>, PT extends Partitioner>
    boolean aggregate(String contextName,
      String operationName, Table<P> table,
      PT partitioner, PF function,
      DataMap dataMap, Workers workers) {
    boolean isSuccess = false;
    long time1 = System.currentTimeMillis();
    isSuccess = regroupAggregate(contextName,
      operationName + ".regroup", table,
      partitioner, function, dataMap, workers);
    dataMap.cleanOperationData(contextName,
      operationName + ".regroup");
    if (!isSuccess) {
      return false;
    }
    long time2 = System.currentTimeMillis();
    isSuccess = AllgatherCollective.allgather(
      contextName, operationName + ".allgather",
      table, dataMap, workers);
    dataMap.cleanOperationData(contextName,
      operationName + ".allgather");
    if (!isSuccess) {
      return false;
    }
    long time3 = System.currentTimeMillis();
    LOG.info("Regroup-aggregate time (ms): "
      + (time2 - time1) + " Allgather time (ms): "
      + (time3 - time2));
    return true;
  }
}
