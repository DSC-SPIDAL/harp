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
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionUtil;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.server.Server;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Allgather Collective communication
 ******************************************************/
public class AllgatherCollective {

  private static final Logger LOG =
    Logger.getLogger(AllgatherCollective.class);

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
    // ------------------------------------------------
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
    // -----------------------------------------------
    // Generate data partition
    Table<DoubleArray> table =
      new Table<>(0, new DoubleArrPlus());
    int doublesSize = partitionByteSize / 8;
    if (doublesSize < 2) {
      doublesSize = 2;
    }
    // Generate partition data
    for (int i = 0; i < numPartitions; i++) {
      DoubleArray doubleArray = DoubleArray.create(doublesSize, false);
      double[] doubles = doubleArray.get();
      doubles[0] = 1; // One row
      doubles[doublesSize - 1] = workerID;
      // The range of partition ids is based on
      // workerID
      Partition<DoubleArray> partition = new Partition<>(
          workerID * numPartitions + i, doubleArray);
      LOG.info("Data Generate, WorkerID: "
        + workerID + " Partition: "
        + partition.id() + " Row count: "
        + doubles[0] + " First element: "
        + doubles[1] + " Last element: "
        + doubles[doublesSize - 1]);
      table.addPartition(partition);
    }
    // -------------------------------------------------
    // AllGather
    try {
      allgather(contextName, "allgather", table,
        dataMap, workers);
    } catch (Exception e) {
      LOG.error("Fail to allgather", e);
    }
    for (Partition<DoubleArray> partition : table
      .getPartitions()) {
      double[] doubles = partition.get().get();
      int size = partition.get().size();
      LOG.info(" Partition: " + partition.id()
        + " Row count: " + doubles[0]
        + " First element: " + doubles[1]
        + " Last element: " + doubles[size - 1]);
    }
    // ---------------------------------------------------
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
   * Allgather communication operation
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @param table
   *          the data Table
   * @param dataMap
   *          the DataMap
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean
    allgather(final String contextName,
      final String operationName,
      final Table<P> table, final DataMap dataMap,
      final Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    LinkedList<Transferable> ownedPartitions =
      new LinkedList<>(table.getPartitions());
    int numOwnedPartitions =
      table.getNumPartitions();
    // Get worker info
    int selfID = workers.getSelfID();
    int nextID = workers.getNextID();
    int numWorkers = workers.getNumWorkers();
    Data sendData =
      new Data(DataType.PARTITION_LIST,
        contextName, selfID, ownedPartitions,
        DataUtil
          .getNumTransListBytes(ownedPartitions),
        operationName, numOwnedPartitions);
    DataSender sender = new DataSender(sendData,
      nextID, workers, Constant.SEND_DECODE);
    sender.execute();
    // Release
    sendData.releaseHeadArray();
    sendData.releaseBodyArray();
    // Receive starts
    boolean isFailed = false;
    // Received data and decoded data
    int recvSize = numWorkers - 1;
    LinkedList<Data> recvDataList =
      new LinkedList<>();
    for (int i = 0; i < recvSize; i++) {
      // Wait data
      Data recvData = IOUtil.waitAndGet(dataMap,
        contextName, operationName);
      if (recvData == null) {
        isFailed = true;
        break;
      }
      // Continue sending to your next neighbor
      if (recvData.getWorkerID() != nextID) {
        sender = new DataSender(recvData, nextID,
          workers, Constant.SEND_DECODE);
        sender.execute();
      }
      recvData.releaseHeadArray();
      recvData.releaseBodyArray();
      recvDataList.add(recvData);
    }
    if (isFailed) {
      for (Data recvData : recvDataList) {
        recvData.release();
      }
    } else {
      List<Transferable> partitions =
        new LinkedList<>();
      for (Data recvData : recvDataList) {
        partitions.addAll(recvData.getBody());
      }
      PartitionUtil
        .addPartitionsToTable(partitions, table);
    }
    return !isFailed;
  }
}
