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

package edu.iu.daal_knn.BatchDense;

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

// daal algorithm module
import com.intel.daal.algorithms.kdtree_knn_classification.Model;
import com.intel.daal.algorithms.kdtree_knn_classification.prediction.*;
import com.intel.daal.algorithms.kdtree_knn_classification.training.*;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;

// daal data structure and service module
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K nearest neighbors 
 */
public class KNNDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
	private int nFeatures;
  	private String testFilePath;
	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	private static Model        model;
    	private static NumericTable results;
    	private static NumericTable testGroundTruth;

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        this.conf = context.getConfiguration();

	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 10);
	this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
        this.harpThreads = Runtime.getRuntime().availableProcessors();

	//set thread number used in DAAL
	LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
	Environment.setNumberOfThreads(numThreads);
	LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 10);
	this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");

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
            runKNN(context);
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
        private void runKNN(Context context) throws IOException 
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

		/* Create an algorithm object to train the k nearest neighbors model with the default dense method */
		TrainingBatch kNearestNeighborsTrain = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense);

		kNearestNeighborsTrain.input.set(InputId.data, trainData);
		kNearestNeighborsTrain.input.set(InputId.labels, trainGroundTruth);

		/* Build the k nearest neighbors model */
		TrainingResult trainingResult = kNearestNeighborsTrain.compute();
		model = trainingResult.get(TrainingResultId.model);
	}

    private void testModel() throws IOException
    {

	    NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.testFilePath, this.nFeatures, 1, ",", this.daal_Context);

	    NumericTable testData = load_table[0];
	    this.testGroundTruth = load_table[1];

	    /* Create algorithm objects to predict values of k nearest neighbors with the default method */
	    PredictionBatch kNearestNeighborsPredict = new PredictionBatch(daal_Context, Double.class,
			    PredictionMethod.defaultDense);

	    kNearestNeighborsPredict.input.set(NumericTableInputId.data, testData);
	    kNearestNeighborsPredict.input.set(ModelInputId.model, model);

	    /* Compute prediction results */
	    PredictionResult predictionResult = kNearestNeighborsPredict.compute();
	    results = predictionResult.get(PredictionResultId.prediction);
    }

    private void printResults() {
        NumericTable expected = testGroundTruth;
        Service.printNumericTable("Classification results (first 20 observations): ", results, 20);
        Service.printNumericTable("KD-tree based kNN classification results (first 20 observations):", expected, 20);
    }
}
