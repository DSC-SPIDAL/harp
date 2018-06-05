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

package edu.iu.daal_mom.csrdistri;

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
import java.nio.DoubleBuffer;

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


//import daal.jar API
import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running Neural Network
 */


public class MOMDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private PartialResult partialResult;
  private SerializableBase[] gather_output;
  private Result result;
  private int num_mappers;
  private int numThreads;
  private int harpThreads; 
  private List<String> inputFiles;
  private Configuration conf;

  private static DaalContext daal_Context = new DaalContext();
  private static HarpDAALComm harpcomm;	
  private static HarpDAALDataSource datasource;

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

        computeOnLocalNode();
        if(this.isMaster())
	{
            computeOnMasterNode();
            printResults(result);

        }

        daal_Context.dispose();


    }

  private void computeOnLocalNode() throws java.io.IOException 
  {

	  //read in csr files with filenames in trainingDataFiles
	  NumericTable featureArray_daal = this.datasource.loadCSRNumericTable(this.inputFiles, ",", daal_Context);
	  /* Create algorithm objects to compute a variance-covariance matrix in the distributed processing mode using the default method */
	  DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, Method.fastCSR);
	  /* Set input objects for the algorithm */
	  algorithm.input.set(InputId.data, featureArray_daal);

	  /* Compute partial estimates on nodes */
	  partialResult = algorithm.compute();

	  this.gather_output = this.harpcomm.harpdaal_gather(partialResult, this.getMasterID(), "Mom", "gather_pres");
  }

  private void computeOnMasterNode()
  {
	  DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Double.class, Method.fastCSR);
	  for(int j=0; j< this.num_mappers;j++)
	  {
		  PartialResult des_output = (PartialResult)(this.gather_output[j]);
		  algorithm.input.add(DistributedStep2MasterInputId.partialResults, des_output);
	  }
	  
	  algorithm.compute();
	  result = algorithm.finalizeCompute();
  }

  private void printResults(Result result)
  {

	  HomogenNumericTable minimum = (HomogenNumericTable) result.get(ResultId.minimum);
	  HomogenNumericTable maximum = (HomogenNumericTable) result.get(ResultId.maximum);
	  HomogenNumericTable sum = (HomogenNumericTable) result.get(ResultId.sum);
	  HomogenNumericTable sumSquares = (HomogenNumericTable) result.get(ResultId.sumSquares);
	  HomogenNumericTable sumSquaresCentered = (HomogenNumericTable) result.get(ResultId.sumSquaresCentered);
	  HomogenNumericTable mean = (HomogenNumericTable) result.get(ResultId.mean);
	  HomogenNumericTable secondOrderRawMoment = (HomogenNumericTable) result.get(ResultId.secondOrderRawMoment);
	  HomogenNumericTable variance = (HomogenNumericTable) result.get(ResultId.variance);
	  HomogenNumericTable standardDeviation = (HomogenNumericTable) result.get(ResultId.standardDeviation);
	  HomogenNumericTable variation = (HomogenNumericTable) result.get(ResultId.variation);

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
