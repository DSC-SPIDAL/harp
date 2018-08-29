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

package edu.iu.daal_kmeans.allreducecsr;

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
import java.nio.DoubleBuffer;

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


//import daal api for algorithms 
import com.intel.daal.algorithms.kmeans.*;
import com.intel.daal.algorithms.kmeans.init.*;

//import daal api for data management/services 
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running Kmeans CSR distri
 *
 */
public class KmeansDaalCollectiveMapper
	extends
	CollectiveMapper<String, String, Object, Object>{

		private long nClasses;
		private int nVectorsInBlock;
		private int nIterations;
		private int num_mappers;
		private int numThreads; //used in computation
		private int harpThreads; //used in data conversion
		private List<String> inputFiles;
		private Configuration conf;

		private NumericTable trainData;
		private InitDistributedStep1Local initLocal;
		private InitPartialResult initPres;
		private InitResult initResult;
		private PartialResult pres;
		private InitDistributedStep2Master initMaster=null;
		private NumericTable centroids;
		private NumericTable assignments;
		private NumericTable objectiveFunction = null;
		private DistributedStep2Master masterAlgorithm = null;

		private static HarpDAALDataSource datasource;
		private static HarpDAALComm harpcomm;	
		private static DaalContext daal_Context = new DaalContext();

		/**
		 * Mapper configuration.
		 */
		@Override
		protected void setup(Context context) throws IOException, InterruptedException 
		{

			long startTime = System.currentTimeMillis();

			this.conf = context.getConfiguration();
			this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
			this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
			this.nClasses = this.conf.getLong(HarpDAALConstants.NUM_CLASS, 20);
			this.nVectorsInBlock = this.conf.getInt(Constants.NUM_VEC_BLOCK, 8000);
			this.nIterations = this.conf.getInt(HarpDAALConstants.NUM_ITERATIONS, 5);

			//always use the maximum hardware threads to load in data and convert data 
			harpThreads = Runtime.getRuntime().availableProcessors();

			//set thread number used in DAAL
			LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
			Environment.setNumberOfThreads(numThreads);
			LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

			LOG.info("Num Mappers " + num_mappers);
			LOG.info("Num Threads " + numThreads);
			LOG.info("Num classes " + nClasses);
			LOG.info("Num harp load data threads " + harpThreads);

			long endTime = System.currentTimeMillis();
			LOG.info("config (ms) :" + (endTime - startTime));
			System.out.println("Collective Mapper launched");

		}

		protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
		{
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

			// create data source
			this.datasource = new HarpDAALDataSource(harpThreads, conf);
			// create communicator
			this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

			runKmeans(context);
			this.freeMemory();
			this.freeConn();
			System.gc();

		}

		private void runKmeans(Context context) throws IOException 
		{

			initDataCentroids();
			calcCentroids();
			calcAssignments();
			
			if (this.isMaster())
			{
				/* Print the results */
				Service.printNumericTable("First 10 cluster assignments from 1st node:", assignments, 10);
				Service.printNumericTable("First 10 dimensions of centroids:", centroids, 20, 10);
				Service.printNumericTable("Objective function value:", objectiveFunction);
			}
			                        
			daal_Context.dispose();
                       
		}

		private void initDataCentroids() throws IOException
		{//{{{
			

			//load csr training table
			this.trainData = this.datasource.loadCSRNumericTable(this.inputFiles, ",", daal_Context);
			initLocal = new InitDistributedStep1Local(daal_Context, Double.class,
                    	InitMethod.randomCSR, nClasses, num_mappers*nVectorsInBlock, this.getSelfID()*nVectorsInBlock);

			/* Set the input data to the algorithm */
            		initLocal.input.set(InitInputId.data, trainData);
            		initPres = initLocal.compute();
	
			if (this.isMaster())
			{
				this.initMaster = new InitDistributedStep2Master(daal_Context, Double.class,
						InitMethod.randomCSR, nClasses);
			}

			// reduce init pres
			SerializableBase[] initPres_comm = this.harpcomm.harpdaal_gather(this.initPres, "Kmeans", "reduce_initPres");
			if (this.isMaster() && initPres_comm != null)
			{
				for (int i=0; i<num_mappers; i++)
				   this.initMaster.input.add(InitDistributedStep2MasterInputId.partialResults, (InitPartialResult)(initPres_comm[i]));
			}

			this.barrier("kmeans", "finish comm init pres");

			if (this.isMaster())
			{
			   initMaster.compute();
			   initResult = initMaster.finalizeCompute();
			   centroids = initResult.get(InitResultId.centroids); 
			}

			this.barrier("kmeans", "finish compute init pres");

			//broadcaset centroids 
			this.centroids = (NumericTable)(this.harpcomm.harpdaal_braodcast(this.centroids, "kmeans", "bcast", true));
			
		}//}}}

		private void calcCentroids() throws IOException
		{//{{{

		   if (this.isMaster())
		     masterAlgorithm = new DistributedStep2Master(daal_Context, Double.class, Method.lloydCSR, nClasses);

		   //start the iterations
        	   for (int it = 0; it < nIterations; it++) 
		   {
			   DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, 
					   Method.lloydCSR, nClasses);

			   /* Set the input data to the algorithm */
			   algorithm.input.set(InputId.data, trainData);
			   algorithm.input.set(InputId.inputCentroids, centroids);
			   pres = algorithm.compute();

			   //reduce pres results to master mapper
			   SerializableBase[] pres_comm = this.harpcomm.harpdaal_gather(this.pres, "Kmeans", "reduce_Pres");
			   if (this.isMaster() && pres_comm != null)
			   {
				   for (int i=0; i<num_mappers; i++)
					masterAlgorithm.input.add(DistributedStep2MasterInputId.partialResults, (PartialResult)(pres_comm[i]));
			   }

			   this.barrier("kmeans", "barrier reduce pres");

			   if (this.isMaster())
			   {
				   //finalize the computation
				   masterAlgorithm.compute();
				   Result result = masterAlgorithm.finalizeCompute();

				   centroids = result.get(ResultId.centroids);
				   objectiveFunction = result.get(ResultId.objectiveFunction);

			   }

			   this.barrier("kmeans", "finish_one_iter");
			   //broadcast centroids
			   this.centroids = (NumericTable)(this.harpcomm.harpdaal_braodcast(this.centroids, "kmeans", "bcast", true));

		   }

		}//}}}

		private void calcAssignments() throws IOException
		{//{{{
			Batch algorithm = new Batch(daal_Context, Double.class, Method.lloydCSR, nClasses, 0);
            		algorithm.parameter.setAssignFlag(true);
			algorithm.input.set(InputId.data, trainData);
            		algorithm.input.set(InputId.inputCentroids, centroids);
			Result result = algorithm.compute();
			assignments = result.get(ResultId.assignments);
		}//}}}


	}
