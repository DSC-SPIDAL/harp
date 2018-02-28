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
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.Data;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.DataType;
import edu.iu.harp.io.DataUtil;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.resource.Transferable;
import edu.iu.harp.resource.Writable;
import edu.iu.harp.util.Barrier;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/*******************************************************
 * These methods are used for communicating
 * non-partition transferable.
 ******************************************************/
public class Communication {

  protected static final Logger LOG =
    Logger.getLogger(Communication.class);

  /**
   * In barrier, each worker send a message to
   * master. If the master gets all the messages,
   * sends true to all workers to leave the
   * barrier. Else it sends false to all the
   * workers.
   * 
   * @param contextName
   *          the name of operation context
   * @param operationName
   *          the name of the operation
   * @param dataMap
   *          the DataMap
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static boolean barrier(
    String contextName, String operationName,
    DataMap dataMap, Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    // Send barrier and wait for replies
    if (workers.isMaster()) {
      boolean isBarrierSuccess = true;
      int numWorkers = workers.getNumWorkers();
      // Collect replies from other workers
      int count = 1;
      while (count < numWorkers) {
        Data recvData = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
        if (recvData != null) {
          LOG.info("Barrier from Worker "
            + recvData.getWorkerID());
          recvData.release();
          count++;
        } else {
          LOG.info("Slaves may fail.");
          isBarrierSuccess = false;
          break;
        }
      }
      // Create the barrier data
      Barrier barrier =
        Writable.create(Barrier.class);
      barrier.setStatus(isBarrierSuccess);
      LinkedList<Transferable> commList =
        new LinkedList<>();
      commList.add(barrier);
      // Send the barrier info to every worker
      Data sendData = new Data(
        DataType.SIMPLE_LIST, contextName,
        workers.getSelfID(), commList,
        DataUtil.getNumTransListBytes(commList),
        operationName);
      for (WorkerInfo worker : workers
        .getWorkerInfoList()) {
        if (worker.getID() != workers
          .getMasterID()) {
          Sender sender = new DataSender(sendData,
            worker.getID(), workers,
            Constant.SEND_DECODE);
          int retryCount = 0;
          do {
            boolean isSuccess = sender.execute();
            if (!isSuccess) {
              retryCount++;
              try {
                Thread
                  .sleep(Constant.SHORT_SLEEP);
              } catch (InterruptedException e) {
              }
            } else {
              break;
            }
          } while (retryCount < Constant.SMALL_RETRY_COUNT);
        }
      }
      // Release all the resource used
      sendData.release();
      sendData = null;
      return isBarrierSuccess;
    } else {
      // From slave workers
      Barrier barrier =
        Writable.create(Barrier.class);
      barrier.setStatus(true);
      LinkedList<Transferable> commList =
        new LinkedList<>();
      commList.add(barrier);
      // Create the barrier data
      Data sendData = new Data(
        DataType.SIMPLE_LIST, contextName,
        workers.getSelfID(), commList,
        DataUtil.getNumTransListBytes(commList),
        operationName);
      Sender sender = new DataSender(sendData,
        workers.getMasterID(), workers,
        Constant.SEND_DECODE);
      // Send to master
      boolean isSuccess = false;
      int retryCount = 0;
      do {
        LOG.info("Start sending barrier.");
        isSuccess = sender.execute();
        LOG.info("Send barrier: " + isSuccess);
        if (!isSuccess) {
          retryCount++;
          try {
            Thread.sleep(Constant.SHORT_SLEEP);
          } catch (InterruptedException e) {
          }
        }
      } while (!isSuccess
        && retryCount < Constant.LARGE_RETRY_COUNT);
      // Release all the resource used
      sendData.release();
      sendData = null;
      barrier = null;
      if (!isSuccess) {
        return false;
      }
      // Wait for the reply from master
      boolean isBarrierSuccess = false;
      Data recvData = IOUtil.waitAndGet(dataMap,
        contextName, operationName);
      if (recvData != null) {
        LOG.info("Barrier is received.");
        barrier =
          (Barrier) recvData.getBody().get(0);
        if (barrier.getStatus()) {
          isBarrierSuccess = true;
        }
        recvData.release();
        recvData = null;
      }
      return isBarrierSuccess;
    }
  }

  /**
   * Gather collective communication operation
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param gatherWorkerID
   *          the WorkerID to send
   * @param dataMap
   *          the DataMap
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static boolean gather(String contextName,
    String operationName, List<Transferable> objs,
    int gatherWorkerID, DataMap dataMap,
    Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() == gatherWorkerID) {
      int numWorkers = workers.getNumWorkers();
      int count = 1;
      while (count < numWorkers) {
        Data data = IOUtil.waitAndGet(dataMap,
          contextName, operationName);
        if (data != null) {
          data.releaseHeadArray();
          data.releaseBodyArray();
          objs.addAll(data.getBody());
          count++;
        } else {
          DataUtil.releaseTransList(objs);
          return false;
        }
      }
      return true;
    } else {
      Data data = new Data(DataType.SIMPLE_LIST,
        contextName, workers.getSelfID(), objs,
        DataUtil.getNumTransListBytes(objs),
        operationName);
      Sender sender =
        new DataSender(data, gatherWorkerID,
          workers, Constant.SEND_DECODE);
      boolean success = sender.execute();
      data.release();
      return success;
    }
  }

  /**
   * Allgather collective communication operation
   * 
   * @param contextName
   *          the name of the context
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param dataMap
   *          the DataMap
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static boolean allgather(
    final String contextName,
    final String operationName,
    List<Transferable> objs, DataMap dataMap,
    Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    final int nextID = workers.getNextID();
    final int selfID = workers.getSelfID();
    final int numWorkers =
      workers.getNumWorkers();
    Data sendData = new Data(DataType.SIMPLE_LIST,
      contextName, selfID, objs,
      DataUtil.getNumTransListBytes(objs),
      operationName);
    Sender sender = new DataSender(sendData,
      nextID, workers, Constant.SEND_DECODE);
    boolean isSuccess = sender.execute();
    if (!isSuccess) {
      sendData.release();
      return false;
    }
    sendData = null;
    for (int i = 1; i < numWorkers; i++) {
      Data recvData = IOUtil.waitAndGet(dataMap,
        contextName, operationName);
      if (recvData != null) {
        if (recvData.getWorkerID() != nextID) {
          sender =
            new DataSender(recvData, nextID,
              workers, Constant.SEND_DECODE);
          sender.execute();
        }
        recvData.releaseHeadArray();
        recvData.releaseBodyArray();
        objs.addAll(recvData.getBody());
      } else {
        DataUtil.releaseTransList(objs);
        return false;
      }
    }
    return true;
  }

  /**
   * The broadcast communication operation using
   * chain method. If the self is not the
   * bcastWorkerID, quit.
   * 
   * @param contextName
   *          the name of the context
   * @param bcastWorkerID
   *          the worker doing broadcast
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static boolean chainBcast(
    String contextName, int bcastWorkerID,
    String operationName, List<Transferable> objs,
    Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() != bcastWorkerID) {
      return false;
    }
    Data data = new Data(DataType.SIMPLE_LIST,
      contextName, bcastWorkerID, objs,
      DataUtil.getNumTransListBytes(objs),
      operationName);
    Sender sender = new DataChainBcastSender(data,
      workers, Constant.CHAIN_BCAST_DECODE);
    boolean isSuccess = sender.execute();
    data.releaseHeadArray();
    data.releaseBodyArray();
    data = null;
    if (!isSuccess) {
      DataUtil.releaseTransList(objs);
      return false;
    } else {
      return true;
    }
  }

  /**
   * The broadcast communication operation using
   * chain method. If the self is not the
   * bcastWorkerID, wait for the data.
   * 
   * @param contextName
   *          the name of the context
   * @param bcastWorkerID
   *          the worker doing broadcast
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param workers
   *          the Workers
   * @param dataMap
   *          the DataMap
   * @return true if succeeded, false otherwise
   */
  public static boolean chainBcastAndRecv(
    String contextName, int bcastWorkerID,
    String operationName, List<Transferable> objs,
    Workers workers, DataMap dataMap) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() == bcastWorkerID) {
      return chainBcast(contextName,
        bcastWorkerID, operationName, objs,
        workers);
    } else {
      Data data = IOUtil.waitAndGet(dataMap,
        contextName, operationName);
      if (data != null) {
        data.releaseHeadArray();
        data.releaseBodyArray();
        objs.addAll(data.getBody());
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * The broadcast communication operation using
   * MST method. If the self is not the
   * bcastWorkerID, quit.
   * 
   * @param contextName
   *          the name of the context
   * @param bcastWorkerID
   *          the worker doing broadcast
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param workers
   *          the Workers
   * @return true if succeeded, false otherwise
   */
  public static boolean mstBcast(
    String contextName, int bcastWorkerID,
    String operationName, List<Transferable> objs,
    Workers workers) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() != bcastWorkerID) {
      return false;
    }
    Data data = new Data(DataType.SIMPLE_LIST,
      contextName, bcastWorkerID, objs,
      DataUtil.getNumTransListBytes(objs),
      operationName);
    Sender sender = new DataMSTBcastSender(data,
      workers, Constant.MST_BCAST_DECODE);
    boolean isSuccess = sender.execute();
    data.releaseHeadArray();
    data.releaseBodyArray();
    if (!isSuccess) {
      DataUtil.releaseTransList(objs);
      return false;
    } else {
      return true;
    }
  }

  /**
   * The broadcast communication operation using
   * MST method. If the self is not the
   * bcastWorkerID, wait for the data.
   * 
   * @param contextName
   *          the name of the context
   * @param bcastWorkerID
   *          the worker doing broadcast
   * @param operationName
   *          the name of the operation
   * @param objs
   *          the list of Transferable objects
   * @param workers
   *          the Workers
   * @param dataMap
   *          the DataMap
   * @return true if succeeded, false otherwise
   */
  public static boolean mstBcastAndRecv(
    String contextName, int bcastWorkerID,
    String operationName, List<Transferable> objs,
    Workers workers, DataMap dataMap) {
    if (workers.isTheOnlyWorker()) {
      return true;
    }
    if (workers.getSelfID() == bcastWorkerID) {
      return mstBcast(contextName, bcastWorkerID,
        operationName, objs, workers);
    } else {
      // Wait for data
      Data data = IOUtil.waitAndGet(dataMap,
        contextName, operationName);
      if (data != null) {
        data.releaseHeadArray();
        data.releaseBodyArray();
        objs.addAll(data.getBody());
        return true;
      } else {
        return false;
      }
    }
  }
}
