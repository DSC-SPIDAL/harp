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

package edu.iu.daal_nn;

import org.apache.commons.io.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Arrays;
import java.nio.DoubleBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

import java.nio.DoubleBuffer;

//import daal.jar API
import com.intel.daal.algorithms.neural_networks.*;
import com.intel.daal.algorithms.neural_networks.initializers.gaussian.*;
import com.intel.daal.algorithms.neural_networks.initializers.truncated_gaussian.*;
import com.intel.daal.algorithms.neural_networks.initializers.uniform.*;
import com.intel.daal.algorithms.neural_networks.initializers.xavier.*;
import com.intel.daal.algorithms.neural_networks.layers.*;
import com.intel.daal.algorithms.neural_networks.prediction.*;
import com.intel.daal.algorithms.neural_networks.training.*;
import com.intel.daal.algorithms.optimization_solver.sgd.Batch;
import com.intel.daal.algorithms.optimization_solver.sgd.Method;

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;

import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running Neural Network
 */


public class NNDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

    private DistributedStep2Master netMaster;
    private int batchSizeLocal;
    private int vectorSize;
    private int fileDim;    
    private int num_mappers;
    private int numThreads;
    private int harpThreads; 
    private Tensor featureTensorInit;
    private Tensor labelTensorInit;
    private DistributedStep2Master net;
    private DistributedStep1Local netLocal;
    private String testFilePath;
    private String testGroundTruthPath;
    private TrainingTopology topology;
    private TrainingTopology topologyLocal;
    private TrainingModel trainingModel;
    private PredictionModel predictionModel;
    private PredictionResult predictionResult;

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
    private static HarpDAALComm harpcomm;	
    private static DaalContext daal_Context = new DaalContext();

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException 
	{//{{{
            long startTime = System.currentTimeMillis();
            Configuration configuration = context.getConfiguration();
            this.num_mappers = configuration.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
            this.numThreads = configuration.getInt(HarpDAALConstants.NUM_THREADS, 10);
	    this.fileDim = configuration.getInt(HarpDAALConstants.FILE_DIM, 21);
	    this.vectorSize = configuration.getInt(HarpDAALConstants.FEATURE_DIM, 20);
            this.batchSizeLocal = configuration.getInt(HarpDAALConstants.BATCH_SIZE, 25);
            this.testFilePath = configuration.get(HarpDAALConstants.TEST_FILE_PATH,"");
            this.testGroundTruthPath = configuration.get(HarpDAALConstants.TEST_TRUTH_PATH,"");

            //always use the maximum hardware threads to load in data and convert data 
            harpThreads = Runtime.getRuntime().availableProcessors();

            LOG.info("Num Mappers " + num_mappers);
            LOG.info("Num Threads " + numThreads);
            LOG.info("Num harp load data threads " + harpThreads);
            LOG.info("BatchSize " + batchSizeLocal);
            long endTime = System.currentTimeMillis();
            LOG.info(
                "config (ms) :" + (endTime - startTime));
            System.out.println("Collective Mapper launched");
        
	}//}}}

        protected void mapCollective(KeyValReader reader, Context context)
        throws IOException, InterruptedException 
        {//{{{
            long startTime = System.currentTimeMillis();
            List<String> trainingDataFiles =
            new LinkedList<String>();
            List<String> trainingDataGroundTruthFiles =                       
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

	    //init data source
	    this.datasource = new HarpDAALDataSource(trainingDataFiles, this.fileDim, harpThreads, conf);
	    // create communicator
	    this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

	    runNN(conf, context);
	    LOG.info("Total iterations in master view: "
			    + (System.currentTimeMillis() - startTime));
            this.freeMemory();
            this.freeConn();
            System.gc();
        }//}}}

        private void runNN(Configuration conf, Context context) throws IOException 
	{//{{{

            ts_start = System.currentTimeMillis();

	    // load training data
	    this.datasource.loadFiles();
	    NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Float.class, this.vectorSize, this.datasource.getTotalLines(), 
			    NumericTable.AllocationFlag.DoAllocate);
	    NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Float.class, 1, this.datasource.getTotalLines(), 
			    NumericTable.AllocationFlag.DoAllocate);
	    MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
	    mergedData.addNumericTable(featureArray_daal);
	    mergedData.addNumericTable(labelArray_daal);

	    /* Retrieve the data from an input file */
	    this.datasource.loadDataBlock(mergedData);

            featureTensorInit = Service.readTensorFromNumericTable(daal_Context, featureArray_daal, true);
            labelTensorInit = Service.readTensorFromNumericTable(daal_Context, labelArray_daal, true);

            System.out.println("tensor size : "+ featureTensorInit.getSize());
            System.out.println("tensor size : "+ labelTensorInit.getSize());

            ts2 = System.currentTimeMillis();
            convert_time += (ts2 - ts1);

	    // initialize nn
            initializeNetwork(featureTensorInit, labelTensorInit);

	    // training 
            trainModel(featureTensorInit, labelTensorInit);

	    // testing
            if(this.isMaster()){
                testModel(conf);
                printResults(conf);
            }

            daal_Context.dispose();

            ts_end = System.currentTimeMillis();
            total_time = (ts_end - ts_start);

            LOG.info("Total Execution Time of NN: "+ total_time);
            LOG.info("Loading Data Time of NN: "+ load_time);
            LOG.info("Computation Time of NN: "+ compute_time);
            LOG.info("Comm Time of NN: "+ comm_time);
            LOG.info("DataType Convert Time of NN: "+ convert_time);
            LOG.info("Misc Time of NN: "+ (total_time - load_time - compute_time - comm_time - convert_time));

        }//}}}

        private void initializeNetwork(Tensor featureTensorInit, Tensor labelTensorInit)
	{//{{{
            com.intel.daal.algorithms.optimization_solver.sgd.Batch sgdAlgorithm =
            new com.intel.daal.algorithms.optimization_solver.sgd.Batch(daal_Context, Float.class, com.intel.daal.algorithms.optimization_solver.sgd.Method.defaultDense);
            sgdAlgorithm.parameter.setBatchSize(batchSizeLocal);

            long[] sampleSize = featureTensorInit.getDimensions();
            sampleSize[0] = batchSizeLocal;
            if(this.isMaster()){
                net = new DistributedStep2Master(daal_Context, sgdAlgorithm);
                topology = NeuralNetConfiguratorDistr.configureNet(daal_Context);
                net.parameter.setOptimizationSolver(sgdAlgorithm);
                net.initialize(sampleSize, topology);
            }
            topologyLocal = NeuralNetConfiguratorDistr.configureNet(daal_Context);
            netLocal = new DistributedStep1Local(daal_Context);
            trainingModel = new TrainingModel(daal_Context);
            trainingModel.initialize(Float.class, sampleSize, topologyLocal);
            netLocal.input.set(DistributedStep1LocalInputId.inputModel, trainingModel);
        }//}}}

        private void trainModel(Tensor featureTensorInit, Tensor labelTensorInit) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{

            com.intel.daal.algorithms.optimization_solver.sgd.Batch sgdAlgorithm =
            new com.intel.daal.algorithms.optimization_solver.sgd.Batch(daal_Context, Float.class, com.intel.daal.algorithms.optimization_solver.sgd.Method.defaultDense);

            double[] learningRateArray = new double[1];
            learningRateArray[0] = 0.0001;

            sgdAlgorithm.parameter.setLearningRateSequence(new HomogenNumericTable(daal_Context, learningRateArray, 1, 1));
            sgdAlgorithm.parameter.setBatchSize(batchSizeLocal);

            if(this.isMaster()){
                net.parameter.setOptimizationSolver(sgdAlgorithm);
            }

            int nSamples = (int)featureTensorInit.getDimensions()[0]; 

            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

            for (int i = 0; i < nSamples - batchSizeLocal + 1; i += batchSizeLocal) 
	    {
		    //local computation
		    ts1 = System.currentTimeMillis();

		    netLocal.input.set(TrainingInputId.data, Service.getNextSubtensor(daal_Context, featureTensorInit, i, batchSizeLocal));
		    netLocal.input.set(TrainingInputId.groundTruth, Service.getNextSubtensor(daal_Context, labelTensorInit, i, batchSizeLocal));

		    PartialResult partialResult = netLocal.compute();

		    ts2 = System.currentTimeMillis();
		    compute_time += (ts2 - ts1);

		    // gather the partialResult
		    SerializableBase[] gather_out = this.harpcomm.harpdaal_gather(partialResult, i+this.getSelfID()*nSamples, this.getMasterID(), "NeuralNetwork", "gather_partialResult"); 
		    
		    if (this.isMaster()) 
		    {
			    ts1 = System.currentTimeMillis();
			    for(int j=0;j<this.num_mappers;j++)
			    {
				PartialResult pres_entry = (PartialResult)(gather_out[j]);
				net.input.add(DistributedStep2MasterInputId.partialResults, j, pres_entry); 
			    }

			    ts2 = System.currentTimeMillis();
			    comm_time += (ts2 - ts1);
		    }

		    if (i == 0 && this.isMaster()) 
		    {
			    TrainingModel trainingModelOnMaster = net.getResult().get(TrainingResultId.model);
			    TrainingModel trainingModelOnLocal  = netLocal.input.get(DistributedStep1LocalInputId.inputModel);
			    trainingModelOnMaster.setWeightsAndBiases(trainingModelOnLocal.getWeightsAndBiases());
			    ForwardLayers forwardLayers = trainingModelOnMaster.getForwardLayers();

			    for (int j = 0; j < forwardLayers.size(); j++) {
				    forwardLayers.get(j).getLayerParameter().setWeightsAndBiasesInitializationFlag(true);
			    }

		    }

		    NumericTable wb = null;
		    if(this.isMaster())
		    {
			    ts1 = System.currentTimeMillis();
			    DistributedPartialResult result = net.compute();
			    ts2 = System.currentTimeMillis();
			    compute_time += (ts2 - ts1);
			    wb = result.get(DistributedPartialResultId.resultFromMaster).get(TrainingResultId.model).getWeightsAndBiases();
		    }

		    ts1 = System.currentTimeMillis();
		    SerializableBase bcst_out = this.harpcomm.harpdaal_braodcast(wb, this.getMasterID(), "NeuralNetwork", "bcast_res", true);
		    ts2 = System.currentTimeMillis();
		    comm_time += (ts2 - ts1);

		    NumericTable wbMaster = (NumericTable)(bcst_out);
	            netLocal.input.get(DistributedStep1LocalInputId.inputModel).setWeightsAndBiases(wbMaster);
		    
	    }

            if(this.isMaster())
	    {
                ts1 = System.currentTimeMillis();
                TrainingResult result = net.finalizeCompute();
                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);

                TrainingModel finalTrainingModel = result.get(TrainingResultId.model);
                NumericTable finalresult = finalTrainingModel.getWeightsAndBiases();
                predictionModel = trainingModel.getPredictionModel(Float.class);
            }
        }//}}}

        private void testModel(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{

            Tensor predictionData = this.datasource.createDenseTensor(testFilePath, this.vectorSize, daal_Context);
            PredictionBatch net = new PredictionBatch(daal_Context);
            long[] predictionDimensions = predictionData.getDimensions();
            net.parameter.setBatchSize(predictionDimensions[0]);
            net.input.set(PredictionTensorInputId.data, predictionData);
            net.input.set(PredictionModelInputId.model, predictionModel);

            ts1 = System.currentTimeMillis();
            predictionResult = net.compute();
            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);

        }//}}}

        private void printResults(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{
            Tensor predictionGroundTruth = this.datasource.createDenseTensor(testGroundTruthPath, 1, daal_Context);
            Service.printTensors("Ground truth", "Neural network predictions: each class probability",
                "Neural network classification results (first 50 observations):",
                predictionGroundTruth, predictionResult.get(PredictionResultId.prediction), 50);
        }//}}}

        
    }
