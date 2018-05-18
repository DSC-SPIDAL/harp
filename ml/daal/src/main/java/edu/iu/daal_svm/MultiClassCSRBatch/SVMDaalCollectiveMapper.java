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

package edu.iu.daal_svm.MultiClassCSRBatch;

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

// import DAAL algorithm API 
import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.multi_class_classifier.Model;
import com.intel.daal.algorithms.multi_class_classifier.prediction.*;
import com.intel.daal.algorithms.multi_class_classifier.training.*;

// import DAAL data management API 
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;

import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class SVMDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int numMappers;
        private int numThreads;
        private int harpThreads; 

	// private int fileDim;
  	private String trainGroundTruthPath;
  	private String testFilePath;
  	private String testGroundTruthPath;

	// private int nFeatures;
    	private int nClasses;

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

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();


	private static TrainingResult   trainingResult;
	private static PredictionResult predictionResult;

	private static com.intel.daal.algorithms.svm.training.TrainingBatch twoClassTraining;
	private static com.intel.daal.algorithms.svm.prediction.PredictionBatch twoClassPrediction;

	private static com.intel.daal.algorithms.kernel_function.linear.Batch kernel =
        new com.intel.daal.algorithms.kernel_function.linear.Batch(
            daal_Context, Double.class, com.intel.daal.algorithms.kernel_function.linear.Method.fastCSR);
		

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        Configuration configuration =
            context.getConfiguration();

	// this.nFeatures = configuration.getInt(Constants.FEATURE_DIM, 10);
	// this.fileDim = configuration.getInt(Constants.FILE_DIM, 10);
	this.nClasses = configuration.getInt(Constants.NUM_CLASS, 5);
        this.numMappers = configuration.getInt(Constants.NUM_MAPPERS, 10);
        this.numThreads = configuration.getInt(Constants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();

	this.testFilePath = configuration.get(Constants.TEST_FILE_PATH,"");
	this.trainGroundTruthPath = configuration.get(Constants.TRAIN_TRUTH,"");
	this.testGroundTruthPath = configuration.get(Constants.TEST_TRUTH,"");

        LOG.info("Num Mappers " + this.numMappers);
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

	    // create data source
	    this.datasource = new HarpDAALDataSource(dataFiles, harpThreads, conf);

	    // ----------------------- start the execution -----------------------
            runSVM(conf, context);
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
        private void runSVM(Configuration conf, Context context) throws IOException 
	{
	    // ---------- training and testing ----------
	    trainModel();
	    testModel();
	    printResults();
	    daal_Context.dispose();
        }

	private void trainModel() throws IOException
	{

		twoClassTraining = new com.intel.daal.algorithms.svm.training.TrainingBatch(
				daal_Context, Double.class, com.intel.daal.algorithms.svm.training.TrainingMethod.boser);
		twoClassTraining.parameter.setKernel(kernel);

		twoClassPrediction = new com.intel.daal.algorithms.svm.prediction.PredictionBatch(
				daal_Context, Double.class, com.intel.daal.algorithms.svm.prediction.PredictionMethod.defaultDense);

		twoClassPrediction.parameter.setKernel(kernel);

		//load in training data in CSR file
		NumericTable trainData = this.datasource.loadCSRNumericTable(daal_Context);

		//load in train groundtruth in dense file
		this.datasource.loadTestFile(trainGroundTruthPath, 1);
		NumericTable trainTruth = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
		this.datasource.loadTestTable(trainTruth);


		/* Create an algorithm to train the multi-class SVM model */
		TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.oneAgainstOne, nClasses);

		/* Set parameters for the multi-class SVM algorithm */
		algorithm.parameter.setTraining(twoClassTraining);
		algorithm.parameter.setPrediction(twoClassPrediction);

		/* Pass a training data set and dependent values to the algorithm */
		algorithm.input.set(InputId.data, trainData);
		algorithm.input.set(InputId.labels, trainTruth);

		/* Train the multi-class SVM model */
		trainingResult = algorithm.compute();
	}
     
    private void testModel() throws IOException
    {
	NumericTable testData = this.datasource.loadExternalCSRNumericTable(testFilePath, daal_Context);

	// // load test set from HDFS
	// this.datasource.loadTestFile(testFilePath, fileDim);
        //
	// NumericTable testData = new HomogenNumericTable(daal_Context, Double.class, nFeatures, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
        // testGroundTruth = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
        //
        // MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
        // mergedData.addNumericTable(testData);
        // mergedData.addNumericTable(testGroundTruth);
        //
	// // load test set to daal table
	// this.datasource.loadTestTable(mergedData);

        /* Create a numeric table to store the prediction results */
        PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.multiClassClassifierWu, nClasses);

        algorithm.parameter.setTraining(twoClassTraining);
        algorithm.parameter.setPrediction(twoClassPrediction);

        Model model = trainingResult.get(TrainingResultId.model);

        /* Pass a testing data set and the trained model to the algorithm */
        algorithm.input.set(NumericTableInputId.data, testData);
        algorithm.input.set(ModelInputId.model, model);

        /* Compute the prediction results */
        predictionResult = algorithm.compute();

    }   

    private void printResults() throws IOException {

	    this.datasource.loadTestFile(testGroundTruthPath, 1);
	    NumericTable testGroundTruth = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
	    this.datasource.loadTestTable(testGroundTruth);

	    NumericTable predictionResults = predictionResult.get(PredictionResultId.prediction);
	    Service.printClassificationResult(testGroundTruth, predictionResults, "Ground truth", "Classification results",
			    "SVM multiclass classification results (first 20 observations):", 20);

    }

}
