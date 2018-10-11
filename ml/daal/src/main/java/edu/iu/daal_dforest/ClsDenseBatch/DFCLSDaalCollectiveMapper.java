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

package edu.iu.daal_dforest.ClsDenseBatch;

import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.decision_forest.ResultsToComputeId;
import com.intel.daal.algorithms.decision_forest.VariableImportanceModeId;
import com.intel.daal.algorithms.decision_forest.classification.Model;
import com.intel.daal.algorithms.decision_forest.classification.prediction.PredictionBatch;
import com.intel.daal.algorithms.decision_forest.classification.prediction.PredictionMethod;
import com.intel.daal.algorithms.decision_forest.classification.training.ResultNumericTableId;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingBatch;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingMethod;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingResult;
import com.intel.daal.data_management.data.DataFeature;
import com.intel.daal.data_management.data.DataFeatureUtils;
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

// intel daal algorithms
// intel daal data structures and services

/**
 * @brief the Harp mapper for running K-means
 */
public class DFCLSDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
	private int nFeatures;
    	private int nClasses;
    	private int nTrees;
    	private int minObservationsInLeafNode;
  	private String testFilePath;
	private List<String> inputFiles;
	private Configuration conf;

    	private static NumericTable testGroundTruth;

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

	this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 3);
	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 4);
	this.nClasses = this.conf.getInt(HarpDAALConstants.NUM_CLASS, 5);
	this.nTrees = this.conf.getInt(Constants.NUM_TREES, 10);
	this.minObservationsInLeafNode = this.conf.getInt(Constants.MIN_OBS_LEAFNODE, 8);

        this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();
	this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");

	// ----------------------- runtime settings -----------------------
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
            runDFCLS(context);
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
        private void runDFCLS(Context context) throws IOException 
	{
		// ---------- training and testing ----------
		TrainingResult trainingResult = trainModel();
		PredictionResult predictionResult = testModel(trainingResult);
		printResults(predictionResult);

		daal_Context.dispose();
	}

	private TrainingResult trainModel() 
	{

		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);
		NumericTable trainData = load_table[0]; 
		NumericTable trainGroundTruth = load_table[1];

		/* Set feature as categorical */
		DataFeature categoricalFeature = trainData.getDictionary().getFeature(2);
		categoricalFeature.setFeatureType(DataFeatureUtils.FeatureType.DAAL_CATEGORICAL);

		/* Create algorithm objects to train the decision forest classification model */
		TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense, nClasses);
		algorithm.parameter.setNTrees(nTrees);
		algorithm.parameter.setFeaturesPerNode(nFeatures);
		algorithm.parameter.setMinObservationsInLeafNode(minObservationsInLeafNode);
		algorithm.parameter.setVariableImportanceMode(VariableImportanceModeId.MDI);
		algorithm.parameter.setResultsToCompute(ResultsToComputeId.computeOutOfBagError);

		/* Pass a training data set and dependent values to the algorithm */
		algorithm.input.set(InputId.data, trainData);
		algorithm.input.set(InputId.labels, trainGroundTruth);

		/* Train the decision forest classification model */
		TrainingResult trainingResult = algorithm.compute();

		Service.printNumericTable("Variable importance results: ", trainingResult.get(ResultNumericTableId.variableImportance));
		Service.printNumericTable("OOB error: ", trainingResult.get(ResultNumericTableId.outOfBagError));
		return trainingResult;
	}


    private PredictionResult testModel(TrainingResult trainingResult) throws IOException 
    {

	    NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.testFilePath, this.nFeatures, 1, ",", this.daal_Context);
	    NumericTable testData = load_table[0];
	    this.testGroundTruth = load_table[1];

	    /* Set feature as categorical */
	    DataFeature categoricalFeature = testData.getDictionary().getFeature(2);
	    categoricalFeature.setFeatureType(DataFeatureUtils.FeatureType.DAAL_CATEGORICAL);

	    /* Create algorithm objects for decision forest classification prediction with the fast method */
	    PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense, nClasses);

	    /* Pass a testing data set and the trained model to the algorithm */
	    Model model = trainingResult.get(TrainingResultId.model);
	    algorithm.input.set(NumericTableInputId.data, testData);
	    algorithm.input.set(ModelInputId.model, model);

	    /* Compute prediction results */
	    return algorithm.compute();
    }
 
    private void printResults(PredictionResult predictionResult) {
        NumericTable predictionResults = predictionResult.get(PredictionResultId.prediction);
        Service.printClassificationResult(testGroundTruth, predictionResults, "Ground truth", "Classification results",
                "Decision forest classification results (first 20 observations):", 20);
    }  
}
