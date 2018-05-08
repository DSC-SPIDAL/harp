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

package edu.iu.daal_optimization_solvers.AdaGradientOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.DoubleBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.example.IntArrPlus;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.LongArray;

import edu.iu.datasource.*;
import edu.iu.data_aux.*;

// daal algorithm specific 
import com.intel.daal.algorithms.optimization_solver.adagrad.*;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.Input;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.InputId;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.OptionalInputId;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.Result;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.ResultId;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.OptionalResultId;
import com.intel.daal.algorithms.OptionalArgument;

// daal data structure and service
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class ADAGDOptDaalCollectiveMapper
	extends
	CollectiveMapper<String, String, Object, Object> {

		//cmd args
		private int numMappers;
		private int numThreads;
		private int harpThreads; 
		private long nFeatures;
		private int fileDim;
	        private double accuracyThreshold;
    		private long nIterations;
    		private long batchSize;
    		private double learningRate;
    		private static double[] startPoint = {8, 2, 1, 4};

		private static HarpDAALDataSource datasource;
		private static DaalContext daal_Context = new DaalContext();

		//to measure the time
		private long load_time = 0;
		private long convert_time = 0;
		private long total_time = 0;
		private long compute_time = 0;
		private long comm_time = 0;
		private long ts_start = 0;
		private long ts_end = 0;
		private long ts1 = 0;
		private long ts2 = 0;

		/**
		 * Mapper configuration.
		 */
		@Override
		protected void setup(Context context)
			throws IOException, InterruptedException {

			long startTime = System.currentTimeMillis();

			Configuration configuration =
				context.getConfiguration();

			this.numMappers = configuration.getInt(Constants.NUM_MAPPERS, 10);
			this.numThreads = configuration.getInt(Constants.NUM_THREADS, 10);
			this.harpThreads = Runtime.getRuntime().availableProcessors();
			this.fileDim = configuration.getInt(Constants.FILE_DIM, 4);
			this.nFeatures = configuration.getLong(Constants.FEATURE_DIM, 3);
			this.accuracyThreshold = configuration.getDouble(Constants.ACC_THRESHOLD, 0.0000001);
			this.nIterations = configuration.getLong(Constants.NUM_ITERATIONS, 1000);
			this.batchSize = configuration.getLong(Constants.BATCH_SIZE, 1);
			this.learningRate = configuration.getDouble(Constants.LEARNING_RATE, 1);

			LOG.info("File Dim " + this.fileDim);
			LOG.info("Num Mappers " + this.numMappers);
			LOG.info("Num Threads " + this.numThreads);
			LOG.info("Num harp load data threads " + harpThreads);
			LOG.info("nfeatures " + this.nFeatures);
			LOG.info("accuracyThreshold " + this.accuracyThreshold);
			LOG.info("nIterations " + this.nIterations);
			LOG.info("batchSize " + this.batchSize);
			LOG.info("learningRate " + this.learningRate);

			long endTime = System.currentTimeMillis();
			LOG.info("config (ms) :"
					+ (endTime - startTime));

		}

		// Assigns the reader to different nodes
		protected void mapCollective(
				KeyValReader reader, Context context)
				throws IOException, InterruptedException {
				long startTime = System.currentTimeMillis();

				// read data file names from HDFS
				List<String> dataFiles =
					new LinkedList<String>();
				while (reader.nextKeyValue()) {
					String key = reader.getCurrentKey();
					String value = reader.getCurrentValue();
					LOG.info("Key: " + key + ", Value: "
							+ value);
					LOG.info("file name: " + value);
					dataFiles.add(value);
				}

				Configuration conf = context.getConfiguration();

				// ----------------------- runtime settings -----------------------
				//set thread number used in DAAL
				LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
				Environment.setNumberOfThreads(numThreads);
				LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

				this.datasource = new HarpDAALDataSource(dataFiles, fileDim, harpThreads, conf);

				// ----------------------- start the execution -----------------------
				runADAGDOpt(conf, context);
				this.freeMemory();
				this.freeConn();
				System.gc();
				}

		/**
		 * @brief run Association Rules by invoking DAAL Java API
		 *
		 * @param fileNames
		 * @param conf
		 * @param context
		 *
		 * @return 
		 */
		private void runADAGDOpt(Configuration conf, Context context) throws IOException 
		{
			// ---------- load data ----------
			this.datasource.loadFiles();
			// ---------- training and testing ----------
			/* Create Numeric Tables for data and values for dependent variable */
			NumericTable data = new HomogenNumericTable(daal_Context, Double.class, nFeatures, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
			NumericTable dataDependents = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
			MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
			mergedData.addNumericTable(data);
			mergedData.addNumericTable(dataDependents);

			/* Retrieve the data from an input file */
			this.datasource.loadDataBlock(mergedData);

			/* Create an MSE objective function to compute a Adagrad */
			com.intel.daal.algorithms.optimization_solver.mse.Batch mseFunction =
				new com.intel.daal.algorithms.optimization_solver.mse.Batch(daal_Context, Double.class,
						com.intel.daal.algorithms.optimization_solver.mse.Method.defaultDense, data.getNumberOfRows());

			mseFunction.getInput().set(com.intel.daal.algorithms.optimization_solver.mse.InputId.data, data);
			mseFunction.getInput().set(com.intel.daal.algorithms.optimization_solver.mse.InputId.dependentVariables, dataDependents);

			/* Create algorithm objects to compute Adagrad results */
			Batch adagradAlgorithm = new Batch(daal_Context, Double.class, Method.defaultDense);
			adagradAlgorithm.parameter.setFunction(mseFunction);
			adagradAlgorithm.parameter.setLearningRate(new HomogenNumericTable(daal_Context, Double.class, 1, 1, NumericTable.AllocationFlag.DoAllocate, learningRate));
			adagradAlgorithm.parameter.setNIterations(nIterations);
			adagradAlgorithm.parameter.setAccuracyThreshold(accuracyThreshold);
			adagradAlgorithm.parameter.setBatchSize(batchSize);
			adagradAlgorithm.parameter.setOptionalResultRequired(true);
			adagradAlgorithm.input.set(InputId.inputArgument, new HomogenNumericTable(daal_Context, startPoint, 1, nFeatures + 1));

			/* Compute the Adagrad result for MSE objective function matrix */
			Result result = adagradAlgorithm.compute();

			Service.printNumericTable("Minimum after first compute():",  result.get(ResultId.minimum));
			Service.printNumericTable("Number of iterations performed:",  result.get(ResultId.nIterations));

			adagradAlgorithm.input.set(InputId.inputArgument, result.get(ResultId.minimum));
			adagradAlgorithm.input.set(OptionalInputId.optionalArgument, result.get(OptionalResultId.optionalResult));
			/* Compute the Adagrad result for MSE objective function matrix */
			result = adagradAlgorithm.compute();

			Service.printNumericTable("Minimum after second compute():",  result.get(ResultId.minimum));
			Service.printNumericTable("Number of iterations performed:",  result.get(ResultId.nIterations));

			daal_Context.dispose();

		}



	}
