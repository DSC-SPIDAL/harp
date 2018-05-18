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
  private int shadSize;
  private int enclave_total_size; //MB
  private int enclave_per_thd_size; //MB
  private int enclave_task_size;  //MB
  private int numCenPars;
  private int cenVecSize;
  private int numMappers;
  private int numThreads;
  private int loadThreads;
  private int numIterations;
  private int cenTableSize;
  private String cenDir;

  private long total_comm_time;
  private long total_comp_time;
  private long total_sgx_comm;
  private long total_sgx_comp_ecall;
  private long total_sgx_comp_mem;
  private long total_sgx_comp_ocall;
  private long total_sgx_init;

  private long timer_start;
  private long timer_start2;


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
    enclave_total_size = configuration
      .getInt(Constants.ENCLAVE_TOTAL, 96);
    enclave_per_thd_size = configuration
      .getInt(Constants.ENCLAVE_PER_THD, 96);
    enclave_task_size = configuration
      .getInt(Constants.ENCLAVE_TASK, 8);

    // point number in each shad (double precision)
    shadSize = enclave_task_size*1024*1024/(vectorSize*8); 
    loadThreads = Runtime.getRuntime().availableProcessors(); 

    // get the cenTable size in communication
    // unit KB
    cenTableSize = dataDoubleSizeKB(numCentroids*cenVecSize); 
    total_comm_time = 0;
    total_comp_time = 0;
    total_sgx_comm = 0;
    total_sgx_comp_ecall = 0;
    total_sgx_comp_mem = 0;
    total_sgx_comp_ocall = 0;
    total_sgx_init = 0;

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
    // List<double[]> pointArrays =
    //   KMUtil.loadPoints(fileNames, this.vectorSize, conf, loadThreads);
    
    //use a second read in method
    List<double[][]> pointArrays =
      KMUtil.loadPoints2(fileNames, this.vectorSize, this.shadSize, conf, loadThreads);

    
    this.timer_start = System.currentTimeMillis();

    if (Constants.enablesimu)
    {
	    // add overhead of creating an enclave of 96MB
	    // per enclave*numThreads
	    // each thread holds a seperated sgx enclave
	    long creation_enclave = (long)((Constants.creation_enclave_fix + this.enclave_total_size*1024*Constants.creation_enclave_kb)*Constants.ms_per_kcycle)*numThreads; 
	    LOG.info("creation_enclave: " + creation_enclave);
	    simuOverhead(creation_enclave);
    }

    this.total_sgx_init += (System.currentTimeMillis() - this.timer_start);

    
    this.timer_start = System.currentTimeMillis();
    if (Constants.enablesimu)
    {
	    // add overhead of local attestation on a single node
	    // for single mapper (process) simulation, we assume that enclave created by threads within the same mapper 
	    // still requires local attestation
	    // long local_attestation_enclave = (long)((numMappers-1)*Constants.local_attestation*Constants.ms_per_kcycle);
	    long local_attestation_enclave = (long)((comb(numThreads,2)+(numMappers-1)*numThreads)*Constants.local_attestation*Constants.ms_per_kcycle);
	    LOG.info("local_attestation_enclave: " + local_attestation_enclave);
	    simuOverhead(local_attestation_enclave);
    }
    
    this.total_sgx_init += (System.currentTimeMillis() - this.timer_start);

    // Initialize tasks
    // List<CenCalcTask> cenCalcTasks =
    //   new LinkedList<>();
    // for (int i = 0; i < numThreads; i++) {
    //   cenCalcTasks.add(
    //     new CenCalcTask(cenTable, cenVecSize));
    // }
    //
    // DynamicScheduler<double[], Object, CenCalcTask> calcCompute =
    //   new DynamicScheduler<>(cenCalcTasks);

    // calculate tasks
    List<CenCalcTask2> cenCalcTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      cenCalcTasks.add(
        new CenCalcTask2(cenTable, cenVecSize, enclave_per_thd_size));
    }

    DynamicScheduler<double[][], Object, CenCalcTask2> calcCompute =
      new DynamicScheduler<>(cenCalcTasks);

    // merge tasks
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
      this.timer_start = System.currentTimeMillis();

      // for (double[] points : pointArrays) {
      //   calcCompute.submit(points);
      // }
      for (double[][] points : pointArrays) {
        calcCompute.submit(points);
      }

      while (calcCompute.hasOutput()) {
        calcCompute.waitForOutput();
      }

      // Merge local centroids
      for (Partition<DoubleArray> partition : cenTable
        .getPartitions()) {
        mergeCompute.submit(partition);
      }
      while (mergeCompute.hasOutput()) {
        mergeCompute.waitForOutput();
      }

      if (Constants.enablesimu)
      {
	      //fetch sgx overhead from every tasks
	      long accumu_calc_sgx_ecall = 0;
	      long accumu_calc_sgx_ocall = 0;
	      long accumu_calc_sgx_mem = 0;

	      for (CenCalcTask2 taskcell : cenCalcTasks) 
	      {
		      accumu_calc_sgx_ecall += taskcell.getSGXEcall();
		      accumu_calc_sgx_ocall += taskcell.getSGXOcall();
		      accumu_calc_sgx_mem += taskcell.getSGXMem();
		      taskcell.resetSGX();
	      }

	      this.total_sgx_comp_ecall += (accumu_calc_sgx_ecall/cenCalcTasks.size());
	      this.total_sgx_comp_mem += (accumu_calc_sgx_mem/cenCalcTasks.size());
	      this.total_sgx_comp_ocall += (accumu_calc_sgx_ocall/cenCalcTasks.size());

	      long accumu_merge_sgx_ecall = 0;
	      long accumu_merge_sgx_ocall = 0;
	      for (CenMergeTask taskcell : tasks) 
	      {
		      accumu_merge_sgx_ecall += taskcell.getSGXEcall();
		      accumu_merge_sgx_ocall += taskcell.getSGXOcall();
		      taskcell.resetSGX();
	      }

	      this.total_sgx_comp_ecall += (accumu_merge_sgx_ecall/tasks.size());
	      this.total_sgx_comp_ocall += (accumu_merge_sgx_ocall/tasks.size());

      }
      
      this.total_comp_time += (System.currentTimeMillis() - this.timer_start);
      // ----------------------------- finish local computation ----------------------------
      //
      // ----------------------------- start interprocess communication ----------------------------
      // regroup-allgather 
      this.timer_start = System.currentTimeMillis();

      regroup("main", "regroup-" + i, cenTable,
      new Partitioner(this.getNumWorkers()));


      if (Constants.enablesimu)
      {
      	      this.timer_start2 = System.currentTimeMillis();
	      //add overhead of cross-enclave communiation
	      //data in the enclave of main thread of mapper A transfers to enclave of main thread of mapper B
	      //transfer data send (numMappers-1) + received data (numMappers-1)
	      long transfer_data = (long)((cenTableSize/numMappers)*(2*numMappers-2)); 
	      LOG.info("transfer_data: " + transfer_data);
	      long cross_enclave_overhead = (long)(((numMappers-1)*(Constants.Ocall + Constants.Ecall))*Constants.ms_per_kcycle);
	      cross_enclave_overhead += (long)(transfer_data*Constants.cross_enclave_per_kb*Constants.ms_per_kcycle);
	      LOG.info("regroup cross_enclave_overhead: " + cross_enclave_overhead);
      	      simuOverhead(cross_enclave_overhead);

      	      this.total_sgx_comm += (System.currentTimeMillis() - this.timer_start2);
      }

      this.total_comm_time += (System.currentTimeMillis() - this.timer_start);


      this.timer_start = System.currentTimeMillis();
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

      this.total_comp_time += (System.currentTimeMillis() - this.timer_start);

      this.timer_start = System.currentTimeMillis();
      allgather("main", "allgather-"+i, cenTable);

      
      if (Constants.enablesimu)
      {
	      this.timer_start2 = System.currentTimeMillis();
	      //add overhead of cross-enclave
	      //allgather operation
	      long transfer_data = (long)(cenTableSize);
	      long cross_enclave_overhead = (long)((Constants.Ocall + Constants.Ecall*(numMappers-1))*Constants.ms_per_kcycle);
	      cross_enclave_overhead += (long)(transfer_data*Constants.cross_enclave_per_kb*Constants.ms_per_kcycle);
	      LOG.info("allgather cross_enclave_overhead: " + cross_enclave_overhead);

	      simuOverhead(cross_enclave_overhead);
      	      this.total_sgx_comm += (System.currentTimeMillis() - this.timer_start2);
      }

      for (CenCalcTask2 task : calcCompute.getTasks()) {
        task.update(cenTable);
      }

      this.total_comm_time += (System.currentTimeMillis() - this.timer_start);

      logMemUsage();
      logGCTime();
      context.progress();
    }

    calcCompute.stop();
    mergeCompute.stop();
    // end of iteration
    long end_iter = System.currentTimeMillis();
    // record execution time per iteration
    long total_execution_time = (end_iter - start_iter);
    LOG.info("Iterations: " + numIterations);
    LOG.info("Total execution: " +total_execution_time+ " ms ; " + total_execution_time/(double)numIterations+" ms per itr");
    LOG.info("Compute: " +this.total_comp_time+ " ms ; " + this.total_comp_time/(double)numIterations+" ms per itr");
    LOG.info("Comm: " +this.total_comm_time+ " ms ; " + this.total_comm_time/(double)numIterations+" ms per itr");
    LOG.info("Compute SGX overheadEcall: " +this.total_sgx_comp_ecall+ " ms ; " + this.total_sgx_comp_ecall/(double)numIterations+" ms per itr");
    LOG.info("Compute SGX overheadMem: " +this.total_sgx_comp_mem+ " ms ; " + this.total_sgx_comp_mem/(double)numIterations+" ms per itr");
    LOG.info("Compute SGX overheadOcall: " +this.total_sgx_comp_ocall+ " ms ; " + this.total_sgx_comp_ocall/(double)numIterations+" ms per itr");
    LOG.info("Comm SGX overhead: " +this.total_sgx_comm+ " ms ; " + this.total_sgx_comm/(double)numIterations+" ms per itr");
    LOG.info("Init SGX overhead: " +this.total_sgx_init+ " ms ; " + this.total_sgx_init/(double)numIterations+" ms per itr");

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
