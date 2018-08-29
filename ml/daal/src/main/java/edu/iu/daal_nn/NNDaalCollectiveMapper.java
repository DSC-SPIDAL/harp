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
	{//{{{
            long startTime = System.currentTimeMillis();
            this.conf = context.getConfiguration();
            this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
            this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
	    this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 21);
	    this.vectorSize = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 20);
            this.batchSizeLocal = this.conf.getInt(HarpDAALConstants.BATCH_SIZE, 25);
            this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");
            this.testGroundTruthPath = this.conf.get(HarpDAALConstants.TEST_TRUTH_PATH,"");

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
            this.inputFiles = new LinkedList<String>();

            //splitting files between mapper
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                    + value);
                System.out.println("file name : " + value);
                this.inputFiles.add(value);
            }

	    //init data source
	    this.datasource = new HarpDAALDataSource(harpThreads, conf);
	    // create communicator
	    this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);

	    runNN(context);
	    LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
            this.freeMemory();
            this.freeConn();
            System.gc();

        }//}}}

        private void runNN(Context context) throws IOException 
	{//{{{

		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, this.vectorSize, 1, ",", this.daal_Context);
		NumericTable featureArray_daal = load_table[0];
		NumericTable labelArray_daal = load_table[1];

		featureTensorInit = Service.readTensorFromNumericTable(daal_Context, featureArray_daal, true);
		labelTensorInit = Service.readTensorFromNumericTable(daal_Context, labelArray_daal, true);

		System.out.println("tensor size : "+ featureTensorInit.getSize());
		System.out.println("tensor size : "+ labelTensorInit.getSize());

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

	}//}}}

        private void initializeNetwork(Tensor featureTensorInit, Tensor labelTensorInit)
	{//{{{
            com.intel.daal.algorithms.optimization_solver.sgd.Batch sgdAlgorithm =
            new com.intel.daal.algorithms.optimization_solver.sgd.Batch(daal_Context, Double.class, com.intel.daal.algorithms.optimization_solver.sgd.Method.defaultDense);
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
            trainingModel.initialize(Double.class, sampleSize, topologyLocal);
            netLocal.input.set(DistributedStep1LocalInputId.inputModel, trainingModel);
        }//}}}

        private void trainModel(Tensor featureTensorInit, Tensor labelTensorInit) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{

            com.intel.daal.algorithms.optimization_solver.sgd.Batch sgdAlgorithm =
            new com.intel.daal.algorithms.optimization_solver.sgd.Batch(daal_Context, Double.class, com.intel.daal.algorithms.optimization_solver.sgd.Method.defaultDense);

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
		    netLocal.input.set(TrainingInputId.data, Service.getNextSubtensor(daal_Context, featureTensorInit, i, batchSizeLocal));
		    netLocal.input.set(TrainingInputId.groundTruth, Service.getNextSubtensor(daal_Context, labelTensorInit, i, batchSizeLocal));

		    PartialResult partialResult = netLocal.compute();

		    // gather the partialResult
		    SerializableBase[] gather_out = this.harpcomm.harpdaal_gather(partialResult, i+this.getSelfID()*nSamples, this.getMasterID(), "NeuralNetwork", "gather_partialResult"); 
		    
		    if (this.isMaster()) 
		    {
			    for(int j=0;j<this.num_mappers;j++)
			    {
				PartialResult pres_entry = (PartialResult)(gather_out[j]);
				net.input.add(DistributedStep2MasterInputId.partialResults, j, pres_entry); 
			    }

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
			    DistributedPartialResult result = net.compute();
			    wb = result.get(DistributedPartialResultId.resultFromMaster).get(TrainingResultId.model).getWeightsAndBiases();
		    }

		    SerializableBase bcst_out = this.harpcomm.harpdaal_braodcast(wb, this.getMasterID(), "NeuralNetwork", "bcast_res", true);

		    NumericTable wbMaster = (NumericTable)(bcst_out);
	            netLocal.input.get(DistributedStep1LocalInputId.inputModel).setWeightsAndBiases(wbMaster);
		    
	    }

            if(this.isMaster())
	    {
                TrainingResult result = net.finalizeCompute();

                TrainingModel finalTrainingModel = result.get(TrainingResultId.model);
                NumericTable finalresult = finalTrainingModel.getWeightsAndBiases();
                predictionModel = trainingModel.getPredictionModel(Double.class);
            }
        }//}}}

        private void testModel(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{

            Tensor predictionData = this.datasource.createDenseTensor(testFilePath, this.vectorSize, ",", daal_Context);
            PredictionBatch net = new PredictionBatch(daal_Context);
            long[] predictionDimensions = predictionData.getDimensions();
            net.parameter.setBatchSize(predictionDimensions[0]);
            net.input.set(PredictionTensorInputId.data, predictionData);
            net.input.set(PredictionModelInputId.model, predictionModel);

            predictionResult = net.compute();

        }//}}}

        private void printResults(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException 
	{//{{{
            Tensor predictionGroundTruth = this.datasource.createDenseTensor(testGroundTruthPath, 1, ",", daal_Context);
            Service.printTensors("Ground truth", "Neural network predictions: each class probability",
                "Neural network classification results (first 50 observations):",
                predictionGroundTruth, predictionResult.get(PredictionResultId.prediction), 50);
        }//}}}

        
    }
