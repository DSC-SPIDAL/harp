/*
 * Copyright 2013-2016 Indiana University
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

package edu.iu.datasource;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class MTReader {

  protected static final Log LOG = LogFactory
      .getLog(MTReader.class);

  private int totalLine;
  private int totalPoints;
  private String sep = ",";

  public MTReader() {
    this.totalLine = 0;
    this.totalPoints = 0;
  }

  public MTReader(String sep) {
    this.totalLine = 0;
    this.totalPoints = 0;
    this.sep = sep;
  }

  /**
   * @param fileNames: filenames in HDFS
   * @param dim
   * @param conf
   * @param numThreads
   * @return
   * @brief read dense matrix files from HDFS in parallel (multi-threading)
   * file format is dense vector (matrix)
   */
  public List<double[]>[] readfiles(
      List<String> fileNames,
      int dim, Configuration conf,
      int numThreads) {//{{{

    List<ReadDenseCSVTask> tasks = new LinkedList<>();
    List<double[]>[] arrays = new List[fileNames.size()];

    for (int i = 0; i < numThreads; i++) {
      tasks.add(new ReadDenseCSVTask(dim, this.sep, conf));
    }

    DynamicScheduler<String, List<double[]>, ReadDenseCSVTask> compute =
        new DynamicScheduler<>(tasks);

    for (String fileName : fileNames) {
      compute.submit(fileName);
    }

    compute.start();
    compute.stop();

    int addidex = 0;
    this.totalLine = 0;
    this.totalPoints = 0;
    while (compute.hasOutput()) {

      List<double[]> output = compute.waitForOutput();
      if (output != null) {
        arrays[addidex++] = output;
        totalLine += output.size();
        totalPoints += output.size() * dim;
      }
    }

    return arrays;
  } //}}}

  public List<double[]> readDenseCSV(
      List<String> fileNames,
      int dim, String seper, Configuration conf,
      int numThreads) {//{{{

    List<ReadDenseCSVTask> tasks = new LinkedList<>();
    List<double[]> arrays = new LinkedList<>();

    for (int i = 0; i < numThreads; i++) {
      tasks.add(new ReadDenseCSVTask(dim, seper, conf));
    }

    DynamicScheduler<String, List<double[]>, ReadDenseCSVTask> compute =
        new DynamicScheduler<>(tasks);

    for (String fileName : fileNames) {
      compute.submit(fileName);
    }

    compute.start();
    compute.stop();

    this.totalLine = 0;
    this.totalPoints = 0;

    while (compute.hasOutput()) {

      List<double[]> output = compute.waitForOutput();
      if (output != null) {
        // totalLine += output.size();
        // totalPoints += output.size()*dim;
        arrays.addAll(output);
      }
    }

    this.totalLine = arrays.size();
    this.totalPoints = arrays.size() * dim;

    return arrays;
  } //}}}

  public List<double[][]> readDenseCSVSharding(
      List<String> fileNames,
      int dim, int shardsize, String seper, Configuration conf,
      int numThreads) {//{{{

    List<ReadDenseCSVShardingTask> tasks = new LinkedList<>();
    List<double[][]> arrays = new LinkedList<>();

    for (int i = 0; i < numThreads; i++) {
      tasks.add(new ReadDenseCSVShardingTask(dim, shardsize, seper, conf));
    }

    DynamicScheduler<String, List<double[][]>, ReadDenseCSVShardingTask> compute =
        new DynamicScheduler<>(tasks);

    for (String fileName : fileNames) {
      compute.submit(fileName);
    }

    compute.start();
    compute.stop();

    this.totalLine = 0;
    this.totalPoints = 0;
    while (compute.hasOutput()) {

      List<double[][]> output = compute.waitForOutput();
      if (output != null) {

        for (double[][] elem : output) {
          totalLine += elem.length;
          totalPoints += elem.length * dim;
        }

        arrays.addAll(output);
      }
    }

    return arrays;

  } //}}}

  public List<COO> readCOO(List<String> fileNames, String regex, Configuration conf, int numThreads) {//{{{

    List<ReadCOOTask> tasks = new LinkedList<>();
    List<COO> outputRes = new LinkedList<>();

    for (int i = 0; i < numThreads; i++) {
      tasks.add(new ReadCOOTask(regex, conf));
    }

    DynamicScheduler<String, List<COO>, ReadCOOTask> compute =
        new DynamicScheduler<>(tasks);

    for (String fileName : fileNames) {
      compute.submit(fileName);
    }

    compute.start();
    compute.stop();

    this.totalLine = 0;
    this.totalPoints = 0;
    while (compute.hasOutput()) {

      List<COO> output = compute.waitForOutput();
      if (output != null) {
        outputRes.addAll(output);
        totalLine += output.size();
        totalPoints += output.size() * 3;
      }
    }

    return outputRes;

  }//}}}

  /**
   * @param inputData
   * @param isRow
   * @param conf
   * @param numThreads
   * @return
   * @brief group COO entries by their row Ids or column ids
   */
  public HashMap<Long, COOGroup> regroupCOO(List<COO> inputData, boolean isRow, Configuration conf, int numThreads) {//{{{
    List<RegroupCOOTask> tasks = new LinkedList<>();
    for (int i = 0; i < numThreads; i++)
      tasks.add(new RegroupCOOTask(conf, isRow));

    DynamicScheduler<List<COO>, Integer, RegroupCOOTask> compute =
        new DynamicScheduler<>(tasks);

    //split inputData into sublists and submit the tasks
    int start_idx = 0;
    int end_idx = 0;
    int total_elem = inputData.size();
    int idx_interval = (total_elem + numThreads - 1) / numThreads;
    while (start_idx < total_elem) {
      end_idx = (start_idx + idx_interval <= total_elem) ? (start_idx + idx_interval) : total_elem;
      compute.submit(inputData.subList(start_idx, end_idx));
      start_idx = end_idx;
    }

    // launch the tasks
    compute.start();
    compute.stop();

    while (compute.hasOutput())
      compute.waitForOutput();

    //retrieval the group_maps from each thread tasks and merge them
    HashMap<Long, COOGroup> base_map = tasks.get(0).getMap();
    for (int j = 1; j < numThreads; j++) {
      HashMap<Long, COOGroup> add_map = tasks.get(j).getMap();
      for (Map.Entry<Long, COOGroup> entry : add_map.entrySet()) {
        Long key = entry.getKey();
        COOGroup val = entry.getValue();

        COOGroup base_entry = base_map.get(key);
        if (base_entry == null)
          base_map.put(key, val);
        else
          base_entry.add(val);
      }
    }

    return base_map;

  }//}}}

  public int getTotalLines() {
    return this.totalLine;
  }

  public int getTotalPoints() {
    return this.totalPoints;
  }

}
