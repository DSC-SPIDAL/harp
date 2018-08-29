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

package edu.iu.daal_svd;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

import java.nio.DoubleBuffer;

//import daa.jar API
import com.intel.daal.algorithms.svd.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.Environment;
import com.intel.daal.services.DaalContext;

/**
 * @brief the Harp mapper for running K-means
 */
public class SVDDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

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

	private static NumericTable   S;
	private static NumericTable   V;
	private static NumericTable   U;      

	// SVD declarations
	private static DistributedStep1Local  svdStep1Local;
	private static DistributedStep2Master svdStep2Master;
	private static DistributedStep3Local  svdStep3Local;

	// In Psuedo Distributed (Intel -- daal) below three are arrays of
	// Datacollection object. But here we want only one object on each
	// node. We will gather the values on this using harp's collective 
	// communication method.

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context)
		throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();
		this.conf = context.getConfiguration();
		this.fileDim = conf.getInt(HarpDAALConstants.FILE_DIM,20);
		this.vectorSize = conf.getInt(HarpDAALConstants.FEATURE_DIM,20);
		this.num_mappers = conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = conf.getInt(HarpDAALConstants.NUM_THREADS, 10);

		//always use the maximum hardware threads to load in data and convert data 
		this.harpThreads = Runtime.getRuntime().availableProcessors();

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("Vector Size " + vectorSize);
		LOG.info("Num Mappers " + num_mappers);
		LOG.info("Num Threads " + numThreads);
		LOG.info("Num harp load data threads " + harpThreads);

		long endTime = System.currentTimeMillis();
		LOG.info("config (ms) :" + (endTime - startTime));
	}

	// Assigns the reader to different nodes
	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException 
	{
		long startTime = System.currentTimeMillis();
		this.inputFiles = new LinkedList<String>();
		while (reader.nextKeyValue()) {
			String key = reader.getCurrentKey();
			String value = reader.getCurrentValue();
			LOG.info("Key: " + key + ", Value: "
					+ value);
			// System.out.println("file name: " + value);
			LOG.info("file name: " + value);
			this.inputFiles.add(value);
		}

		//init data source
		this.datasource = new HarpDAALDataSource(this.harpThreads, this.conf);
		// create communicator
		this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

		runSVD(context);
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
	private void runSVD(Context context) throws IOException 
	{//{{{

		// loading training data
		NumericTable pointsArray_daal = this.datasource.createDenseNumericTable(this.inputFiles, this.vectorSize, ",", this.daal_Context);

		// Compute the results
		Service.printNumericTable("Model pointsArray_daal: ", pointsArray_daal, 10,10);
		svdStep1Local = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense);

		/* Set the input data on local nodes */
		NumericTable input = pointsArray_daal;
		svdStep1Local.input.set(InputId.data, input);

		/* Compute SVD */
		DistributedStep1LocalPartialResult pres = svdStep1Local.compute();

		/* Get the results for next steps */
		DataCollection dataFromStep1ForStep2 = pres.get(PartialResultId.outputOfStep1ForStep2);
		DataCollection dataFromStep1ForStep3 = pres.get(PartialResultId.outputOfStep1ForStep3);

		// Communicate the results
		SerializableBase[] gather_output = this.harpcomm.harpdaal_gather(dataFromStep1ForStep2, this.getMasterID(), "SVD", "gather_step1forstep2");

		KeyValueDataCollection inputForStep3FromStep2 = null;

		if (this.isMaster()) 
		{
			svdStep2Master = new DistributedStep2Master(daal_Context, Double.class, Method.defaultDense);
			for(int j=0;j<this.num_mappers;j++)
			{
				DataCollection des_output = (DataCollection)(gather_output[j]);
				svdStep2Master.input.add(DistributedStep2MasterInputId.inputOfStep2FromStep1, j , des_output);
			}

			DistributedStep2MasterPartialResult pres2master = svdStep2Master.compute();

			/* Get the result for step 3 */
			inputForStep3FromStep2 = pres2master.get(DistributedPartialResultCollectionId.outputOfStep2ForStep3);

			Result result = svdStep2Master.finalizeCompute();

			/* Get final singular values and a matrix of right singular vectors */
			S = result.get(ResultId.singularValues);
			V = result.get(ResultId.rightSingularMatrix);

			Service.printNumericTable("Model S:", S, 10,10);
			Service.printNumericTable("Model V:", V, 10,10);
		}

		// broadcast the results
		SerializableBase bcast_output = this.harpcomm.harpdaal_braodcast(inputForStep3FromStep2, this.getMasterID(), "svd", "bcast_step2tostep3", true);
		
		/* Create an algorithm to compute SVD on local nodes */
		svdStep3Local = new DistributedStep3Local(daal_Context, Double.class, Method.defaultDense);
		svdStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep1, dataFromStep1ForStep3);

		KeyValueDataCollection dataFromStep2ForStep3_keyValue = (KeyValueDataCollection)(bcast_output); 
		svdStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep2, (DataCollection)dataFromStep2ForStep3_keyValue.get(this.getSelfID()));

		/* Compute SVD */
		svdStep3Local.compute();

		Result result = svdStep3Local.finalizeCompute();

		/* Get final matrices of left singular vectors */
		U = result.get(ResultId.leftSingularMatrix);

		pointsArray_daal.freeDataMemory();
		Service.printNumericTable("Model U:", U, 10,10);
		// System.out.println("Final factorization ");
		LOG.info("Final factorization ");
		Service.printNumericTable("Left orthogonal matrix U (10 first vectors):", U, 10,10);


	}//}}}


}
