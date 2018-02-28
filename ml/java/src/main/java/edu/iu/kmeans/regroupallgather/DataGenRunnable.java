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

package edu.iu.kmeans.regroupallgather;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class DataGenRunnable implements Runnable {

  private int pointsPerFile;
  private String localDir;
  private String fileName;
  int vectorSize;

  public DataGenRunnable(int pointsPerFile,
    String localInputDir, String fileName,
    int vectorSize) {
    this.pointsPerFile = pointsPerFile;
    this.localDir = localInputDir;
    this.fileName = fileName;
    this.vectorSize = vectorSize;
  }

  @Override
  public void run() {
    double point;
    Random random = new Random();
    try {
      DataOutputStream out =
        new DataOutputStream(new FileOutputStream(
          this.localDir + File.separator + "data_"
            + this.fileName));
      for (int i = 0; i < pointsPerFile; i++) {
        for (int j = 0; j < vectorSize; j++) {
          // point = 1000;
          point = random.nextDouble() * 1000;
          out.writeDouble(point);
        }
      }
      out.close();
      System.out
        .println("Write file " + this.fileName);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
