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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @brief generator ascii file for Naive Bayes training datasets 
 * each line contains vec size random double value and a random label value [0, nClasses] 
 * used by daal_naive 
 */
public class DataGenNaiveBayes implements Runnable {

  private int pointsPerFile;
  private String localDir;
  private String fileName;
  int vectorSize;
  int nClasses;

  public DataGenNaiveBayes(String localInputDir, String fileName,int pointsPerFile, 
    int vectorSize, int nClasses) {
    this.localDir = localInputDir;
    this.fileName = fileName;
	this.pointsPerFile = pointsPerFile;
    this.vectorSize = vectorSize;
	this.nClasses = nClasses;
  }

  @Override
  public void run() {
    double point;
	int label;
    Random random = new Random();
    try {

      BufferedWriter writer = new BufferedWriter(new FileWriter(this.localDir + File.separator + "data_" + this.fileName));

      for (int i = 0; i < pointsPerFile; i++) {
        for (int j = 0; j < vectorSize; j++) {
          point = random.nextDouble()*2 -1;
          writer.write(String.valueOf(point));
		  writer.write(",");
        }

		label = random.nextInt(nClasses);
		writer.write(String.valueOf(label));
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
