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

package edu.iu.harp.partition;

import edu.iu.harp.client.DataSender;
import edu.iu.harp.collective.Communication;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.util.PartitionCount;
import edu.iu.harp.util.PartitionSet;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*******************************************************
 * Utils used on partitions, including receive,
 * regroup, rotate, gather, allgather operations
 * and etc.
 ******************************************************/
public class PartitionUtil {

  @SuppressWarnings("unused")
  private static final Logger LOG =
      Logger.getLogger(PartitionUtil.class);

  public static <P extends Simple> boolean receivePartitions(String contextName,
                    String operationName, Table<P> table,
                    int numRecvPartitions,
                    IntArrayList rmPartitionIDs,
                    DataMap dataMap) {
    List<Transferable> recvPartitionList =
        new LinkedList<>();
    for (int i = 0; i < numRecvPartitions; ) {
      Data data = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (data != null) {
        data.releaseHeadArray();
        data.releaseBodyArray();
        recvPartitionList.addAll(data.getBody());
        i += data.getPartitionID();
      } else {
        DataUtil
            .releaseTransList(recvPartitionList);
        return false;
      }
    }
    for (int partitionID : rmPartitionIDs) {
      Partition<P> partition =
          table.removePartition(partitionID);
      partition.release();
    }
    addPartitionsToTable(recvPartitionList,
        table);
    return true;
  }

  /**
   * Add a list of partitions to table in parallel
   *
   * @param partitions the list of partitions
   * @param table      the table to be added partitions
   */
  public static <P extends Simple> void addPartitionsToTable(
      List<Transferable> partitions,
      Table<P> table) {
    Int2ObjectOpenHashMap<List<Partition<P>>> combineMap =
        new Int2ObjectOpenHashMap<>();
    for (Transferable obj : partitions) {
      Partition<P> partition = (Partition<P>) obj;
      Partition<P> curPar =
          table.getPartition(partition.id());
      if (curPar == null) {
        table.addPartition(partition);
      } else {
        List<Partition<P>> list =
            combineMap.get(partition.id());
        if (list == null) {
          list = new ObjectArrayList<>();
          combineMap.put(partition.id(), list);
        }
        list.add(partition);
      }
    }
    if (!combineMap.isEmpty()) {
      PartitionCombiner<P> combiner =
          table.getCombiner();
      combineMap.int2ObjectEntrySet()
          .parallelStream().forEach(e -> {
        int id = e.getIntKey();
        List<Partition<P>> list = e.getValue();
        Partition<P> partition =
            table.getPartition(id);
        for (Partition<P> p : list) {
          combiner.combine(partition.get(),
              p.get());
          p.release();
        }
      });
    }
    partitions.clear();
  }

  public static <P extends Simple> boolean regroupPartitionCount(String contextName,
                        String operationName, Table<P> table,
                        List<Transferable> recvPCounts,
                        Int2IntOpenHashMap partitionMap,
                        Partitioner regroupPartitioner,
                        DataMap dataMap, Workers workers) {
    partitionMap.defaultReturnValue(
        Constant.UNKNOWN_WORKER_ID);
    int selfID = workers.getSelfID();
    int numWorkers = workers.getNumWorkers();
    int[] workerParCounts = new int[numWorkers];
    for (Partition<P> partition : table
        .getPartitions()) {
      int partitionID = partition.id();
      int workerID =
          partitionMap.get(partitionID);
      if (workerID == Constant.UNKNOWN_WORKER_ID
          && regroupPartitioner != null) {
        workerID = regroupPartitioner
            .getWorkerID(partitionID);
        if (workerID != Constant.UNKNOWN_WORKER_ID) {
          partitionMap.put(partitionID, workerID);
          workerParCounts[workerID]++;
        }
      } else {
        // Put the partition ID and its count to
        // the related worker entry
        workerParCounts[workerID]++;
      }
    }
    // Send and receive partition distribution
    int[] sendOrder = createSendOrder(workers);
    for (int i = 0; i < sendOrder.length; i++) {
      PartitionCount pCount =
          Writable.create(PartitionCount.class);
      pCount.setWorkerID(selfID);
      pCount.setPartitionCount(
          workerParCounts[sendOrder[i]]);
      if (sendOrder[i] != selfID) {
        List<Transferable> commList =
            new LinkedList<>();
        // Send
        commList.add(pCount);
        Data data = new Data(DataType.SIMPLE_LIST,
            contextName, selfID, commList,
            DataUtil.getNumTransListBytes(commList),
            operationName);
        DataSender sender =
            new DataSender(data, sendOrder[i],
                workers, Constant.SEND_DECODE);
        sender.execute();
        data.release();
      } else {
        recvPCounts.add(pCount);
      }
    }
    workerParCounts = null;
    // Receive
    boolean isFailed = false;
    for (int i = 1; i < numWorkers; i++) {
      Data data = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (data != null) {
        data.releaseHeadArray();
        data.releaseBodyArray();
        recvPCounts.addAll(data.getBody());
      } else {
        DataUtil.releaseTransList(recvPCounts);
        isFailed = true;
        break;
      }
    }
    dataMap.cleanOperationData(contextName,
        operationName);
    return !isFailed;
  }

  public static <P extends Simple> boolean rotatePartitionCount(String contextName,
                       String operationName, Table<P> table,
                       List<Transferable> recvPCounts, int destID,
                       DataMap dataMap, Workers workers) {
    int selfID = workers.getSelfID();
    PartitionCount pCount =
        Writable.create(PartitionCount.class);
    pCount.setWorkerID(selfID);
    pCount.setPartitionCount(
        table.getNumPartitions());
    boolean isFailed = false;
    if (destID != selfID) {
      recvPCounts.add(pCount);
      Data sendData = new Data(
          DataType.SIMPLE_LIST, contextName, selfID,
          recvPCounts, DataUtil
          .getNumTransListBytes(recvPCounts),
          operationName);
      DataSender sender = new DataSender(sendData,
          destID, workers, Constant.SEND_DECODE);
      sender.execute();
      sendData.release();
      sendData = null;
      Data recvData = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (recvData != null) {
        recvData.releaseHeadArray();
        recvData.releaseBodyArray();
        recvPCounts.addAll(recvData.getBody());
        recvData = null;
      } else {
        isFailed = true;
      }
    }
    dataMap.cleanOperationData(contextName,
        operationName);
    return !isFailed;
  }

  /**
   * Get the partition ID set from the table
   *
   * @param table a Table
   * @return the partition ID set
   */
  public static <P extends Simple> IntArrayList getPartitionSet(Table<P> table) {
    IntArrayList parIDCount = new IntArrayList();
    for (Partition<P> partition : table
        .getPartitions()) {
      parIDCount.add(partition.id());
    }
    return parIDCount;
  }

  /**
   * The status indicates the failure of
   * regrouping partition set
   */
  public static int FAIL_TO_REGROUP_PARTITION_SET =
      -1;

  public static <P extends Simple> int regroupPartitionSet(String contextName,
                      String operationName, Table<P> table,
                      List<Transferable> recvPSets,
                      Int2IntOpenHashMap partitionMap,
                      Partitioner regroupPartitioner,
                      DataMap dataMap, Workers workers) {
    partitionMap.defaultReturnValue(
        Constant.UNKNOWN_WORKER_ID);
    int selfID = workers.getSelfID();
    int numWorkers = workers.getNumWorkers();
    // Initialize entries in workerParIDCounts
    // for every worker
    IntArrayList[] workerParIDs =
        new IntArrayList[numWorkers];
    for (int i = 0; i < numWorkers; i++) {
      workerParIDs[i] = new IntArrayList();
    }
    // Regroup partition IDs based on the
    // worker ID
    int numRegroupPartitions = 0;
    for (Partition<P> partition : table
        .getPartitions()) {
      int partitionID = partition.id();
      int workerID =
          partitionMap.get(partitionID);
      if (workerID == Constant.UNKNOWN_WORKER_ID
          && regroupPartitioner != null) {
        // Complete the global partition map
        workerID = regroupPartitioner
            .getWorkerID(partitionID);
        if (workerID != Constant.UNKNOWN_WORKER_ID) {
          partitionMap.put(partitionID, workerID);
          workerParIDs[workerID].add(partitionID);
          numRegroupPartitions++;
        }
      } else {
        workerParIDs[workerID].add(partitionID);
        numRegroupPartitions++;
      }
    }
    // Send and receive partition sets
    int[] sendOrder = createSendOrder(workers);
    for (int i = 0; i < sendOrder.length; i++) {
      IntArrayList workerParIDSet =
          workerParIDs[sendOrder[i]];
      PartitionSet pSet =
          Writable.create(PartitionSet.class);
      pSet.setWorkerID(selfID);
      pSet.setParSet(workerParIDSet);
      if (sendOrder[i] != selfID) {
        // Send
        List<Transferable> commList =
            new LinkedList<>();
        commList.add(pSet);
        Data data = new Data(DataType.SIMPLE_LIST,
            contextName, selfID, commList,
            DataUtil.getNumTransListBytes(commList),
            operationName);
        DataSender sender =
            new DataSender(data, sendOrder[i],
                workers, Constant.SEND_DECODE);
        sender.execute();
        data.release();
      } else {
        recvPSets.add(pSet);
      }
    }
    workerParIDs = null;
    // Receive local table's partition
    // distribution
    boolean isFailed = false;
    for (int i = 1; i < numWorkers; i++) {
      Data data = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (data != null) {
        data.releaseHeadArray();
        data.releaseBodyArray();
        recvPSets.addAll(data.getBody());
      } else {
        DataUtil.releaseTransList(recvPSets);
        isFailed = true;
        break;
      }
    }
    dataMap.cleanOperationData(contextName,
        operationName);
    if (isFailed) {
      return FAIL_TO_REGROUP_PARTITION_SET;
    } else {
      return numRegroupPartitions;
    }
  }

  /**
   * Allgather the PartitionSet among workers
   *
   * @param contextName
   * @param operationName
   * @param table
   * @param recvPSet
   * @param dataMap
   * @param workers
   * @return true if succeeded, false if failed
   */
  public static <P extends Simple> boolean allgatherPartitionSet(String contextName,
                        String operationName, Table<P> table,
                        List<Transferable> recvPSet,
                        DataMap dataMap, Workers workers) {
    PartitionSet pSet =
        Writable.create(PartitionSet.class);
    pSet.setWorkerID(workers.getSelfID());
    pSet.setParSet(getPartitionSet(table));
    recvPSet.add(pSet);
    boolean isSuccess = Communication.allgather(
        contextName, operationName, recvPSet,
        dataMap, workers);
    dataMap.cleanOperationData(contextName,
        operationName);
    return isSuccess;
  }

  /**
   * Gather the PartitionSet from workers
   *
   * @param contextName
   * @param operationName
   * @param table
   * @param recvPSets
   * @param dataMap
   * @param workers
   * @return true if succeeded, false if failed
   */
  public static <P extends Simple> boolean gatherPartitionSet(String contextName,
                     String operationName, Table<P> table,
                     List<Transferable> recvPSets,
                     DataMap dataMap, Workers workers) {
    int selfID = workers.getSelfID();
    int masterID = workers.getMasterID();
    PartitionSet pSet =
        Writable.create(PartitionSet.class);
    pSet.setWorkerID(selfID);
    pSet.setParSet(
        PartitionUtil.getPartitionSet(table));
    recvPSets.add(pSet);
    boolean isSuccess = Communication.gather(
        contextName, operationName, recvPSets,
        masterID, dataMap, workers);
    // Clean the queue
    dataMap.cleanOperationData(contextName,
        operationName);
    return isSuccess;
  }

  /**
   * Define the order of workers to send.
   *
   * @param workers Workers
   * @return the order of sending operation
   */
  public static int[] createSendOrder(Workers workers) {
    Random random =
        new Random(System.currentTimeMillis());
    int numWorkers = workers.getNumWorkers();
    int[] sendOrder = new int[numWorkers];
    int index = 0;
    for (WorkerInfo worker : workers
        .getWorkerInfoList()) {
      sendOrder[index] = worker.getID();
      index++;
    }
    for (int i = numWorkers - 1; i >= 0; i--) {
      int tmpi = random.nextInt(i + 1);
      int tmp = sendOrder[tmpi];
      sendOrder[tmpi] = sendOrder[i];
      sendOrder[i] = tmp;
    }
    return sendOrder;
  }
}
