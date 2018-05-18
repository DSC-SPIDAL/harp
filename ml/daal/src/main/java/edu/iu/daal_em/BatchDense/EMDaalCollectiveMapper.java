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

package edu.iu.daal_em.BatchDense;

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
import com.intel.daal.algorithms.em_gmm.*;
import com.intel.daal.algorithms.em_gmm.init.*;

// daal data structure and service
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running K-means
 */
public class EMDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int numMappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
    	private int nComponents;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	/* Apriori algorithm parameters */
	private NumericTable input;

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

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        Configuration configuration =
            context.getConfiguration();

	this.numMappers = configuration.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = configuration.getInt(HarpDAALConstants.NUM_THREADS, 10);
        this.harpThreads = Runtime.getRuntime().availableProcessors();
	this.fileDim = configuration.getInt(HarpDAALConstants.FILE_DIM, 3);
	this.nComponents = configuration.getInt(Constants.NUM_COMPONENTS, 2);

        LOG.info("File Dim " + this.fileDim);
        LOG.info("Num Mappers " + this.numMappers);
        LOG.info("Num Threads " + this.numThreads);
        LOG.info("Num harp load data threads " + harpThreads);
        LOG.info("Num components " + nComponents);

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
            List<String> dataFiles =
                new LinkedList<String>();
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                        + value);
                LOG.info("file name: " + value);
                dataFiles.add(value);
            }
            
            Configuration conf = context.getConfiguration();

	    // ----------------------- runtime settings -----------------------
            //set thread number used in DAAL
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	    this.datasource = new HarpDAALDataSource(dataFiles, fileDim, harpThreads, conf);

	    // ----------------------- start the execution -----------------------
            runEM(conf, context);
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
        private void runEM(Configuration conf, Context context) throws IOException 
	{
		// ---------- load data ----------
		this.datasource.loadFiles();

		// // ---------- training and testing ----------
		input = new HomogenNumericTable(daal_Context, Double.class, this.fileDim, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
		this.datasource.loadDataBlock(input);

		/* Create an algorithm to initialize the EM algorithm for the GMM */
		InitBatch initAlgorithm = new InitBatch(daal_Context, Double.class, InitMethod.defaultDense, nComponents);

		/* Set an input object for the initialization algorithm */
		initAlgorithm.input.set(InitInputId.data, input);

		InitResult initResult = initAlgorithm.compute();

		/* Create an algorithm for EM clustering */
		Batch algorithm = new Batch(daal_Context, Double.class, Method.defaultDense, nComponents);

		/* Set an input object for the algorithm */
		algorithm.input.set(InputId.data, input);
		algorithm.input.set(InputValuesId.inputValues, initResult);

		/* Clusterize the data */
		Result result = algorithm.compute();

		NumericTable means = result.get(ResultId.means);
		NumericTable weights = result.get(ResultId.weights);

		/* Print the results */
		Service.printNumericTable("Means", means);
		Service.printNumericTable("Weights", weights);

		for (int i = 0; i < nComponents; i++) {
			NumericTable covariance = result.get(ResultCovariancesId.covariances, i);
			Service.printNumericTable("Covariance", covariance);
		}

		daal_Context.dispose();

	}

	

}
