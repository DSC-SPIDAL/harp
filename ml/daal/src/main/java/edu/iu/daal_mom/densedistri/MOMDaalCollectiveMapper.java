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

package edu.iu.daal_mom.densedistri;

import org.apache.commons.io.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.nio.DoubleBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running Neural Network
 */


public class MOMDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private PartialResult partialResult;
  private Result result;
  private int fileDim; 
  private int vectorSize; 
  private int num_mappers;
  private int numThreads;
  private int harpThreads; 
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
      this.conf = context.getConfiguration();
      this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
      this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 10);
      this.vectorSize = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 10);

      //always use the maximum hardware threads to load in data and convert data 
      harpThreads = Runtime.getRuntime().availableProcessors();

      //set thread number used in DAAL
      LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
      Environment.setNumberOfThreads(numThreads);
      LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

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
      this.datasource = new HarpDAALDataSource(harpThreads, conf);
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

      runMOM(context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

    private void runMOM(Context context) throws IOException 
    {

	    // ---------- load data ----------
	    NumericTable featureArray_daal = this.datasource.createDenseNumericTable(this.inputFiles, this.vectorSize, "," , this.daal_Context);

	    PartialResult[] outcome = computeOnLocalNode(featureArray_daal);

	    if(this.isMaster()){
		    computeOnMasterNode(outcome);
		    printResults(result);
	    }

	    daal_Context.dispose();


    }

  private PartialResult[] computeOnLocalNode(NumericTable featureArray_daal) throws java.io.IOException 
  {

	  /* Create algorithm objects to compute a variance-covariance matrix in the distributed processing mode using the default method */
	  DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense);
	  /* Set input objects for the algorithm */
	  algorithm.input.set(InputId.data, featureArray_daal);

	  /* Compute partial estimates on nodes */
	  partialResult = algorithm.compute();

	  //comm gather
	  SerializableBase[] partial_res = this.harpcomm.harpdaal_gather(partialResult, this.getMasterID(), "MOM", "gather_partial_res");
	  PartialResult[] partial_output = new PartialResult[this.num_mappers];
	  if (this.isMaster() == true)
	  {
		  for(int j=0;j<this.num_mappers;j++)
			  partial_output[j] = (PartialResult)(partial_res[j]);
	  }

	  return partial_output;
  }

  private void computeOnMasterNode(PartialResult[] partialResultTable)
  {
	  DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Double.class, Method.defaultDense);
	  for(int j=0; j<this.num_mappers;j++)
		  algorithm.input.add(DistributedStep2MasterInputId.partialResults, partialResultTable[j]);

	  algorithm.compute();
	  result = algorithm.finalizeCompute();
  }

  private void printResults(Result result)
  {
	  NumericTable minimum = result.get(ResultId.minimum);
	  NumericTable maximum = result.get(ResultId.maximum);
	  NumericTable sum = result.get(ResultId.sum);
	  NumericTable sumSquares = result.get(ResultId.sumSquares);
	  NumericTable sumSquaresCentered = result.get(ResultId.sumSquaresCentered);
	  NumericTable mean = result.get(ResultId.mean);
	  NumericTable secondOrderRawMoment = result.get(ResultId.secondOrderRawMoment);
	  NumericTable variance = result.get(ResultId.variance);
	  NumericTable standardDeviation = result.get(ResultId.standardDeviation);
	  NumericTable variation = result.get(ResultId.variation);

	  System.out.println("Low order moments:");
	  Service.printNumericTable("Min:", minimum);
	  Service.printNumericTable("Max:", maximum);
	  Service.printNumericTable("Sum:", sum);
	  Service.printNumericTable("SumSquares:", sumSquares);
	  Service.printNumericTable("SumSquaredDiffFromMean:", sumSquaresCentered);
	  Service.printNumericTable("Mean:", mean);
	  Service.printNumericTable("SecondOrderRawMoment:", secondOrderRawMoment);
	  Service.printNumericTable("Variance:", variance);
	  Service.printNumericTable("StandartDeviation:", standardDeviation);
	  Service.printNumericTable("Variation:", variation);

  }


}
