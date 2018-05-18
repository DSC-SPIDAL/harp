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

package edu.iu.daal_quality_metrics.SVMMultiMetrics;

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
import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.quality_metric.multi_class_confusion_matrix.*;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.multi_class_classifier.Model;
import com.intel.daal.algorithms.multi_class_classifier.prediction.*;
import com.intel.daal.algorithms.multi_class_classifier.quality_metric_set.*;
import com.intel.daal.algorithms.multi_class_classifier.training.*;

// intel daal data structures and services
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import com.intel.daal.data_management.data.*;

/**
 * @brief the Harp mapper for running K-means
 */
public class SVMMultiMESDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int numMappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
  	private String testFilePath;

	private int nFeatures;
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

	private static TrainingResult   trainingResult;
	private static PredictionResult predictionResult;
	private static ResultCollection qualityMetricSetResult;

	private static NumericTable groundTruthLabels;
	private static NumericTable predictedLabels;

	private static HarpDAALDataSource datasource;
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

	this.nFeatures = configuration.getInt(Constants.FEATURE_DIM, 20);
	this.fileDim = configuration.getInt(Constants.FILE_DIM, 21);
	this.nClasses = configuration.getInt(Constants.NUM_CLASS, 5);

        this.numMappers = configuration.getInt(Constants.NUM_MAPPERS, 10);
        this.numThreads = configuration.getInt(Constants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();
	this.testFilePath = configuration.get(Constants.TEST_FILE_PATH,"");

        LOG.info("File Dim " + this.fileDim);
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

	    this.datasource = new HarpDAALDataSource(dataFiles, fileDim, harpThreads, conf);

	    // ----------------------- start the execution -----------------------
            runSVMMulti(conf, context);
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
        private void runSVMMulti(Configuration conf, Context context) throws IOException 
	{
		// ---------- load data ----------
		this.datasource.loadFiles();
		// ---------- training and testing ----------
		trainModel();
		testModel();
		testModelQuality();
		printResults();
		daal_Context.dispose();
	}

	private void trainModel() {
        /* Retrieve the data from input data sets */
        com.intel.daal.algorithms.svm.training.TrainingBatch training = new com.intel.daal.algorithms.svm.training.TrainingBatch(
                daal_Context, Double.class, com.intel.daal.algorithms.svm.training.TrainingMethod.boser);

        com.intel.daal.algorithms.svm.prediction.PredictionBatch prediction = new com.intel.daal.algorithms.svm.prediction.PredictionBatch(
                daal_Context, Double.class, com.intel.daal.algorithms.svm.prediction.PredictionMethod.defaultDense);

        /* Retrieve the data from input data sets */
        // FileDataSource trainDataSource = new FileDataSource(daal_Context, trainDatasetFileName,
        //         DataSource.DictionaryCreationFlag.DoDictionaryFromdaal_Context,
        //         DataSource.NumericTableAllocationFlag.NotAllocateNumericTable);

        /* Create Numeric Tables for training data and labels */
        NumericTable trainData = new HomogenNumericTable(daal_Context, Double.class, nFeatures, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
        NumericTable trainGroundTruth = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
        MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
        mergedData.addNumericTable(trainData);
        mergedData.addNumericTable(trainGroundTruth);

        /* Retrieve the data from an input file */
	this.datasource.loadDataBlock(mergedData);

        /* Create algorithm objects to train the multi-class SVM model */
        TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.oneAgainstOne, nClasses);

        /* Set parameters for the multi-class SVM algorithm */
        algorithm.parameter.setTraining(training);
        algorithm.parameter.setPrediction(prediction);

        /* Pass a training data set and dependent values to the algorithm */
        algorithm.input.set(InputId.data, trainData);
        algorithm.input.set(InputId.labels, trainGroundTruth);

        /* Train the multi-class SVM model */
        trainingResult = algorithm.compute();
    }


    private void testModel() throws IOException
    {

	this.datasource.loadTestFile(testFilePath, fileDim);
        com.intel.daal.algorithms.svm.training.TrainingBatch training = new com.intel.daal.algorithms.svm.training.TrainingBatch(
                daal_Context, Double.class, com.intel.daal.algorithms.svm.training.TrainingMethod.boser);

        com.intel.daal.algorithms.svm.prediction.PredictionBatch prediction = new com.intel.daal.algorithms.svm.prediction.PredictionBatch(
                daal_Context, Double.class, com.intel.daal.algorithms.svm.prediction.PredictionMethod.defaultDense);

        // FileDataSource testDataSource = new FileDataSource(daal_Context, testDatasetFileName,
        //         DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
        //         DataSource.NumericTableAllocationFlag.NotAllocateNumericTable);

        /* Create Numeric Tables for testing data and labels */
        NumericTable testData = new HomogenNumericTable(daal_Context, Double.class, nFeatures, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
        groundTruthLabels = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
        MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
        mergedData.addNumericTable(testData);
        mergedData.addNumericTable(groundTruthLabels);

        /* Retrieve the data from an input file */
	this.datasource.loadTestTable(mergedData);

        /* Create algorithm objects to predict multi-class SVM values with the defaultDense method */
        PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.multiClassClassifierWu, nClasses);

        algorithm.parameter.setTraining(training);
        algorithm.parameter.setPrediction(prediction);

        Model model = trainingResult.get(TrainingResultId.model);

        /* Pass a testing data set and the trained model to the algorithm */
        algorithm.input.set(NumericTableInputId.data, testData);
        algorithm.input.set(ModelInputId.model, model);

        /* Compute the prediction results */
        predictionResult = algorithm.compute();
    }
    
 
    private void testModelQuality() {
        /* Retrieve predicted labels */
        predictedLabels = predictionResult.get(PredictionResultId.prediction);

        /* Create a quality metric set object to compute quality metrics of the SVM algorithm */
        QualityMetricSetBatch quality_metric_set = new QualityMetricSetBatch(daal_Context, nClasses);

        MultiClassConfusionMatrixInput input = quality_metric_set.getInputDataCollection()
                .getInput(QualityMetricId.confusionMatrix);

        input.set(MultiClassConfusionMatrixInputId.predictedLabels, predictedLabels);
        input.set(MultiClassConfusionMatrixInputId.groundTruthLabels, groundTruthLabels);

        /* Compute quality metrics */
        qualityMetricSetResult = quality_metric_set.compute();
    }
 
    private void printResults() {
        /* Print the classification results */
        Service.printClassificationResult(groundTruthLabels, predictedLabels, "Ground truth", "Classification results",
                "Multi-class SVM classification results (first 20 observations):", 20);
        /* Print the quality metrics */
        MultiClassConfusionMatrixResult qualityMetricResult = qualityMetricSetResult
                .getResult(QualityMetricId.confusionMatrix);
        NumericTable confusionMatrix = qualityMetricResult.get(MultiClassConfusionMatrixResultId.confusionMatrix);
        NumericTable multiClassMetrics = qualityMetricResult.get(MultiClassConfusionMatrixResultId.multiClassMetrics);

        Service.printNumericTable("Confusion matrix:", confusionMatrix);

        DoubleBuffer qualityMetricsData = DoubleBuffer
                .allocate((int) (multiClassMetrics.getNumberOfColumns() * multiClassMetrics.getNumberOfRows()));
        qualityMetricsData = multiClassMetrics.getBlockOfRows(0, multiClassMetrics.getNumberOfRows(),
                qualityMetricsData);

        System.out
                .println("Average accuracy: " + qualityMetricsData.get(MultiClassMetricId.averageAccuracy.getValue()));
        System.out.println("Error rate:       " + qualityMetricsData.get(MultiClassMetricId.errorRate.getValue()));
        System.out.println("Micro precision:  " + qualityMetricsData.get(MultiClassMetricId.microPrecision.getValue()));
        System.out.println("Micro recall:     " + qualityMetricsData.get(MultiClassMetricId.microRecall.getValue()));
        System.out.println("Micro F-score:    " + qualityMetricsData.get(MultiClassMetricId.microFscore.getValue()));
        System.out.println("Macro precision:  " + qualityMetricsData.get(MultiClassMetricId.macroPrecision.getValue()));
        System.out.println("Macro recall:     " + qualityMetricsData.get(MultiClassMetricId.macroRecall.getValue()));
        System.out.println("Macro F-score:    " + qualityMetricsData.get(MultiClassMetricId.macroFscore.getValue()));
    }    
}
