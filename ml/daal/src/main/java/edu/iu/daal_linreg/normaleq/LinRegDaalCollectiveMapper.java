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

package edu.iu.daal_linreg.normaleq;

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

import java.nio.DoubleBuffer;

//import daal.jar API
import com.intel.daal.algorithms.linear_regression.Model;
import com.intel.daal.algorithms.linear_regression.prediction.*;
import com.intel.daal.algorithms.linear_regression.training.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data_source.*;

import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running Linear Regression
 */


public class LinRegDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private int fileDim; 
  private int vectorSize; 
  private int nDependentVariables; 
  private int num_mappers;
  private int numThreads;
  private int harpThreads; 
  private TrainingResult trainingResult;
  private PredictionResult predictionResult;
  private String testFilePath;
  private String testGroundTruth;
  private Model model;
  private NumericTable results;

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
  private static HarpDAALComm harpcomm;	
  private static DaalContext daal_Context = new DaalContext();
    /**
   * Mapper configuration.
   */
    @Override
    protected void setup(Context context)
    throws IOException, InterruptedException {
      long startTime = System.currentTimeMillis();

      Configuration configuration = context.getConfiguration();
      this.num_mappers = configuration.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      this.numThreads = configuration.getInt(HarpDAALConstants.NUM_THREADS, 10);
      this.fileDim = configuration.getInt(HarpDAALConstants.FILE_DIM, 12);
      this.vectorSize = configuration.getInt(HarpDAALConstants.FEATURE_DIM, 10);
      this.nDependentVariables = configuration.getInt(HarpDAALConstants.NUM_DEPVAR, 2);
      this.testFilePath = configuration.get(HarpDAALConstants.TEST_FILE_PATH,"");
      this.testGroundTruth = configuration.get(HarpDAALConstants.TEST_TRUTH_PATH,"");

      //always use the maximum hardware threads to load in data and convert data 
      harpThreads = Runtime.getRuntime().availableProcessors();

      LOG.info("Num Mappers " + num_mappers);
      LOG.info("Num Threads " + numThreads);
      LOG.info("Num harp load data threads " + harpThreads);

      long endTime = System.currentTimeMillis();
      LOG.info(
        "config (ms) :" + (endTime - startTime));
      System.out.println("Collective Mapper launched");

    }

    protected void mapCollective(
      KeyValReader reader, Context context)
    throws IOException, InterruptedException {
      long startTime = System.currentTimeMillis();
      List<String> trainingDataFiles =
      new LinkedList<String>();

    //splitting files between mapper

      while (reader.nextKeyValue()) {
        String key = reader.getCurrentKey();
        String value = reader.getCurrentValue();
        LOG.info("Key: " + key + ", Value: "
          + value);
        System.out.println("file name : " + value);
        trainingDataFiles.add(value);
      }

      Configuration conf = context.getConfiguration();

      Path pointFilePath = new Path(trainingDataFiles.get(0));
      System.out.println("path = "+ pointFilePath.getName());
      FileSystem fs = pointFilePath.getFileSystem(conf);
      FSDataInputStream in = fs.open(pointFilePath);

      //init data source
      this.datasource = new HarpDAALDataSource(trainingDataFiles, this.fileDim, harpThreads, conf);
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

      runLinReg(conf, context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

  private void runLinReg(Configuration conf, Context context) throws IOException 
  {

	  ts_start = System.currentTimeMillis();
	  this.datasource.loadFiles();

	  NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, vectorSize, this.datasource.getTotalLines(), 
			  NumericTable.AllocationFlag.DoAllocate);
	  NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Double.class, nDependentVariables, this.datasource.getTotalLines(), 
			  NumericTable.AllocationFlag.DoAllocate);
	  MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
	  mergedData.addNumericTable(featureArray_daal);
	  mergedData.addNumericTable(labelArray_daal);

	  /* Retrieve the data from an input file */
	  this.datasource.loadDataBlock(mergedData);

	  Service.printNumericTable("featureArray_daal", featureArray_daal, 5, featureArray_daal.getNumberOfColumns());
	  Service.printNumericTable("labelArray_daal", labelArray_daal, 5, labelArray_daal.getNumberOfColumns());

	  trainModel(featureArray_daal, labelArray_daal);
	  if(this.isMaster())
	  {
		  testModel(testFilePath, conf);
		  printResults(testGroundTruth, predictionResult, conf);
	  }

	  daal_Context.dispose();

	  ts_end = System.currentTimeMillis();
	  total_time = (ts_end - ts_start);

	  LOG.info("Total Execution Time of LinReg: "+ total_time);
	  LOG.info("Loading Data Time of LinReg: "+ load_time);
	  LOG.info("Computation Time of LinReg: "+ compute_time);
	  LOG.info("Comm Time of LinReg: "+ comm_time);
	  LOG.info("DataType Convert Time of LinReg: "+ convert_time);
	  LOG.info("Misc Time of LinReg: "+ (total_time - load_time - compute_time - comm_time - convert_time));
  }
  
  private void trainModel(NumericTable trainData, NumericTable trainDependentVariables) throws java.io.IOException 
  {

    LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
    Environment.setNumberOfThreads(numThreads);
    LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

    ts1 = System.currentTimeMillis();
    TrainingDistributedStep1Local linearRegressionTraining = new TrainingDistributedStep1Local(daal_Context, Double.class,
                    TrainingMethod.normEqDense);
    linearRegressionTraining.input.set(TrainingInputId.data, trainData);
    linearRegressionTraining.input.set(TrainingInputId.dependentVariable, trainDependentVariables);

    PartialResult pres = linearRegressionTraining.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    //gather the pres to master mappers
    SerializableBase[] des_output = this.harpcomm.harpdaal_gather(pres, this.getMasterID(), "LinearReg", "gather_pres");
  
    if(this.isMaster())
    {
      TrainingDistributedStep2Master linearRegressionTrainingMaster = new TrainingDistributedStep2Master(daal_Context, Double.class,
                TrainingMethod.normEqDense);

      for(int j=0;j<this.num_mappers;j++)
      {
	      PartialResult pres_entry = (PartialResult)(des_output[j]); 
	      linearRegressionTrainingMaster.input.add(MasterInputId.partialModels, pres_entry); 
      }
 
      ts1 = System.currentTimeMillis();
      linearRegressionTrainingMaster.compute();
      trainingResult = linearRegressionTrainingMaster.finalizeCompute();
      ts2 = System.currentTimeMillis();
      compute_time += (ts2 - ts1);
      model = trainingResult.get(TrainingResultId.model);
    }

  }

  private void testModel(String testFilePath, Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
  {

    // load test data
    this.datasource.loadTestFile(testFilePath, fileDim);
    NumericTable testData = new HomogenNumericTable(daal_Context, Double.class, this.vectorSize, this.datasource.getTestRows(), 
		    NumericTable.AllocationFlag.DoAllocate);
    NumericTable testLabel = new HomogenNumericTable(daal_Context, Double.class, this.nDependentVariables, this.datasource.getTestRows(), 
		    NumericTable.AllocationFlag.DoAllocate);

    MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
    mergedData.addNumericTable(testData);
    mergedData.addNumericTable(testLabel);
    this.datasource.loadTestTable(mergedData);

    PredictionBatch linearRegressionPredict = new PredictionBatch(daal_Context, Double.class, PredictionMethod.defaultDense);

    linearRegressionPredict.input.set(PredictionInputId.data, testData);
    linearRegressionPredict.input.set(PredictionInputId.model, model);

    /* Compute the prediction results */
    ts1 = System.currentTimeMillis();
    predictionResult = linearRegressionPredict.compute();
    results = predictionResult.get(PredictionResultId.prediction);
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

  }

  private void printResults(String testGroundTruth, PredictionResult predictionResult, Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
  {

        NumericTable beta = model.getBeta();
	//load the test groudtruth 
	this.datasource.loadTestFile(testGroundTruth, this.nDependentVariables);
    	NumericTable expected = new HomogenNumericTable(daal_Context, Double.class, this.nDependentVariables, this.datasource.getTestRows(), 
		    NumericTable.AllocationFlag.DoAllocate);
        this.datasource.loadTestTable(expected);

        Service.printNumericTable("Coefficients: ", beta);
        Service.printNumericTable("First 10 rows of results (obtained): ", results, 10);
        Service.printNumericTable("First 10 rows of results (expected): ", expected, 10);
  }


}
