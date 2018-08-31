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

public class ReadCOOTask implements
    Task<String, List<COO>> {

  protected static final Log LOG = LogFactory
      .getLog(ReadCOOTask.class);

  private String regex;
  private Configuration conf;
  private long threadId;

  public ReadCOOTask(String regex, Configuration conf) {
    this.regex = regex;
    this.conf = conf;
    this.threadId = 0;
  }

  /**
   * @param fileName the hdfs file name
   * @return
   * @brief Java thread kernel
   */
  @Override
  public List<COO> run(String fileName)
      throws Exception {

    // threadId = Thread.currentThread().getId();
    int count = 0;
    boolean isSuccess = false;
    do {

      try {

        List<COO> res = loadCOO(fileName, conf);
        return res;

      } catch (Exception e) {
        LOG.error("load " + fileName
            + " fails. Count=" + count, e);
        Thread.sleep(100);
        isSuccess = false;
        count++;
      }

    } while (!isSuccess && count < 100);

    LOG.error("Fail to load files.");
    return null;
  }

  private List<COO> loadCOO(String file, Configuration conf)
      throws Exception {

    System.out.println("filename: " + file);
    List<COO> points = new LinkedList<COO>();

    Path pointFilePath = new Path(file);
    FileSystem fs = pointFilePath.getFileSystem(conf);
    FSDataInputStream in = fs.open(pointFilePath);

    String readline = null;
    try {

      while ((readline = in.readLine()) != null) {
        String[] line = readline.split(this.regex);
        long rowId = Long.parseLong(line[0]);
        long colId = Long.parseLong(line[1]);
        double val = Double.parseDouble(line[2]);
        points.add(new COO(rowId, colId, val));
      }

    } finally {
      in.close();
    }

    return points;
  }

}
