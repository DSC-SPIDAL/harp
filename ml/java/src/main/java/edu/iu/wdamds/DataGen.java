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

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Random;

class DataGen {

  static boolean generateXData(int numDataPoints,
    int targetDim, Path xFilePath, FileSystem fs)
    throws IOException {
    // if (fs.exists(xFilePath)) {
    // fs.delete(xFilePath, true);
    // }
    System.out.println(
      "Generate X data: " + xFilePath.toString());
    Random rand =
      new Random(System.currentTimeMillis()); // Real
                                              // random
    FSDataOutputStream out =
      fs.create(xFilePath, true);
    double[] data =
      new double[numDataPoints * targetDim];
    for (int i = 0; i < data.length; i++) {
      if (rand.nextBoolean()) {
        data[i] = rand.nextDouble();
      } else {
        data[i] = -rand.nextDouble();
      }
    }
    out.flush();
    out.close();
    System.out.println("Wrote X data to file");
    return true;
  }

  /**
   * Partition file generation is very important.
   * We need to verify it later. The format in
   * each partition file is: #ROWS Row ID ...
   * #FILES Distance File Weight File V File
   * 
   * @param inputFolder
   * @param inputPrefix
   * @param weightPrefix
   * @param vPrefix
   * @param workDirName
   * @param fs
   * @param numMapTasks
   * @return
   */
  public static boolean generatePartitionFiles(
    String inputFolder, String inputPrefix,
    String weightPrefix, String vPrefix,
    Path dataDirPath, FileSystem fs,
    int numMapTasks) {
    // Assume inputFolder is a place where all
    // processes can access
    // (on some shared file system such as NFS)
    File inputDir = new File(inputFolder);
    if (!inputDir.exists()
      || !inputDir.isDirectory()) {
      return false;
    }
    String inputDirPath =
      inputDir.getAbsolutePath();
    File[] files = inputDir.listFiles();
    // The number of files should be even
    // One data file matches with one weight file
    // and one v file
    if (files.length % 3 != 0) {
      return false;
    }
    int numFileSets = files.length / 3;
    System.out.println(
      "Number of file sets. " + numFileSets);
    int numSetPerPartition =
      numFileSets / numMapTasks;
    System.out.println(
      "Number of file set per partition. "
        + numSetPerPartition);
    int restPartitions =
      numFileSets % numMapTasks;
    int partitionID = 0;
    int count = 0;
    int countLimit = numSetPerPartition;
    LinkedList<Integer> rowIDs =
      new LinkedList<>();
    LinkedList<String> filePaths =
      new LinkedList<>();
    for (int i = 0; i < files.length; i++) {
      String fileName = files[i].getName();
      System.out
        .println("File_" + i + " " + fileName);
      if (fileName.startsWith(inputPrefix)) {
        // Find a set of files and create rowID
        int rowID = Integer.parseInt(
          fileName.replaceFirst(inputPrefix, ""));
        // String inputFileName = fileName;
        String weightFileName =
          fileName.replaceFirst(inputPrefix,
            weightPrefix);
        String vFileName = fileName
          .replaceFirst(inputPrefix, vPrefix);
        rowIDs.add(rowID);
        filePaths
          .add(inputDirPath + "/" + fileName);
        filePaths.add(
          inputDirPath + "/" + weightFileName);
        filePaths
          .add(inputDirPath + "/" + vFileName);
        count++;
        // The base count limit is
        // numSetPerPartition,
        // I adjust it if there is still
        // partitions left.
        if (count == numSetPerPartition) {
          if (restPartitions > 0) {
            countLimit = numSetPerPartition + 1;
            restPartitions--;
          } else {
            countLimit = numSetPerPartition;
          }
        }
        if (count == countLimit) {
          // Create a partition file
          Path partitionFilePath =
            new Path(dataDirPath,
              "partition_" + partitionID);
          try {
            FSDataOutputStream out =
              fs.create(partitionFilePath, true);
            BufferedWriter bw =
              new BufferedWriter(
                new OutputStreamWriter(out));
            bw.write(MDSConstants.ROWS_TAG);
            bw.newLine();
            for (int j = 0; j < rowIDs
              .size(); j++) {
              bw.write(
                rowIDs.get(j).intValue() + "");
              bw.newLine();
            }
            bw.write(MDSConstants.FILES_TAG);
            bw.newLine();
            for (int j = 0; j < filePaths
              .size(); j++) {
              bw.write(filePaths.get(j));
              bw.newLine();
            }
            bw.flush();
            out.hflush();
            out.hsync();
            bw.close();
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
          // Reset row ID holder
          rowIDs.clear();
          // Reset file list holder
          filePaths.clear();
          // Reset count
          count = 0;
          // Reset count limit
          countLimit = numSetPerPartition;
          // Increase partition ID
          partitionID++;
        }
      }
    }
    return true;
  }
}
