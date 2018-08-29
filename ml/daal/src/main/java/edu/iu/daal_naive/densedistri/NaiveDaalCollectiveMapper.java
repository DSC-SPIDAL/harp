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

package edu.iu.daal_naive.densedistri;

import org.apache.commons.io.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.util.ListIterator;
import java.nio.DoubleBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

import java.nio.DoubleBuffer;

//import daal.jar API
import com.intel.daal.algorithms.classifier.prediction.ModelInputId;
import com.intel.daal.algorithms.classifier.prediction.NumericTableInputId;
import com.intel.daal.algorithms.classifier.prediction.PredictionResult;
import com.intel.daal.algorithms.classifier.prediction.PredictionResultId;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.multinomial_naive_bayes.Model;
import com.intel.daal.algorithms.multinomial_naive_bayes.prediction.*;
import com.intel.daal.algorithms.multinomial_naive_bayes.training.*;

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;

import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running Naive Bayes
 */


public class NaiveDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private int fileDim;
  private int vectorSize = 20;
  private long nClasses = 20;
  private int num_mappers;
  private int numThreads; 
  private int harpThreads; 
  private String testFilePath;
  private String testGroundTruth;
  private List<String> inputFiles;
  private Configuration conf;

  private TrainingResult trainingResult;
  private PredictionResult predictionResult;
  private NumericTable testData;

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
      this.conf = context.getConfiguration();
      this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
      this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 21);
      this.vectorSize = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 20);
      this.nClasses = this.conf.getInt(HarpDAALConstants.NUM_CLASS, 20);
      this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");
      this.testGroundTruth = this.conf.get(HarpDAALConstants.TEST_TRUTH_PATH,"");

      //always use the maximum hardware threads to load in data and convert data 
      this.harpThreads = Runtime.getRuntime().availableProcessors();
      LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
      Environment.setNumberOfThreads(numThreads);
      LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

      LOG.info("Num Mappers " + num_mappers);
      LOG.info("Num Threads " + numThreads);
      LOG.info("Feature Dim " + vectorSize);
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

      this.inputFiles =
      new LinkedList<String>();

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
      this.datasource = new HarpDAALDataSource(this.harpThreads, this.conf);
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, this.daal_Context, this);

      runNaive(context);
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

  private void runNaive(Context context) throws IOException 
  {//{{{

	  // load training data/labels
	  NumericTable[] train_data_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, this.vectorSize, 1, ",", this.daal_Context);
	  NumericTable featureArray_daal = train_data_table[0];
	  NumericTable labelArray_daal = train_data_table[1];

	  //load test data
	  NumericTable[] test_data_table = this.datasource.createDenseNumericTableSplit(testFilePath, this.vectorSize, 1,  "," , this.daal_Context);
	  testData = test_data_table[0];

	  trainModel(featureArray_daal, labelArray_daal);
	  if(this.isMaster()){
		  testModel();
		  printResults(testGroundTruth, predictionResult);
	  }

	  this.barrier("naive", "testmodel-sync");

	  daal_Context.dispose();


  }//}}}
  
  private void trainModel(NumericTable featureArray_daal, NumericTable labelArray_daal) throws java.io.IOException 
  {//{{{

	  TrainingDistributedStep1Local algorithm = new TrainingDistributedStep1Local(daal_Context, Double.class, TrainingMethod.defaultDense, nClasses);
	  algorithm.input.set(InputId.data, featureArray_daal);
	  algorithm.input.set(InputId.labels, labelArray_daal);

	  TrainingPartialResult pres = algorithm.compute();

	  //gather pres to master mapper
	  SerializableBase[] gather_output = this.harpcomm.harpdaal_gather(pres, this.getMasterID(), "NaiveBayes", "gather_pres");

	  if(this.isMaster())
	  {
		  TrainingDistributedStep2Master masterAlgorithm = new TrainingDistributedStep2Master(daal_Context, Double.class, TrainingMethod.defaultDense, nClasses);

		  for(int j=0;j<this.num_mappers; j++)
		  {
			  TrainingPartialResult des_output = (TrainingPartialResult)(gather_output[j]);
			  masterAlgorithm.input.add(TrainingDistributedInputId.partialModels, des_output);
		  }

		  masterAlgorithm.compute();
		  trainingResult = masterAlgorithm.finalizeCompute();
	  }

	  this.barrier("naive", "master-compute-sync");

  }//}}}

  private void testModel() throws java.io.FileNotFoundException, java.io.IOException 
  {//{{{

	  PredictionBatch algorithm = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense, nClasses);

	  algorithm.input.set(NumericTableInputId.data, testData);
	  Model model = trainingResult.get(TrainingResultId.model);
	  algorithm.input.set(ModelInputId.model, model);

	  /* Compute the prediction results */
	  predictionResult = algorithm.compute();

  }//}}}

  private void printResults(String testGroundTruth, PredictionResult predictionResult) throws java.io.FileNotFoundException, java.io.IOException 
  {//{{{
	  NumericTable expected = this.datasource.createDenseNumericTable(testGroundTruth, 1, "," , this.daal_Context);
	  NumericTable prediction = predictionResult.get(PredictionResultId.prediction);
	  Service.printClassificationResult(expected, prediction, "Ground truth", "Classification results",
			  "NaiveBayes classification results (first 20 observations):", 20);
  }//}}}


}
