/*
 * Copyright 2013-2018 Indiana University
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

// import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
// import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
// import it.unimi.dsi.fastutil.ints.IntArrays;
// import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
// import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.lang.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.util.LinkedList;
import java.util.List;

import edu.iu.harp.schdynamic.Task;

public class RegroupCOOTask implements
    Task<List<COO>, Integer> {

  protected static final Log LOG = LogFactory
      .getLog(RegroupCOOTask.class);

  private Configuration conf;
  private long threadId;
  private HashMap<Long, COOGroup> group_map;
  private boolean isRow;

  public RegroupCOOTask(Configuration conf, boolean isRow) {
    this.conf = conf;
    this.threadId = 0;
    this.group_map = new HashMap<>();
    this.isRow = isRow;
  }

  public HashMap<Long, COOGroup> getMap() {
    return this.group_map;
  }

  /**
   * @param fileName the hdfs file name
   * @return
   * @brief Java thread kernel
   */
  @Override
  public Integer run(List<COO> inputData)
      throws Exception {

    int count = 0;
    boolean isSuccess = false;
    do {

      try {
        for (COO elem : inputData) {
          //add elements of input COO data into hashmap
          long gId = 0;
          if (this.isRow)
            gId = elem.getRowId();
          else
            gId = elem.getColId();

          COOGroup group = this.group_map.get(gId);
          if (group == null) {
            //new a COOGroup
            group = new COOGroup(gId);
            group.add(elem, this.isRow);
            this.group_map.put(gId, group);
          } else
            group.add(elem, this.isRow);

        }

        return new Integer(1);

      } catch (Exception e) {
        LOG.error("error in regrouping coo files", e);
        Thread.sleep(100);
        isSuccess = false;
        count++;
      }

    } while (!isSuccess && count < 100);

    LOG.error("Fail to regroup coo files.");
    return new Integer(0);
  }

}
