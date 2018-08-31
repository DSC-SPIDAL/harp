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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import it.unimi.dsi.fastutil.ints.IntArrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class DataGenerator {
  /**
   * @param total_points
   * @param int
   * @return
   * @brief
   */
  public static void generateDenseDataMulti(int num_points, int nFeatures, int num_files, double norm,
                                            double offset, String sep, Path dataDir, String tmpDirName, FileSystem fs)
      throws IOException, InterruptedException, ExecutionException {//{{{

    int pointsPerFile = num_points / num_files;
    // clean data dir content
    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }

    // clean tmp local dir
    File tmpDir = new File(tmpDirName);
    if (tmpDir.exists() && tmpDir.isDirectory()) {
      for (File file : tmpDir.listFiles())
        file.delete();

      tmpDir.delete();
    }

    boolean success = tmpDir.mkdir();
    if (success)
      System.out.println("Directory: " + tmpDirName + " created");

    if (pointsPerFile == 0)
      throw new IOException("No point to write.");

    // create parallel Java threads pool
    int poolSize = Runtime.getRuntime().availableProcessors();
    ExecutorService service = Executors.newFixedThreadPool(poolSize);

    List<Future<?>> futures = new LinkedList<Future<?>>();

    // generate each file in parallel
    for (int k = 0; k < num_files; k++) {
      Future<?> f = service.submit(new DataGenMMDenseTask(pointsPerFile,
          tmpDirName, Integer.toString(k), nFeatures, norm, offset, sep));

      futures.add(f); // add a new thread

    }

    for (Future<?> f : futures) {
      f.get();
    }

    // Shut down the executor service so that this
    // thread can exit
    service.shutdownNow();
    Path tmpDirPath = new Path(tmpDirName);
    fs.copyFromLocalFile(tmpDirPath, dataDir);
    DeleteFileFolder(tmpDirName);

  }//}}}

  public static void generateDenseLabelMulti(int num_points, int nFeatures, int num_files, int labelRange,
                                             String sep, Path dataDir, String tmpDirName, FileSystem fs)
      throws IOException, InterruptedException, ExecutionException {//{{{

    int pointsPerFile = num_points / num_files;
    // clean data dir content
    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }

    // clean tmp local dir
    File tmpDir = new File(tmpDirName);
    if (tmpDir.exists() && tmpDir.isDirectory()) {
      for (File file : tmpDir.listFiles())
        file.delete();

      tmpDir.delete();
    }

    boolean success = tmpDir.mkdir();
    if (success)
      System.out.println("Directory: " + tmpDirName + " created");

    if (pointsPerFile == 0)
      throw new IOException("No point to write.");

    // create parallel Java threads pool
    int poolSize = Runtime.getRuntime().availableProcessors();
    ExecutorService service = Executors.newFixedThreadPool(poolSize);

    List<Future<?>> futures = new LinkedList<Future<?>>();

    // generate each file in parallel
    for (int k = 0; k < num_files; k++) {
      Future<?> f = service.submit(new DataGenMMDenseLabelTask(pointsPerFile,
          tmpDirName, Integer.toString(k), nFeatures, labelRange, sep));

      futures.add(f); // add a new thread

    }

    for (Future<?> f : futures) {
      f.get();
    }

    // Shut down the executor service so that this
    // thread can exit
    service.shutdownNow();
    Path tmpDirPath = new Path(tmpDirName);
    fs.copyFromLocalFile(tmpDirPath, dataDir);
    DeleteFileFolder(tmpDirName);

  }//}}}

  public static void generateDenseDataAndIntLabelMulti(int num_points, int nFeatures, int num_files, double norm,
                                                       double offset, int labelRange, String sep, Path dataDir, String tmpDirName, FileSystem fs)
      throws IOException, InterruptedException, ExecutionException {//{{{

    int pointsPerFile = num_points / num_files;
    // clean data dir content
    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }

    // clean tmp local dir
    File tmpDir = new File(tmpDirName);
    if (tmpDir.exists() && tmpDir.isDirectory()) {
      for (File file : tmpDir.listFiles())
        file.delete();

      tmpDir.delete();
    }

    boolean success = tmpDir.mkdir();
    if (success)
      System.out.println("Directory: " + tmpDirName + " created");

    if (pointsPerFile == 0)
      throw new IOException("No point to write.");

    // create parallel Java threads pool
    int poolSize = Runtime.getRuntime().availableProcessors();
    ExecutorService service = Executors.newFixedThreadPool(poolSize);

    List<Future<?>> futures = new LinkedList<Future<?>>();

    // generate each file in parallel
    for (int k = 0; k < num_files; k++) {
      Future<?> f = service.submit(new DataGenMMDenseAndIntLabelTask(pointsPerFile,
          tmpDirName, Integer.toString(k), nFeatures, norm, offset, labelRange, sep));

      futures.add(f); // add a new thread

    }

    for (Future<?> f : futures) {
      f.get();
    }

    // Shut down the executor service so that this
    // thread can exit
    service.shutdownNow();
    Path tmpDirPath = new Path(tmpDirName);
    fs.copyFromLocalFile(tmpDirPath, dataDir);
    DeleteFileFolder(tmpDirName);

  }//}}}

  public static void generateDenseDataSingle(int num_points, int nFeatures, double norm, double offset,
                                             String sep, Path dataDir, FileSystem fs) throws IOException {//{{{

    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }

    Random random = new Random();
    double[] data = new double[num_points * nFeatures];
    for (int i = 0; i < data.length; i++) {
      data[i] = random.nextDouble() * norm - offset;
    }


    System.out.println("Generate data " + dataDir.toString());
    FSDataOutputStream out = fs.create(dataDir, true);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
    for (int i = 0; i < data.length; i++) {
      if ((i % nFeatures) == (nFeatures - 1)) {
        bw.write(String.valueOf(data[i]));
        bw.newLine();
      } else {
        bw.write(data[i] + sep);
      }
    }

    bw.flush();
    bw.close();

  }//}}}

  public static void DeleteFileFolder(String path) {//{{{
    File file = new File(path);
    if (file.exists()) {
      do {
        delete(file);
      } while (file.exists());
    } else {
      System.out.println("File or Folder not found : " + path);
    }

  }//}}}

  private static void delete(File file) {//{{{
    if (file.isDirectory()) {
      String fileList[] = file.list();
      if (fileList.length == 0) {
        System.out.println("Deleting Directory : " + file.getPath());
        file.delete();
      } else {
        int size = fileList.length;
        for (int i = 0; i < size; i++) {
          String fileName = fileList[i];
          System.out.println("File path : " + file.getPath() + " and name :" + fileName);
          String fullPath = file.getPath() + "/" + fileName;
          File fileOrFolder = new File(fullPath);
          System.out.println("Full Path :" + fileOrFolder.getPath());
          delete(fileOrFolder);
        }
      }
    } else {
      System.out.println("Deleting file : " + file.getPath());
      file.delete();
    }
  }//}}}
}



















