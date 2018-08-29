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

package edu.iu.daal_quality_metrics.LinRegMetrics;

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

// intel daal algorithms 
import com.intel.daal.algorithms.linear_regression.Model;
import com.intel.daal.algorithms.linear_regression.prediction.*;
import com.intel.daal.algorithms.linear_regression.training.*;
import com.intel.daal.algorithms.linear_regression.quality_metric.*;
import com.intel.daal.algorithms.linear_regression.quality_metric_set.*;

// intel daal data structures and services
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class LINREGMESDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	//cmd args
	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int fileDim;

	private int nFeatures;
	private int nDependentVariables;
	private int iBeta1;
	private int iBeta2;

	private List<String> inputFiles;
	private Configuration conf;

	static Model        model;
	static NumericTable trainData;
	static NumericTable expectedResponses;
	static NumericTable predictedResponses;
	static NumericTable predictedReducedModelResponses;
	static ResultCollection qualityMetricSetResult;
	static double[] savedBetas;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
	{

		long startTime = System.currentTimeMillis();

		this.conf = context.getConfiguration();

		this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 10);
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 12);
		this.nDependentVariables = this.conf.getInt(HarpDAALConstants.NUM_DEPVAR, 2);
		this.iBeta1 = this.conf.getInt(Constants.IBETA_ONE, 2);
		this.iBeta2 = this.conf.getInt(Constants.IBETA_TWO, 10);

		this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
		//always use the maximum hardware threads to load in data and convert data 
		this.harpThreads = Runtime.getRuntime().availableProcessors();

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("File Dim " + this.fileDim);
		LOG.info("Num Mappers " + this.num_mappers);
		LOG.info("Num Threads " + this.numThreads);
		LOG.info("Num harp load data threads " + harpThreads);

		long endTime = System.currentTimeMillis();
		LOG.info("config (ms) :" + (endTime - startTime));
	}

	// Assigns the reader to different nodes
	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
	{
		long startTime = System.currentTimeMillis();

		// read data file names from HDFS
		this.inputFiles = new LinkedList<String>();
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
		runLINREGMES(context);
		this.freeMemory();
		this.freeConn();
		System.gc();
	}

	/**
	 * @brief run SVD by invoking DAAL Java API
	 *
	 * @param fileNames
	 * @param conf
	 * @param context
	 *
	 * @return 
	 */
	private void runLINREGMES(Context context) throws IOException 
	{
		// ---------- training and testing ----------
		trainModel();
		testModelQuality();
		printResults();
		daal_Context.dispose();
	}

	private void trainModel() 
	{

		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, this.nFeatures, this.nDependentVariables, ",", this.daal_Context);
		this.trainData = load_table[0]; 
		this.expectedResponses = load_table[1];

		/* Create an algorithm object to train the multiple linear regression model with the normal equations method */
		TrainingBatch linearRegressionTrain = new TrainingBatch(daal_Context, Double.class, TrainingMethod.normEqDense);

		linearRegressionTrain.input.set(TrainingInputId.data, trainData);
		linearRegressionTrain.input.set(TrainingInputId.dependentVariable, expectedResponses);

		/* Build the multiple linear regression model */
		TrainingResult trainingResult = linearRegressionTrain.compute();

		model = trainingResult.get(TrainingResultId.model);
	}

	private NumericTable predictResults() 
	{

		/* Create algorithm objects to predict values of multiple linear regression with the default method */
		PredictionBatch linearRegressionPredict = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense);

		linearRegressionPredict.input.set(PredictionInputId.data, trainData);
		linearRegressionPredict.input.set(PredictionInputId.model, model);

		/* Compute prediction results */
		PredictionResult predictionResult = linearRegressionPredict.compute();

		return predictionResult.get(PredictionResultId.prediction);
	}

	private void reduceModel() 
	{
		final int nBeta = (int)model.getNumberOfBetas();
		savedBetas = new double[nBeta * nDependentVariables];

		/* Read a block of rows */
		DoubleBuffer betas = DoubleBuffer.allocate(nBeta * nDependentVariables);
		betas = model.getBeta().getBlockOfRows(0, nDependentVariables, betas);
		savedBetas[iBeta1] = betas.get(iBeta1);
		savedBetas[iBeta2] = betas.get(iBeta2);
		savedBetas[iBeta1 + nBeta] = betas.get(iBeta1 + nBeta);
		savedBetas[iBeta2 + nBeta] = betas.get(iBeta2 + nBeta);
		betas.put(iBeta1, 0);
		betas.put(iBeta2, 0);
		betas.put(iBeta1 + nBeta, 0);
		betas.put(iBeta2 + nBeta, 0);
		model.getBeta().releaseBlockOfRows(0, nDependentVariables, betas);
	}

	private void restoreModel() 
	{
		final int nBeta = (int)model.getNumberOfBetas();

		/* Read a block of rows */
		DoubleBuffer betas = DoubleBuffer.allocate(nBeta * nDependentVariables);
		betas = model.getBeta().getBlockOfRows(0, nDependentVariables, betas);
		betas.put(iBeta1, savedBetas[iBeta1]);
		betas.put(iBeta2, savedBetas[iBeta2]);
		betas.put(iBeta1 + nBeta, savedBetas[iBeta1 + nBeta]);
		betas.put(iBeta2 + nBeta, savedBetas[iBeta2 + nBeta]);
		model.getBeta().releaseBlockOfRows(0, nDependentVariables, betas);
	}

	private void testModelQuality() 
	{

		/* Compute prediction results */
		predictedResponses = predictResults();

		/* Predict results with the reduced model */
		reduceModel();
		predictedReducedModelResponses = predictResults();
		restoreModel();

		/* Create a quality metric set object to compute quality metrics of the linear regression algorithm */
		final long nBeta = model.getNumberOfBetas();
		final long nBetaReducedModel = nBeta - 2;

		QualityMetricSetBatch qms = new QualityMetricSetBatch(daal_Context, nBeta, nBetaReducedModel);

		SingleBetaInput singleBetaInput = (SingleBetaInput)qms.getInputDataCollection().getInput(QualityMetricId.singleBeta);
		singleBetaInput.set(SingleBetaModelInputId.model, model);
		singleBetaInput.set(SingleBetaDataInputId.expectedResponses, expectedResponses);
		singleBetaInput.set(SingleBetaDataInputId.predictedResponses, predictedResponses);

		GroupOfBetasInput groupOfBetasInput = (GroupOfBetasInput)qms.getInputDataCollection().getInput(QualityMetricId.groupOfBetas);
		groupOfBetasInput.set(GroupOfBetasInputId.expectedResponses, expectedResponses);
		groupOfBetasInput.set(GroupOfBetasInputId.predictedResponses, predictedResponses);
		groupOfBetasInput.set(GroupOfBetasInputId.predictedReducedModelResponses, predictedReducedModelResponses);

		/* Compute quality metrics */
		qualityMetricSetResult = qms.compute();
	}

	private void printResults() 
	{
		NumericTable beta = model.getBeta();
		Service.printNumericTable("Coefficients: ", beta);
		Service.printNumericTable("Expected responses (first 10 rows):", expectedResponses, 10);
		Service.printNumericTable("Predicted responses (first 10 rows):", predictedResponses, 10);
		Service.printNumericTable("Responses predicted with reduced model (first 10 rows):", predictedReducedModelResponses, 10);

		/* Print the quality metrics for a single beta */
		System.out.println("Quality metrics for a single beta");
		SingleBetaResult singleBetaResult = (SingleBetaResult)qualityMetricSetResult.getResult(QualityMetricId.singleBeta);

		Service.printNumericTable("Root means square errors for each response (dependent variable):", singleBetaResult.get(SingleBetaResultId.rms), 10);
		Service.printNumericTable("Variance for each response (dependent variable):", singleBetaResult.get(SingleBetaResultId.variance), 10);
		Service.printNumericTable("Z-score statistics:", singleBetaResult.get(SingleBetaResultId.zScore), 10);
		Service.printNumericTable("Confidence intervals for each beta coefficient:", singleBetaResult.get(SingleBetaResultId.confidenceIntervals), 10);
		Service.printNumericTable("Inverse(Xt * X) matrix:", singleBetaResult.get(SingleBetaResultId.inverseOfXtX), 10);

		DataCollection coll = singleBetaResult.get(SingleBetaResultDataCollectionId.betaCovariances);
		for (int i = 0; i < coll.size(); i++) {
			NumericTable tbl = (NumericTable)coll.get(i);
			Service.printNumericTable("Variance-covariance matrix for betas of " + i + "-th response", tbl, 10);
		}

		/* Print quality metrics for a group of betas */
		System.out.println("Quality metrics for a group of betas");
		GroupOfBetasResult groupOfBetasResult = (GroupOfBetasResult)qualityMetricSetResult.getResult(QualityMetricId.groupOfBetas);
		Service.printNumericTable("Means of expected responses for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.expectedMeans), 10);
		Service.printNumericTable("Variance of expected responses for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.expectedVariance), 10);
		Service.printNumericTable("Regression sum of squares of expected responses:", groupOfBetasResult.get(GroupOfBetasResultId.regSS), 10);
		Service.printNumericTable("Sum of squares of residuals for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.resSS), 10);
		Service.printNumericTable("Total sum of squares for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.tSS), 10);
		Service.printNumericTable("Determination coefficient for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.determinationCoeff), 10);
		Service.printNumericTable("F-statistics for each dependent variable:", groupOfBetasResult.get(GroupOfBetasResultId.fStatistics), 10);
	}
}
