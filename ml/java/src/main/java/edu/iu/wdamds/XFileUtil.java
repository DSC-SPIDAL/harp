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

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

class XFileUtil {

  static void storeXOnMaster(
    Configuration configuration,
    Table<DoubleArray> table, int d,
    String xOutFile, boolean isMaster)
    throws IOException {
    if (isMaster) {
      Path xOutPath = new Path(xOutFile);
      FileSystem fs =
        FileSystem.get(configuration);
      // fs.delete(xOutPath, true);
      FSDataOutputStream out =
        fs.create(xOutPath);
      PrintWriter writer =
        new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(out)));
      DecimalFormat format =
        new DecimalFormat("#.##########");
      Partition<DoubleArray> partition = null;
      DoubleArray parArray = null;
      double[] doubles = null;
      for (int id = 0; id < table
        .getNumPartitions(); id++) {
        partition = table.getPartition(id);
        parArray = partition.get();
        doubles = parArray.get();
        for (int i = 0; i < parArray
          .size(); i++) {
          if ((i % d) == 0) {
            writer.print(id + "\t");// print ID.
          }
          writer.print(
            format.format(doubles[i]) + "\t");
          if ((i % d) == (d - 1)) {
            writer.println("1");
            id++;
          }
        }
      }
      writer.flush();
      writer.close();
    }
  }

  static void storeXOnMaster(
    Configuration configuration,
    Table<DoubleArray> table, int d,
    String xOutFile, boolean isMaster,
    String labelFile) throws IOException {
    if (isMaster) {
      // Read label file
      BufferedReader reader = new BufferedReader(
        new FileReader(labelFile));
      String line = null;
      String parts[] = null;
      Map<Integer, Integer> labels =
        new HashMap<Integer, Integer>();
      while ((line = reader.readLine()) != null) {
        parts = line.split(" ");
        if (parts.length < 2) {
          // Don't need to throw an error because
          // this is the last part of
          // the computation
        }
        labels.put(Integer.parseInt(parts[1]),
          Integer.parseInt(parts[1]));
      }
      reader.close();
      Path xOutPath = new Path(xOutFile);
      FileSystem fs =
        FileSystem.get(configuration);
      // fs.delete(xOutPath, true);
      FSDataOutputStream out =
        fs.create(xOutPath);
      PrintWriter writer =
        new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(out)));
      DecimalFormat format =
        new DecimalFormat("#.##########");
      Partition<DoubleArray> partition = null;
      DoubleArray parArray = null;
      double[] doubles = null;
      for (int id = 0; id < table
        .getNumPartitions(); id++) {
        partition = table.getPartition(id);
        parArray = partition.get();
        doubles = parArray.get();
        for (int i = 0; i < parArray
          .size(); i++) {
          if ((i % d) == 0) {
            writer.print(id + "\t");// print ID.
          }
          writer.print(
            format.format(doubles[i]) + "\t");
          writer.print(String.valueOf(i) + "\t");
          if ((i % d) == (d - 1)) {
            writer.println(labels.get(id));
            // it seems that we need to put id++
            // after a line is printed
            id++;
          }
        }
      }
      writer.flush();
      writer.close();
    }
  }
}
