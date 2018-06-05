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

package edu.iu.daal_dtree.RegDenseBatch;

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
import com.intel.daal.algorithms.decision_tree.regression.Model;
import com.intel.daal.algorithms.decision_tree.regression.prediction.*;
import com.intel.daal.algorithms.decision_tree.regression.training.*;
import com.intel.daal.algorithms.decision_tree.*;

// intel daal data structures and services
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class DTREGDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
	private int nFeatures;
  	private String testFilePath;
  	private String pruneFilePath;
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

	this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 5);
	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 6);
        this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();
	this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");
	this.pruneFilePath = this.conf.get(HarpDAALConstants.TRAIN_PRUNE_PATH,"");

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
            runDTREG(context);
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
        private void runDTREG(Context context) throws IOException 
	{
		
		// ---------- training and testing ----------
		TrainingResult trainingResult = trainModel();
		PredictionResult predictionResult = testModel(trainingResult);
		printResults(predictionResult);

		daal_Context.dispose();
	}

	private TrainingResult trainModel() throws IOException 
	{

		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);

		NumericTable trainData = load_table[0];
		NumericTable trainGroundTruth = load_table[1];

		NumericTable[] load_prune_table = this.datasource.createDenseNumericTableSplit(this.pruneFilePath, this.nFeatures, 1, ",", this.daal_Context);

		NumericTable pruneData = load_prune_table[0];
		NumericTable pruneGroundTruth = load_prune_table[1];

		/* Create algorithm objects to train the decision tree regression model */
		TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense);

		/* Pass the training data set with labels, and pruning dataset with labels to the algorithm */
		algorithm.input.set(TrainingInputId.data, trainData);
		algorithm.input.set(TrainingInputId.dependentVariables, trainGroundTruth);
		algorithm.input.set(TrainingInputId.dataForPruning, pruneData);
		algorithm.input.set(TrainingInputId.dependentVariablesForPruning, pruneGroundTruth);

		/* Train the decision tree regression model */
		TrainingResult trainingResult = algorithm.compute();

		return trainingResult;
	}


    private PredictionResult testModel(TrainingResult trainingResult) throws IOException
    {
	NumericTable[] load_test_table = this.datasource.createDenseNumericTableSplit(this.testFilePath, this.nFeatures, 1, ",", this.daal_Context);

	NumericTable testData = load_test_table[0];
	this.testGroundTruth = load_test_table[1];

	/* Create algorithm objects for decision tree regression prediction */
        PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense);

        /* Pass a testing data set and the trained model to the algorithm */
        System.out.println("Number of columns: " + testData.getNumberOfColumns());
        Model model = trainingResult.get(TrainingResultId.model);
        algorithm.input.set(NumericTableInputId.data, testData);
        algorithm.input.set(ModelInputId.model, model);

        /* Compute prediction results */
        return algorithm.compute();

    }

    private void printResults(PredictionResult predictionResult) {

	    NumericTable predictionResults = predictionResult.get(PredictionResultId.prediction);
	    Service.printClassificationResult(testGroundTruth, predictionResults, "Ground truth", "Regression results",
			    "Decision tree regression results (first 20 observations):", 20);

    }
}
