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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class MDSDataSplit {
  public static void main(String[] args) {
    // java edu.iu.wdamds.MDSDataSplit
    // mds_data/4640_pid.bin mds_data_split/data/
    // distance_ mds_data_split/ids/distance_ids
    // 32 4640 4640 0
    //
    // java edu.iu.wdamds.MDSDataSplit
    // mds_data/4640_weight_all1.bin
    // mds_data_split/data/ weight_
    // mds_data_split/ids/weight_ids 32 4640 4640
    // 0
    //
    // java edu.iu.wdamds.MDSDataSplit
    // mds_data/4640_v_all1.bin
    // mds_data_split/data/ v_
    // mds_data_split/ids/v_ids 32 4640 4640 1
    if (args.length != 8) {
      System.out.println("Usage: ");
      System.out.println("[1. Data File]");
      System.out.println(
        "[2. Temporary directory to split data]");
      System.out.println("[3. Temp file prefix]");
      System.out.println("[4. Output IDs file]");
      System.out
        .println("[5. Num of partitions]");
      System.out.println("[6. row size]");
      System.out.println("[7. width size]");
      System.out.println(
        "[8. Type of input value format (0: short; 1: double)]");
      System.exit(0);
    }
    double beginTime = System.currentTimeMillis();
    String dataFile = args[0];
    String tmpDir = args[1];
    String tmpFilePrefix = args[2];
    String idsFile = args[3];
    int numPartitions = Integer.parseInt(args[4]);
    int row = Integer.parseInt(args[5]);
    int width = Integer.parseInt(args[6]);
    int choice = Integer.parseInt(args[7]);
    // Create a temporary directory to hold data
    // splits.
    if (!(new File(tmpDir)).exists()) {
      if (!(new File(tmpDir)).mkdir()) {
        System.err.println(
          "Failed to create the temporary directory to split data");
        System.exit(-1);
      }
    }
    try {
      if (choice == 0)
        splitDataforShort(dataFile, tmpDir,
          tmpFilePrefix, idsFile, numPartitions,
          row, width);
      else if (choice == 1)
        splitDataforDouble(dataFile, tmpDir,
          tmpFilePrefix, idsFile, numPartitions,
          row, width);
      else {
        System.err
          .println("The choice must be 1 or 0");
        System.exit(2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    double endTime = System.currentTimeMillis();
    System.out.println(
      "==========================================================");
    System.out.println("Time to split data = "
      + (endTime - beginTime) / 1000
      + " Seconds.");
    System.out.println(
      "==========================================================");
    System.exit(0);
  }

  private static void splitDataforDouble(
    String dataFile, String tmpDir,
    String tmpFilePrefix, String idsFile,
    int numPartitions, int row, int width)
    throws IOException {
    BufferedInputStream reader =
      new BufferedInputStream(
        new FileInputStream(dataFile));
    DataInputStream din =
      new DataInputStream(reader);
    BufferedWriter idsWriter =
      new BufferedWriter(new FileWriter(idsFile));
    String outputFile = null;
    int blockHeight = row / numPartitions;
    int rest = row % numPartitions;
    double[][] rows;
    int startRow = 0;
    int endRow = 0;
    int curHeight = 0;
    int count = 0;
    for (int i = 0; i < numPartitions; i++) {
      System.out
        .println("The " + i + "th partition");
      idsWriter.write(i + "\t");
      outputFile =
        tmpDir + "/" + tmpFilePrefix + i;
      endRow += blockHeight;
      if (rest > 0) {
        endRow++;
        rest--;
      }
      curHeight = endRow - startRow;
      rows = new double[curHeight][width];
      count = 0;
      for (int j = startRow; j < endRow; j++) {
        for (int k = 0; k < width; k++) { // Note
                                          // the
                                          // start
          rows[count][k] = din.readDouble();
        }
        count++;
      }
      // Height, width, row, rowOffset
      // Row ID starts from 0 to numPartitions - 1
      idsWriter.write(curHeight + "\t" + width
        + "\t" + i + "\t" + startRow + "\n");
      writeToBinFile(rows, curHeight, width,
        outputFile);
      startRow = endRow;
    }
    din.close();
    idsWriter.flush();
    idsWriter.close();
  }

  public static void writeToBinFile(
    double[][] row, int curHeight, int size,
    String fileName) throws IOException {
    DataOutputStream dout = new DataOutputStream(
      new BufferedOutputStream(
        new FileOutputStream(fileName)));
    for (int i = 0; i < curHeight; i++) {
      for (int j = 0; j < size; j++) {
        dout.writeDouble(row[i][j]);
      }
    }
    dout.flush();
    dout.close();
  }

  /**
   * data size should be provided for reading from
   * bin file
   * 
   * @param dataFile
   * @param tmpDir
   * @param tmpFilePrefix
   * @param idsFile
   * @param numPartitions
   * @param size
   * @throws IOException
   */

  private static void splitDataforShort(
    String dataFile, String tmpDir,
    String tmpFilePrefix, String idsFile,
    int numPartitions, int row, int width)
    throws IOException {
    BufferedInputStream reader =
      new BufferedInputStream(
        new FileInputStream(dataFile));
    DataInputStream din =
      new DataInputStream(reader);
    BufferedWriter idsWriter =
      new BufferedWriter(new FileWriter(idsFile));
    String outputFile = null;
    int blockHeight = row / numPartitions;
    int rest = row % numPartitions;
    short[][] rows;
    int startRow = 0;
    int endRow = 0;
    int curHeight = 0;
    int count = 0;
    for (int i = 0; i < numPartitions; i++) {
      System.out
        .println("The " + i + "th partition");
      idsWriter.write(i + "\t");
      outputFile =
        tmpDir + "/" + tmpFilePrefix + i;
      endRow += blockHeight;
      if (rest > 0) {
        endRow++;
        rest--;
      }
      curHeight = endRow - startRow;
      rows = new short[curHeight][width];
      count = 0;
      for (int j = startRow; j < endRow; j++) {
        for (int k = 0; k < width; k++) { // Note
                                          // the
                                          // start
          rows[count][k] = din.readShort();
        }
        count++;
      }
      // Height, width, row, rowOffset
      // Row ID starts from 0 to numPartitions - 1
      idsWriter.write(curHeight + "\t" + width
        + "\t" + i + "\t" + startRow + "\n");
      startRow = endRow;
      writeToBinFile(rows, curHeight, width,
        outputFile);
    }
    din.close();
    idsWriter.flush();
    idsWriter.close();
  }

  public static void writeToBinFile(short[][] row,
    int curHeight, int width, String fileName)
    throws IOException {
    DataOutputStream dout = new DataOutputStream(
      new BufferedOutputStream(
        new FileOutputStream(fileName)));
    for (int i = 0; i < curHeight; i++) {
      for (int j = 0; j < width; j++) {
        dout.writeShort(row[i][j]);
      }
    }
    dout.flush();
    dout.close();
  }
}
