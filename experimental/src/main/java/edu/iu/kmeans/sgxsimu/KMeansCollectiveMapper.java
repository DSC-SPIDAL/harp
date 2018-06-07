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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.lang.*;
import it.unimi.dsi.fastutil.ints.IntArrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

public class KMeansCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	private int numCentroids;
	private int vectorSize;
	private int fileDim;
	private int numCenPars;
	private int cenVecSize;

	private int shadSize;
	private int enclave_total_size; //MB
	private int enclave_per_thd_size; //MB
	private int enclave_task_size;  //MB
	private int cenTableSize;
	private boolean enablesimu;

	private int num_mappers;
	private int numThreads;
	private int harpThreads;

	private int numIterations;
	private String cenDir;
	private String cenDirInit;

	private List<String> inputFiles;
	private Configuration conf;
	private static HarpDAALDataSource datasource;

	private long total_comm_time;
	private long total_comp_time;
	private long total_sgx_comm;
	private long total_sgx_comp_ecall;
	private long total_sgx_comp_mem;
	private long total_sgx_comp_ocall;
	private long total_sgx_init;

	private long timer_start;
	private long timer_start2;

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
	{
		long startTime = System.currentTimeMillis();

		this.conf = context.getConfiguration();

		numCentroids = this.conf.getInt(HarpDAALConstants.NUM_CENTROIDS, 20);
		vectorSize = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 20);
		num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		numCenPars = num_mappers;
		cenVecSize = vectorSize + 1;
		numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
		numIterations = this.conf.getInt(HarpDAALConstants.NUM_ITERATIONS, 10);
		enclave_total_size = this.conf.getInt(Constants.ENCLAVE_TOTAL, 96);
		enclave_per_thd_size = this.conf.getInt(Constants.ENCLAVE_PER_THD, 96);
		enclave_task_size = this.conf.getInt(Constants.ENCLAVE_TASK, 8);
		enablesimu = this.conf.getBoolean(Constants.ENABLE_SIMU, true);
		cenDir = this.conf.get(HarpDAALConstants.CEN_DIR);
		cenDirInit = this.conf.get(HarpDAALConstants.CENTROID_FILE_NAME);

		// point number in each shad (double precision)
		shadSize = enclave_task_size*1024*1024/(vectorSize*8); 
		harpThreads = Runtime.getRuntime().availableProcessors(); 

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

		LOG.info("Num Centroids " + numCentroids);
		LOG.info("Vector Size " + vectorSize);
		LOG.info("Num Mappers " + num_mappers);
		LOG.info("Num Threads " + numThreads);
		LOG.info("Num Iterations " + numIterations);
		LOG.info("Cen Dir " + cenDir);
		long endTime = System.currentTimeMillis();
		LOG.info("config (ms) :" + (endTime - startTime));
	}

	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
	{
		long startTime = System.currentTimeMillis();
		this.inputFiles = new LinkedList<String>();
		while (reader.nextKeyValue()) 
		{
			String key = reader.getCurrentKey();
			String value = reader.getCurrentValue();
			LOG.info("Key: " + key + ", Value: " + value);
			this.inputFiles.add(value);
		}

		this.datasource = new HarpDAALDataSource(this.harpThreads, this.conf);
		runKmeans(context);
		LOG.info("Total execution time: " + (System.currentTimeMillis() - startTime));
	}

	private void runKmeans(Context context) throws IOException 
	{
		// Load centroids
		Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());
		if (this.isMaster()) 
		{
			createCenTable(cenTable, numCentroids, numCenPars, cenVecSize);
			loadCentroids(cenTable, cenVecSize, this.cenDir + File.separator + this.cenDirInit);
		}
		// Bcast centroids
		bcastCentroids(cenTable, this.getMasterID());

		//use a second read in method
		List<double[][]> pointArrays = this.datasource.loadDenseCSVFilesSharding(this.inputFiles, this.vectorSize, this.shadSize, ",");

		this.timer_start = System.currentTimeMillis();

		if (enablesimu)
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
		if (enablesimu)
		{
			// add overhead of local attestation on a single node
			// for single mapper (process) simulation, we assume that enclave created by threads within the same mapper 
			// still requires local attestation
			// long local_attestation_enclave = (long)((num_mappers-1)*Constants.local_attestation*Constants.ms_per_kcycle);
			long local_attestation_enclave = (long)((comb(numThreads,2)+(num_mappers-1)*numThreads)*Constants.local_attestation*Constants.ms_per_kcycle);
			LOG.info("local_attestation_enclave: " + local_attestation_enclave);
			simuOverhead(local_attestation_enclave);
		}

		this.total_sgx_init += (System.currentTimeMillis() - this.timer_start);

		// calculate tasks
		List<CenCalcTask> cenCalcTasks = new LinkedList<>();
		for (int i = 0; i < numThreads; i++) {
			cenCalcTasks.add(new CenCalcTask(cenTable, cenVecSize, enclave_per_thd_size, this.enablesimu));
		}

		DynamicScheduler<double[][], Object, CenCalcTask> calcCompute = new DynamicScheduler<>(cenCalcTasks);

		// merge tasks
		List<CenMergeTask> tasks = new LinkedList<>();

		for (int i = 0; i < numThreads; i++) {
			tasks.add(new CenMergeTask(calcCompute.getTasks(), this.enablesimu));
		}

		DynamicScheduler<Partition<DoubleArray>, Object, CenMergeTask> mergeCompute = new DynamicScheduler<>(tasks);

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

			if (enablesimu)
			{
				//fetch sgx overhead from every tasks
				long accumu_calc_sgx_ecall = 0;
				long accumu_calc_sgx_ocall = 0;
				long accumu_calc_sgx_mem = 0;

				for (CenCalcTask taskcell : cenCalcTasks) 
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
			// ----------------------------- start interprocess communication ----------------------------
			// regroup-allgather 
			this.timer_start = System.currentTimeMillis();
			regroup("main", "regroup-" + i, cenTable, new Partitioner(this.getNumWorkers()));

			if (enablesimu)
			{
				this.timer_start2 = System.currentTimeMillis();
				//add overhead of cross-enclave communiation
				//data in the enclave of main thread of mapper A transfers to enclave of main thread of mapper B
				//transfer data send (num_mappers-1) + received data (num_mappers-1)
				long transfer_data = (long)((cenTableSize/num_mappers)*(2*num_mappers-2)); 
				LOG.info("transfer_data: " + transfer_data);
				long cross_enclave_overhead = (long)(((num_mappers-1)*(Constants.Ocall + Constants.Ecall))*Constants.ms_per_kcycle);
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
			for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			{
				double[] doubles = partition.get().get();
				int size = partition.get().size();
				for (int j = 0; j < size; j +=cenVecSize) 
				{
					for (int k = 1; k < cenVecSize; k++) 
					{
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


			if (enablesimu)
			{
				this.timer_start2 = System.currentTimeMillis();
				//add overhead of cross-enclave
				//allgather operation
				long transfer_data = (long)(cenTableSize);
				long cross_enclave_overhead = (long)((Constants.Ocall + Constants.Ecall*(num_mappers-1))*Constants.ms_per_kcycle);
				cross_enclave_overhead += (long)(transfer_data*Constants.cross_enclave_per_kb*Constants.ms_per_kcycle);
				LOG.info("allgather cross_enclave_overhead: " + cross_enclave_overhead);

				simuOverhead(cross_enclave_overhead);
				this.total_sgx_comm += (System.currentTimeMillis() - this.timer_start2);
			}

			for (CenCalcTask task : calcCompute.getTasks()) {
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
		if (this.isMaster()) 
		{
			LOG.info("Start to write out centroids.");
			long startTime = System.currentTimeMillis();
			storeCentroids(conf, cenDir, cenTable, cenVecSize, "output");
			long endTime = System.currentTimeMillis();
			LOG.info("Store centroids time (ms): "+ (endTime - startTime));
		}
		cenTable.release();
	}

	private void createCenTable(Table<DoubleArray> cenTable, int numCentroids, int numCenPartitions, int cenVecSize) 
	{
		int cenParSize = numCentroids / numCenPartitions;
		int cenRest = numCentroids % numCenPartitions;
		for (int i = 0; i < numCenPartitions; i++) 
		{
			if (cenRest > 0) {
				int size = (cenParSize + 1) * cenVecSize;
				DoubleArray array = DoubleArray.create(size, false);
				cenTable.addPartition(new Partition<>(i, array));
				cenRest--;
			} else if (cenParSize > 0) {
				int size = cenParSize * cenVecSize;
				DoubleArray array = DoubleArray.create(size, false);
				cenTable.addPartition(new Partition<>(i, array));
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
	private void loadCentroids(Table<DoubleArray> cenTable, int cenVecSize, String cFileName) throws IOException 
	{
		long startTime = System.currentTimeMillis();
		Path cPath = new Path(cFileName);
		FileSystem fs = FileSystem.get(this.conf);
		FSDataInputStream in = fs.open(cPath);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String[] curLine = null;
		int curPos = 0;
		for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
		{
			DoubleArray array = partition.get();
			double[] cData = array.get();
			int start = array.start();
			int size = array.size();
			for (int i = start; i < (start + size); i++) 
			{
				// Don't set the first element in each row
				if (i % cenVecSize != 0) 
				{
					// cData[i] = in.readDouble();
					if (curLine == null || curPos == curLine.length) 
					{
						curLine = br.readLine().split(" ");
						curPos = 0;
					}
					cData[i] = Double.parseDouble(curLine[curPos]);
					curPos++;
				}
			}
		}
		br.close();
		long endTime = System.currentTimeMillis();
		LOG.info("Load centroids (ms): " + (endTime - startTime));
	}

	/**
	 * Broadcast centroids data in partitions
	 * 
	 * @param table
	 * @param numPartitions
	 * @throws IOException
	 */
	private void bcastCentroids(Table<DoubleArray> table, int bcastID) throws IOException 
	{
		long startTime = System.currentTimeMillis();
		boolean isSuccess = false;
		try {
			isSuccess = this.broadcast("main", "broadcast-centroids", table, bcastID, false);
		} catch (Exception e) {
			LOG.error("Fail to bcast.", e);
		}
		long endTime = System.currentTimeMillis();
		LOG.info("Bcast centroids (ms): " + (endTime - startTime));
		if (!isSuccess) {
			throw new IOException("Fail to bcast");
		}
	}

	public void storeCentroids(Configuration configuration, String cenDir, Table<DoubleArray> cenTable, 
			int cenVecSize, String name) throws IOException 
	{
		String cFile = cenDir + File.separator + "out" + File.separator + name;
		Path cPath = new Path(cFile);
		LOG.info("centroids path: " + cPath.toString());
		FileSystem fs = FileSystem.get(configuration);
		fs.delete(cPath, true);
		FSDataOutputStream out = fs.create(cPath);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
		int linePos = 0;
		int[] idArray = cenTable.getPartitionIDs().toArray(new int[0]);
		IntArrays.quickSort(idArray);
		for (int i = 0; i < idArray.length; i++) 
		{
			Partition<DoubleArray> partition = cenTable.getPartition(idArray[i]);
			for (int j = 0; j < partition.get().size(); j++) 
			{
				linePos = j % cenVecSize;
				if (linePos == (cenVecSize - 1)) 
				{
					bw.write(partition.get().get()[j] + "\n");

				} else if (linePos > 0) {
					// Every row with vectorSize + 1 length,
					// the first one is a count,
					// ignore it in output
					bw.write(partition.get().get()[j] + " ");
				}
			}
		}
		bw.flush();
		bw.close();
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
