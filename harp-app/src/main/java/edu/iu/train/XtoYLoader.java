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

package edu.iu.train;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.schdynamic.Task;

class VLoadTask<V extends XtoY> implements
  Task<String, Object> {
  protected static final Log LOG = LogFactory
    .getLog(VLoadTask.class);

  private final Configuration conf;
  private final Table<V> vTable;
  private int numLines;
  private final XtoYLoader<V> loader;

  public VLoadTask(Configuration conf,
    Table<V> vTable, XtoYLoader<V> loader) {
    this.conf = conf;
    this.vTable = vTable;
    numLines = 0;
    this.loader = loader;
  }

  @Override
  public Object run(String inputFile)
    throws Exception {
    Path inputFilePath = new Path(inputFile);
    // Open the file
    boolean isFailed = false;
    FSDataInputStream in = null;
    BufferedReader reader = null;
    do {
      isFailed = false;
      try {
        FileSystem fs =
          inputFilePath.getFileSystem(conf);
        in = fs.open(inputFilePath);
        reader =
          new BufferedReader(
            new InputStreamReader(in), 1048576);
      } catch (Exception e) {
        LOG.error("Fail to open " + inputFile, e);
        isFailed = true;
        if (reader != null) {
          try {
            reader.close();
          } catch (Exception e1) {
          }
        }
        if (in != null) {
          try {
            in.close();
          } catch (Exception e1) {
          }
        }
      }
    } while (isFailed);
    // Read the file
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        loader.add(vTable, line);
        numLines++;
      }
    } catch (Exception e) {
      LOG.error("Fail to read " + inputFile, e);
    } finally {
      reader.close();
    }
    return null;
  }

  public int getNumLines() {
    return numLines;
  }
}

public abstract class XtoYLoader<V extends XtoY> {
  protected static final Log LOG = LogFactory
    .getLog(XtoYLoader.class);

  private final List<String> inputs;
  private final Table<V> vTable;
  private final int numThreads;
  private final Configuration conf;

  public XtoYLoader(List<String> inputs,
    int numThreads, Configuration configuration,
    Table<V> vTable) {
    this.inputs = inputs;
    this.numThreads = numThreads;
    conf = configuration;
    this.vTable = vTable;
  }

  /**
   * Load input based on the number of threads
   * 
   * @return
   */
  public void load() {
    long start = System.currentTimeMillis();
    List<VLoadTask<V>> vLoadTasks =
      new LinkedList<>();
    List<Table<V>> tables = new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      Table<V> table =
        new Table<>(i, vTable.getCombiner());
      tables.add(table);
      vLoadTasks.add(new VLoadTask<>(conf, table,
        this));
    }
    DynamicScheduler<String, Object, VLoadTask<V>> vLoadCompute =
      new DynamicScheduler<>(vLoadTasks);
    vLoadCompute.start();
    vLoadCompute.submitAll(inputs);
    vLoadCompute.stop();
    while (vLoadCompute.hasOutput()) {
      vLoadCompute.waitForOutput();
    }
    for (Table<V> table : tables) {
      for (Partition<V> partition : table
        .getPartitions()) {
        table.addPartition(partition);
      }
    }
    long totalNumLines = 0L;
    for (VLoadTask<V> task : vLoadTasks) {
      totalNumLines += (long) task.getNumLines();
    }
    long end = System.currentTimeMillis();
    // Report the total number of training points
    // loaded
    LOG.info("Load num train data lines: "
      + totalNumLines + ", took: "
      + (end - start));
  }

  public abstract void add(Table<V> table,
    String line);
}
