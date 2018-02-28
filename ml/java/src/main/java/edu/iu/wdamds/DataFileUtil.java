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

package edu.iu.wdamds;

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.ShortArray;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Map;

public class DataFileUtil {

  /** Class logger */
  protected static final Logger LOG =
    Logger.getLogger(DataFileUtil.class);

  /**
   * Read partition file of this worker. We can
   * get descriptions of row data. Put all RowData
   * to rowDataList, get the number of total
   * partitions.
   * 
   * @param fileName
   * @throws IOException
   */
  public static int readPartitionFile(
    String partitionFile, String idsFile,
    Configuration conf,
    Map<Integer, RowData> rowDataMap)
    throws IOException {
    // Begin reading the input data file
    Path partitionFilePath =
      new Path(partitionFile);
    FileSystem fs =
      partitionFilePath.getFileSystem(conf);
    FSDataInputStream in =
      fs.open(partitionFilePath);
    BufferedReader br1 = new BufferedReader(
      new InputStreamReader(in));
    // Read row IDs
    // Read files
    LinkedList<Integer> rowIDs =
      new LinkedList<>();
    LinkedList<String> filePaths =
      new LinkedList<>();
    try {
      String line;
      boolean inRowSection = false;
      boolean inFileSection = false;
      while ((line = br1.readLine()) != null) {
        if (line.equals(MDSConstants.ROWS_TAG)) {
          inRowSection = true;
          inFileSection = false;
        } else if (line
          .equals(MDSConstants.FILES_TAG)) {
          inRowSection = false;
          inFileSection = true;
        } else {
          if (inRowSection) {
            rowIDs.add(Integer.parseInt(line));
          } else if (inFileSection) {
            filePaths.add(line);
          }
        }
      }
    } finally {
      br1.close();
    }
    // Process row IDs and files paths to generate
    // row data
    RowData rowData = null;
    for (int i = 0, j = 0; i < rowIDs
      .size(); i++, j += 3) {
      rowData = new RowData();
      rowData.distPath = filePaths.get(j);
      rowData.weightPath = filePaths.get(j + 1);
      rowData.vPath = filePaths.get(j + 2);
      rowDataMap.put(rowIDs.get(i), rowData);
    }
    // Read IDs file
    BufferedReader br2 =
      new BufferedReader(new FileReader(idsFile));
    int totalPartitions = 0;
    try {
      String line;
      String[] tokens;
      int rowID;
      while ((line = br2.readLine()) != null) {
        tokens = line.split("\t");
        rowID = Integer.parseInt(tokens[0]);
        if (rowDataMap.containsKey(rowID)) {
          rowData = rowDataMap.get(rowID);
          rowData.height =
            Integer.parseInt(tokens[1]);
          rowData.width =
            Integer.parseInt(tokens[2]);
          rowData.row =
            Integer.parseInt(tokens[3]);
          rowData.rowOffset =
            Integer.parseInt(tokens[4]);
        }
        totalPartitions++;
      }
    } finally {
      br2.close();
    }
    return totalPartitions;
  }

  public static boolean loadRowData(
    RowData rowData,
    Mapper<String, String, Object, Object>.Context context)
    throws IOException {
    loadDistData(rowData);
    context.progress();
    loadWeightData(rowData);
    context.progress();
    loadVData(rowData);
    context.progress();
    return true;
  }

  private static void loadDistData(
    RowData rowData) throws IOException {
    // LOG.info("distPath: " + rowData.distPath);
    DataInputStream din =
      new DataInputStream(new BufferedInputStream(
        new FileInputStream(rowData.distPath)));
    try {
      // Because these data are loaded from files
      // and cached,
      // we allocate the memory automatically
      // without using resource pool.
      // Use short array to save the memory,
      // remember to be divided by
      // Short.MAX_VALUE
      // in any computation
      short[] shorts =
        new short[rowData.height * rowData.width];
      for (int i = 0; i < shorts.length; i++) {
        shorts[i] = din.readShort();
        if (shorts[i] < 0) {
          System.out.println("Negative distance");
        }
      }
      ShortArray distArray =
        new ShortArray(shorts, 0, shorts.length);
      rowData.distArray = distArray;
    } finally {
      din.close();
    }
  }

  private static void loadWeightData(
    RowData rowData) throws IOException {
    // LOG.info("weightPath: " +
    // rowData.weightPath);
    DataInputStream din =
      new DataInputStream(new BufferedInputStream(
        new FileInputStream(rowData.weightPath)));
    try {
      // Because these data are loaded from files
      // and cached,
      // we allocate the memory automatically
      // without using resource pool.
      double[] doubles = new double[rowData.height
        * rowData.width];
      for (int i = 0; i < doubles.length; i++) {
        doubles[i] = (double) din.readShort();
      }
      DoubleArray weightArray = new DoubleArray(
        doubles, 0, doubles.length);
      rowData.weightArray = weightArray;
    } finally {
      din.close();
    }
  }

  private static void loadVData(RowData rowData)
    throws IOException {
    // LOG.info("vPath: " + rowData.vPath);
    DataInputStream din =
      new DataInputStream(new BufferedInputStream(
        new FileInputStream(rowData.vPath)));
    try {
      // Because these data are loaded from files
      // and cached,
      // we allocate the memory automatically
      // without using resource pool.
      int[] ints =
        new int[rowData.height * rowData.width];
      for (int i = 0; i < ints.length; i++) {
        // Convert to integers to save the memory
        ints[i] = (int) din.readDouble();
      }
      IntArray vArray =
        new IntArray(ints, 0, ints.length);
      rowData.vArray = vArray;
    } finally {
      din.close();
    }
  }
}
