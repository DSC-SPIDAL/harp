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

import edu.iu.harp.example.IntCount;
import edu.iu.harp.example.StringKey;
import edu.iu.harp.example.WordAvgFunction;
import edu.iu.harp.example.WordCountPartition;
import edu.iu.harp.example.WordCountTable;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.server.Server;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/*******************************************************
 * Group By Key Collective communication
 ******************************************************/
public class GroupByKeyCollective {

  protected static final Logger LOG =
    Logger.getLogger(GroupByKeyCollective.class);

  public static void main(String args[])
    throws Exception {
    String driverHost = args[0];
    int driverPort = Integer.parseInt(args[1]);
    int workerID = Integer.parseInt(args[2]);
    long jobID = Long.parseLong(args[3]);
    // Initialize log
    Driver.initLogger(workerID);
    LOG.info(
      "args[] " + driverHost + " " + driverPort
        + " " + workerID + " " + jobID);
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
    LOG.info("Start Barrier");
    boolean isSuccess = Communication.barrier(
      contextName, "barrier", dataMap, workers);
    LOG.info("Barrier: " + isSuccess);
    // -----------------------------------------------
    // Generate words into wordcount table
    WordCountTable table = new WordCountTable(0);
    // Don't create StringKey through the
    // resource pool
    table.addKeyVal(new StringKey("Apple"),
      new IntCount(1, 1));
    table.addKeyVal(new StringKey("Banana"),
      new IntCount(2, 1));
    table.addKeyVal(new StringKey("Cherry"),
      new IntCount(3, 1));
    table.addKeyVal(new StringKey("Durian"),
      new IntCount(4, 1));
    table.addKeyVal(new StringKey("Eggplant"),
      new IntCount(5, 1));
    table.addKeyVal(new StringKey("Fig"),
      new IntCount(6, 1));
    table.addKeyVal(new StringKey("Grape"),
      new IntCount(7, 1));
    LOG.info("regroupAggregate before: ");
    for (Partition<WordCountPartition> partition : table
      .getPartitions()) {
      ObjectIterator<Entry<StringKey, IntCount>> iterator =
        partition.get().getKVMap()
          .object2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        Entry<StringKey, IntCount> entry =
          iterator.next();
        LOG.info(partition.id() + " "
          + entry.getKey().getStringKey() + " "
          + entry.getValue().getInt() + " "
          + entry.getValue().getCount());
      }
    }
    // --------------------------------------------------------
    try {
      RegroupCollective.regroupAggregate(
        contextName, "group-by-key-aggregate",
        table,
        new Partitioner(workers.getNumWorkers()),
        new WordAvgFunction(), dataMap, workers);
    } catch (Exception e) {
      LOG.error("Fail to regorupAggregate.", e);
    }
    // Print new table
    LOG.info("regroupAggregate after: ");
    for (Partition<WordCountPartition> partition : table
      .getPartitions()) {
      ObjectIterator<Entry<StringKey, IntCount>> iterator =
        partition.get().getKVMap()
          .object2ObjectEntrySet().fastIterator();
      while (iterator.hasNext()) {
        Entry<StringKey, IntCount> entry =
          iterator.next();
        LOG.info(partition.id() + " "
          + entry.getKey().getStringKey() + " "
          + entry.getValue().getInt() + " "
          + entry.getValue().getCount());
      }
    }
    // -------------------------------------------------------
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
}
