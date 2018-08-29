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

package edu.iu.daal_pivoted_qr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.DoubleBuffer;
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

// daal algorithm specific 
import com.intel.daal.algorithms.pivoted_qr.Batch;
import com.intel.daal.algorithms.pivoted_qr.InputId;
import com.intel.daal.algorithms.pivoted_qr.Method;
import com.intel.daal.algorithms.pivoted_qr.Result;
import com.intel.daal.algorithms.pivoted_qr.ResultId;


// daal data structure and service
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class PQRDaalCollectiveMapper extends CollectiveMapper<String, String, Object, Object> 
{

	//cmd args
	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int fileDim;
	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	/* Apriori algorithm parameters */
	private NumericTable input;

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
		this.harpThreads = Runtime.getRuntime().availableProcessors();
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 18);

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("File Dim " + this.fileDim);
		LOG.info("Num Mappers " + this.num_mappers);
		LOG.info("Num Threads " + this.numThreads);
		LOG.info("Num harp load data threads " + harpThreads);

		long endTime = System.currentTimeMillis();
		LOG.info("config (ms) :"
				+ (endTime - startTime));

	}

	// Assigns the reader to different nodes
	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
	{
		long startTime = System.currentTimeMillis();

		// read data file names from HDFS
		this.inputFiles = new LinkedList<String>();
		while (reader.nextKeyValue()) {
			String key = reader.getCurrentKey();
			String value = reader.getCurrentValue();
			LOG.info("Key: " + key + ", Value: "
					+ value);
			LOG.info("file name: " + value);
			this.inputFiles.add(value);
		}

		this.datasource = new HarpDAALDataSource(harpThreads, conf);

		// ----------------------- start the execution -----------------------
		runPQR(context);
		this.freeMemory();
		this.freeConn();
		System.gc();
	}

	/**
	 * @brief run Association Rules by invoking DAAL Java API
	 *
	 * @param fileNames
	 * @param conf
	 * @param context
	 *
	 * @return 
	 */
	private void runPQR(Context context) throws IOException 
	{
		// ---------- load data ----------
		this.input = this.datasource.createDenseNumericTable(this.inputFiles, this.fileDim, "," , this.daal_Context);
		/* Create an algorithm to compute pivoted QR decomposition */
		Batch pivotedQRAlgorithm = new Batch(daal_Context, Double.class, Method.defaultDense);
		pivotedQRAlgorithm.input.set(InputId.data, input);

		/* Compute pivoted QR decomposition */
		Result res = pivotedQRAlgorithm.compute();

		NumericTable matrixQ = res.get(ResultId.matrixQ);
		NumericTable matrixR = res.get(ResultId.matrixR);
		NumericTable permutationMatrix = res.get(ResultId.permutationMatrix);

		/* Print the results */
		printResults(input, matrixQ, matrixR, permutationMatrix);

		daal_Context.dispose();

	}

	private void printResults(NumericTable data, NumericTable Q, NumericTable R, NumericTable P) {
		Service.printNumericTable("Input matrix (10 first vectors):", data, 10);
		Service.printNumericTable("Orthogonal matrix Q (10 first vectors):", Q, 10);
		Service.printNumericTable("Triangular matrix R:", R);
		Service.printNumericTable("Permutation matrix P:", P);
	}


}
