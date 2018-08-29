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

package edu.iu.daal_kernel_func.LinDenseBatch;

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

// daal algorithm module
import com.intel.daal.algorithms.kernel_function.InputId;
import com.intel.daal.algorithms.kernel_function.ResultId;

// daal data structure and service module
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K nearest neighbors 
 */
public class LinDenseDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
  	private String rightfilepath;

	private int nFeatures;
	private double k;
	private double b;

	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        this.conf = context.getConfiguration();

	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 4);
	this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
        this.harpThreads = Runtime.getRuntime().availableProcessors();

	//set thread number used in DAAL
	LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
	Environment.setNumberOfThreads(numThreads);
	LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 4);
	this.k = this.conf.getDouble(Constants.K, 1.0);
	this.b = this.conf.getDouble(Constants.B, 0.0);

	this.rightfilepath = this.conf.get(Constants.RIGHT_FILE_PATH,"");

        LOG.info("File Dim " + this.fileDim);
        LOG.info("Num Mappers " + this.num_mappers);
        LOG.info("Num Threads " + this.numThreads);
        LOG.info("Num harp load data threads " + harpThreads);

        long endTime = System.currentTimeMillis();
        LOG.info("config (ms) :"
                + (endTime - startTime));
        }

        // Assigns the reader to different nodes
        protected void mapCollective(
                KeyValReader reader, Context context)
            throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();

	    // read data file names from HDFS
            this.inputFiles =
                new LinkedList<String>();
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
            runLinDense(context);
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
        private void runLinDense(Context context) throws IOException 
	{
		// // ---------- load data ----------
		NumericTable inputX = this.datasource.createDenseNumericTable(this.inputFiles, this.nFeatures, "," , this.daal_Context);
		NumericTable inputY = this.datasource.createDenseNumericTable(this.rightfilepath, this.nFeatures, "," , this.daal_Context);
		// test new HarpDAALNumericTable
		HarpDAALNumericTable merged = new HarpDAALNumericTable(this.daal_Context, inputX);
		LOG.info("HarpDAALNumericTable column number: " + merged.getNumberOfColumns());
		merged.addPartition(inputY, 1);
		LOG.info("HarpDAALNumericTable column number: " + merged.getNumberOfColumns() + "; num partitions " + merged.getNumberOfPartitions());
		NumericTable get_inputX = merged.getPartition(0);
		NumericTable get_inputY = merged.getPartition(1);
		int row_num = (int)get_inputX.getNumberOfRows() + 5;
		int col_num = (int)get_inputX.getNumberOfColumns() + 5;
		HarpNumericTable inputZ = new HarpNumericTable(daal_Context, col_num, row_num);

		for(int j=0;j<col_num;j++)
                  inputZ.setArray(new double[row_num], j, j);

		merged.addPartition(inputZ, 2);
		LOG.info("HarpDAALNumericTable column number: " + merged.getNumberOfColumns() + "; num partitions " + merged.getNumberOfPartitions());

		// HarpDAALNumericTable testTable = 
		// replace this by HDFSDataSource
		// check rightpath name
		// LOG.info(this.rightfilepath);
	        // HDFSDataSource inputY_source = new HDFSDataSource(this.daal_Context, this.rightfilepath, 
		// 		DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
		// 	        DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);
                //
		// inputY_source.loadDataBlock();
		// NumericTable inputY = inputY_source.getNumericTable();
		// inputY_source.dispose();

		/* Create an algorithm */
		com.intel.daal.algorithms.kernel_function.linear.Batch algorithm = new com.intel.daal.algorithms.kernel_function.linear.Batch(daal_Context, Double.class);

		/* Set the kernel algorithm parameter */
		algorithm.parameter.setK(k);
		algorithm.parameter.setB(b);
		algorithm.parameter.setComputationMode(com.intel.daal.algorithms.kernel_function.ComputationMode.matrixMatrix);

		/* Set an input data table for the algorithm */
		algorithm.input.set(InputId.X, inputX);
		algorithm.input.set(InputId.Y, inputY);

		/* Compute the linear kernel function */
		com.intel.daal.algorithms.kernel_function.linear.Result result = algorithm.compute();

		/* Get the computed results */
		NumericTable values = result.get(ResultId.values);

		/* Print the results */
		Service.printNumericTable("Result of kernel function:", values);

		daal_Context.dispose();

	}
	   
    
}
