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

import edu.iu.harp.client.DataChainBcastSender;
import edu.iu.harp.client.DataSender;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataStatus;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionUtil;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.server.Server;
import edu.iu.harp.util.PartitionCount;
import edu.iu.harp.util.PartitionSet;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Local-Global synchronization Collective
 * communication
 ******************************************************/
public class LocalGlobalSyncCollective {

  private static final Logger LOG = Logger
      .getLogger(LocalGlobalSyncCollective.class);

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
    Table<DoubleArray> localTable =
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
      doubles[doublesSize - 1] = i;
      Partition<DoubleArray> partition =
          new Partition<DoubleArray>(i,
              doubleArray);
      localTable.addPartition(partition);
      LOG.info("Data Generate, WorkerID: "
          + workerID + " Partition: "
          + partition.id() + " Count: " + doubles[0]
          + " First element: " + doubles[1]
          + " Last element: "
          + doubles[doublesSize - 1]);
    }
    Table<DoubleArray> globalTable =
        new Table<DoubleArray>(0,
            new DoubleArrPlus());
    // -------------------------------------------------
    // Regroup
    long startTime = System.currentTimeMillis();
    try {
      LOG.info("local->global");
      push(contextName, "local-global",
          localTable, globalTable,
          new Partitioner(workers.getNumWorkers()),
          dataMap, workers);
      LOG.info("global->local");
      pull(contextName, "global-local",
          localTable, globalTable, true, dataMap,
          workers);
    } catch (Exception e) {
      LOG.info("Fail to perform sync. ", e);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("local-global sync time: "
        + (endTime - startTime));
    for (Partition<DoubleArray> partition : localTable
        .getPartitions()) {
      double[] doubles = partition.get().get();
      int size = partition.get().size();
      LOG.info("Local partition: "
          + partition.id() + " Count: " + doubles[0]
          + " First element: " + doubles[1]
          + " Last element: " + doubles[size - 1]);
    }
    for (Partition<DoubleArray> partition : globalTable
        .getPartitions()) {
      double[] doubles = partition.get().get();
      int size = partition.get().size();
      LOG.info("Global partition: "
          + partition.id() + " Count: " + doubles[0]
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
   * The pull communication operation. Pull the
   * global data to local
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param localTable    the local Table
   * @param globalTable   the global Table
   * @param useBcast      use broadcast or not
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean pull(
      final String contextName,
      final String operationName,
      Table<P> localTable, Table<P> globalTable,
      boolean useBcast, DataMap dataMap,
      Workers workers) {
    return pullGlobalToLocal(contextName,
        operationName, localTable, globalTable,
        useBcast, dataMap, workers);
  }

  /**
   * The push communication operation. Push the
   * local data to global
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param localTable    the local Table
   * @param globalTable   the global Table
   * @param partitioner   the Partitioner
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple, PT extends Partitioner>
  boolean push(final String contextName,
               final String operationName,
               Table<P> localTable, Table<P> globalTable,
               PT partitioner, DataMap dataMap,
               Workers workers) {
    return pushLocalToGlobal(contextName,
        operationName, localTable, globalTable,
        partitioner, dataMap, workers);
  }

  /**
   * The broadcast communication operation
   *
   * @param contextName     the name of the context
   * @param operationName   the name of the operation
   * @param ownedPartitions the partitions to broadcast
   * @param workers         the Workers
   */
  static <P extends Simple> void broadcast(
      String contextName, String operationName,
      List<Partition<P>> ownedPartitions,
      Workers workers) {
    final int numPartitions =
        ownedPartitions.size();
    List<Transferable> bcastPartitions =
        new LinkedList<>();
    long size = 0L;
    int count = 0;
    int selfID = workers.getSelfID();
    for (Partition<P> partition : ownedPartitions) {
      long partitionSize =
          partition.getNumEnocdeBytes();
      if (size
          + partitionSize > Constant.MAX_ARRAY_SIZE) {
        broadcastPartitions(contextName,
            operationName, selfID, bcastPartitions,
            (int) size, workers);
        bcastPartitions.clear();
        bcastPartitions.add(partition);
        size = partitionSize;
        count++;
      } else {
        bcastPartitions.add(partition);
        size += partitionSize;
        count++;
      }
      if (count == numPartitions
          && size <= Constant.MAX_ARRAY_SIZE) {
        broadcastPartitions(contextName,
            operationName, selfID, bcastPartitions,
            (int) size, workers);
        bcastPartitions.clear();
        size = 0L;
        count = 0;
      }
    }
  }

  /**
   * The broadcast communication operation
   *
   * @param contextName     the name of the context
   * @param operationName   the name of the operation
   * @param selfID          the self ID
   * @param bcastPartitions the partitions to broadcast
   * @param size            the size of partitions to broadcast
   * @param workers         the Workers
   */
  private static void broadcastPartitions(
      String contextName, String operationName,
      int selfID,
      List<Transferable> bcastPartitions, int size,
      Workers workers) {
    if (bcastPartitions.size() > 0) {
      Data data = new Data(
          DataType.PARTITION_LIST, contextName,
          selfID, bcastPartitions, size,
          operationName, bcastPartitions.size());
      DataChainBcastSender bcaster =
          new DataChainBcastSender(data, workers,
              Constant.CHAIN_BCAST_DECODE);
      bcaster.execute();
      data.releaseHeadArray();
      data.releaseBodyArray();
    }
  }

  /**
   * Send partitions to destinations according to
   * the partitionMap
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param partitionMap  the map records where to send each
   *                      partition
   * @param workers       the Workers
   */
  static <P extends Simple> void dispatch(
      String contextName, String operationName,
      List<Partition<P>>[] partitionMap,
      Workers workers) {
    int[] sendOrder =
        PartitionUtil.createSendOrder(workers);
    int selfID = workers.getSelfID();
    for (int i = 0; i < sendOrder.length; i++) {
      List<Partition<P>> partitionList =
          partitionMap[sendOrder[i]];
      if (partitionList != null) {
        int numPartitions = partitionList.size();
        long size = 0L;
        int count = 0;
        List<Transferable> sendPartitions =
            new LinkedList<>();
        for (Partition<P> partition : partitionList) {
          long partitionSize =
              partition.getNumEnocdeBytes();
          if (size
              + partitionSize > Constant.MAX_ARRAY_SIZE) {
            sendPartitions(contextName,
                operationName, selfID, sendOrder[i],
                sendPartitions, (int) size,
                workers);
            sendPartitions.clear();
            sendPartitions.add(partition);
            size = partitionSize;
            count++;
          } else {
            sendPartitions.add(partition);
            size += partitionSize;
            count++;
          }
          if (count == numPartitions
              && size <= Constant.MAX_ARRAY_SIZE) {
            sendPartitions(contextName,
                operationName, selfID, sendOrder[i],
                sendPartitions, (int) size,
                workers);
            sendPartitions.clear();
            size = 0L;
            count++;
          }
        }
      }
    }
  }

  /**
   * Copy local data
   *
   * @param contextName     the name of the context
   * @param operationName   the name of the operation
   * @param ownedPartitions the worker's own partitions
   * @param workers         the Workers
   * @param dataMap         the DataMap
   */
  private static <P extends Simple> void copy(
      String contextName, String operationName,
      List<Partition<P>> ownedPartitions,
      Workers workers, DataMap dataMap) {
    final int numPartitions =
        ownedPartitions.size();
    List<Transferable> localPartitions =
        new LinkedList<>();
    long size = 0L;
    int count = 0;
    int selfID = workers.getSelfID();
    for (Partition<P> partition : ownedPartitions) {
      long partitionSize =
          partition.getNumEnocdeBytes();
      if (size
          + partitionSize > Constant.MAX_ARRAY_SIZE) {
        copyPartitions(contextName, operationName,
            selfID, localPartitions, (int) size,
            workers, dataMap);
        localPartitions.clear();
        localPartitions.add(partition);
        size = partitionSize;
        count++;
      } else {
        localPartitions.add(partition);
        size += partitionSize;
        count++;
      }
      if (count == numPartitions
          && size <= Constant.MAX_ARRAY_SIZE) {
        copyPartitions(contextName, operationName,
            selfID, localPartitions, (int) size,
            workers, dataMap);
        localPartitions.clear();
        size = 0L;
        count = 0;
      }
    }
  }

  /**
   * Copy local partitions
   *
   * @param contextName     the name of the context
   * @param operationName   the name of the operation
   * @param selfID          the self ID
   * @param localPartitions the local partitions
   * @param size            the size of the local partitions
   * @param workers         the Workers
   * @param dataMap         the DataMap
   */
  private static void copyPartitions(
      String contextName, String operationName,
      int selfID,
      List<Transferable> localPartitions, int size,
      Workers workers, DataMap dataMap) {
    if (localPartitions.size() > 0) {
      Data data = new Data(
          DataType.PARTITION_LIST, contextName,
          selfID, localPartitions, size,
          operationName, localPartitions.size());
      data.encodeHead();
      data.encodeBody();
      final Data newData = new Data(
          data.getHeadArray(), data.getBodyArray());
      data = null;
      newData.decodeHeadArray();
      newData.decodeBodyArray();
      if ((newData
          .getHeadStatus() == DataStatus.ENCODED_ARRAY_DECODED
          && newData
          .getBodyStatus() == DataStatus.ENCODED_ARRAY_DECODED)
          && newData.isOperationData()) {
        dataMap.putData(newData);
      }
    }
  }

  /**
   * The push communication operation. Push the
   * local data to global
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param localTable    the local Table
   * @param globalTable   the global Table
   * @param partitioner   the Partitioner
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple, PT extends Partitioner>
  boolean pushLocalToGlobal(String contextName,
                            String operationName, Table<P> localTable,
                            Table<P> globalTable, PT partitioner,
                            DataMap dataMap, Workers workers) {
    final int selfID = workers.getSelfID();
    final int numWorkers =
        workers.getNumWorkers();
    // ---------------------------------------------------
    // Broadcast global table's partition
    // distribution to all the workers
    List<Transferable> commList =
        new LinkedList<>();
    boolean isSuccess = PartitionUtil
        .allgatherPartitionSet(contextName,
            operationName + ".allgather.global",
            globalTable, commList, dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    // ------------------------------------------
    Int2IntOpenHashMap globalTableMap =
        new Int2IntOpenHashMap();
    for (Transferable trans : commList) {
      PartitionSet recvPSet =
          (PartitionSet) trans;
      final IntArrayList pList =
          recvPSet.getParSet();
      if (pList != null) {
        int workerID = recvPSet.getWorkerID();
        for (int pID : pList) {
          globalTableMap.put(pID, workerID);
        }
      }
    }
    DataUtil.releaseTransList(commList);
    // ---------------------------------------------
    isSuccess = PartitionUtil
        .regroupPartitionCount(contextName,
            operationName + ".regroup.local",
            localTable, commList, globalTableMap,
            partitioner, dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    // --------------------------------------------
    int numRecvPartitions = 0;
    for (Transferable trans : commList) {
      PartitionCount recvPCount =
          (PartitionCount) trans;
      numRecvPartitions +=
          recvPCount.getPartitionCount();
    }
    DataUtil.releaseTransList(commList);
    commList = null;
    // -------------------------------------------
    @SuppressWarnings("unchecked")
    List<Partition<P>>[] sendPartitionMap =
        new List[numWorkers];
    int numSendWorkers = 0;
    List<Partition<P>> localPartitions =
        new LinkedList<>();
    for (Partition<P> partition : localTable
        .getPartitions()) {
      final int partitionID = partition.id();
      int workerID =
          globalTableMap.get(partitionID);
      if (workerID == selfID) {
        localPartitions.add(partition);
      } else if (workerID != Constant.UNKNOWN_WORKER_ID) {
        if (sendPartitionMap[workerID] == null) {
          sendPartitionMap[workerID] =
              new LinkedList<>();
          numSendWorkers++;
        }
        sendPartitionMap[workerID].add(partition);
      }
    }
    // ---------------------------------------------
    if (numSendWorkers > 0) {
      dispatch(contextName, operationName,
          sendPartitionMap, workers);
    }
    if (!localPartitions.isEmpty()) {
      copy(contextName, operationName,
          localPartitions, workers, dataMap);
    }
    // ---------------------------------------------
    // Start receiving
    return PartitionUtil.receivePartitions(
        contextName, operationName, globalTable,
        numRecvPartitions, new IntArrayList(),
        dataMap);
  }

  /**
   * The pull communication operation. Pull the
   * global data to local
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param localTable    the local Table
   * @param globalTable   the global Table
   * @param useBcast      use broadcast or not
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean
  pullGlobalToLocal(final String contextName,
                    final String operationName,
                    final Table<P> localTable,
                    final Table<P> globalTable,
                    final boolean useBcast,
                    final DataMap dataMap,
                    final Workers workers) {
    final int selfID = workers.getSelfID();
    final int numWorkers =
        workers.getNumWorkers();
    // ------------------------------------------------
    List<Transferable> commList =
        new LinkedList<>();
    boolean isSuccess = PartitionUtil
        .allgatherPartitionSet(contextName,
            operationName + ".allgather.global",
            globalTable, commList, dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    // -----------------------------------------------
    // Record partition distribution on the global
    // table partition ID <-> worker ID
    Int2IntOpenHashMap globalParDistrMap =
        new Int2IntOpenHashMap();
    for (Transferable trans : commList) {
      PartitionSet recvPSet =
          (PartitionSet) trans;
      IntArrayList pIDList = recvPSet.getParSet();
      if (pIDList != null) {
        int workerID = recvPSet.getWorkerID();
        for (int pID : pIDList) {
          globalParDistrMap.put(pID, workerID);
        }
      }
    }
    DataUtil.releaseTransList(commList);
    // -----------------------------------------------
    // Regroup local table info to global table
    // locations, let the global table holders
    // know the data sending request
    int numRecvPartitions = PartitionUtil
        .regroupPartitionSet(contextName,
            operationName + ".regroup.local",
            localTable, commList, globalParDistrMap,
            null, dataMap, workers);
    if (numRecvPartitions == PartitionUtil.FAIL_TO_REGROUP_PARTITION_SET) {
      return false;
    }
    // ----------------------------------------------
    // Detect which and where to send
    Int2ObjectOpenHashMap<IntArrayList> globalParRecvWorkerMap =
        new Int2ObjectOpenHashMap<>();
    for (Transferable trans : commList) {
      PartitionSet recvPSet =
          (PartitionSet) trans;
      IntArrayList pList = recvPSet.getParSet();
      if (pList != null) {
        int workerID = recvPSet.getWorkerID();
        for (int pID : pList) {
          IntArrayList recvWorkerIDs =
              globalParRecvWorkerMap.get(pID);
          if (recvWorkerIDs == null) {
            recvWorkerIDs = new IntArrayList();
            globalParRecvWorkerMap.put(pID,
                recvWorkerIDs);
          }
          recvWorkerIDs.add(workerID);
        }
      }
    }
    DataUtil.releaseTransList(commList);
    commList = null;
    // -----------------------------------------
    // Build communication lists
    List<Partition<P>> bcastPartitions =
        new LinkedList<>();
    List<Partition<P>> localPartitions =
        new LinkedList<>();
    @SuppressWarnings("unchecked")
    List<Partition<P>>[] sendPartitions =
        new List[numWorkers];
    int numSendWorkers = 0;
    for (Partition<P> partition : globalTable
        .getPartitions()) {
      int partitionID = partition.id();
      IntArrayList recvWorkerIDs =
          globalParRecvWorkerMap.get(partitionID);
      if (recvWorkerIDs != null) {
        if (useBcast
            && recvWorkerIDs.size() == numWorkers) {
          if (numWorkers > 1) {
            bcastPartitions.add(partition);
          }
          localPartitions.add(partition);
        } else {
          for (int recvWorkerID : recvWorkerIDs) {
            if (recvWorkerID == selfID) {
              localPartitions.add(partition);
            } else {
              if (sendPartitions[recvWorkerID] == null) {
                sendPartitions[recvWorkerID] =
                    new LinkedList<>();
                numSendWorkers++;
              }
              sendPartitions[recvWorkerID]
                  .add(partition);
            }
          }
        }
      }
    }
    // -----------------------------------------
    // Then send/broadcast partitions
    if (!bcastPartitions.isEmpty()) {
      broadcast(contextName, operationName,
          bcastPartitions, workers);
    }
    if (numSendWorkers > 0) {
      dispatch(contextName, operationName,
          sendPartitions, workers);
    }
    // Copy local data
    if (!localPartitions.isEmpty()) {
      copy(contextName, operationName,
          localPartitions, workers, dataMap);
    }
    // ---------------------------------------------
    // Receive all the partitions
    return PartitionUtil.receivePartitions(
        contextName, operationName, localTable,
        numRecvPartitions, new IntArrayList(),
        dataMap);
  }

  /**
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param globalTable   the global Table
   * @param rotateMap     the map indicating the order of
   *                      rotation
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean rotate(
      final String contextName,
      final String operationName,
      Table<P> globalTable, Int2IntMap rotateMap,
      DataMap dataMap, Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    int selfID = workers.getSelfID();
    int destID = workers.getNextID();
    if (rotateMap != null) {
      destID = rotateMap.get(selfID);
    }
    // ----------------------------------------------
    if (selfID != destID) {
      return rotateGlobal(contextName,
          operationName, globalTable, selfID,
          destID, dataMap, workers);
    } else {
      return true;
    }
  }

  /**
   * Send the local data to the destination,
   * receive the data from one of other worker
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param globalTable   the globle Table
   * @param selfID        the self ID
   * @param destID        the destination ID
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  private static <P extends Simple> boolean
  rotateGlobal(final String contextName,
               final String operationName,
               Table<P> globalTable, int selfID,
               int destID, DataMap dataMap,
               Workers workers) {
    LinkedList<Transferable> recvPCounts =
        new LinkedList<>();
    PartitionUtil.rotatePartitionCount(
        contextName, operationName + ".rotate",
        globalTable, recvPCounts, destID, dataMap,
        workers);
    int numRecvPartitions =
        ((PartitionCount) recvPCounts.getFirst())
            .getPartitionCount();
    DataUtil.releaseTransList(recvPCounts);
    recvPCounts = null;
    IntArrayList rmPartitionIDs =
        new IntArrayList(
            globalTable.getPartitionIDs());
    // Send partitions
    shift(contextName, operationName, globalTable,
        selfID, destID, workers);
    return PartitionUtil.receivePartitions(
        contextName, operationName, globalTable,
        numRecvPartitions, rmPartitionIDs, dataMap);
  }

  /**
   * Send the partitions in the local table to the
   * detination
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param table         the data Table
   * @param selfID        the self ID
   * @param destID        the destination ID
   * @param workers       the Workers
   */
  private static <P extends Simple> void shift(
      String contextName, String operationName,
      Table<P> table, int selfID, int destID,
      Workers workers) {
    final int numPartitions =
        table.getNumPartitions();
    List<Transferable> sendPartitions =
        new LinkedList<>();
    long size = 0L;
    int count = 0;
    for (Partition<P> partition : table
        .getPartitions()) {
      long partitionSize =
          partition.getNumEnocdeBytes();
      if (size
          + partitionSize > Constant.MAX_ARRAY_SIZE) {
        sendPartitions(contextName, operationName,
            selfID, destID, sendPartitions,
            (int) size, workers);
        sendPartitions.clear();
        sendPartitions.add(partition);
        size = partitionSize;
        count++;
      } else {
        sendPartitions.add(partition);
        size += partitionSize;
        count++;
      }
      if (count == numPartitions
          && size <= Constant.MAX_ARRAY_SIZE) {
        sendPartitions(contextName, operationName,
            selfID, destID, sendPartitions,
            (int) size, workers);
        sendPartitions.clear();
        size = 0L;
        count = 0;
      }
    }
  }

  /**
   * Send partitions
   *
   * @param contextName    the name of the context
   * @param operationName  the name of the operation
   * @param selfID         the self ID
   * @param destID         the destination ID
   * @param sendPartitions the partitions to send
   * @param size           the size of partitions
   * @param workers        the Workers
   */
  private static void sendPartitions(
      String contextName, String operationName,
      int selfID, int destID,
      List<Transferable> sendPartitions, int size,
      Workers workers) {
    if (sendPartitions.size() > 0) {
      Data data = new Data(
          DataType.PARTITION_LIST, contextName,
          selfID, sendPartitions, size,
          operationName, sendPartitions.size());
      DataSender sender = new DataSender(data,
          destID, workers, Constant.SEND_DECODE);
      sender.execute();
      data.releaseHeadArray();
      data.releaseBodyArray();
    }
  }
}
