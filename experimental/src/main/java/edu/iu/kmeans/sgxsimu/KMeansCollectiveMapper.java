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

package edu.iu.kmeans.sgxsimu;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import java.lang.*;
import java.util.concurrent.TimeUnit;

public class KMeansCollectiveMapper extends
  CollectiveMapper<String, String, Object, Object> {

  private int pointsPerFile;
  private int numCentroids;
  private int vectorSize;
  private int numCenPars;
  private int cenVecSize;
  private int numMappers;
  private int numThreads;
  private int numIterations;
  private int cenTableSize;
  private String cenDir;
  private long mt_access_enclave;
  private long total_comm_time;
  private boolean enable_simu;

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    Configuration configuration =
      context.getConfiguration();
    pointsPerFile = configuration
      .getInt(Constants.POINTS_PER_FILE, 20);
    numCentroids = configuration
      .getInt(Constants.NUM_CENTROIDS, 20);
    vectorSize = configuration
      .getInt(Constants.VECTOR_SIZE, 20);
    numMappers = configuration
      .getInt(Constants.NUM_MAPPERS, 10);
    numCenPars = numMappers;
    cenVecSize = vectorSize + 1;
    numThreads = configuration
      .getInt(Constants.NUM_THREADS, 10);
    numIterations = configuration
      .getInt(Constants.NUM_ITERATIONS, 10);


    // get the cenTable size in communication
    // unit KB
    // cenTableSize = ((numCentroids*cenVecSize*Double.SIZE)/Byte.SIZE)/1024;
    cenTableSize = dataDoubleSizeKB(numCentroids*cenVecSize); 
    mt_access_enclave = 0;
    total_comm_time = 0;

    cenDir = configuration.get(Constants.CEN_DIR);
    LOG.info("Points Per File " + pointsPerFile);
    LOG.info("Num Centroids " + numCentroids);
    LOG.info("Vector Size " + vectorSize);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Cen Dir " + cenDir);
    long endTime = System.currentTimeMillis();
    LOG.info(
      "config (ms) :" + (endTime - startTime));
  }

  protected void mapCollective(
    KeyValReader reader, Context context)
    throws IOException, InterruptedException {
    long startTime = System.currentTimeMillis();
    List<String> pointFiles =
      new LinkedList<String>();
    while (reader.nextKeyValue()) {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info(
        "Key: " + key + ", Value: " + value);
      pointFiles.add(value);
    }
    Configuration conf =
      context.getConfiguration();
    runKmeans(pointFiles, conf, context);
    LOG.info("Total execution time: "
      + (System.currentTimeMillis() - startTime));
  }

  private void runKmeans(List<String> fileNames,
    Configuration conf, Context context)
    throws IOException {
    // Load centroids
    Table<DoubleArray> cenTable =
      new Table<>(0, new DoubleArrPlus());
    if (this.isMaster()) {
      createCenTable(cenTable, numCentroids,
        numCenPars, cenVecSize);
      loadCentroids(cenTable, cenVecSize,
        cenDir + File.separator
          + Constants.CENTROID_FILE_NAME,
        conf);
    }
    // Bcast centroids
    bcastCentroids(cenTable, this.getMasterID());
    // Load data points
    List<double[]> pointArrays =
      KMUtil.loadPoints(fileNames, this.vectorSize, conf, numThreads);

    // add overhead of creating an enclave of 96MB
    // per enclave*numThreads
    // each thread holds a seperated sgx enclave
    long creation_enclave = (long)((Constants.creation_enclave_fix + 96*1024*Constants.creation_enclave_kb)*Constants.ms_per_kcycle)*numThreads; 
    LOG.info("creation_enclave: " + creation_enclave);

    if (Constants.enablesimu)
    	simuOverhead(creation_enclave);

    // add overhead of local attestation on a single node
    // for single mapper (process) simulation, we assume that enclave created by threads within the same mapper 
    // still requires local attestation
    // long local_attestation_enclave = (long)((numMappers-1)*Constants.local_attestation*Constants.ms_per_kcycle);
    long local_attestation_enclave = (long)((comb(numThreads,2)+(numMappers-1)*numThreads)*Constants.local_attestation*Constants.ms_per_kcycle);
    LOG.info("local_attestation_enclave: " + local_attestation_enclave);

    if (Constants.enablesimu)
    	simuOverhead(local_attestation_enclave);
    
    // Initialize tasks
    List<CenCalcTask> cenCalcTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      cenCalcTasks.add(
        new CenCalcTask(cenTable, cenVecSize));
    }

    DynamicScheduler<double[], Object, CenCalcTask> calcCompute =
      new DynamicScheduler<>(cenCalcTasks);

    List<CenMergeTask> tasks = new LinkedList<>();

    for (int i = 0; i < numThreads; i++) {
      tasks.add(
        new CenMergeTask(calcCompute.getTasks()));
    }

    DynamicScheduler<Partition<DoubleArray>, Object, CenMergeTask> mergeCompute =
      new DynamicScheduler<>(tasks);

    calcCompute.start();
    mergeCompute.start();

    // ----------------------------------------------------
    // record time for all iterations of execution
    long start_iter = System.currentTimeMillis();
    // For iterations
    for (int i = 0; i < numIterations; i++) 
    {
      LOG.info("Iteration: " + i);

      // ----------------------------- start local computation ----------------------------
      // Calculate new centroids
      long t1 = System.currentTimeMillis();
      for (double[] points : pointArrays) {
        calcCompute.submit(points);
      }
      while (calcCompute.hasOutput()) {
        calcCompute.waitForOutput();
      }
      // Merge local centroids
      long t2 = System.currentTimeMillis();
      for (Partition<DoubleArray> partition : cenTable
        .getPartitions()) {
        mergeCompute.submit(partition);
      }
      while (mergeCompute.hasOutput()) {
        mergeCompute.waitForOutput();
      }

      // ----------------------------- finish local computation ----------------------------
      //
      // ----------------------------- start interprocess communication ----------------------------
      // regroup-allgather 
      long t3 = System.currentTimeMillis();
      regroup("main", "regroup-" + i, cenTable,
      new Partitioner(this.getNumWorkers()));

      //add overhead of cross-enclave communiation
      //data in the enclave of main thread of mapper A transfers to enclave of main thread of mapper B
      //transfer data send (numMappers-1) + received data (numMappers-1)
      long transfer_data = (long)((cenTableSize/numMappers)*(2*numMappers-2)); 
      LOG.info("transfer_data: " + transfer_data);

      long cross_enclave_overhead = (long)(((numMappers-1)*(Constants.Ocall + Constants.Ecall))*Constants.ms_per_kcycle);
      cross_enclave_overhead += (long)(transfer_data*Constants.cross_enclave_per_kb*Constants.ms_per_kcycle);

      LOG.info("regroup cross_enclave_overhead: " + cross_enclave_overhead);

      if (Constants.enablesimu)
      	simuOverhead(cross_enclave_overhead);

      // compute the reduction results on each mapper
      // communicated data already inside enclave of main thread
      // no need to swap data across enclaves
      for (Partition<DoubleArray> partition : cenTable
        .getPartitions()) 
      {
        double[] doubles = partition.get().get();
        int size = partition.get().size();
        for (int j = 0; j < size; j +=
          cenVecSize) {
          for (int k = 1; k < cenVecSize; k++) {
            // Calculate avg
            if (doubles[j] != 0) {
              doubles[j + k] /= doubles[j];
            }
          }
        }
      }

      allgather("main", "allgather-"+i, cenTable);

      //add overhead of cross-enclave
      //allgather operation
      transfer_data = (long)(cenTableSize);
      cross_enclave_overhead = (long)((Constants.Ocall + Constants.Ecall*(numMappers-1))*Constants.ms_per_kcycle);
      cross_enclave_overhead += (long)(transfer_data*Constants.cross_enclave_per_kb*Constants.ms_per_kcycle);
      LOG.info("allgather cross_enclave_overhead: " + cross_enclave_overhead);

      if (Constants.enablesimu)
      	simuOverhead(cross_enclave_overhead);

      for (CenCalcTask task : calcCompute.getTasks()) {
        task.update(cenTable);
      }

      long t4 = System.currentTimeMillis();
      
      // add overhead of concurrently enclave access by mutiple threads
      // long compute_time = ( t2 - t1 ); // in ms
      // double speedup_enclave = (-0.034)*Math.pow((double)numThreads,2) + 0.332*(double)numThreads + 0.814; 
      //
      // if (speedup_enclave > (double) numThreads)
	//       speedup_enclave = numThreads;
      // LOG.info("speedup_enclave: " + speedup_enclave);
      // this.mt_access_enclave += ((1.0/speedup_enclave)*numThreads -1 )*compute_time;

      LOG.info("Compute: " + (t2 - t1)
        + ", Merge: " + (t3 - t2)
        + ", Aggregate: " + (t4 - t3));

      this.total_comm_time += (t4 - t2);
      logMemUsage();
      logGCTime();
      context.progress();
    }

    calcCompute.stop();
    mergeCompute.stop();
    // end of iteration
    long end_iter = System.currentTimeMillis();
    // record execution time per iteration
    // long total_execution_time = (end_iter - start_iter + this.mt_access_enclave);
    long total_execution_time = (end_iter - start_iter);
    LOG.info("Total execution time for " + numIterations + " itrs : " + total_execution_time + " ms");
    LOG.info("Execution Time per iter: " + total_execution_time/(double)numIterations + "ms");
    LOG.info("Comm Ratio: " + this.total_comm_time/(double)total_execution_time);

    // Write out centroids
    if (this.isMaster()) {
      LOG.info("Start to write out centroids.");
      long startTime = System.currentTimeMillis();
      KMUtil.storeCentroids(conf, cenDir,
        cenTable, cenVecSize, "output");
      long endTime = System.currentTimeMillis();
      LOG.info("Store centroids time (ms): "
        + (endTime - startTime));
    }
    cenTable.release();
  }

  private void createCenTable(
    Table<DoubleArray> cenTable, int numCentroids,
    int numCenPartitions, int cenVecSize) {
    int cenParSize =
      numCentroids / numCenPartitions;
    int cenRest = numCentroids % numCenPartitions;
    for (int i = 0; i < numCenPartitions; i++) {
      if (cenRest > 0) {
        int size = (cenParSize + 1) * cenVecSize;
        DoubleArray array =
          DoubleArray.create(size, false);
        cenTable.addPartition(
          new Partition<>(i, array));
        cenRest--;
      } else if (cenParSize > 0) {
        int size = cenParSize * cenVecSize;
        DoubleArray array =
          DoubleArray.create(size, false);
        cenTable.addPartition(
          new Partition<>(i, array));
      } else {
        break;
      }
    }
  }

  /**
   * Fill data from centroid file to cenDataMap
   * 
   * @param cenDataMap
   * @param vectorSize
   * @param cFileName
   * @param configuration
   * @throws IOException
   */
  private void loadCentroids(
    Table<DoubleArray> cenTable, int cenVecSize,
    String cFileName, Configuration configuration)
    throws IOException {
    long startTime = System.currentTimeMillis();
    Path cPath = new Path(cFileName);
    FileSystem fs = FileSystem.get(configuration);
    FSDataInputStream in = fs.open(cPath);
    BufferedReader br = new BufferedReader(
      new InputStreamReader(in));
    String[] curLine = null;
    int curPos = 0;
    for (Partition<DoubleArray> partition : cenTable
      .getPartitions()) {
      DoubleArray array = partition.get();
      double[] cData = array.get();
      int start = array.start();
      int size = array.size();
      for (int i =
        start; i < (start + size); i++) {
        // Don't set the first element in each row
        if (i % cenVecSize != 0) {
          // cData[i] = in.readDouble();
          if (curLine == null
            || curPos == curLine.length) {
            curLine = br.readLine().split(" ");
            curPos = 0;
          }
          cData[i] =
            Double.parseDouble(curLine[curPos]);
          curPos++;
        }
      }
    }
    br.close();
    long endTime = System.currentTimeMillis();
    LOG.info("Load centroids (ms): "
      + (endTime - startTime));
  }

  /**
   * Broadcast centroids data in partitions
   * 
   * @param table
   * @param numPartitions
   * @throws IOException
   */
  private void bcastCentroids(
    Table<DoubleArray> table, int bcastID)
    throws IOException {
    long startTime = System.currentTimeMillis();
    boolean isSuccess = false;
    try {
      isSuccess = this.broadcast("main",
        "broadcast-centroids", table, bcastID,
        false);
    } catch (Exception e) {
      LOG.error("Fail to bcast.", e);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("Bcast centroids (ms): "
      + (endTime - startTime));
    if (!isSuccess) {
      throw new IOException("Fail to bcast");
    }
  }

  /**
   * @brief calculate the data size in and out enclave (KB)
   * double precision assumed
   *
   * @param size
   *
   * @return 
   */
  private int dataDoubleSizeKB(int size)
  {
     return size*Double.SIZE/Byte.SIZE/1024;
  }

  /**
   * @brief simulate the overhead (ms)
   * of a SGX-related operation
   *
   * @param time
   *
   * @return 
   */
  private void simuOverhead(long time)
  {
	  try{
		  Thread.sleep(time);
	  }catch (Exception e)
	  {
		  System.out.println(e);
	  }
  }

  /**
   * @brief compute combination
   *
   * @param n
   * @param r
   *
   * @return 
   */
  private int comb(int n,int r)
  {
	  if (n < r)
	     return 0;

	  if( r== 0 || n == r )
		  return 1;
	  else
		  return comb(n-1,r)+comb(n-1,r-1);
  }
}
