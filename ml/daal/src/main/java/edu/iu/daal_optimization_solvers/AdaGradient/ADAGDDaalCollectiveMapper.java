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

package edu.iu.daal_optimization_solvers.AdaGradient;

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
import com.intel.daal.algorithms.optimization_solver.iterative_solver.InputId;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.Result;
import com.intel.daal.algorithms.optimization_solver.iterative_solver.ResultId;

// daal data structure and service
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class ADAGDDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	//cmd args
	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int nFeatures;
	private int fileDim;
	private double accuracyThreshold;
	private long nIterations;
	private long batchSize;
	private double learningRate;
	private static double[] startPoint = {8, 2, 1, 4};
	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context)
		throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();

		this.conf = context.getConfiguration();

		this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
		this.harpThreads = Runtime.getRuntime().availableProcessors();
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 4);
		this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 3);
		this.accuracyThreshold = this.conf.getDouble(HarpDAALConstants.ACC_THRESHOLD, 0.0000001);
		this.nIterations = this.conf.getLong(HarpDAALConstants.NUM_ITERATIONS, 1000);
		this.batchSize = this.conf.getLong(HarpDAALConstants.BATCH_SIZE, 1);
		this.learningRate = this.conf.getDouble(HarpDAALConstants.LEARNING_RATE, 1);

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("File Dim " + this.fileDim);
		LOG.info("Num Mappers " + this.num_mappers);
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
			this.inputFiles =
				new LinkedList<String>();
			while (reader.nextKeyValue()) {
				String key = reader.getCurrentKey();
				String value = reader.getCurrentValue();
				LOG.info("Key: " + key + ", Value: "
						+ value);
				LOG.info("file name: " + value);
				this.inputFiles.add(value);
			}

			

			this.datasource = new HarpDAALDataSource(harpThreads, conf);

			// ----------------------- start the execution -----------------------
			runADAGD(context);
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
	private void runADAGD(Context context) throws IOException 
	{
		// ---------- load data ----------
		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);
		NumericTable data = load_table[0];
		NumericTable dataDependents = load_table[1];

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
		adagradAlgorithm.input.set(InputId.inputArgument, new HomogenNumericTable(daal_Context, startPoint, 1, nFeatures + 1));

		/* Compute the Adagrad result for MSE objective function matrix */
		Result result = adagradAlgorithm.compute();

		Service.printNumericTable("Minimum:",  result.get(ResultId.minimum));
		Service.printNumericTable("Number of iterations performed:",  result.get(ResultId.nIterations));

		daal_Context.dispose();

	}



}
