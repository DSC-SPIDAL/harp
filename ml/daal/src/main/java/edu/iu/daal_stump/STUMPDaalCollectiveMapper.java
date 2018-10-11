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

package edu.iu.daal_stump;

import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.stump.prediction.PredictionBatch;
import com.intel.daal.algorithms.stump.prediction.PredictionMethod;
import com.intel.daal.algorithms.stump.training.TrainingBatch;
import com.intel.daal.algorithms.stump.training.TrainingMethod;
import com.intel.daal.algorithms.weak_learner.Model;
import com.intel.daal.algorithms.weak_learner.training.TrainingResult;
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

// daal algorithm module
// daal data structure and service module

/**
 * @brief the Harp mapper for running K nearest neighbors 
 */
public class STUMPDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	//cmd args
	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int fileDim;
	private String testFilePath;
	private int nFeatures;

	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	private static TrainingResult   trainingResult;
	private static PredictionResult predictionResult;
	private static NumericTable     testGroundTruth;

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
	{

		long startTime = System.currentTimeMillis();

		this.conf = context.getConfiguration();

		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 10);
		this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
		this.harpThreads = Runtime.getRuntime().availableProcessors();

		this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 10);
		this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");

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
		runSTUMP(context);
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
	private void runSTUMP(Context context) throws IOException 
	{
		// ---------- training and testing ----------
		trainModel();
		testModel();
		printResults();
		daal_Context.dispose();
	}

	private void trainModel() 
	{

		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);
		NumericTable trainData = load_table[0]; 
		NumericTable trainGroundTruth = load_table[1];

		/* Create algorithm objects to train the stump model */
		TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense);

		algorithm.input.set(InputId.data, trainData);
		algorithm.input.set(InputId.labels, trainGroundTruth);

		/* Train the stump model */
		trainingResult = algorithm.compute();
	}

	private void testModel() throws IOException
	{
		// load test set from HDFS
		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.testFilePath, this.nFeatures, 1, ",", this.daal_Context);
		NumericTable testData = load_table[0];
		this.testGroundTruth = load_table[1];

		/* Create algorithm objects to predict values with the fast method */
		PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense);

		Model model = trainingResult.get(TrainingResultId.model);

		/* Pass a testing data set and the trained model to the algorithm */
		algorithm.input.set(NumericTableInputId.data, testData);
		algorithm.input.set(ModelInputId.model, model);

		/* Compute the prediction results */
		predictionResult = algorithm.compute();
	}

	private void printResults() 
	{
		NumericTable predictionResults = predictionResult.get(PredictionResultId.prediction);
		Service.printClassificationResult(testGroundTruth, predictionResults, "Ground truth", "Classification results",
				"Stump classification results (first 20 observations):", 20);
	}

}
