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

package edu.iu.daal_als_batch;

import com.intel.daal.algorithms.implicit_als.Model;
import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;
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
public class ALSBatchDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
	private long nFactors; 	
	private List<String> inputFiles;
	private Configuration conf;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

	/* Apriori algorithm parameters */
	private Model        initialModel;
    	private Model        trainedModel;
    	private NumericTable data = null;

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
		this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 21);
		this.nFactors = this.conf.getLong(HarpDAALConstants.NUM_FACTOR, 2);

		//set thread number used in DAAL
		LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
		Environment.setNumberOfThreads(numThreads);
		LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

		LOG.info("File Dim " + this.fileDim);
		LOG.info("Num Mappers " + this.num_mappers);
		LOG.info("Num Threads " + this.numThreads);
		LOG.info("Num harp load data threads " + harpThreads);
		LOG.info("nFactors " + this.nFactors);

		long endTime = System.currentTimeMillis();
		LOG.info("config (ms) :" + (endTime - startTime));

	}

        // Assigns the reader to different nodes
        protected void mapCollective(
                KeyValReader reader, Context context)
            throws IOException, InterruptedException {
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
            
	    this.datasource = new HarpDAALDataSource(this.harpThreads, this.conf);

	    // ----------------------- start the execution -----------------------
            runALSBatch(context);
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
        private void runALSBatch(Context context) throws IOException 
	{

		initializeModel();
		trainModel();
		testModel();
		daal_Context.dispose();

	}

	private void initializeModel() 
	{

		// load training data
		this.data = this.datasource.createDenseNumericTable(this.inputFiles, this.fileDim, "," , this.daal_Context);

		/* Create an algorithm object to initialize the implicit ALS model with the default method */
		InitBatch initAlgorithm = new InitBatch(daal_Context, Double.class, InitMethod.defaultDense);
		initAlgorithm.parameter.setNFactors(nFactors);

		/* Pass a training data set and dependent values to the algorithm */
		initAlgorithm.input.set(InitInputId.data, data);

		/* Initialize the implicit ALS model */
		InitResult initResult = initAlgorithm.compute();
		initialModel = initResult.get(InitResultId.model);
	}

	private void trainModel() 
	{
		/* Create an algorithm object to train the implicit ALS model with the default method */
		TrainingBatch alsTrain = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense);

		alsTrain.input.set(NumericTableInputId.data, data);
		alsTrain.input.set(ModelInputId.inputModel, initialModel);
		alsTrain.parameter.setNFactors(nFactors);

		/* Build the implicit ALS model */
		TrainingResult trainingResult = alsTrain.compute();
		trainedModel = trainingResult.get(TrainingResultId.model);
	}


	private void testModel() 
	{
		/* Create an algorithm object to predict recommendations of the implicit ALS model */
		RatingsBatch algorithm = new RatingsBatch(daal_Context, Double.class, RatingsMethod.defaultDense);
		algorithm.parameter.setNFactors(nFactors);

		algorithm.input.set(RatingsModelInputId.model, trainedModel);

		RatingsResult result = algorithm.compute();

		NumericTable predictedRatings = result.get(RatingsResultId.prediction);

		Service.printNumericTable("Predicted ratings:", predictedRatings);
	}
}
