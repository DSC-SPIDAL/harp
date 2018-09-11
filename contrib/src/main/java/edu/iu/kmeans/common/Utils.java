package edu.iu.kmeans.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.schstatic.StaticScheduler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Utils {
  private final static int DATA_RANGE = 10;

  public static void generateData(
    int numOfDataPoints, int vectorSize,
    int numMapTasks, FileSystem fs,
    String localDirStr, Path dataDir)
    throws IOException, InterruptedException,
    ExecutionException {
    int numOfpointFiles = numMapTasks;
    int pointsPerFile =
      numOfDataPoints / numOfpointFiles;
    int pointsRemainder =
      numOfDataPoints % numOfpointFiles;
    System.out.println("Writing "
      + numOfDataPoints + " vectors to "
      + numMapTasks + " file evenly");

    // Check data directory
    if (fs.exists(dataDir)) {
      fs.delete(dataDir, true);
    }
    // Check local directory
    File localDir = new File(localDirStr);
    // If existed, regenerate data
    if (localDir.exists()
      && localDir.isDirectory()) {
      for (File file : localDir.listFiles()) {
        file.delete();
      }
      localDir.delete();
    }

    boolean success = localDir.mkdir();
    if (success) {
      System.out.println(
        "Directory: " + localDirStr + " created");
    }
    if (pointsPerFile == 0) {
      throw new IOException("No point to write.");
    }

    double point;
    int hasRemainder = 0;
    Random random = new Random();
    for (int k = 0; k < numOfpointFiles; k++) {
      try {
        String filename = Integer.toString(k);
        File file = new File(localDirStr
          + File.separator + "data_" + filename);
        FileWriter fw =
          new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw =
          new BufferedWriter(fw);

        if (pointsRemainder > 0) {
          hasRemainder = 1;
          pointsRemainder--;
        } else {
          hasRemainder = 0;
        }
        int pointsForThisFile =
          pointsPerFile + hasRemainder;
        for (int i =
          0; i < pointsForThisFile; i++) {
          for (int j = 0; j < vectorSize; j++) {
            point =
              random.nextDouble() * DATA_RANGE;
            // System.out.println(point+"\t");
            if (j == vectorSize - 1) {
              bw.write(point + "");
              bw.newLine();
            } else {
              bw.write(point + " ");
            }
          }
        }
        bw.close();
        System.out.println(
          "Done written" + pointsForThisFile
            + "points" + "to file " + filename);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Path localPath = new Path(localDirStr);
    fs.copyFromLocalFile(localPath, dataDir);
  }

  public static void generateInitialCentroids(
    int numCentroids, int vectorSize,
    Configuration configuration, Path cDir,
    FileSystem fs, int JobID) throws IOException {
    Random random = new Random();
    double[] data = null;
    if (fs.exists(cDir))
      fs.delete(cDir, true);
    if (!fs.mkdirs(cDir)) {
      throw new IOException(
        "Mkdirs failed to create "
          + cDir.toString());
    }

    data = new double[numCentroids * vectorSize];
    for (int i = 0; i < data.length; i++) {
      data[i] = random.nextDouble() * DATA_RANGE;
    }
    Path initClustersFile = new Path(cDir,
      KMeansConstants.CENTROID_FILE_PREFIX
        + JobID);
    System.out.println("Generate centroid data."
      + initClustersFile.toString());

    FSDataOutputStream out =
      fs.create(initClustersFile, true);
    BufferedWriter bw = new BufferedWriter(
      new OutputStreamWriter(out));
    for (int i = 0; i < data.length; i++) {
      if ((i % vectorSize) == (vectorSize - 1)) {
        bw.write(data[i] + "");
        bw.newLine();
      } else {
        bw.write(data[i] + " ");
      }
    }
    bw.flush();
    bw.close();
    // out.flush();
    // out.sync();
    // out.close();
    System.out
      .println("Wrote centroids data to file");
  }

  /**
   * @brief compute the distance by
   * using multi-threading
   * dynamic scheduler
   *
   * @param cenTable
   * @param previousCenTable
   * @param dataPoints
   *
   * @return 
   */
  public static double computationMultiThd(
    Table<DoubleArray> cenTable,
    Table<DoubleArray> previousCenTable,
    ArrayList<DoubleArray> dataPoints, int threadNum, int vectorSize) 
  {//{{{

      double err = 0;
      // create the task executor
      List<calcCenTask> taskExecutor = new LinkedList<>();
      for(int i=0;i<threadNum;i++)
          taskExecutor.add(new calcCenTask(previousCenTable, vectorSize));

      // create the dynamic scheduler 
      DynamicScheduler<double[], Object, calcCenTask> calcScheduler =
          new DynamicScheduler<>(taskExecutor);

      // launching the scheduler
      calcScheduler.start();

      // feed the scheduler with tasks
      for (DoubleArray aPoint : dataPoints) 
          calcScheduler.submit(aPoint.get());

      // wait until all of the tasks finished
      while(calcScheduler.hasOutput())
          calcScheduler.waitForOutput();

      // update the new centroid table
      for(int i=0;i<threadNum;i++)
      {
          // adds up all error
          err += taskExecutor.get(i).getError(); 
          Table<DoubleArray> pts_assign_sum = taskExecutor.get(i).getPtsAssignSum();

          for(Partition<DoubleArray> par : pts_assign_sum.getPartitions())
          {
              if (cenTable.getPartition(par.id()) != null)
              {
                  double[] newCentroids = cenTable.getPartition(par.id()).get().get();
                  for(int k=0;k<vectorSize+1;k++)
                      newCentroids[k] += par.get().get()[k];
              }
              else
              {
                  cenTable.addPartition(new Partition<DoubleArray>(par.id(), 
                              new DoubleArray(par.get().get(), 0, vectorSize+1)));
              }
              
          }
          
      }
      
      System.out.println("Errors: " + err);
      return err;

  }//}}}

  public static double computationMultiThdStatic(
    Table<DoubleArray> cenTable,
    Table<DoubleArray> previousCenTable,
    ArrayList<DoubleArray> dataPoints, int threadNum, int vectorSize) 
  {//{{{

      double err = 0;
      // create the task executor
      List<calcCenTaskStatic> taskExecutor = new LinkedList<>();
      for(int i=0;i<threadNum;i++)
          taskExecutor.add(new calcCenTaskStatic(previousCenTable, vectorSize));

      // create the static scheduler 
      StaticScheduler<double[], Integer, calcCenTaskStatic> calcScheduler = new StaticScheduler<>(taskExecutor);

      // launching the scheduler
      calcScheduler.start();

      // feed the scheduler with tasks
      for(int i=0;i<dataPoints.size();i++)
          calcScheduler.submit((i%threadNum), dataPoints.get(i).get());

      // wait until all of the tasks finished
      for(int i=0;i<threadNum;i++)
      {
          while(calcScheduler.hasOutput(i))
              calcScheduler.waitForOutput(i);
      }

      // update the new centroid table
      for(int i=0;i<threadNum;i++)
      {
          // adds up all error
          err += taskExecutor.get(i).getError(); 
          Table<DoubleArray> pts_assign_sum = taskExecutor.get(i).getPtsAssignSum();

          for(Partition<DoubleArray> par : pts_assign_sum.getPartitions())
          {
              if (cenTable.getPartition(par.id()) != null)
              {
                  double[] newCentroids = cenTable.getPartition(par.id()).get().get();
                  for(int k=0;k<vectorSize+1;k++)
                      newCentroids[k] += par.get().get()[k];
              }
              else
              {
                  cenTable.addPartition(new Partition<DoubleArray>(par.id(), 
                              new DoubleArray(par.get().get(), 0, vectorSize+1)));
              }
              
          }
          
      }
      
      System.out.println("Errors: " + err);
      return err;

  }//}}}

}
