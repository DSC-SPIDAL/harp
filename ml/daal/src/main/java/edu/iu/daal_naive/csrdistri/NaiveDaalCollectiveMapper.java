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

package edu.iu.daal_naive.csrdistri;

import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.multinomial_naive_bayes.Model;
import com.intel.daal.algorithms.multinomial_naive_bayes.prediction.PredictionBatch;
import com.intel.daal.algorithms.multinomial_naive_bayes.prediction.PredictionMethod;
import com.intel.daal.algorithms.multinomial_naive_bayes.training.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.data_aux.Service;
import edu.iu.data_comm.HarpDAALComm;
import edu.iu.datasource.HarpDAALDataSource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

//import daal.jar API


/**
 * @brief the Harp mapper for running Naive Bayes
 */


public class NaiveDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private long nClasses;
  private int num_mappers;
  private int numThreads; //used in computation
  private int harpThreads; //used in data conversion
  private TrainingResult trainingResult;
  private PredictionResult predictionResult;
  private String testFilePath;
  private String testGroundTruth;
  private List<String> inputFiles;
  private Configuration conf;

  private static HarpDAALDataSource datasource;
  private static HarpDAALComm harpcomm;	
  private static DaalContext daal_Context = new DaalContext();
    /**
   * Mapper configuration.
   */
    @Override
    protected void setup(Context context)
    throws IOException, InterruptedException {
      long startTime = System.currentTimeMillis();
      this.conf =
      context.getConfiguration();

      num_mappers = this.conf
      .getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      numThreads = this.conf
      .getInt(HarpDAALConstants.NUM_THREADS, 10);
      nClasses = this.conf
      .getInt(HarpDAALConstants.NUM_CLASS, 20);

      testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");
      testGroundTruth =this.conf.get(HarpDAALConstants.TEST_TRUTH_PATH,"");

      //always use the maximum hardware threads to load in data and convert data 
      harpThreads = Runtime.getRuntime().availableProcessors();

      LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
      Environment.setNumberOfThreads(numThreads);
      LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

      LOG.info("Num Mappers " + num_mappers);
      LOG.info("Num Threads " + numThreads);
      LOG.info("Num classes " + nClasses);
      LOG.info("Num harp load data threads " + harpThreads);

      long endTime = System.currentTimeMillis();
      LOG.info(
        "config (ms) :" + (endTime - startTime));
      System.out.println("Collective Mapper launched");

    }

    protected void mapCollective(
      KeyValReader reader, Context context)
    throws IOException, InterruptedException {

      this.inputFiles = new LinkedList<String>();

	  //splitting files between mapper
      while (reader.nextKeyValue()) {
        String key = reader.getCurrentKey();
        String value = reader.getCurrentValue();
        LOG.info("Key: " + key + ", Value: "
          + value);
        System.out.println("file name : " + value);
        this.inputFiles.add(value);
      }

      //init data source
      this.datasource = new HarpDAALDataSource(harpThreads, conf);
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

      runNaive(context);
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

  private void runNaive(Context context) throws IOException {
         
    // load training data and training labels 
    //read in csr files with filenames in trainingDataFiles
    NumericTable[] load_table = this.datasource.loadCSRNumericTableAndLabel(this.inputFiles, ",", daal_Context);
    NumericTable featureArray_daal = load_table[0];
    NumericTable labelArray_daal = load_table[1];

    trainModel(featureArray_daal, labelArray_daal);

    //load test set and labels
    NumericTable testData = this.datasource.loadCSRNumericTable(this.testFilePath, ",", daal_Context);

    if(this.isMaster()){
      testModel(testData, conf);
      printResults(testGroundTruth, predictionResult, conf);
    }
    
    this.barrier("naive", "testmodel-sync");

    daal_Context.dispose();
    
}
  
  private void trainModel(NumericTable featureArray_daal, NumericTable labelArray_daal) throws java.io.IOException 
  {

	TrainingDistributedStep1Local algorithm = new TrainingDistributedStep1Local(this.daal_Context, Double.class,
			TrainingMethod.fastCSR, nClasses);

	algorithm.input.set(InputId.data, featureArray_daal);
	algorithm.input.set(InputId.labels, labelArray_daal);

	TrainingPartialResult pres = algorithm.compute();

	SerializableBase[] gather_output = this.harpcomm.harpdaal_gather(pres, this.getMasterID(), "Naive_Bayes", "gather_pres");

	if(this.isMaster())
	{

		TrainingDistributedStep2Master masterAlgorithm = new TrainingDistributedStep2Master(daal_Context, Double.class,
				TrainingMethod.fastCSR, nClasses);

		for(int j=0;j<this.num_mappers;j++)
		{
			TrainingPartialResult des_output = (TrainingPartialResult)(gather_output[j]);
			masterAlgorithm.input.add(TrainingDistributedInputId.partialModels, des_output);
		}

		masterAlgorithm.compute();
		trainingResult = masterAlgorithm.finalizeCompute();
	}

	this.barrier("naive", "master-compute-sync");

}

  private void testModel(NumericTable testData, Configuration conf) throws java.io.FileNotFoundException, java.io.IOException {

    PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.fastCSR, nClasses);

    algorithm.input.set(NumericTableInputId.data, testData);
    Model model = trainingResult.get(TrainingResultId.model);
    algorithm.input.set(ModelInputId.model, model);

    /* Compute the prediction results */
    predictionResult = algorithm.compute();

  }

  private void printResults(String testGroundTruth, PredictionResult predictionResult, Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
  {

	  NumericTable expected = this.datasource.createDenseNumericTable(testGroundTruth, 1, ",", this.daal_Context); 
	  NumericTable prediction = predictionResult.get(PredictionResultId.prediction);

        Service.printClassificationResult(expected, prediction, "Ground truth", "Classification results",
                "NaiveBayes classification results (first 20 observations):", 20);
    }

}
