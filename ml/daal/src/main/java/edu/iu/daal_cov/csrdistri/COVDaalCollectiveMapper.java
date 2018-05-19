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

package edu.iu.daal_cov.csrdistri;

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
import com.intel.daal.algorithms.covariance.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running covariance 
 */


public class COVDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{



  private PartialResult partialResult;
  private SerializableBase[] partialResult_comm;
  private Result result;
  private int pointsPerFile = 50;
  private int vectorSize = 10;
  private int numMappers;
  private int numThreads;
  private int harpThreads; 

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
      Configuration configuration =
      context.getConfiguration();
      numMappers = configuration
      .getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      numThreads = configuration
      .getInt(HarpDAALConstants.NUM_THREADS, 10);

      //always use the maximum hardware threads to load in data and convert data 
      harpThreads = Runtime.getRuntime().availableProcessors();

      LOG.info("Num Mappers " + numMappers);
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
      this.datasource = new HarpDAALDataSource(trainingDataFiles, harpThreads, conf);
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.numMappers, daal_Context, this);

      runCOV(conf, context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

    private void runCOV(Configuration conf, Context context) throws IOException {

        //set thread number used in DAAL
        LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
        Environment.setNumberOfThreads(numThreads);
        LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

        ts_start = System.currentTimeMillis();

	//read in csr files with filenames in trainingDataFiles
	NumericTable featureArray_daal = this.datasource.loadCSRNumericTable(daal_Context);
	// compute on local nodes
        computeOnLocalNode(featureArray_daal);

	// compute on master node
        if(this.isMaster()){
            computeOnMasterNode();
            HomogenNumericTable covariance = (HomogenNumericTable) result.get(ResultId.covariance);
            HomogenNumericTable mean = (HomogenNumericTable) result.get(ResultId.mean);
            Service.printNumericTable("Covariance matrix:", covariance);
            Service.printNumericTable("Mean vector:", mean);
        }

        daal_Context.dispose();

        ts_end = System.currentTimeMillis();
        total_time = (ts_end - ts_start);

        LOG.info("Total Execution Time of Cov: "+ total_time);
        LOG.info("Loading Data Time of Cov: "+ load_time);
        LOG.info("Computation Time of Cov: "+ compute_time);
        LOG.info("Comm Time of Cov: "+ comm_time);
        LOG.info("DataType Convert Time of Cov: "+ convert_time);
        LOG.info("Misc Time of Cov: "+ (total_time - load_time - compute_time - comm_time - convert_time));
    }

  private void computeOnLocalNode(NumericTable featureArray_daal) throws java.io.IOException {

    ts1 = System.currentTimeMillis();
    /* Create algorithm objects to compute a variance-covariance matrix in the distributed processing mode using the default method */
    DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, Method.fastCSR);

    /* Set input objects for the algorithm */
    algorithm.input.set(InputId.data, featureArray_daal);

    /* Compute partial estimates on nodes */
    partialResult = algorithm.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    // gather the partial result
    ts1 = System.currentTimeMillis();
    this.partialResult_comm = this.harpcomm.harpdaal_gather(partialResult, "Covariance", "local-reduce");
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);
    
  }

  private void computeOnMasterNode(){

    DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Double.class, Method.fastCSR);
    ts1 = System.currentTimeMillis();
    for(int j=0;j<this.numMappers; j++)
	algorithm.input.add(DistributedStep2MasterInputId.partialResults, (PartialResult)(partialResult_comm[j])); 
    
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    algorithm.compute();
    result = algorithm.finalizeCompute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);
  }
   

}
