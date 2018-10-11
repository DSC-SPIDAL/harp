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

import com.intel.daal.algorithms.covariance.*;
import com.intel.daal.data_management.data.HomogenNumericTable;
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
 * @brief the Harp mapper for running covariance 
 */


public class COVDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{



  private PartialResult partialResult;
  private SerializableBase[] partialResult_comm;
  private Result result;
  private int numMappers;
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
      this.numMappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
      numThreads = this.conf
      .getInt(HarpDAALConstants.NUM_THREADS, 10);

      //always use the maximum hardware threads to load in data and convert data 
      harpThreads = Runtime.getRuntime().availableProcessors();
      //set thread number used in DAAL
      LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
      Environment.setNumberOfThreads(numThreads);
      LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

      LOG.info("Num Mappers " + numMappers);
      LOG.info("Num Threads " + numThreads);
      LOG.info("Num harp load data threads " + harpThreads);

      long endTime = System.currentTimeMillis();
      LOG.info("config (ms) :" + (endTime - startTime));
      System.out.println("Collective Mapper launched");

    }

    protected void mapCollective(
      KeyValReader reader, Context context)
    throws IOException, InterruptedException {

      long startTime = System.currentTimeMillis();
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
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.numMappers, daal_Context, this);

      runCOV(context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

    private void runCOV(Context context) throws IOException {

	//read in csr files with filenames in trainingDataFiles
	NumericTable featureArray_daal = this.datasource.loadCSRNumericTable(this.inputFiles, ",", daal_Context);
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


    }

  private void computeOnLocalNode(NumericTable featureArray_daal) throws java.io.IOException {

    /* Create algorithm objects to compute a variance-covariance matrix in the distributed processing mode using the default method */
    DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, Method.fastCSR);

    /* Set input objects for the algorithm */
    algorithm.input.set(InputId.data, featureArray_daal);

    /* Compute partial estimates on nodes */
    partialResult = algorithm.compute();

    // gather the partial result
    this.partialResult_comm = this.harpcomm.harpdaal_gather(partialResult, "Covariance", "local-reduce");
    
  }

  private void computeOnMasterNode(){

    DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Double.class, Method.fastCSR);
    for(int j=0;j<this.numMappers; j++)
	algorithm.input.add(DistributedStep2MasterInputId.partialResults, (PartialResult)(partialResult_comm[j])); 
    
    algorithm.compute();
    result = algorithm.finalizeCompute();
  }
   

}
