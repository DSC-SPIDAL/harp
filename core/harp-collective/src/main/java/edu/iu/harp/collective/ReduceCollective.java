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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Reduce Collective communication
 ******************************************************/
public class ReduceCollective {

  private static final Logger LOG =
      Logger.getLogger(ReduceCollective.class);

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
      DoubleArray doubleArray =
          DoubleArray.create(doublesSize, false);
      double[] doubles = doubleArray.get();
      doubles[0] = 1; // One row
      doubles[doublesSize - 1] = workerID;
      // The range of partition ids is based on
      // workerID
      Partition<DoubleArray> partition =
          new Partition<DoubleArray>(i,
              doubleArray);
      LOG.info("Data Generate, WorkerID: "
          + workerID + " Partition: "
          + partition.id() + " Row count: "
          + doubles[0] + " First element: "
          + doubles[1] + " Last element: "
          + doubles[doublesSize - 1]);
      table.addPartition(partition);
    }
    // -------------------------------------------------
    // Reduce
    try {
      reduce(contextName, "reduce", table,
          workers.getMasterID(), dataMap, workers);
    } catch (Exception e) {
      LOG.error("Fail to reduce", e);
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
   * Reduce communication operation
   *
   * @param contextName    the name of the context
   * @param operationName  the operation of the context
   * @param table          the data Table
   * @param reduceWorkerID the workerID to send
   * @param dataMap        the DataMap
   * @param workers        the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean reduce(
      final String contextName,
      final String operationName,
      final Table<P> table,
      final int reduceWorkerID,
      final DataMap dataMap,
      final Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    int selfID = workers.getSelfID();
    int left = workers.getMinID();
    int right = workers.getMaxID();
    int middle = workers.getMiddleID();
    int half = middle - left + 1;
    int range = right - left + 1;
    int destID = 0;
    int sourceID = 0;
    // Check if this worker needs to send data
    // or receive data
    boolean isSend = false;
    boolean isRecv = false;
    boolean isFailed = false;
    Int2ObjectOpenHashMap<Data> cachedDataMap =
        new Int2ObjectOpenHashMap<>();
    while (left < right) {
      if (selfID <= middle
          && reduceWorkerID > middle) {
        destID = selfID + half;
        isSend = true;
        isRecv = false;
        // If the range is odd, middle's destID
        // will be out of range.
        if (destID > right) {
          destID = middle + 1;
        }
      } else if (selfID <= middle
          && reduceWorkerID <= middle) {
        sourceID = selfID + half;
        isSend = false;
        if (sourceID <= right) {
          isRecv = true;
        } else {
          isRecv = false;
        }
      } else if (selfID > middle
          && reduceWorkerID <= middle) {
        destID = selfID - half;
        isSend = true;
        isRecv = false;
      } else if (selfID > middle
          && reduceWorkerID > middle) {
        sourceID = selfID - half;
        isSend = false;
        isRecv = true;
      }
      // LOG.info("left " + left + ", right "
      // + right + ", middle " + middle
      // + ", half " + half + ", range " + range
      // + ", selfID " + selfID + ", destID "
      // + destID + ", sourceID " + sourceID
      // + ", isSend " + isSend + ", isRecv "
      // + isSend);
      if (isSend) {
        LinkedList<Transferable> ownedPartitions =
            new LinkedList<>(table.getPartitions());
        int numOwnedPartitions =
            table.getNumPartitions();
        // Send owned partitions
        Data sendData =
            new Data(DataType.PARTITION_LIST,
                contextName, selfID, ownedPartitions,
                DataUtil.getNumTransListBytes(
                    ownedPartitions),
                operationName, numOwnedPartitions);
        DataSender sender =
            new DataSender(sendData, destID,
                workers, Constant.SEND_DECODE);
        sender.execute();
        // Release
        sendData.releaseHeadArray();
        sendData.releaseBodyArray();
        ownedPartitions = null;
        sendData = null;
        // Remove and release all the partitions
        // in the table
        table.release();
        // Once partitions are sent, it is done.
        break;
      } else if (isRecv) {
        {
          Data recvData =
              cachedDataMap.remove(sourceID);
          // Wait data
          if (recvData == null) {
            while (true) {
              recvData =
                  IOUtil.waitAndGet(dataMap,
                      contextName, operationName);
              if (recvData == null) {
                isFailed = true;
                break;
              } else {
                recvData.releaseHeadArray();
                recvData.releaseBodyArray();
                if (recvData
                    .getWorkerID() != sourceID) {
                  cachedDataMap.put(
                      recvData.getWorkerID(),
                      recvData);
                } else {
                  break;
                }
              }
            }
          }
          // Add partitions to the table, note
          // that
          // the data has been decoded
          if (!isFailed) {
            PartitionUtil.addPartitionsToTable(
                recvData.getBody(), table);
          }
        }
        // If range is odd, midID + 1 receive
        // additional data from midID
        if (range % 2 == 1
            && selfID == (middle + 1)
            && !isFailed) {
          // LOG.info("Get extra data from middle:
          // "
          // + middle);
          Data extraData =
              cachedDataMap.remove(middle);
          // Wait data
          if (extraData == null) {
            while (true) {
              extraData =
                  IOUtil.waitAndGet(dataMap,
                      contextName, operationName);
              if (extraData == null) {
                isFailed = true;
                break;
              } else {
                extraData.releaseHeadArray();
                extraData.releaseBodyArray();
                if (extraData
                    .getWorkerID() != middle) {
                  cachedDataMap.put(
                      extraData.getWorkerID(),
                      extraData);
                } else {
                  break;
                }
              }
            }
          }
          // Add partitions to the table, note
          // that the data has been decoded
          if (!isFailed) {
            PartitionUtil.addPartitionsToTable(
                extraData.getBody(), table);
          }
        }
        if (isFailed) {
          for (Data d : cachedDataMap.values()) {
            d.release();
          }
          cachedDataMap.clear();
          cachedDataMap = null;
          // Release the partitions in the current
          // table
          table.release();
          return false;
        }
      }
      if (selfID <= middle) {
        right = middle;
      } else {
        left = middle + 1;
      }
      middle = (left + right) / 2;
      half = middle - left + 1;
      range = right - left + 1;
    }
    return true;
  }
}
