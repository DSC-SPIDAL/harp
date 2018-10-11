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

package edu.iu.daal_optimization_solvers.LBFGSOptDenseBatch;

import com.intel.daal.algorithms.optimization_solver.iterative_solver.*;
import com.intel.daal.algorithms.optimization_solver.lbfgs.Batch;
import com.intel.daal.algorithms.optimization_solver.lbfgs.Method;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.data_aux.Service;
import edu.iu.datasource.HarpDAALDataSource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

// daal algorithm specific
// daal data structure and service

/**
 * @brief the Harp mapper for running K-means
 */
public class LBFGSOptDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	//cmd args
	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int fileDim;
	private int nFeatures;
	private int nIterations;
	private double stepLength;
	private static double[] initialPoint  = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100};
	private static double[] expectedPoint = { 11,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10};

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
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 11);
		this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 10);
		this.nIterations = this.conf.getInt(HarpDAALConstants.NUM_ITERATIONS, 1000);
		this.stepLength = this.conf.getDouble(HarpDAALConstants.STEP_LENGTH, 1.0e-4);

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("File Dim " + this.fileDim);
		LOG.info("Num Mappers " + this.num_mappers);
		LOG.info("Num Threads " + this.numThreads);
		LOG.info("Num harp load data threads " + harpThreads);

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
			runLBFGSOpt(context);
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
	private void runLBFGSOpt(Context context) throws IOException 
	{
		// ---------- load data ----------
		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);
		NumericTable data = load_table[0];
		NumericTable dependentVariables = load_table[1];

		/* Create an MSE objective function for LBFGS */
		com.intel.daal.algorithms.optimization_solver.mse.Batch mseObjectiveFunction =
			new com.intel.daal.algorithms.optimization_solver.mse.Batch(daal_Context, Double.class,
					com.intel.daal.algorithms.optimization_solver.mse.Method.defaultDense, data.getNumberOfRows());

		mseObjectiveFunction.getInput().set(com.intel.daal.algorithms.optimization_solver.mse.InputId.data, data);
		mseObjectiveFunction.getInput().set(com.intel.daal.algorithms.optimization_solver.mse.InputId.dependentVariables,
				dependentVariables);

		/* Create objects to compute LBFGS result using the default method */
		Batch algorithm = new Batch(daal_Context, Double.class, Method.defaultDense);
		algorithm.parameter.setFunction(mseObjectiveFunction);
		algorithm.parameter.setNIterations(nIterations/2);
		algorithm.parameter.setStepLengthSequence(
				new HomogenNumericTable(daal_Context, Double.class, 1, 1, NumericTable.AllocationFlag.DoAllocate, stepLength));

		algorithm.parameter.setOptionalResultRequired(true);
		algorithm.input.set(InputId.inputArgument, new HomogenNumericTable(daal_Context, initialPoint, 1, nFeatures + 1));

		/* Compute LBFGS result */
		Result result = algorithm.compute();
		Service.printNumericTable("Resulting coefficients after first compute():", result.get(ResultId.minimum));
		Service.printNumericTable("Number of iterations performed:", result.get(ResultId.nIterations));

		algorithm.input.set(InputId.inputArgument, result.get(ResultId.minimum));
		algorithm.input.set(OptionalInputId.optionalArgument, result.get(OptionalResultId.optionalResult));

		/* Compute LBFGS result */
		result = algorithm.compute();

		NumericTable expected = new HomogenNumericTable(daal_Context, expectedPoint, 1, nFeatures + 1);
		Service.printNumericTable("Expected coefficients:",          expected);
		Service.printNumericTable("Resulting coefficients after second compute():", result.get(ResultId.minimum));
		Service.printNumericTable("Number of iterations performed:", result.get(ResultId.nIterations));

		daal_Context.dispose();

	}



}
