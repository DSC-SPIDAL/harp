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

package edu.iu.benchmark;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class DataGenRunnable implements Runnable {

  private String localDir;
  private String fileID;

  public DataGenRunnable(String localDir,
    String fileID) {
    this.localDir = localDir;
    this.fileID = fileID;
  }

  @Override
  public void run() {
    try {
      DataOutputStream out = new DataOutputStream(
        new FileOutputStream(localDir
          + File.separator + "data_" + fileID));
      out.writeDouble(1000);
      out.close();
      System.out.println("Write file " + fileID);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

public class DataGen {

  public static void generateInputData(int numMappers,
    String localDirName, Path inputDirPath,
    FileSystem fs) throws IOException,
    InterruptedException, ExecutionException {
    // Check data directory
    if (fs.exists(inputDirPath)) {
      fs.delete(inputDirPath, true);
    }
    // Check local directory
    // If existed, regenerate data
    File localDir = new File(localDirName);
    if (localDir.exists()
      && localDir.isDirectory()) {
      for (File file : localDir.listFiles()) {
        file.delete();
      }
      localDir.delete();
    }
    boolean success = localDir.mkdirs();
    if (success) {
      System.out.println(
        "Directory: " + localDir + " created");
    }
    // Create random data points
    int poolSize =
      Runtime.getRuntime().availableProcessors();
    ExecutorService service =
      Executors.newFixedThreadPool(poolSize);
    List<Future<?>> futures =
      new LinkedList<Future<?>>();
    for (int k = 0; k < numMappers; k++) {
      Future<?> f =
        service.submit(new DataGenRunnable(
          localDirName, k + ""));
      futures.add(f);
    }
    for (Future<?> f : futures) {
      f.get();
    }
    // Shut down the executor service so that this
    // thread can exit
    service.shutdownNow();
    // Copy to HDFS
    Path localDirPath = new Path(localDirName);
    fs.copyFromLocalFile(localDirPath,
      inputDirPath);
  }

  public static void generateData(int numMappers,
    Path inputDirPath, String localDirName,
    FileSystem fs) throws IOException,
    InterruptedException, ExecutionException {
    generateInputData(numMappers, localDirName,
      inputDirPath, fs);
  }
}
