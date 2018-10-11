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

import com.intel.daal.algorithms.em_gmm.*;
import com.intel.daal.algorithms.em_gmm.init.InitBatch;
import com.intel.daal.algorithms.em_gmm.init.InitInputId;
import com.intel.daal.algorithms.em_gmm.init.InitMethod;
import com.intel.daal.algorithms.em_gmm.init.InitResult;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.data_aux.Service;
import edu.iu.datasource.HarpDAALDataSource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

// daal algorithm specific
// daal data structure and service

/**
 * @brief the Harp mapper for running K-means
 */
public class EMDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
    	private int nComponents;
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
	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 3);
	this.nComponents = this.conf.getInt(Constants.NUM_COMPONENTS, 2);

	//set thread number used in DAAL
	LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
	Environment.setNumberOfThreads(numThreads);
	LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	LOG.info("File Dim " + this.fileDim);
        LOG.info("Num Mappers " + this.num_mappers);
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
            runEM(context);
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
        private void runEM(Context context) throws IOException 
	{

		// ---------- training and testing ----------
		this.input = this.datasource.createDenseNumericTable(this.inputFiles, this.fileDim, "," , this.daal_Context);

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
