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

package edu.iu.data_gen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @brief generator ascii Matrix Market format for Dense matrix
 * (Array format column oriented)
 * used by daal_pca, daal_svd, daal_kmeans
 * each line contains one feature vector
 */
public class DataGenMMDenseTask implements Runnable {

  private int pointsPerFile;
  private int vectorSize;
  private double norm;
  private double offset;
  private String sep;
  private String localDir;
  private String fileName;

  /**
   * @param pointsPerFile
   * @param localInputDir
   * @param fileName
   * @param vectorSize
   * @return
   * @brief multi-threading data points generator
   * each thread generates pointsPerFile number of "vectorSize" dimensioned
   * point seperated by comma
   */
  public DataGenMMDenseTask(int pointsPerFile,
                            String localInputDir, String fileName,
                            int vectorSize, double norm, double offset, String sep) {
    this.pointsPerFile = pointsPerFile;
    this.localDir = localInputDir;
    this.fileName = fileName;
    this.vectorSize = vectorSize;
    this.norm = norm;
    this.offset = offset;
    this.sep = sep;
  }

  @Override
  public void run() {
    double point;
    Random random = new Random();
    try {

      BufferedWriter writer = new BufferedWriter(new FileWriter(this.localDir +
          File.separator + "data_" + this.fileName));

      for (int i = 0; i < pointsPerFile; i++) {

        for (int j = 0; j < vectorSize; j++) {

          point = random.nextDouble() * this.norm - this.offset;
          writer.write(String.valueOf(point));
          if (j != (vectorSize - 1))
            writer.write(sep);
        }

        writer.newLine();
      }

      writer.close();

      System.out.println("Write file "
          + this.fileName);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
