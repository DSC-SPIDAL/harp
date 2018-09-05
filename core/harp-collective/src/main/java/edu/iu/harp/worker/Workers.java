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

package edu.iu.harp.worker;

import edu.iu.harp.io.Constant;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*******************************************************
 * The information of the workers. Not modifiable
 * from outside. Workers: 0... Self, Next, ... Max
 * Master/Slave Master: 0 Slave: else
 ******************************************************/
public class Workers extends Nodes {

  /**
   * Map from worker id to worker info
   */
  private final Map<Integer, WorkerInfo> workerInfos;
  /**
   * Map from rack id to worker id
   */
  private final Map<Integer, List<Integer>> rackWorkers;
  /**
   * Worker ID of the current worker
   */
  private final int selfID;
  /**
   * Master ID (communication coordinator)
   */
  private final int masterID;
  /**
   * Master info
   */
  private final WorkerInfo masterInfo;
  /**
   * Max worker ID
   */
  private final int maxID;
  /**
   * Min worker ID
   */
  private final int minID;
  /**
   * Middle worker ID
   */
  private final int middleID;
  /**
   * Worker ID of the next worker
   */
  private final int nextID;
  private final int initCapacity =
      Constant.NUM_THREADS;

  public Workers() throws Exception {
    // Get workers,
    // but self is not a member of workers
    // Then next is pointed to Worker 0 (master)
    this(Constant.UNKNOWN_WORKER_ID);
  }

  public Workers(int selfID) throws Exception {
    this(null, selfID);
  }

  public Workers(int selfID, int masterID, Map<Integer, WorkerInfo> workerInfos,
                 Map<Integer, List<Integer>> rackWorkers, int maxID, int minID,
                 int middleID, int nextID) throws Exception {
    super();
    this.selfID = selfID;
    this.masterID = masterID;
    this.workerInfos = workerInfos;
    this.rackWorkers = rackWorkers;
    this.maxID = maxID;
    this.middleID = middleID;
    this.nextID = nextID;

    this.masterInfo = workerInfos.get(masterID);
    this.minID = minID;
  }

  /**
   * Initialization the workers. Assign IDs to
   * workers; Master is the worker with ID 0;
   *
   * @param reader the BufferedReader
   * @param selfid this worker's id
   * @throws Exception
   */
  public Workers(BufferedReader reader,
                 int selfid) throws Exception {
    super(reader);
    int workerPortBase =
        Constant.DEFAULT_WORKER_POART_BASE;
    workerInfos =
        new ConcurrentHashMap<>(initCapacity);
    rackWorkers =
        new ConcurrentHashMap<>(initCapacity);
    Map<Integer, List<String>> nodes =
        this.getNodes();
    // Load based on the order in node file.
    int workerID = -1;
    for (int rackID : getRackList()) {
      List<Integer> workerIDs =
          new LinkedList<>();
      rackWorkers.put(rackID, workerIDs);
      for (String node : nodes.get(rackID)) {
        // Generate next worker ID
        workerID++;
        // Port: workerPortBase + workerID
        workerInfos.put(workerID,
            new WorkerInfo(workerID, node,
                workerPortBase + workerID, rackID));
        workerIDs.add(workerID);
      }
    }
    selfID = selfid;
    masterID = 0;
    masterInfo = workerInfos.get(masterID);
    minID = 0;
    maxID = workerID;
    middleID = workerID / 2;
    // Set next worker ID
    if (selfID >= 0 && selfID < maxID) {
      nextID = selfID + 1;
    } else {
      nextID = 0;
    }
  }

  /**
   * Get the number of the workers
   *
   * @return
   */
  public int getNumWorkers() {
    return workerInfos.size();
  }

  /**
   * Check if this is the only worker or not
   *
   * @return true if this is the only worker,
   * false otherwise
   */
  public boolean isTheOnlyWorker() {
    return workerInfos.size() <= 1;
  }

  /**
   * Get the ID of the master
   *
   * @return the ID of the master
   */
  public int getMasterID() {
    return this.masterID;
  }

  /**
   * Check if this is the master or not
   *
   * @return true if this is the master, false
   * otherwise
   */
  public boolean isMaster() {
    return selfID == masterID;
  }

  /**
   * Get the WorkerInfo of the master
   *
   * @return the WorkerInfo of the master
   */
  public WorkerInfo getMasterInfo() {
    return masterInfo;
  }

  /**
   * Get the ID of this worker
   *
   * @return the ID of this worker
   */
  public int getSelfID() {
    return selfID;
  }

  /**
   * Check if this worker is in the Workers
   *
   * @return true if this worker is in the
   * workers, false otherwise
   */
  public boolean isSelfInWorker() {
    if (selfID >= 0 && selfID <= maxID) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get the WorkerInfo of this worker
   *
   * @return the WorkerInfo of this worker
   */
  public WorkerInfo getSelfInfo() {
    return workerInfos.get(selfID);
  }

  /**
   * Check if the ID of this worker is the maximum
   * among all the workers
   *
   * @return true if yes, false otherwise
   */
  public boolean isMax() {
    return selfID == maxID;
  }

  /**
   * Get the minimum ID among all the workers
   *
   * @return the minimum ID
   */
  public int getMinID() {
    return minID;
  }

  /**
   * Get the middle ID among all the workers
   *
   * @return the middle ID
   */
  public int getMiddleID() {
    return middleID;
  }

  /**
   * Get the maximum ID among all the workers
   *
   * @return the maximum ID
   */
  public int getMaxID() {
    return maxID;
  }

  /**
   * Get the next ID of this worker
   *
   * @return the next ID
   */
  public int getNextID() {
    return nextID;
  }

  /**
   * Get the WorkerInfo of the next worker
   *
   * @return the WorkerInfo of the next worker
   */
  public WorkerInfo getNextInfo() {
    return workerInfos.get(nextID);
  }

  /**
   * Get the WorkerInfo of the worker
   *
   * @param workerID the worker
   * @return the WorkerInfo of the worker
   */
  public WorkerInfo getWorkerInfo(int workerID) {
    return workerInfos.get(workerID);
  }

  /**
   * Get the iterable class of the WorkerInfos
   *
   * @return the iterable class of the WorkerInfos
   */
  public WorkerInfoList getWorkerInfoList() {
    return new WorkerInfoList();
  }

  /*******************************************************
   * The iterable list of WorkerInfo.
   ******************************************************/
  public class WorkerInfoList
      implements Iterable<WorkerInfo> {

    /**
     * Get the iterator
     */
    @Override
    public Iterator<WorkerInfo> iterator() {
      return new WorkerInfoIterator();
    }
  }

  /*******************************************************
   * The iterator class of WorkerInfo.
   ******************************************************/
  public class WorkerInfoIterator
      implements Iterator<WorkerInfo> {
    protected int workerID = -1;

    /**
     * The overridden hasNext() method
     */
    @Override
    public boolean hasNext() {
      if ((workerID + 1) <= getMaxID()) {
        return true;
      }
      return false;
    }

    /**
     * The overridden next() method
     */
    @Override
    public WorkerInfo next() {
      workerID = workerID + 1;
      return workerInfos.get(workerID);
    }

    /**
     * The overridden remove() method
     */
    @Override
    public void remove() {
    }
  }
}
