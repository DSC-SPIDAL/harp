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

package edu.iu.daal_pca.svddensedistr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.ByteArray;

import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

import java.nio.DoubleBuffer;

//import PCA from DAAL library
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.algorithms.pca.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.data_management.data.*;

import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running PCA
 */
public class PCADaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object>
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

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context)
		throws IOException, InterruptedException
	{
		long startTime = System.currentTimeMillis();
		this.conf = context.getConfiguration();
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 20);
		this.vectorSize = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 20);
		this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);

		//always use the maximum hardware threads to load in data and convert data 
		harpThreads = Runtime.getRuntime().availableProcessors();

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

	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException
	{
		long startTime = System.currentTimeMillis();
		this.inputFiles = new LinkedList<String>();
		while (reader.nextKeyValue())
		{
			String key = reader.getCurrentKey();
			String value = reader.getCurrentValue();
			LOG.info("Key: " + key + ", Value: " + value);
			this.inputFiles.add(value);
		}

		//init data source
		this.datasource = new HarpDAALDataSource(this.harpThreads, this.conf);
		// create communicator
		this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, this.daal_Context, this);
		runPCA(context);
		LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
	}

	/**
	 * @brief run PCA by invoking DAAL Java API
	 *
	 * @param fileNames
	 * @param conf
	 * @param context
	 *
	 * @return
	 */
	private void runPCA(Context context) throws IOException
	{//{{{

		// loading training data
		NumericTable pointsArray_daal = this.datasource.createDenseNumericTable(this.inputFiles, this.vectorSize, ",", this.daal_Context);

		/* Create an algorithm to compute PCA decomposition using the svd method on local nodes */
		DistributedStep1Local pcaLocal = new DistributedStep1Local(daal_Context, Double.class, Method.svdDense);

		/* Set the input data on local nodes */
		pcaLocal.input.set(InputId.data, pointsArray_daal);


		/*Compute the partial results on the local data nodes*/
		PartialResult pres = (PartialResult)pcaLocal.compute();

		/*Do an reduce to send all the data to the master node*/
		// Table<ByteArray> step1LocalResult_table = communicate(pres);
		SerializableBase[] gather_output = this.harpcomm.harpdaal_gather(pres, this.getMasterID(), "pca", "gather_pres");

		/*Start the Step 2 on the master node*/
		if(this.isMaster())
		{
			/*create a new algorithm for the master node computations*/
			DistributedStep2Master pcaMaster = new DistributedStep2Master(daal_Context, Double.class, Method.svdDense);
			for(int j=0;j<this.num_mappers;j++)
			{
				PartialResult des_output = (PartialResult)(gather_output[j]);
				pcaMaster.input.add(MasterInputId.partialResults, des_output);
			}

			pcaMaster.compute();

			/*get the results from master node*/
			Result res = pcaMaster.finalizeCompute();

			NumericTable eigenValues = res.get(ResultId.eigenValues);
			NumericTable eigenVectors = res.get(ResultId.eigenVectors);

			/*printing the results*/
			Service.printNumericTable("Eigenvalues:", eigenValues);
			Service.printNumericTable("Eigenvectors:", eigenVectors);

			/*free the memory*/
			daal_Context.dispose();
		}


	}//}}}



}
