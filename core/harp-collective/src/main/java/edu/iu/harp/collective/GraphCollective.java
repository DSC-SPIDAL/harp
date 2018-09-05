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

import edu.iu.harp.example.EdgePartition;
import edu.iu.harp.example.EdgeTable;
import edu.iu.harp.example.EdgeVal;
import edu.iu.harp.example.IntVal;
import edu.iu.harp.example.MessagePartition;
import edu.iu.harp.example.MessageTable;
import edu.iu.harp.example.VertexPartition;
import edu.iu.harp.example.VertexTable;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.keyval.ValStatus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionUtil;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.server.Server;
import edu.iu.harp.util.Join;
import edu.iu.harp.util.PartitionSet;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Graph Collective communication
 ******************************************************/
public class GraphCollective {

  private static final Logger LOG =
      Logger.getLogger(GraphCollective.class);

  public static void main(String args[])
      throws Exception {
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    int workerID = Integer.parseInt(args[2]);
    long jobID = Long.parseLong(args[3]);
    Driver.initLogger(workerID);
    LOG.info(
        "args[] " + driverHost + " " + driverPort
            + " " + workerID + " " + jobID);
    // ------------------------------------------------
    // Worker initialize
    EventQueue dataQueue = new EventQueue();
    DataMap dataMap = new DataMap();
    Workers workers = new Workers(workerID);
    Server server =
        new Server(workers.getSelfInfo().getNode(),
            workers.getSelfInfo().getPort(),
            dataQueue, dataMap, workers);
    server.start();
    String contextName = jobID + "";
    // Barrier guarantees the living workers get
    // the same view of the barrier result
    boolean isSuccess = Communication.barrier(
        contextName, "barrier", dataMap, workers);
    LOG.info("Barrier: " + isSuccess);
    // -----------------------------------------------
    // Generate in-edge table and vertex table
    // (for initial page-rank value) , (for out
    // edge count)
    int vtxCountPerWorker = 5;
    int edgeCountPerWorker = 3;
    int numWorkers = workers.getNumWorkers();
    int totalVtxCount =
        vtxCountPerWorker * numWorkers;
    // Partition seed needs to be the same
    // across all graph tables
    LOG.info("Total vtx count: " + totalVtxCount);
    EdgeTable inEdgeTable = new EdgeTable(0);
    try {
      EdgeVal val = new EdgeVal();
      Random random = new Random(workerID);
      for (int i =
           0; i < edgeCountPerWorker; i++) {
        int target =
            random.nextInt(totalVtxCount);
        int source = 0;
        do {
          source = random.nextInt(totalVtxCount);
        } while (source == target);
        val.addEdge(source, 0, target);
        ValStatus status =
            inEdgeTable.addKeyVal(target, val);
        if (status == ValStatus.ADDED) {
          val = new EdgeVal();
        } else {
          val.clear();
        }
      }
    } catch (Exception e) {
      LOG.error("Fail to create edge table.", e);
    }
    // --------------------------------------------------
    // Regroup edges
    try {
      LOG.info("PRINT EDGE TABLE START");
      printEdgeTable(inEdgeTable);
      LOG.info("PRINT EDGE TABLE END");
      LOG.info("REGROUP START");
      isSuccess = RegroupCollective
          .regroupCombine(contextName,
              "regroup-edges", inEdgeTable,
              new Partitioner(numWorkers), dataMap,
              workers);
      LOG.info("REGROUP End " + isSuccess);
      LOG.info("PRINT EDGE TABLE START");
      printEdgeTable(inEdgeTable);
      LOG.info("PRINT EDGE TABLE END");
    } catch (Exception e) {
      LOG.error("Error when adding edges", e);
    }
    // --------------------------------------------------
    // Initialize page rank of each vtx
    VertexTable vtxTable = new VertexTable(1);
    try {
      for (Partition<EdgePartition> partition : inEdgeTable
          .getPartitions()) {
        ObjectIterator<Int2ObjectMap.Entry<EdgeVal>> iterator =
            partition.get().getKVMap()
                .int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
          EdgeVal edgeVal =
              iterator.next().getValue();
          for (int i = 0; i < edgeVal
              .getNumEdges(); i++) {
            vtxTable.addKeyVal(
                edgeVal.getDest()[i],
                new IntVal(1));
          }
        }
      }
    } catch (Exception e) {
      LOG.error(
          "Fail to create vertex table from the edge table.",
          e);
    }
    LOG.info("PRINT VTX TABLE START");
    printVtxTable(vtxTable);
    LOG.info("PRINT VTX TABLE END");
    // --------------------------------------------------
    // Generate message table for counting out
    // edges
    MessageTable msgTable = new MessageTable(2);
    IntVal msgVal = new IntVal();
    for (Partition<EdgePartition> partition : inEdgeTable
        .getPartitions()) {
      ObjectIterator<Int2ObjectMap.Entry<EdgeVal>> iterator =
          partition.get().getKVMap()
              .int2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        EdgeVal edgeVal =
            iterator.next().getValue();
        for (int i = 0; i < edgeVal
            .getNumEdges(); i++) {
          msgVal.setInt(1);
          ValStatus status = msgTable.addKeyVal(
              edgeVal.getSrc()[i], msgVal);
          if (status == ValStatus.ADDED) {
            msgVal = new IntVal();
          }
        }
      }
    }
    LOG.info("PRINT MSG TABLE START");
    printMsgTable(msgTable);
    LOG.info("PRINT MSG TABLE END");
    // All-to-all communication, moves message
    // partition to the place
    // where the vertex partition locate
    LOG.info("All MSG TO ALL VTX START");
    try {
      join(contextName, "send-msg-to-vtx",
          msgTable, null, vtxTable, dataMap,
          workers);
    } catch (Exception e) {
      LOG.error("Error in all msg to all vtx", e);
    }
    LOG.info("All MSG TO ALL VTX END");
    LOG.info("PRINT MSG TABLE START");
    printMsgTable(msgTable);
    LOG.info("PRINT MSG TABLE END");
    // ----------------------------------------------------
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
   * Print the EdgeTable
   *
   * @param edgeTable the EdgeTable to be printed
   */
  private static void
  printEdgeTable(EdgeTable edgeTable) {
    for (Partition<EdgePartition> partition : edgeTable
        .getPartitions()) {
      ObjectIterator<Int2ObjectMap.Entry<EdgeVal>> iterator =
          partition.get().getKVMap()
              .int2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        EdgeVal edgeVal =
            iterator.next().getValue();
        for (int i = 0; i < edgeVal
            .getNumEdges(); i++) {
          LOG.info(
              "Partiiton ID: " + partition.id()
                  + ", Edge: " + edgeVal.getSrc()[i]
                  + "->" + edgeVal.getDest()[i]);
        }
      }
    }
  }

  /**
   * Print the MessageTable
   *
   * @param msgTable the MessageTable to be printed
   */
  private static void
  printMsgTable(MessageTable msgTable) {
    for (Partition<MessagePartition> partition : msgTable
        .getPartitions()) {
      ObjectIterator<Int2ObjectMap.Entry<IntVal>> iterator =
          partition.get().getKVMap()
              .int2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<IntVal> entry =
            iterator.next();
        int key = entry.getIntKey();
        IntVal msgVal = entry.getValue();
        LOG.info("Partiiton ID: " + partition.id()
            + ", Msg: " + key + "->"
            + msgVal.getInt());
      }
    }
  }

  /**
   * Print the VertexTable
   *
   * @param vtxTable the VertexTable to be printed
   */
  private static void
  printVtxTable(VertexTable vtxTable) {
    for (Partition<VertexPartition> partition : vtxTable
        .getPartitions()) {
      ObjectIterator<Int2ObjectMap.Entry<IntVal>> iterator =
          partition.get().getKVMap()
              .int2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<IntVal> entry =
            iterator.next();
        int key = entry.getIntKey();
        IntVal vtxVal = entry.getValue();
        LOG.info("Partiiton ID: " + partition.id()
            + ", Val: " + key + "->"
            + vtxVal.getInt());
      }
    }
  }

  /**
   * The Join communication operation
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param dynamicTable  the dynamic data Table
   * @param partitioner   the Partitioner
   * @param staticTable   the static data Table
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P1 extends Simple, P2 extends Simple>
  boolean join(String contextName,
               String operationName,
               Table<P1> dynamicTable,
               Partitioner partitioner,
               Table<P2> staticTable, DataMap dataMap,
               Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    final int selfID = workers.getSelfID();
    final int masterID = workers.getMasterID();
    final int numWorkers =
        workers.getNumWorkers();
    // ---------------------------------------------------
    // Gather the partition information of the
    // static table to master
    // LOG.info("Gather static table info.");
    LinkedList<Transferable> recvStaticPSets =
        new LinkedList<>();
    boolean isSuccess = PartitionUtil
        .gatherPartitionSet(contextName,
            operationName + ".gatherstatic",
            staticTable, recvStaticPSets, dataMap,
            workers);
    if (!isSuccess) {
      return false;
    }
    // ---------------------------------------------------
    // Gather the information of dynamic table
    // partitions to master
    // LOG.info("Gather dynamic table info.");
    LinkedList<Transferable> recvDynamicPSets =
        new LinkedList<>();
    isSuccess = PartitionUtil.gatherPartitionSet(
        contextName,
        operationName + ".gatherdynamic",
        dynamicTable, recvDynamicPSets, dataMap,
        workers);
    // LOG.info("Dynamic table info is
    // gathered.");
    if (!isSuccess) {
      return false;
    }
    // ---------------------------------------------------
    // Generate partition and worker mapping for
    // join
    // Bcast partition regroup request
    LinkedList<Transferable> recvJoin =
        new LinkedList<>();
    if (workers.isMaster()) {
      Join join = createJoin(recvStaticPSets,
          recvDynamicPSets, partitioner);
      recvStaticPSets = null;
      recvDynamicPSets = null;
      recvJoin.add(join);
    }
    // --------------------------------------------------
    // LOG.info("Bcast all-to-all information.");
    isSuccess =
        Communication.mstBcastAndRecv(contextName,
            masterID, operationName + ".bcast",
            recvJoin, workers, dataMap);
    dataMap.cleanOperationData(contextName,
        operationName + ".bcast");
    Join join = (Join) recvJoin.removeFirst();
    recvJoin = null;
    // LOG.info("Join info is bcasted.");
    // -------------------------------------------------
    // Send partition
    // Optimize with broadcast
    LinkedList<Partition<P1>> bcastPartitions =
        new LinkedList<>();
    @SuppressWarnings("unchecked")
    LinkedList<Partition<P1>>[] sendPartitionMap =
        new LinkedList[numWorkers];
    int numSendWorkers = 0;
    IntArrayList rmPartitionIDs =
        new IntArrayList();
    int localCount = 0;
    for (Partition<P1> partition : dynamicTable
        .getPartitions()) {
      int partitionID = partition.id();
      IntArrayList workerIDs =
          join.getParToWorkerMap().get(partitionID);
      if (workerIDs != null) {
        boolean isLocal = false;
        if (workerIDs.size() == numWorkers) {
          bcastPartitions.add(partition);
          isLocal = true;
        } else {
          for (int workerID : workerIDs) {
            if (workerID != selfID) {
              if (sendPartitionMap[workerID] == null) {
                sendPartitionMap[workerID] =
                    new LinkedList<>();
                numSendWorkers++;
              }
              sendPartitionMap[workerID]
                  .add(partition);
            } else {
              isLocal = true;
            }
          }
        }
        if (!isLocal) {
          rmPartitionIDs.add(partitionID);
        } else {
          localCount++;
        }
      }
    }
    if (!bcastPartitions.isEmpty()) {
      LocalGlobalSyncCollective.broadcast(
          contextName, operationName,
          bcastPartitions, workers);
    }
    if (numSendWorkers > 0) {
      LocalGlobalSyncCollective.dispatch(
          contextName, operationName,
          sendPartitionMap, workers);
    }
    // --------------------------------------------------
    // Receive all the partitions
    int numRecvPartitions =
        join.getWorkerParCountMap().get(selfID)
            - localCount;
    // LOG.info("Total receive: "
    // + numRecvPartitions);
    join.release();
    join = null;
    return PartitionUtil.receivePartitions(
        contextName, operationName, dynamicTable,
        numRecvPartitions, rmPartitionIDs, dataMap);
  }

  /**
   * Generate partition and worker mapping for
   * join
   *
   * @param recvStaticPSets
   * @param recvDynamicPSets
   * @param partitioner
   * @return a Join object
   */
  private static Join createJoin(
      LinkedList<Transferable> recvStaticPSets,
      LinkedList<Transferable> recvDynamicPSets,
      Partitioner partitioner) {
    // partition <-> workers mapping
    // based on static partition info
    Int2ObjectOpenHashMap<IntArrayList> parToWorkerMap =
        new Int2ObjectOpenHashMap<>();
    for (Transferable trans : recvStaticPSets) {
      PartitionSet recvPSet =
          (PartitionSet) trans;
      int workerID = recvPSet.getWorkerID();
      IntArrayList pList = recvPSet.getParSet();
      // See which partition is on which workers
      for (int pID : pList) {
        IntArrayList destWorkerIDs =
            parToWorkerMap.get(pID);
        if (destWorkerIDs == null) {
          destWorkerIDs = new IntArrayList();
          parToWorkerMap.put(pID, destWorkerIDs);
        }
        destWorkerIDs.add(workerID);
      }
    }
    DataUtil.releaseTransList(recvStaticPSets);
    // worker <-> partition count
    // based on dynamic partition info
    Int2IntOpenHashMap workerParCountMap =
        new Int2IntOpenHashMap();
    for (Transferable trans : recvDynamicPSets) {
      PartitionSet recvPSet =
          (PartitionSet) trans;
      IntArrayList pList = recvPSet.getParSet();
      for (int pID : pList) {
        IntArrayList destWorkerIDs =
            parToWorkerMap.get(pID);
        if (destWorkerIDs != null) {
          for (int destID : destWorkerIDs) {
            workerParCountMap.addTo(destID, 1);
          }
        } else if (partitioner != null) {
          int destWorkerID =
              partitioner.getWorkerID(pID);
          if (destWorkerID != Constant.UNKNOWN_WORKER_ID) {
            destWorkerIDs = new IntArrayList();
            parToWorkerMap.put(pID,
                destWorkerIDs);
            destWorkerIDs.add(destWorkerID);
            workerParCountMap.addTo(destWorkerID,
                1);
          }
        }
      }
    }
    DataUtil.releaseTransList(recvDynamicPSets);
    Join join = Writable.create(Join.class);
    join.setParToWorkerMap(parToWorkerMap);
    join.setWorkerParCountMap(workerParCountMap);
    return join;
  }
}
