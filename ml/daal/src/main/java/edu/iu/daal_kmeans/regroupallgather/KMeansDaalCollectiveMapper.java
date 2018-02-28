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

package edu.iu.daal_kmeans.regroupallgather;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

import edu.iu.daal.*;
import java.nio.DoubleBuffer;

//import daa.jar API
import com.intel.daal.algorithms.kmeans.*;
import com.intel.daal.algorithms.kmeans.init.*;

import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class KMeansDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

        // private int pointsPerFile;
        private int numCentroids;
        private int vectorSize;
        private int numCenPars;
        private int cenVecSize;
        private int numMappers;
        private int numThreads;
        private int numIterations;
		private Configuration conf;
		private List<String> fileNames;
        private String cenDir;

		private long[] array_startP_cen;
        private long[] sentinel_startP_cen;
        private double[][] array_data_cen;

		private double[] buffer_array_cen;
		private Table<DoubleArray> pushpullGlobal = null;


        //to measure the time
        private long convert_time = 0;
        private long train_time = 0;
        private long compute_time = 0;
        private long comm_time = 0;

        private static DaalContext daal_Context = new DaalContext();

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        Configuration configuration =
            context.getConfiguration();
        // pointsPerFile =
        //     configuration.getInt(
        //             Constants.POINTS_PER_FILE, 20);
        numCentroids =
            configuration.getInt(
                    Constants.NUM_CENTROIDS, 20);
        vectorSize =
            configuration.getInt(Constants.VECTOR_SIZE,
                    20);
        numMappers =
            configuration.getInt(Constants.NUM_MAPPERS,
                    10);
        cenVecSize = vectorSize + 1;
        numThreads =
            configuration.getInt(Constants.NUM_THREADS,
                    10);
        numCenPars = numThreads;
        numIterations =
            configuration.getInt(
                    Constants.NUM_ITERATIONS, 10);
        cenDir = configuration.get(Constants.CEN_DIR);
        // LOG.info("Points Per File " + pointsPerFile);
        LOG.info("Num Centroids " + numCentroids);
        LOG.info("Vector Size " + vectorSize);
        LOG.info("Num Mappers " + numMappers);
        LOG.info("Num Threads " + numThreads);
        LOG.info("Num Iterations " + numIterations);
        LOG.info("Cen Dir " + cenDir);
        long endTime = System.currentTimeMillis();
        LOG.info("config (ms) :"
                + (endTime - startTime));
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
                LOG.info("Key: " + key + ", Value: "
                        + value);
                pointFiles.add(value);
            }
            Configuration conf =
                context.getConfiguration();
            runKmeans(pointFiles, conf, context);
            LOG.info("Total iterations in master view: "
                    + (System.currentTimeMillis() - startTime));
                }

        /**
         * @brief run K-means by invoking DAAL Java API
         *
         * @param fileNames
         * @param conf
         * @param context
         *
         * @return 
         */
        private void runKmeans(List<String> fileNames,
                Configuration conf, Context context)
            throws IOException {

			long start_execution = System.currentTimeMillis();
			this.fileNames = fileNames;
			this.conf = conf;

			// ---------------- load in training data ----------------
			// create a pointArray
			List<double[]> pointArrays = LoadTrainingData();

			// ---------- load in centroids (model) data ----------
			// create a table to hold centroids data
			Table<DoubleArray> cenTable = new Table<>(0, new DoubleArrPlus());
            //
			if (this.isMaster()) 
			{
				createCenTable(cenTable);
				loadCentroids(cenTable);
			}
			// Bcast centroids to other mappers
			bcastCentroids(cenTable, this.getMasterID());
			// convert training data fro harp to daal
			NumericTable trainingdata_daal = convertTrainData(pointArrays);
			// create a daal kmeans kernel object
			DistributedStep1Local kmeansLocal = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense, this.numCentroids);
			// set up input training data
			kmeansLocal.input.set(InputId.data, trainingdata_daal);
			// specify the threads used in DAAL kernel
			Environment.setNumberOfThreads(numThreads);
			// create cenTable at daal side
			NumericTable cenTable_daal = createCenTableDAAL();

			// start the iteration
            for (int i = 0; i < numIterations; i++) {

				//Convert Centroids data from Harp to DAAL
				printTable(cenTable, 10, 10, i); 
				convertCenTableHarpToDAAL(cenTable, cenTable_daal);
				// specify centroids data to daal kernel 
				kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);
				// first step of local computation by using DAAL kernels to get partial result
				PartialResult pres = kmeansLocal.compute();
				// comm by regroup-allgather
				comm_regroup_allgather(cenTable, pres);
				// comm_allreduce(cenTable, pres);
				// comm_broadcastreduce(cenTable, pres);
				// comm_push_pull(cenTable, pres);
			}
            
			// free memory and record time
            cenTable_daal.freeDataMemory();
            trainingdata_daal.freeDataMemory();

			// Write out centroids
			if (this.isMaster()) {
				KMUtil.storeCentroids(this.conf, this.cenDir,
						cenTable, this.cenVecSize, "output");
			}
		
			cenTable.release();
			LOG.info("Execution Time: " + (System.currentTimeMillis() - start_execution));

        }
		
		/**
		 * @brief Load training data from hdfs to harp
		 *
		 * @return linkedlist of data points (vector) 
		 */
		private List<double[]> LoadTrainingData()
		{
			List<double[]> pointArrays = KMUtil.loadPoints(this.fileNames,
                        this.vectorSize, this.conf, this.numThreads);
			LOG.info("read in pointArray size: " + pointArrays.size());
			return pointArrays;
		}

		
		/**
		 * @brief create a harp table to hold centroids
		 *
		 * @param cenTable
		 *
		 * @return 
		 */
        private void createCenTable(Table<DoubleArray> cenTable)
		{

			int cenParSize =
				this.numCentroids / this.numCenPars;

			int cenRest = this.numCentroids % this.numCenPars;

			for (int i = 0; i < this.numCenPars; i++) {
				if (cenRest > 0) {
					int size = (cenParSize + 1) * this.cenVecSize;
					DoubleArray array =
						DoubleArray.create(size, false);
					cenTable.addPartition(new Partition<>(i,
								array));
					cenRest--;
				} else if (cenParSize > 0) {
					int size = cenParSize * cenVecSize;
					DoubleArray array =
						DoubleArray.create(size, false);
					cenTable.addPartition(new Partition<>(i,
								array));
				} else {
					break;
				}
			}
		}

		
		/**
		 * @brief load centroids data from hdfs to harp table
		 *
		 * @param cenTable
		 *
		 * @return 
		 */
        private void loadCentroids(Table<DoubleArray> cenTable) throws IOException 
		{

			String cFileName = this.cenDir + File.separator + Constants.CENTROID_FILE_NAME;

			long startTime = System.currentTimeMillis();
			Path cPath = new Path(cFileName);
			FileSystem fs = FileSystem.get(this.conf);
			FSDataInputStream in = fs.open(cPath);
			BufferedReader br =
				new BufferedReader(
						new InputStreamReader(in));
			String[] curLine = null;
			int curPos = 0;
			for (Partition<DoubleArray> partition : cenTable
					.getPartitions()) {
				DoubleArray array = partition.get();
				double[] cData = array.get();
				int start = array.start();
				int size = array.size();
				for (int i = start; i < (start + size); i++) {
					// Don't set the first element in each row
					if (i % cenVecSize != 0) {
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
		 * @brief broadcast centroids data from master mapper to other 
		 * mappers
		 *
		 * @param table
		 * @param bcastID
		 *
		 * @return 
		 */
        private void bcastCentroids(
                Table<DoubleArray> table, int bcastID)
            throws IOException {
            long startTime = System.currentTimeMillis();
            boolean isSuccess = false;
            try {
                isSuccess =
                    this.broadcast("main",
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
		 * @brief convert training data from harp to daal
		 *
		 * @param pointArrays
		 *
		 * @return 
		 */
		private NumericTable convertTrainData(List<double[]> pointArrays)
		{

			//---------------- convert cenTable and pointArrays to Daal table ------------------
			LOG.info("Convert pointArray size: " + pointArrays.size());

			//create the daal table for pointsArrays
			long nFeature = this.vectorSize;
			long totalLength = 0;
			//
			long[] array_startP = new long[pointArrays.size()];
			double[][] array_data = new double[pointArrays.size()][];
			//
			for(int k=0;k<pointArrays.size();k++)
			{
			    array_data[k] = pointArrays.get(k);
			    array_startP[k] = totalLength;
			    totalLength += pointArrays.get(k).length;
			}
			 
			long tableSize = totalLength/nFeature;
			LOG.info("Table size of training daal table: " + tableSize);
			NumericTable pointsArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);

			int row_idx = 0;
			int row_len = 0;
			for (int k=0; k<pointArrays.size(); k++) 
			{
			    row_len = (array_data[k].length)/(int)nFeature;
			    DoubleBuffer array_data_buf = DoubleBuffer.wrap(array_data[k]);
			    pointsArray_daal.releaseBlockOfRows(row_idx, row_len, array_data_buf);
			    row_idx += row_len;
			}

			return pointsArray_daal;
		}


		/**
		 * @brief create centroid table at DAAL side
		 *
		 * @return NumericTable at DAAL side 
		 */
		private NumericTable createCenTableDAAL()
		{
			//create daal table for cenTable first 
			long nFeature_cen = this.vectorSize;
			long totalLength_cen = this.numCentroids*nFeature_cen;
			long tableSize_cen = totalLength_cen/nFeature_cen;
			NumericTable cenTable_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature_cen, tableSize_cen, NumericTable.AllocationFlag.DoAllocate);
			this.buffer_array_cen = new double[(int)totalLength_cen];
			return cenTable_daal;
		}


		/**
		 * @brief convert model data (centroids) from harp to DAAL
		 *
		 * @param cenTable
		 * @param cenTable_daal
		 *
		 * @return 
		 */
		private void convertCenTableHarpToDAAL(Table<DoubleArray> cenTable, NumericTable cenTable_daal)
		{

			long nFeature_cen = cenTable_daal.getNumberOfColumns(); 
			long tableSize_cen = cenTable_daal.getNumberOfRows();
			int cenTableNumPartition = cenTable.getNumPartitions();

			this.array_startP_cen = new long[cenTable.getNumPartitions()];
			this.sentinel_startP_cen = new long[cenTable.getNumPartitions()];
			this.array_data_cen = new double[cenTable.getNumPartitions()][];

			int ptr = 0;
			long startP = 0;
			long sentinel_startP = 0;
			for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			{
				array_data_cen[ptr] = partition.get().get();
				array_startP_cen[ptr] = startP;
				sentinel_startP_cen[ptr] = sentinel_startP;
				long increment = ((array_data_cen[ptr].length)/(this.cenVecSize));
				sentinel_startP += increment;
				startP += (increment*nFeature_cen);
				ptr++;
			}
			
			Arrays.fill(this.buffer_array_cen, 0);
			Thread[] threads_cen = new Thread[this.numThreads];

			//parallel copy partitions of cenTale into an entire primitive array
			for (int q = 0; q<this.numThreads; q++) 
			{
				threads_cen[q] = new Thread(new TaskSentinelListToBufferDouble(q, this.numThreads, (int)nFeature_cen, cenTableNumPartition, array_startP_cen, array_data_cen, this.buffer_array_cen));
				threads_cen[q].start();
			}

			for (int q = 0; q< this.numThreads; q++) {

				try
				{
					threads_cen[q].join();

				}catch(InterruptedException e)
				{
					System.out.println("Thread interrupted.");
				}
			}

			//release the array into daal side cenTable
			DoubleBuffer array_cen_buf = DoubleBuffer.wrap(this.buffer_array_cen);
			cenTable_daal.releaseBlockOfRows(0, tableSize_cen, array_cen_buf);

		}


		/**
		 * @brief Convert daal locally computed partial result to harp 
		 * centroids table
		 *
		 * @param cenTable
		 * @param pres
		 *
		 * @return 
		 */
		private void convertCenTableDAALToHarp(Table<DoubleArray> cenTable, PartialResult pres)
		{

			int cenTableNumPartition = cenTable.getNumPartitions();
			double[] partialSum = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.partialSums)).getDoubleArray();
            double[] nObservations = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.nObservations)).getDoubleArray();

			//copy partialSum and nObservations back to cenTable
			Thread[] threads_cen = new Thread[this.numThreads];
			for (int q = 0; q<this.numThreads; q++) 
			{
				threads_cen[q] = new Thread(new TaskSentinelListUpdateDouble(q, this.numThreads, this.vectorSize, cenTableNumPartition, 
							this.array_startP_cen, this.sentinel_startP_cen, this.array_data_cen, partialSum, nObservations));
				threads_cen[q].start();
			}

			for (int q = 0; q< this.numThreads; q++) {

				try
				{
					threads_cen[q].join();

				}catch(InterruptedException e)
				{
					System.out.println("Thread interrupted.");
				}
			}
		}

		/**
		 * @brief comm centroids data among mappers by using regroup and allgather operations
		 *
		 * @param cenTable
		 * @param pres
		 *
		 * @return 
		 */
		private void comm_regroup_allgather(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			regroup("main", "regroup", cenTable, new Partitioner(this.getNumWorkers()));
			calculateAvgCenTable(cenTable);
			allgather("main", "allgather", cenTable);
			this.barrier("main", "regroupallgather-sync");
		}

		/**
		 * @brief comm centroids data among mappers by using an allreduce operation 
		 *
		 * @param cenTable
		 * @param pres
		 *
		 * @return 
		 */
		private void comm_allreduce(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			allreduce("main", "allreduce", cenTable);
			this.barrier("main", "allreduce-sync");
			calculateAvgCenTable(cenTable);
		}

		/**
		 * @brief comm centroids data among mappers by first reducing centroids to the master 
		 * mapper, then averaged centroids broadcasted to all the mappers
		 *
		 * @param cenTable
		 * @param pres
		 *
		 * @return 
		 */
		private void comm_broadcastreduce(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			reduce("main", "reduce", cenTable, this.getMasterID());

			if (this.isMaster())
				calculateAvgCenTable(cenTable);

			broadcast("main", "bcast", cenTable, this.getMasterID(), false);
			this.barrier("main", "braodcast-sync");
		}

		/**
		 * @brief clean contents of dataTable
		 * used in push-pull approach 
		 * @param dataTable
		 *
		 * @return 
		 */
		private void cleanTableContent(Table<DoubleArray> dataTable)
		{
			for(Partition<DoubleArray> ap : dataTable.getPartitions())
				Arrays.fill(ap.get().get(), 0);
		}

		/**
		 * @brief comm centroids data among mappers by first push local centroids table to a 
		 * global table, then the averaged centroids in global table being pulled by 
		 * all the local mappers
		 *
		 * @param cenTable
		 * @param pres
		 *
		 * @return 
		 */
		private void comm_push_pull(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			if (pushpullGlobal == null)
			{
				pushpullGlobal = new Table<>(0, new DoubleArrPlus());
				for(Partition<DoubleArray> ap : cenTable.getPartitions())
				{
					if (ap.id() % this.numMappers == this.getSelfID())
					{
						DoubleArray dummy = DoubleArray.create(ap.get().get().length, false);
						pushpullGlobal.addPartition(new Partition<>(ap.id(), dummy));
					}
				}
			}
			else
				cleanTableContent(pushpullGlobal);

			push("main", "push", cenTable, pushpullGlobal, new Partitioner(this.getNumWorkers()));

			calculateAvgCenTable(pushpullGlobal);

			//clean centable befor pull
			cleanTableContent(cenTable);
			
			pull("main", "pull", cenTable, pushpullGlobal, false);
			this.barrier("main", "pullsync");

		}

		/**
		 * @brief calculate the centroids by averaging the values from all
		 * the mappers
		 *
		 * @param cenTable
		 *
		 * @return 
		 */
		private void calculateAvgCenTable(Table<DoubleArray> cenTable)
		{

			LinkedList<AvgCalcTask> avg_tasks = new LinkedList<>();
            for (int i = 0; i < this.numThreads; i++) {
                avg_tasks.add(new AvgCalcTask(this.cenVecSize));
            }

            DynamicScheduler<DoubleArray, Object, AvgCalcTask> avg_compute =
                new DynamicScheduler<>(avg_tasks);

			for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			{
				DoubleArray array = partition.get();
				avg_compute.submit(array);
			}

            avg_compute.start();
            avg_compute.stop();

            while (avg_compute.hasOutput()) {
                avg_compute.waitForOutput();
            }

		}
		
		/**
		 * @brief print out the centroids table 
		 *
		 * @param dataTable
		 * @param row number of rows to print
		 * @param dim number of dimension to print 
		 * @param iter iteration id
		 *
		 * @return 
		 */
		private void printTable(Table<DoubleArray> dataTable, int row, int dim, int iter) 
		{
			// print header
			System.out.println("Centroids values in iteration: " + iter);

			int row_print = (row < dataTable.getNumPartitions()) ? row: dataTable.getNumPartitions();
			int col_print = 0; 

			for(int i=0; i<row_print; i++)
			{
				Partition<DoubleArray> ap = dataTable.getPartition(i);
				if (ap != null)
				{
					double res[] = ap.get().get();
					System.out.print("ID: " + ap.id() + ": ");
					System.out.flush();
					col_print = (dim < res.length) ? dim : res.length;
					for (int j = 0; j < col_print; j++)
						System.out.print(res[j] + "\t");
					System.out.println();
				}
			}
		}

		private void printTableAll(Table<DoubleArray> dataTable, int dim, int iter) 
		{
			// print header
			System.out.println("Centroids values in iteration: " + iter);
			int col_print = 0; 

			// for(int i=0; i<row_print; i++)
			// {
				for (Partition<DoubleArray> ap : dataTable.getPartitions())
				{
					double res[] = ap.get().get();
					System.out.print("ID: " + ap.id() + ": ");
					System.out.flush();
					col_print = (dim < res.length) ? dim : res.length;
					for (int j = 0; j < col_print; j++)
						System.out.print(res[j] + "\t");
					System.out.println();
				}
			// }
		}
		/**
		 * @brief print out a single row of centroids table (a single centroid)
		 *
		 * @param dataTable
		 * @param row_id the specified centroid id 
		 * @param dim the dimension to print
		 *
		 * @return 
		 */
		private void printTableRow(Table<DoubleArray> dataTable, int row_id, int dim) 
		{
			int col_print = 0; 
			// only print out a specified number of centroids
			Partition<DoubleArray> ap = dataTable.getPartition(row_id);
			if (ap != null)
			{
				double res[] = ap.get().get();
				System.out.print("ID: " + ap.id() + ": ");
				System.out.flush();
				col_print = (dim < res.length) ? dim : res.length;
				for (int i = 0; i < col_print; i++)
					System.out.print(res[i] + "\t");
				System.out.println();
			}
		}
	}
