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

package edu.iu.daal_qr;

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
import com.intel.daal.algorithms.qr.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

public class QRDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object>
{

	private DataCollection dataFromStep1ForStep2;
	private DataCollection dataFromStep1ForStep3;
	private DataCollection dataFromStep2ForStep3;
	private static KeyValueDataCollection inputForStep3FromStep2;

	private NumericTable R;
	private NumericTable Qi;

	private DistributedStep1Local qrStep1Local;
	private DistributedStep2Master qrStep2Master;
	private DistributedStep3Local qrStep3Local;

	private int num_mappers;
	private int numThreads;
	private int harpThreads; 
	private int fileDim = 18;
	private int vectorSize = 18;

	private static HarpDAALDataSource datasource;
	private static HarpDAALComm harpcomm;	
	private static DaalContext daal_Context = new DaalContext();

	//to measure the time
	private long total_time = 0;
	private long load_time = 0;
	private long convert_time = 0;
	private long compute_time = 0;
	private long comm_time = 0;
	private long ts_start = 0;
	private long ts_end = 0;
	private long ts1 = 0;
	private long ts2 = 0;

	/**
	 * Mapper configuration.
	 */
	@Override
	protected void setup(Context context)
		throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		Configuration configuration =
			context.getConfiguration();
		this.num_mappers = configuration
			.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
		this.numThreads = configuration
			.getInt(HarpDAALConstants.NUM_THREADS, 10);
		this.fileDim = configuration
			.getInt(HarpDAALConstants.FILE_DIM, 18);
		this.vectorSize = configuration
			.getInt(HarpDAALConstants.FEATURE_DIM, 18);

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
			throws IOException, InterruptedException 
		{//{{{
			long startTime = System.currentTimeMillis();
			List<String> trainingDataFiles =
				new LinkedList<String>();

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

			runQR(conf, context);
			LOG.info("Total iterations in master view: "
					+ (System.currentTimeMillis() - startTime));
			this.freeMemory();
			this.freeConn();
			System.gc();

		}//}}}



	private void runQR(Configuration conf, Context context) throws IOException 
	{//{{{

		ts_start = System.currentTimeMillis();

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		this.datasource.loadFiles();
		NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, this.vectorSize, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
		this.datasource.loadDataBlock(featureArray_daal);

		qrStep1Local = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense);
		qrStep1Local.input.set(InputId.data, featureArray_daal);
		DistributedStep1LocalPartialResult pres = qrStep1Local.compute();
		dataFromStep1ForStep2 = pres.get(PartialResultId.outputOfStep1ForStep2);
		dataFromStep1ForStep3 = pres.get(PartialResultId.outputOfStep1ForStep3);

		ts1 = System.currentTimeMillis();

		//gather dataFromStep1forStep2 on master mapper
		SerializableBase[] gather_out = this.harpcomm.harpdaal_gather(dataFromStep1ForStep2, this.getMasterID(), "QR", "gather_step1forstep2");

		if(this.isMaster())
		{

			qrStep2Master = new DistributedStep2Master(daal_Context, Double.class, Method.defaultDense);
			for(int j=0;j<this.num_mappers;j++)
			{
				DataCollection des_out = (DataCollection)(gather_out[j]); 
				qrStep2Master.input.add(DistributedStep2MasterInputId.inputOfStep2FromStep1, j, des_out);
			}

			ts1 = System.currentTimeMillis();
			DistributedStep2MasterPartialResult presStep2 = qrStep2Master.compute();
			inputForStep3FromStep2 = presStep2.get(DistributedPartialResultCollectionId.outputOfStep2ForStep3);

			Result result = qrStep2Master.finalizeCompute();
			R = result.get(ResultId.matrixR);

			ts2 = System.currentTimeMillis();
			compute_time += (ts2 - ts1);
		}


		//broadcast inputForStep3FromStep2
		SerializableBase bcast_out = this.harpcomm.harpdaal_braodcast(inputForStep3FromStep2, this.getMasterID(), 
				"QR", "bcast_step3fromstep2", true);

		// System.out.println("number of partition in partialstep32 after broadcast :" + partialStep32.getNumPartitions());
		qrStep3Local = new DistributedStep3Local(daal_Context, Double.class, Method.defaultDense);
		qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep1,dataFromStep1ForStep3); 

		KeyValueDataCollection des_bcast_out = (KeyValueDataCollection)(bcast_out);
		qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep2, (DataCollection)des_bcast_out.get(this.getSelfID())); 

		ts1 = System.currentTimeMillis();
		qrStep3Local.compute();
		Result result = qrStep3Local.finalizeCompute();
		ts2 = System.currentTimeMillis();
		compute_time += (ts2 - ts1);

		Qi = result.get(ResultId.matrixQ);
		System.out.println("number of rows" + Qi.getNumberOfRows());
		System.out.println("number of columns" + Qi.getNumberOfColumns());

		Service.printNumericTable("Orthogonal matrix Q (10 first vectors):", Qi, 10);
		if(this.isMaster()){
			Service.printNumericTable("Triangular matrix R:", R);
		}

		ts_end = System.currentTimeMillis();
		total_time = (ts_end - ts_start);

		LOG.info("Total Execution Time of QR: "+ total_time);
		LOG.info("Loading Data Time of QR: "+ load_time);
		LOG.info("Computation Time of QR: "+ compute_time);
		LOG.info("Comm Time of QR: "+ comm_time);
		LOG.info("DataType Convert Time of QR: "+ convert_time);
		LOG.info("Misc Time of QR: "+ (total_time - load_time - compute_time - comm_time - convert_time));

	}//}}}


}
