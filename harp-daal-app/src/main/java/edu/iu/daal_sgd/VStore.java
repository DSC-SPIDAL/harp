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

package edu.iu.daal_sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

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

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.schdynamic.Task;

class VLoadTask implements Task<String, Object> {
  protected static final Log LOG = LogFactory
    .getLog(VLoadTask.class);

  private final Configuration conf;
  private final boolean useVHMap;
  private final boolean useVWMap;
  private final Int2ObjectOpenHashMap<VRowCol> vHMap;
  private final Int2ObjectOpenHashMap<VRowCol> vWMap;
  private int numPoints;

  public VLoadTask(Configuration conf,
    boolean useVHMap, boolean useVWMap) {
    this.conf = conf;
    this.useVHMap = useVHMap;
    this.useVWMap = useVWMap;
    vHMap = new Int2ObjectOpenHashMap<VRowCol>();
    vWMap = new Int2ObjectOpenHashMap<VRowCol>();
    numPoints = 0;
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
        line =
          line.replaceAll("\\p{Blank}+", " ");
        if (line.charAt(0) == ' ') {
          line = line.substring(1);
        }
        String[] tokens =
          line.split("\\p{Blank}+");
        int rowID = Integer.parseInt(tokens[0]);
        int colID = Integer.parseInt(tokens[1]);
        double vVal =
          Double.parseDouble(tokens[2]);
        if (useVHMap) {
          VStore.add(vHMap, colID, rowID, vVal);
        }
        if (useVWMap) {
          VStore.add(vWMap, rowID, colID, vVal);
        }
        numPoints++;
      }
    } catch (Exception e) {
      LOG.error("Fail to read " + inputFile, e);
    } finally {
      reader.close();
    }
    return null;
  }

  public Int2ObjectOpenHashMap<VRowCol>
    getVHMap() {
    return vHMap;
  }

  public Int2ObjectOpenHashMap<VRowCol>
    getVWMap() {
    return vWMap;
  }

  public int getNumPoints() {
    return numPoints;
  }
}

public class VStore {
  protected static final Log LOG = LogFactory
    .getLog(VStore.class);

  private final List<String> inputs;
  private final Int2ObjectOpenHashMap<VRowCol> vHMap;
  private final Int2ObjectOpenHashMap<VRowCol> vWMap;
  private final int numThreads;
  private final Configuration conf;

  public VStore(List<String> inputs,
    int numThreads, Configuration configuration) {
    this.inputs = inputs;
    this.vHMap =
      new Int2ObjectOpenHashMap<VRowCol>();
    this.vWMap =
      new Int2ObjectOpenHashMap<VRowCol>();
    this.numThreads = numThreads;
    conf = configuration;
  }

  /**
   * Load input based on the number of threads
   * 
   * @return
   */
  public void load(boolean useVHMap,
    boolean useVWMap) {
    long start = System.currentTimeMillis();
    List<VLoadTask> vLoadTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      vLoadTasks.add(new VLoadTask(conf,
        useVHMap, useVWMap));
    }
    DynamicScheduler<String, Object, VLoadTask> vLoadCompute =
      new DynamicScheduler<>(vLoadTasks);
    vLoadCompute.start();
    vLoadCompute.submitAll(inputs);
    vLoadCompute.stop();
    while (vLoadCompute.hasOutput()) {
      vLoadCompute.waitForOutput();
    }
    List<Int2ObjectOpenHashMap<VRowCol>> localVHMaps =
      new LinkedList<>();
    List<Int2ObjectOpenHashMap<VRowCol>> localVWMaps =
      new LinkedList<>();
    int totalNumPoints = 0;
    for (VLoadTask task : vLoadTasks) {
      localVHMaps.add(task.getVHMap());
      localVWMaps.add(task.getVWMap());
      totalNumPoints += task.getNumPoints();
    }
    if (useVHMap) {
      // Merge thread local vHMap
      // Should be done in multi-thread?
      merge(vHMap, localVHMaps);
    }
    if (useVWMap) {
      // Merge thread local vWMap
      merge(vWMap, localVWMaps);
    }
    long end = System.currentTimeMillis();
    // Report the total number of training points
    // loaded
    LOG.info("Load num of points: "
      + totalNumPoints + ", took: "
      + (end - start));
  }

  public static void add(
    Int2ObjectOpenHashMap<VRowCol> map, int id1,
    int id2, double val) {
    VRowCol rowCol = map.get(id1);
    if (rowCol == null) {
      rowCol = new VRowCol();
      rowCol.id = id1;
      rowCol.ids = new int[Constants.ARR_LEN];
      rowCol.v = new double[Constants.ARR_LEN];
      rowCol.numV = 0;
      map.put(id1, rowCol);
    }
    if (rowCol.ids.length == rowCol.numV) {
      int len = rowCol.ids.length << 1;
      int[] ids = new int[len];
      double[] v = new double[len];
      System.arraycopy(rowCol.ids, 0, ids, 0,
        rowCol.numV);
      System.arraycopy(rowCol.v, 0, v, 0,
        rowCol.numV);
      rowCol.ids = ids;
      rowCol.v = v;
    }
    rowCol.ids[rowCol.numV] = id2;
    rowCol.v[rowCol.numV] = val;
    rowCol.numV++;
  }

  public static
    void
    merge(
      Int2ObjectOpenHashMap<VRowCol> map,
      List<Int2ObjectOpenHashMap<VRowCol>> localVMaps) {
    for (Int2ObjectOpenHashMap<VRowCol> localVMap : localVMaps) {
      ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
        localVMap.int2ObjectEntrySet()
          .fastIterator();
      while (iterator.hasNext()) {
        Int2ObjectMap.Entry<VRowCol> entry =
          iterator.next();
        int rowColID = entry.getIntKey();
        VRowCol rowCol = entry.getValue();
        VRowCol vRowCol = map.get(rowColID);
        if (vRowCol == null) {
          vRowCol = new VRowCol();
          vRowCol.id = rowColID;
          vRowCol.ids =
            new int[Constants.ARR_LEN];
          vRowCol.v =
            new double[Constants.ARR_LEN];
          vRowCol.numV = 0;
          map.put(rowColID, vRowCol);
        }
        if (vRowCol.numV + rowCol.numV > vRowCol.ids.length) {
          int len =
            getArrLen(vRowCol.numV + rowCol.numV);
          int[] ids = new int[len];
          double[] v = new double[len];
          if (vRowCol.numV > 0) {
            System.arraycopy(vRowCol.ids, 0, ids,
              0, vRowCol.numV);
            System.arraycopy(vRowCol.v, 0, v, 0,
              vRowCol.numV);
          }
          vRowCol.ids = ids;
          vRowCol.v = v;
        }
        System.arraycopy(rowCol.ids, 0,
          vRowCol.ids, vRowCol.numV, rowCol.numV);
        System.arraycopy(rowCol.v, 0, vRowCol.v,
          vRowCol.numV, rowCol.numV);
        vRowCol.numV += rowCol.numV;
      }
    }
  }

  private static int getArrLen(int numV) {
    return 1 << (32 - Integer
      .numberOfLeadingZeros(numV - 1));
  }

  public Int2ObjectOpenHashMap<VRowCol>
    getVHMap() {
    return vHMap;
  }

  public Int2ObjectOpenHashMap<VRowCol>
    getVWMap() {
    return vWMap;
  }
}
