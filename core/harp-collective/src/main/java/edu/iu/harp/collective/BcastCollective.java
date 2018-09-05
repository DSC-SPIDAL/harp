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
import edu.iu.harp.client.DataMSTBcastSender;
import edu.iu.harp.client.DataSender;
import edu.iu.harp.client.Sender;
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
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.server.Server;
import edu.iu.harp.util.Ack;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Broadcast Collective communication
 ******************************************************/
public class BcastCollective {

  protected static final Logger LOG =
      Logger.getLogger(BcastCollective.class);

  public static void main(String args[])
      throws Exception {
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    int workerID = Integer.parseInt(args[2]);
    long jobID = Long.parseLong(args[3]);
    int numBytes = Integer.parseInt(args[4]);
    int numLoops = Integer.parseInt(args[5]);
    // Initialize log
    Driver.initLogger(workerID);
    LOG.info("args[] " + driverHost + " "
        + driverPort + " " + workerID + " " + jobID
        + " " + numBytes + " " + numLoops);
    // ------------------------------------------
    // Worker initialize
    EventQueue eventQueue = new EventQueue();
    DataMap dataMap = new DataMap();
    Workers workers = new Workers(workerID);
    String host = workers.getSelfInfo().getNode();
    int port = workers.getSelfInfo().getPort();
    Server server = new Server(host, port,
        eventQueue, dataMap, workers);
    server.start();
    String contextName = jobID + "";
    // Barrier guarantees the living workers get
    // the same view of the barrier result
    boolean isSuccess = Communication.barrier(
        contextName, "barrier", dataMap, workers);
    LOG.info("Barrier: " + isSuccess);
    // ----------------------------------------------
    if (isSuccess) {
      try {
        runChainBcast(contextName,
            workers.getSelfID(), numBytes, numLoops,
            dataMap, workers);
      } catch (Exception e) {
        LOG.error("Fail to broadcast.", e);
      }
    }
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
   * Test for broadcasting using chain method
   */
  public static boolean runChainBcast(
      String contextName, int workerID,
      int numBytes, int numLoops, DataMap dataMap,
      Workers workers) {
    boolean isSuccess = runByteArrayChainBcast(
        contextName, workerID, numBytes, numLoops,
        dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    isSuccess = runDoubleArrayTableBcast(
        contextName, workerID, numBytes, numLoops,
        dataMap, workers);
    if (!isSuccess) {
      return false;
    }
    return true;
  }

  static ByteArray
  generateByteArray(int numBytes) {
    long a = System.currentTimeMillis();
    ByteArray byteArray =
        ByteArray.create(numBytes, false);
    byte[] bytes = byteArray.get();
    bytes[0] = (byte) (Math.random() * 255);
    bytes[numBytes - 1] =
        (byte) (Math.random() * 255);
    LOG.info("Generate ByteArray: First byte: "
        + bytes[0] + ", Last byte: "
        + bytes[numBytes - 1]);
    LOG.info("Byte array generation time: "
        + (System.currentTimeMillis() - a));
    return byteArray;
  }

  static DoubleArray
  generateDoubleArray(int numBytes) {
    long start = System.currentTimeMillis();
    int size = (int) (numBytes / (double) 8);
    DoubleArray doubleArray =
        DoubleArray.create(size, false);
    double[] doubles = doubleArray.get();
    doubles[0] = Math.random() * 1000;
    doubles[size - 1] = Math.random() * 1000;
    LOG.info(
        "Generate Double Array: First double: "
            + doubles[0] + ", last double: "
            + doubles[size - 1]);
    long end = System.currentTimeMillis();
    LOG.info("DoubleArray generate time: "
        + (end - start));
    return doubleArray;
  }

  public static boolean runByteArrayChainBcast(
      String contextName, int workerID,
      int numBytes, int numLoops, DataMap dataMap,
      Workers workers) {
    // Byte array bcast
    LinkedList<Transferable> objs =
        new LinkedList<>();
    if (workers.isMaster()) {
      ByteArray byteArray =
          generateByteArray(numBytes);
      objs.add(byteArray);
    }
    boolean isSuccess = false;
    for (int i = 0; i < numLoops; i++) {
      long t1 = System.currentTimeMillis();
      isSuccess = Communication.chainBcastAndRecv(
          contextName, workers.getMasterID(),
          "byte-array-bcast-" + i, objs, workers,
          dataMap);
      // Release recv data
      if (!workers.isMaster()) {
        ByteArray recvByteArr =
            (ByteArray) objs.removeFirst();
        int start = recvByteArr.start();
        int size = recvByteArr.size();
        LOG.info("Receive ByteArray: First byte: "
            + recvByteArr.get()[start]
            + ", Last byte: "
            + recvByteArr.get()[start + size - 1]);
        recvByteArr.release();
      }
      if (!isSuccess) {
        break;
      }
      // Wait for ACK
      waitForACK(contextName,
          "byte-array-ack-" + i,
          workers.getMasterID(), dataMap, workers);
      long t2 = System.currentTimeMillis();
      LOG.info("Loop " + i
          + " byte array bcast time: " + (t2 - t1));
    }
    // Release send data
    if (workers.isMaster()) {
      DataUtil.releaseTransList(objs);
    }
    return isSuccess;
  }

  private static void waitForACK(
      String contextName, String operationName,
      int bcastID, DataMap dataMap,
      Workers workers) {
    // Wait for the ACK object
    if (bcastID == workers.getSelfID()) {
      Data data = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (data != null) {
        data.release();
      }
    } else if (workers.getNextID() == bcastID) {
      Ack ack = Writable.create(Ack.class);
      LinkedList<Transferable> transList =
          new LinkedList<>();
      transList.add(ack);
      Data ackData =
          new Data(DataType.SIMPLE_LIST,
              contextName, workers.getSelfID(),
              transList, DataUtil
              .getNumTransListBytes(transList),
              operationName);
      Sender sender = new DataSender(ackData,
          bcastID, workers, Constant.SEND_DECODE);
      sender.execute();
      ackData.release();
    }
  }

  public static boolean runDoubleArrayTableBcast(
      String contextName, int workerID,
      int numBytes, int numLoops, DataMap dataMap,
      Workers workers) {
    // Double table bcast
    Table<DoubleArray> arrTable =
        new Table<>(0, new DoubleArrPlus());
    if (workers.isMaster()) {
      // Generate 8 partitions
      for (int i = 0; i < 2; i++) {
        DoubleArray doubleArray =
            generateDoubleArray(numBytes / 8);
        arrTable.addPartition(
            new Partition<DoubleArray>(i,
                doubleArray));
      }
    }
    boolean isSuccess = false;
    for (int i = 0; i < numLoops; i++) {
      long t1 = System.currentTimeMillis();
      isSuccess = broadcast(contextName,
          "chain-array-table-bcast-" + i, arrTable,
          workers.getMasterID(), false, dataMap,
          workers);
      long t2 = System.currentTimeMillis();
      LOG.info(
          "Total array table chain bcast time: "
              + (t2 - t1));
      // Release
      if (!workers.isMaster()) {
        for (Partition<DoubleArray> partition : arrTable
            .getPartitions()) {
          DoubleArray doubleArray =
              partition.get();
          int start = doubleArray.start();
          int size = doubleArray.size();
          LOG.info("Receive Double Array: first: "
              + doubleArray.get()[start]
              + ", last: " + doubleArray.get()[start
              + size - 1]);
        }
        arrTable.release();
      }
      if (!isSuccess) {
        break;
      }
    }
    for (int i = 0; i < numLoops; i++) {
      long t1 = System.currentTimeMillis();
      isSuccess = broadcast(contextName,
          "mst-array-table-bcast-" + i, arrTable,
          workers.getMasterID(), true, dataMap,
          workers);
      long t2 = System.currentTimeMillis();
      LOG
          .info("Total array table mst bcast time: "
              + (t2 - t1));
      // Release
      if (!workers.isMaster()) {
        for (Partition<DoubleArray> partition : arrTable
            .getPartitions()) {
          DoubleArray doubleArray =
              partition.get();
          int start = doubleArray.start();
          int size = doubleArray.size();
          LOG.info("Receive Double Array. first: "
              + doubleArray.get()[start]
              + ", last: " + doubleArray.get()[start
              + size - 1]);
        }
        arrTable.release();
      }
      if (!isSuccess) {
        break;
      }
    }
    // Release array table on Master
    if (workers.isMaster()) {
      arrTable.release();
    }
    return isSuccess;
  }

  /**
   * The broadcast communication operation
   *
   * @param contextName   the name of the context
   * @param operationName the name of the operation
   * @param table         the data Table
   * @param bcastWorkerID the worker which broadcasts
   * @param useMSTBcast   use MST method or not
   * @param dataMap       the DataMap
   * @param workers       the Workers
   * @return true if succeeded, false otherwise
   */
  public static <P extends Simple> boolean
  broadcast(String contextName,
            String operationName, Table<P> table,
            int bcastWorkerID, boolean useMSTBcast,
            DataMap dataMap, Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() == bcastWorkerID) {
      int numOwnedPartitions =
          table.getNumPartitions();
      LinkedList<Transferable> ownedPartitions =
          new LinkedList<>(table.getPartitions());
      Data sendData = new Data(
          DataType.PARTITION_LIST, contextName,
          bcastWorkerID, ownedPartitions,
          DataUtil
              .getNumTransListBytes(ownedPartitions),
          operationName, numOwnedPartitions);
      Sender sender = null;
      if (useMSTBcast) {
        sender = new DataMSTBcastSender(sendData,
            workers, Constant.MST_BCAST_DECODE);
      } else {
        sender =
            new DataChainBcastSender(sendData,
                workers, Constant.CHAIN_BCAST_DECODE);
      }
      boolean isSuccess = sender.execute();
      sendData.releaseHeadArray();
      sendData.releaseBodyArray();
      sendData = null;
      ownedPartitions = null;
      return isSuccess;
    } else {
      // Wait for data
      Data recvData = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
      if (recvData != null) {
        recvData.releaseHeadArray();
        recvData.releaseBodyArray();
        PartitionUtil.addPartitionsToTable(
            recvData.getBody(), table);
        return true;
      } else {
        return false;
      }
    }
  }
}
