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

package edu.iu.daal_tutorial.daal_kmeans;

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

        private int pointsPerFile;
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
        pointsPerFile =
            configuration.getInt(
                    Constants.POINTS_PER_FILE, 20);
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
        LOG.info("Points Per File " + pointsPerFile);
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

			this.fileNames = fileNames;
			this.conf = conf;

			// ---------------- load in training data ----------------
			// create a pointArray
			// List<double[]> pointArrays = new LinkedList<>();
			// LoadTrainingData(pointArrays);
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
            //
			// Bcast centroids to other mappers
			bcastCentroids(cenTable, this.getMasterID());
			// convert training data fro harp to daal
			NumericTable trainingdata_daal = convertTrainData(pointArrays);
            //
			// create a daal kmeans kernel object
			DistributedStep1Local kmeansLocal = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense, this.numCentroids);
			// set up input training data
			kmeansLocal.input.set(InputId.data, trainingdata_daal);
            //
			// specify the threads used in DAAL kernel
			Environment.setNumberOfThreads(numThreads);
            //
			// create cenTable at daal side
			NumericTable cenTable_daal = createCenTableDAAL();

			// start the iteration
            for (int i = 0; i < numIterations; i++) {

				//Convert Centroids data from Harp to DAAL
				convertModelData(cenTable, cenTable_daal);
				// specify centroids data to daal kernel 
				kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);
				// first step of local computation by using DAAL kernels to get partial result
				PartialResult pres = kmeansLocal.compute();
				// comm by regroup-allgather
				// comm_regroup_allgather(cenTable, pres);
				// comm by allreduce
				// comm_allreduce(cenTable, pres);
				// comm by broadcast & reduce
				// comm_broadcastreduce(cenTable, pres);
				// comm by push and pull
				comm_push_pull(cenTable, pres);

				printTableRow(cenTable, 0, 10); 
			}
            //
			// // free daal side memory
            cenTable_daal.freeDataMemory();
            trainingdata_daal.freeDataMemory();

            // // Load centroids
            // Table<DoubleArray> cenTable =
            //     new Table<>(0, new DoubleArrPlus());
            // if (this.isMaster()) {
            //     createCenTable(cenTable, numCentroids,
            //             numCenPars, cenVecSize);
            //     loadCentroids(cenTable, cenVecSize, cenDir
            //             + File.separator
            //             + Constants.CENTROID_FILE_NAME, conf);
            // }
            //
			// // a global table used in push-pull operation
			// Table<DoubleArray> globalTable = new Table<DoubleArray>(0, new DoubleArrPlus());
            //
            // // Bcast centroids
            // bcastCentroids(cenTable, this.getMasterID());
            //
            // //pointArrays are used in daal table with feature dimension to be
            // //vectorSize instead of cenVecSize
            // List<double[]> pointArrays =
            //     KMUtil.loadPoints(fileNames, pointsPerFile,
            //             vectorSize, conf, numThreads);
            //
            // //---------------- convert cenTable and pointArrays to Daal table ------------------
            // //create the daal table for pointsArrays
            // long nFeature = vectorSize;
            // long totalLength = 0;
            //
            // long[] array_startP = new long[pointArrays.size()];
            // double[][] array_data = new double[pointArrays.size()][];
            //
            // for(int k=0;k<pointArrays.size();k++)
            // {
            //     array_data[k] = pointArrays.get(k);
            //     array_startP[k] = totalLength;
            //     totalLength += pointArrays.get(k).length;
            // }
            //
            // //create daal table for cenTable first 
            // long nFeature_cen = vectorSize;
            // long totalLength_cen = numCentroids*nFeature_cen;
            // long tableSize_cen = totalLength_cen/nFeature_cen;
            // NumericTable cenTable_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature_cen, tableSize_cen, NumericTable.AllocationFlag.DoAllocate);
            // double[] buffer_array_cen = new double[(int)totalLength_cen]; 
            //
            // long tableSize = totalLength/nFeature;
            // NumericTable pointsArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);
            //
            // int row_idx = 0;
            // int row_len = 0;
            // for (int k=0; k<pointArrays.size(); k++) 
            // {
            //     row_len = (array_data[k].length)/(int)nFeature;
            //     DoubleBuffer array_data_buf = DoubleBuffer.wrap(array_data[k]);
            //     pointsArray_daal.releaseBlockOfRows(row_idx, row_len, array_data_buf);
            //     row_idx += row_len;
            // }
            //
            // //create the algorithm DistributedStep1Local 
            // //to accomplish the first step of computing distances between training points and centroids
            // DistributedStep1Local kmeansLocal = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense, numCentroids);
            // kmeansLocal.input.set(InputId.data, pointsArray_daal);
            //
            // // ----------------------------------------------------- For iterations -----------------------------------------------------
            // // set up the maximal threads used by DAAL kernels
            // LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            // Environment.setNumberOfThreads(numThreads);
            // LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            //
            // for (int i = 0; i < numIterations; i++) {
            //
            //     LOG.info("Iteration: " + i);
            //
			// 	
            //     long t1 = System.currentTimeMillis();
            //
            //     long[] array_startP_cen = new long[cenTable.getNumPartitions()];
            //     long[] sentinel_startP_cen = new long[cenTable.getNumPartitions()];
            //     double[][] array_data_cen = new double[cenTable.getNumPartitions()][];
            //
            //     int ptr = 0;
            //     long startP = 0;
            //     long sentinel_startP = 0;
            //     for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			// 	{
            //         array_data_cen[ptr] = partition.get().get();
            //         array_startP_cen[ptr] = startP;
            //         sentinel_startP_cen[ptr] = sentinel_startP;
            //         long increment = ((array_data_cen[ptr].length)/(cenVecSize));
            //         sentinel_startP += increment;
            //         startP += (increment*nFeature_cen);
            //         ptr++;
            //     }
            //
			// 	//convert data from harp side to daal side
            //     convertTrainPtHarpToDAAL(cenTable_daal, nFeature_cen,  cenTable.getNumPartitions(), tableSize_cen,
			// 							 array_startP_cen, array_data_cen, buffer_array_cen);
            //
            //     kmeansLocal.input.set(InputId.inputCentroids, cenTable_daal);
            //
			// 	// first step of local computation by using DAAL kernels
            //     long t2 = System.currentTimeMillis();
            //     PartialResult pres = kmeansLocal.compute();
            //     long t3 = System.currentTimeMillis();
            //
			// 	// start inter-node (mapper) communication
            //     // partialSum: double array vectorSize*numCentroids 
            //     double[] partialSum = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.partialSums)).getDoubleArray(); 
            //     // nObservations: int array numCentroids 
            //     double[] nObservations = (double[]) ((HomogenNumericTable)pres.get(PartialResultId.nObservations)).getDoubleArray();
            //
			// 	//convert daal partial result to harp table
            //     convertCenTableDAALToHarp(nFeature_cen, cenTable.getNumPartitions(), sentinel_startP_cen,
			// 			array_startP_cen, array_data_cen, partialSum, nObservations);
            //
            //     long t4 = System.currentTimeMillis();
            //
			// 	// start the inter-node communication 
			// 	long[] avg_timer = new long[2];
            //     // choice one: regroup-allgather
			// 	// comm_regroup_allgather(cenTable, avg_timer, i);
			// 	// choice two: allreduce
			// 	// comm_allreduce(cenTable, avg_timer, i);
			// 	// choice three: reduce plus broadcast
			// 	// comm_broadcastreduce(cenTable, avg_timer, i);
			// 	// choice four: push and pull
			// 	comm_push_pull(cenTable, globalTable, avg_timer, i);
            //
			// 	long t5 = avg_timer[0];
			// 	long t6 = avg_timer[1];
            //     long t7 = System.currentTimeMillis();
            //
            //     train_time += (t7 - t1);
            //     compute_time += ((t3 -t2) + (t6 - t5));
            //     convert_time += ((t2- t1) + (t4 - t3));
            //     comm_time += ((t5 - t4) + (t7 - t6));
            //
			// 	// printout the results
			// 	// printTable(cenTable, 10, 10); 
			// 	// printTableRow(cenTable, 0, 10); 
            //
            //     LOG.info("Compute: " + ((t3 -t2) + (t6 - t5))
            //             + ", Convert: " + ((t2- t1) + (t4 - t3))
            //             + ", Aggregate: " + ((t5 - t4) + (t7 - t6)));
            //     logMemUsage();
            //     logGCTime();
            //     context.progress();
            //
            // }//for iteration
            //
            // //After the iteration, free the cenTable
            // cenTable_daal.freeDataMemory();
            //
            // LOG.info("Time Summary Per Itr: Training: " + train_time/numIterations + 
            //         " Compute: " + compute_time/numIterations + 
            //         " Convert: " + convert_time/numIterations +
            //         " Comm: " + comm_time/numIterations);
            //
            // trainingdata_daal.freeDataMemory();
            // // Write out centroids
            // if (this.isMaster()) {
            //     LOG.info("Start to write out centroids.");
            //     long startTime = System.currentTimeMillis();
            //     KMUtil.storeCentroids(conf, cenDir,
            //             cenTable, cenVecSize, "output");
            //     long endTime = System.currentTimeMillis();
            //     LOG.info("Store centroids time (ms): "
            //             + (endTime - startTime));
            // }
            // cenTable.release();
        }


		//start all the member functions needed by the tutorial session

		/**
		 * @brief Load Training datasets from HDFS to harp
		 *
		 * @param pointArrays
		 *
		 * @return 
		 */
		// private void LoadTrainingData(List<double[]> pointArrays)
		private List<double[]> LoadTrainingData()
		{
			List<double[]> pointArrays = KMUtil.loadPoints(this.fileNames, this.pointsPerFile,
                        this.vectorSize, this.conf, this.numThreads);
			LOG.info("read in pointArray size: " + pointArrays.size());
			return pointArrays;
		}

		/**
         * @brief generate the Harp side centroid table
         *
         * @param cenTable
         * @param numCentroids
         * @param numCenPartitions
         * @param cenVecSize
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
         * Fill data from centroid file to cenDataMap
         * 
         * @param cenDataMap
         * @param vectorSize
         * @param cFileName
         * @param configuration
         * @throws IOException
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


		private void convertModelData(Table<DoubleArray> cenTable, NumericTable cenTable_daal)
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

		private void comm_regroup_allgather(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			regroup("main", "regroup", cenTable, new Partitioner(this.getNumWorkers()));
			calculateAvgCenTable(cenTable);
			allgather("main", "allgather", cenTable);
		}

		private void comm_allreduce(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			allreduce("main", "allreduce", cenTable);
			calculateAvgCenTable(cenTable);
		}

		private void comm_broadcastreduce(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			reduce("main", "reduce", cenTable, this.getMasterID());

			if (this.isMaster())
				calculateAvgCenTable(cenTable);

			broadcast("main", "bcast", cenTable, this.getMasterID(), false);
		}

		//NewMemFunc
		private void comm_push_pull(Table<DoubleArray> cenTable, PartialResult pres)
		{
			convertCenTableDAALToHarp(cenTable, pres);
			Table<DoubleArray> globalTable = new Table<DoubleArray>(0, new DoubleArrPlus());

			push("main", "push", cenTable, globalTable, new Partitioner(this.getNumWorkers()));
			calculateAvgCenTable(globalTable);

			pull("main", "pull", cenTable, globalTable, true);

		}

		private void calculateAvgCenTable_old(Table<DoubleArray> cenTable)
		{
			//calculate the average value in multi-threading
			int num_partition = cenTable.getNumPartitions();
			Thread[] threads_avg = new Thread[num_partition];

			int thread_id = 0;
			for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			{
				DoubleArray array = partition.get();
				threads_avg[thread_id] = new Thread(new TaskAvgCalc(cenVecSize, array));
				threads_avg[thread_id].start();
				thread_id++;
			}

			for (int q = 0; q< num_partition; q++) {

				try
				{
					threads_avg[q].join();

				}catch(InterruptedException e)
				{
					System.out.println("Thread interrupted.");
				}
			}

		}


		private void calculateAvgCenTable(Table<DoubleArray> cenTable)
		{

			for (Partition<DoubleArray> partition : cenTable.getPartitions()) 
			{
				DoubleArray array = partition.get();
				double[] doubles = array.get();

				for (int j = 0; j < doubles.length; j += cenVecSize) 
				{
					if (doubles[j] != 0) 
					{
						for (int k = 1; k < cenVecSize; k++) {
							doubles[j + k] /= doubles[j];
						}
					}
				}
			}

		}
		

		// for testing
		private void printTable(Table<DoubleArray> dataTable, int row, int dim) 
		{
			int row_print = (row < dataTable.getNumPartitions()) ? row: dataTable.getNumPartitions();
			int row_count = 0;

			int col_print = 0; 
			for (Partition<DoubleArray> ap : dataTable.getPartitions()) 
			{
				// only print out a specified number of centroids
				if (row_count < row)
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

		private void printTableRow(Table<DoubleArray> dataTable, int row_id, int dim) 
		{

			int col_print = 0; 
			// only print out a specified number of centroids
			Partition<DoubleArray> ap = dataTable.getPartition(row_id);
			if (ap != null)
			{
				double res[] = ap.get().get();
				System.out.print("ID: " + ap.id() + ":");
				col_print = (dim < res.length) ? dim : res.length;
				for (int i = 0; i < col_print; i++)
					System.out.print(res[i] + "\t");
				System.out.println();
			}
		}
	}
