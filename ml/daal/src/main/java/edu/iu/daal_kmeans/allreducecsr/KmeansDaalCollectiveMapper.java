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

		private long nClasses = 20;
		private int  nVectorsInBlock = 8000;
		private int  nIterations = 5;

		private int numMappers;
		private int numThreads; //used in computation
		private int harpThreads; //used in data conversion

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
				.getInt(Constants.NUM_MAPPERS, 10);
			numThreads = configuration
				.getInt(Constants.NUM_THREADS, 10);
			nClasses = configuration
				.getLong(Constants.NUM_CLASS, 20);

			nVectorsInBlock = configuration
				.getInt(Constants.NUM_VEC_BLOCK, 8000);
			nIterations = configuration
				.getInt(Constants.NUM_ITERATIONS, 5);

			//always use the maximum hardware threads to load in data and convert data 
			harpThreads = Runtime.getRuntime().availableProcessors();

			LOG.info("Num Mappers " + numMappers);
			LOG.info("Num Threads " + numThreads);
			LOG.info("Num classes " + nClasses);
			LOG.info("Num harp load data threads " + harpThreads);

			long endTime = System.currentTimeMillis();
			LOG.info("config (ms) :" + (endTime - startTime));
			System.out.println("Collective Mapper launched");

		}

		protected void mapCollective(
				KeyValReader reader, Context context)
				throws IOException, InterruptedException 
			{

				// long startTime = System.currentTimeMillis();
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

				// create data source
				this.datasource = new HarpDAALDataSource(trainingDataFiles, harpThreads, conf);

				runKmeans(conf, context);
				// LOG.info("Total time of iterations in master view: "
				//   + (System.currentTimeMillis() - startTime));
				this.freeMemory();
				this.freeConn();
				System.gc();

			}

		private void runKmeans(Configuration conf, Context context) throws IOException 
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
			if (this.isMaster())
			{
				initMaster = new InitDistributedStep2Master(daal_Context, Double.class,
						InitMethod.randomCSR, nClasses);
			}

			this.barrier("kmeans", "init-master");

			//load csr training table
			trainData = this.datasource.loadCSRNumericTable(daal_Context);
			initLocal = new InitDistributedStep1Local(daal_Context, Double.class,
                    	InitMethod.randomCSR, nClasses, numMappers*nVectorsInBlock, this.getSelfID()*nVectorsInBlock);

			/* Set the input data to the algorithm */
            		initLocal.input.set(InitInputId.data, trainData);
            		initPres = initLocal.compute();
	
			// reduce init pres
			reduce_initpres();
			this.barrier("kmeans", "finish comm init pres");

			if (this.isMaster())
			{
			   initMaster.compute();
			   initResult = initMaster.finalizeCompute();
			   centroids = initResult.get(InitResultId.centroids); 
			}

			this.barrier("kmeans", "finish compute init pres");

			//broadcaset centroids 
			broadcast_numerictable();
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
			   reduce_pres();

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
			   broadcast_numerictable();

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

		// reduce initpartialresult to master mapper
		private void reduce_initpres() throws IOException
		{//{{{
			Table<ByteArray> initpresTable = new Table<>(0, new ByteArrPlus());
			initpresTable.addPartition(new Partition<>(this.getSelfID(), serializeInitPres(initPres)));

			boolean reduceStatus = false;
			reduceStatus = this.reduce("kmeans", "sync-initPres", initpresTable, this.getMasterID());
			this.barrier("kmeans", "barrier-initPres");

			if(!reduceStatus){
				System.out.println("reduce not successful");
			}
			else{
				System.out.println("reduce successful");
			}

			try {
				//deserialize and add results
				if (this.isMaster())
				{
					for (int i=0; i<numMappers; i++)
					{
						initMaster.input.add(InitDistributedStep2MasterInputId.partialResults, 
								deserializeInitPres(initpresTable.getPartition(i).get()));
					}

				}

			}catch (Exception e) 
			{  
				System.out.println("Fail to deserilize" + e.toString());
				e.printStackTrace();
			}

		}//}}}

		private void reduce_pres() throws IOException
		{//{{{

			Table<ByteArray> presTable = new Table<>(0, new ByteArrPlus());
			presTable.addPartition(new Partition<>(this.getSelfID(), serializePres(pres)));

			boolean reduceStatus = false;
			reduceStatus = this.reduce("kmeans", "sync-Pres", presTable, this.getMasterID());
			this.barrier("kmeans", "barrier-Pres");

			if(!reduceStatus){
				System.out.println("reduce not successful");
			}
			else{
				System.out.println("reduce successful");
			}

			try {
				//deserialize and add results
				if (this.isMaster())
				{
					for (int i=0; i<numMappers; i++)
					{
                				masterAlgorithm.input.add(DistributedStep2MasterInputId.partialResults, 
								deserializePres(presTable.getPartition(i).get()));
					}

				}

			}catch (Exception e) 
			{  
				System.out.println("Fail to deserilize" + e.toString());
				e.printStackTrace();
			}

			this.barrier("kmeans", "barrier reduce pres");

		}//}}}

		// broadcast centroids from master to all mappers
		private void broadcast_numerictable() throws IOException
		{//{{{
		    Table<ByteArray> comm_table = new Table<>(0, new ByteArrPlus());
		    if (this.isMaster())
		       comm_table.addPartition(new Partition<>(this.getMasterID(), serializeNTable(centroids)));

		    this.barrier("kmeans", "bcast add centroids");

		    boolean bcastStatus = false;
		    bcastStatus = this.broadcast("kmeans", "bcast-centroids", comm_table, this.getMasterID(), true);
		    this.barrier("kmeans", "bcast-centorids-finish");

		    if(!bcastStatus){
			    System.out.println("bcast not successful");
		    }
		    else{
			    System.out.println("bcast successful");
		    }

		    try{

			 centroids = deserializeNTable(comm_table.getPartition(this.getMasterID()).get());

		    }catch (Exception e)
		    {
			    System.out.println("Fail to deserilize" + e.toString());
				e.printStackTrace();
		    }

		}//}}}

		private static ByteArray serializeInitPres(InitPartialResult initpres) throws IOException 
		{//{{{

			/* Create an output stream to serialize the numeric table */
			ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
			ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

			/* Serialize the numeric table into the output stream */
			initpres.pack();
			outputStream.writeObject(initpres);

			/* Store the serialized data in an array */
			byte[] serializedInitPres = outputByteStream.toByteArray();

		 	return new ByteArray(serializedInitPres, 0, serializedInitPres.length);
		}//}}}

		private static ByteArray serializePres(PartialResult pres) throws IOException 
		{//{{{

			/* Create an output stream to serialize the numeric table */
			ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
			ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

			/* Serialize the numeric table into the output stream */
			pres.pack();
			outputStream.writeObject(pres);

			/* Store the serialized data in an array */
			byte[] serializedPres = outputByteStream.toByteArray();

		 	return new ByteArray(serializedPres, 0, serializedPres.length);
		}//}}}

		private static InitPartialResult deserializeInitPres(ByteArray byteArray) throws IOException, ClassNotFoundException 
		{//{{{
			/* Create an input stream to deserialize the numeric table from the array */
			byte[] buffer = byteArray.get();
			ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
			ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

			/* Create a numeric table object */
			InitPartialResult restoredDataTable = (InitPartialResult) inputStream.readObject();
			restoredDataTable.unpack(daal_Context);

			return restoredDataTable;
		}//}}}

		private static PartialResult deserializePres(ByteArray byteArray) throws IOException, ClassNotFoundException 
		{//{{{
			/* Create an input stream to deserialize the numeric table from the array */
			byte[] buffer = byteArray.get();
			ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
			ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

			/* Create a numeric table object */
			PartialResult restoredDataTable = (PartialResult) inputStream.readObject();
			restoredDataTable.unpack(daal_Context);

			return restoredDataTable;
		}//}}}

	        private static ByteArray serializeNTable(NumericTable inputTable) throws IOException 
		{//{{{

			/* Create an output stream to serialize the numeric table */
			ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
			ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

			/* Serialize the numeric table into the output stream */
			inputTable.pack();
			outputStream.writeObject(inputTable);

			/* Store the serialized data in an array */
			byte[] serializedTable = outputByteStream.toByteArray();

		 	return new ByteArray(serializedTable, 0, serializedTable.length);

		}//}}}

		private static NumericTable deserializeNTable(ByteArray byteArray) throws IOException, ClassNotFoundException 
		{//{{{
			/* Create an input stream to deserialize the numeric table from the array */
			byte[] buffer = byteArray.get();
			ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
			ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

			/* Create a numeric table object */
			NumericTable restoredDataTable = (NumericTable) inputStream.readObject();
			restoredDataTable.unpack(daal_Context);

			return restoredDataTable;

		}//}}}


	}
