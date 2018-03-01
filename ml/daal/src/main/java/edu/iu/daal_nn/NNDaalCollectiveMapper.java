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
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.optimization_solver.sgd.Batch;
import com.intel.daal.algorithms.optimization_solver.sgd.Method;

import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running Neural Network
 */


public class NNDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

    private DistributedStep2Master netMaster;
    private int batchSizeLocal = 25;
    private int pointsPerFile = 1500;
    private int vectorSize = 20;                                
    private int groundTruthvectorSize = 1;
    private int numMappers;
    private int numThreads;
    private int harpThreads; 
    private Tensor featureTensorInit;
    private Tensor labelTensorInit;
    private int numWbPars;
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

    private static DaalContext daal_Context = new DaalContext();

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            Configuration configuration =
            context.getConfiguration();
            numMappers = configuration
            .getInt(Constants.NUM_MAPPERS, 10);
            numThreads = configuration
            .getInt(Constants.NUM_THREADS, 10);

            batchSizeLocal = configuration
            .getInt(Constants.BATCH_SIZE, 10);

            numWbPars = numThreads;
            testFilePath =
            configuration.get(Constants.TEST_FILE_PATH,"");

            testGroundTruthPath =
            configuration.get(Constants.TEST_TRUTH_PATH,"");

            //always use the maximum hardware threads to load in data and convert data 
            harpThreads = Runtime.getRuntime().availableProcessors();

            LOG.info("Num Mappers " + numMappers);
            LOG.info("Num Threads " + numThreads);
            LOG.info("Num harp load data threads " + harpThreads);
            LOG.info("BatchSize " + batchSizeLocal);
            long endTime = System.currentTimeMillis();
            LOG.info(
                "config (ms) :" + (endTime - startTime));
            System.out.println("Collective Mapper launched");

        }

        protected void mapCollective(
            KeyValReader reader, Context context)
        throws IOException, InterruptedException {
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

            runNN(trainingDataFiles, conf, context);
            LOG.info("Total iterations in master view: "
                + (System.currentTimeMillis() - startTime));
            this.freeMemory();
            this.freeConn();
            System.gc();
        }



        private void runNN(List<String> trainingDataFiles, Configuration conf, Context context) throws IOException {

            ts_start = System.currentTimeMillis();

            ts1 = System.currentTimeMillis();

            // extracting points from csv files
            List<List<double[]>> pointArrays = NNUtil.loadPoints(trainingDataFiles, pointsPerFile,
                vectorSize, conf, harpThreads);
            List<double[]> featurePoints = new LinkedList<>();
            for(int i = 0; i<pointArrays.size(); i++){
                featurePoints.add(pointArrays.get(i).get(0));
            }
            List<double[]> labelPoints = new LinkedList<>();
            for(int i = 0; i<pointArrays.size(); i++){
                labelPoints.add(pointArrays.get(i).get(1));
            }

            ts2 = System.currentTimeMillis();
            load_time += (ts2 - ts1);

            // converting data to Numeric Table
            ts1 = System.currentTimeMillis();

            long nFeature = vectorSize;
            long nLabel = 1;
            long totalLengthFeature = 0;
            long totalLengthLabel = 0;

            long[] array_startP_feature = new long[featurePoints.size()];
            double[][] array_data_feature = new double[featurePoints.size()][];
            long[] array_startP_label = new long[labelPoints.size()];
            double[][] array_data_label = new double[labelPoints.size()][];

            for(int k=0;k<featurePoints.size();k++)
            {
                array_data_feature[k] = featurePoints.get(k);
                array_startP_feature[k] = totalLengthFeature;
                totalLengthFeature += featurePoints.get(k).length;
            }

            for(int k=0;k<labelPoints.size();k++)
            {
                array_data_label[k] = labelPoints.get(k);
                array_startP_label[k] = totalLengthLabel;
                totalLengthLabel += labelPoints.get(k).length;
            }

            long featuretableSize = totalLengthFeature/nFeature;
            long labeltableSize = totalLengthLabel/nLabel;

            //initializing Numeric Table

            NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);
            NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Double.class, nLabel, labeltableSize, NumericTable.AllocationFlag.DoAllocate);


            int row_idx_feature = 0;
            int row_len_feature = 0;

            for (int k=0; k<featurePoints.size(); k++) 
            {
                row_len_feature = (array_data_feature[k].length)/(int)nFeature;
                //release data from Java side to native side
                ((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, DoubleBuffer.wrap(array_data_feature[k]));
                row_idx_feature += row_len_feature;
            }

            int row_idx_label = 0;
            int row_len_label = 0;

            for (int k=0; k<labelPoints.size(); k++) 
            {
                row_len_label = (array_data_label[k].length)/(int)nLabel;
                //release data from Java side to native side
                ((HomogenNumericTable)labelArray_daal).releaseBlockOfRows(row_idx_label, row_len_label, DoubleBuffer.wrap(array_data_label[k]));
                row_idx_label += row_len_label;
            }

            featureTensorInit = Service.readTensorFromNumericTable(daal_Context, featureArray_daal, true);
            labelTensorInit = Service.readTensorFromNumericTable(daal_Context, labelArray_daal, true);
            System.out.println("tensor size : "+ featureTensorInit.getSize());
            System.out.println("tensor size : "+ labelTensorInit.getSize());

            ts2 = System.currentTimeMillis();
            convert_time += (ts2 - ts1);

            initializeNetwork(featureTensorInit, labelTensorInit);
            trainModel(featureTensorInit, labelTensorInit);
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

        }

        private void initializeNetwork(Tensor featureTensorInit, Tensor labelTensorInit){
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
            // trainingModel.initialize(Float.class, sampleSize, topologyLocal, null);
            trainingModel.initialize(Float.class, sampleSize, topologyLocal);
            netLocal.input.set(DistributedStep1LocalInputId.inputModel, trainingModel);
        }

        private void trainModel(Tensor featureTensorInit, Tensor labelTensorInit) throws java.io.FileNotFoundException, java.io.IOException {
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

            for (int i = 0; i < nSamples - batchSizeLocal + 1; i += batchSizeLocal) {

            //local computation
                ts1 = System.currentTimeMillis();

                netLocal.input.set(TrainingInputId.data, Service.getNextSubtensor(daal_Context, featureTensorInit, i, batchSizeLocal));
                netLocal.input.set(TrainingInputId.groundTruth, Service.getNextSubtensor(daal_Context, labelTensorInit, i, batchSizeLocal));

                PartialResult partialResult = netLocal.compute();

                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);

                ts1 = System.currentTimeMillis();
                Table<ByteArray> partialResultTable = new Table<>(0, new ByteArrPlus());

                partialResultTable.addPartition(new Partition<>(i+this.getSelfID()*nSamples, serializePartialResult(partialResult)));

                boolean reduceStatus = false;
                reduceStatus = this.reduce("nn", "sync-partialresult", partialResultTable, this.getMasterID()); 

                ts2 = System.currentTimeMillis();
                comm_time += (ts2 - ts1);

                if(!reduceStatus){
                    System.out.println("reduce not successful");
                }

                if (this.isMaster()) {
                    int[] pid = partialResultTable.getPartitionIDs().toIntArray();

                    ts1 = System.currentTimeMillis();
                    for(int j = 0; j< pid.length; j++){
                        try {
                            net.input.add(DistributedStep2MasterInputId.partialResults, j, deserializePartialResult(partialResultTable.getPartition(pid[j]).get()));
                        } catch (Exception e) 
                        {  
                            System.out.println("Fail to deserilize partialResultTable" + e.toString());
                            e.printStackTrace();
                        }
                    }

                    ts2 = System.currentTimeMillis();
                    comm_time += (ts2 - ts1);
                }

                if (i == 0) {
                    if(this.isMaster()){
                        TrainingModel trainingModelOnMaster = net.getResult().get(TrainingResultId.model);
                        TrainingModel trainingModelOnLocal  = netLocal.input.get(DistributedStep1LocalInputId.inputModel);
                        trainingModelOnMaster.setWeightsAndBiases(trainingModelOnLocal.getWeightsAndBiases());
                        ForwardLayers forwardLayers = trainingModelOnMaster.getForwardLayers();
                        for (int j = 0; j < forwardLayers.size(); j++) {
                            forwardLayers.get(j).getLayerParameter().setWeightsAndBiasesInitializationFlag(true);
                        }
                    }
                }

                Table<ByteArray> wbHarpTable = new Table<>(0, new ByteArrPlus());

                if(this.isMaster()){
                    ts1 = System.currentTimeMillis();
                    DistributedPartialResult result = net.compute();
                    ts2 = System.currentTimeMillis();
                    compute_time += (ts2 - ts1);

                    ts1 = System.currentTimeMillis();
                    NumericTable wb = result.get(DistributedPartialResultId.resultFromMaster).get(TrainingResultId.model).getWeightsAndBiases();

                    wbHarpTable.addPartition(new Partition<>(this.getMasterID(), 
                        serializeNumericTable(wb)));
                    ts2 = System.currentTimeMillis();
                    comm_time += (ts2 - ts1);
                }

                ts1 = System.currentTimeMillis();
                bcastTrainingModel(wbHarpTable, this.getMasterID());

                try {
                    NumericTable wbMaster = deserializeNumericTable(wbHarpTable.getPartition(this.getMasterID()).get()); 
                    netLocal.input.get(DistributedStep1LocalInputId.inputModel).setWeightsAndBiases(wbMaster);

                } catch (Exception e) 
                {  
                    System.out.println("Fail to deserilize partialResultTable" + e.toString());
                    e.printStackTrace();
                }
                ts2 = System.currentTimeMillis();
                comm_time += (ts2 - ts1);
            }

            if(this.isMaster()){
                ts1 = System.currentTimeMillis();
                TrainingResult result = net.finalizeCompute();
                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);

                TrainingModel finalTrainingModel = result.get(TrainingResultId.model);
                NumericTable finalresult = finalTrainingModel.getWeightsAndBiases();
                predictionModel = trainingModel.getPredictionModel(Float.class);
            }
        }

        private void testModel(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException {
            Tensor predictionData = getTensorHDFS(daal_Context, conf, testFilePath, vectorSize, 2000);
            PredictionBatch net = new PredictionBatch(daal_Context);
            long[] predictionDimensions = predictionData.getDimensions();
            net.parameter.setBatchSize(predictionDimensions[0]);
            net.input.set(PredictionTensorInputId.data, predictionData);
            net.input.set(PredictionModelInputId.model, predictionModel);

            ts1 = System.currentTimeMillis();
            predictionResult = net.compute();
            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);


        }

        private void printResults(Configuration conf) throws java.io.FileNotFoundException, java.io.IOException {
            Tensor predictionGroundTruth = getTensorHDFS(daal_Context, conf, testGroundTruthPath, 1, 2000);
            Service.printTensors("Ground truth", "Neural network predictions: each class probability",
                "Neural network classification results (first 50 observations):",
                predictionGroundTruth, predictionResult.get(PredictionResultId.prediction), 50);
        }

        private static ByteArray serializePartialResult(PartialResult partialResult) throws IOException {
            /* Create an output stream to serialize the numeric table */
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

            /* Serialize the numeric table into the output stream */
            partialResult.pack();
            outputStream.writeObject(partialResult);

            /* Store the serialized data in an array */
            byte[] serializedPartialResult = outputByteStream.toByteArray();

            ByteArray partialResultHarp = new ByteArray(serializedPartialResult, 0, serializedPartialResult.length);
            return partialResultHarp;
        }

        private static PartialResult deserializePartialResult(ByteArray byteArray) throws IOException, ClassNotFoundException {
            /* Create an input stream to deserialize the numeric table from the array */
            byte[] buffer = byteArray.get();
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

            /* Create a numeric table object */
            PartialResult restoredDataTable = (PartialResult) inputStream.readObject();
            restoredDataTable.unpack(daal_Context);

            return restoredDataTable;
        } 

        private static ByteArray serializeNumericTable(NumericTable dataTable) throws IOException {
            /* Create an output stream to serialize the numeric table */
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

            /* Serialize the numeric table into the output stream */
            dataTable.pack();
            outputStream.writeObject(dataTable);

            /* Store the serialized data in an array */
            byte[] buffer = outputByteStream.toByteArray();
            ByteArray numericTableHarp = new ByteArray(buffer, 0, buffer.length);
            return numericTableHarp;
        }

        private static NumericTable deserializeNumericTable(ByteArray byteArray) throws IOException, ClassNotFoundException {
            /* Create an input stream to deserialize the numeric table from the array */
            byte[] buffer = byteArray.get();
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

            /* Create a numeric table object */
            NumericTable restoredDataTable = (NumericTable) inputStream.readObject();
            restoredDataTable.unpack(daal_Context);

            return restoredDataTable;
        }

        private void bcastTrainingModel(Table<ByteArray> table, int bcastID) throws IOException {
            long startTime = System.currentTimeMillis();
            boolean isSuccess = false;
            try {
                isSuccess =
                this.broadcast("main",
                    "broadcast-trainingmodel", table, bcastID,
                    false);
            } catch (Exception e) {
                LOG.error("Fail to bcast.", e);
            }
            long endTime = System.currentTimeMillis();
            LOG.info("Bcast trainingmodel (ms): "
                + (endTime - startTime));
            if (!isSuccess) {
                System.out.println("broadcast not successful");
                throw new IOException("Fail to bcast");
            }
        }


        private Tensor getTensor(DaalContext daal_Context, Configuration conf, String inputFile, int vectorSize, int numRows) 
        throws IOException{
            Path inputFilePath = new Path(inputFile);
            boolean isFailed = false;
            FSDataInputStream in = null;
            try {
                FileSystem fs =
                inputFilePath.getFileSystem(conf);
                in = fs.open(inputFilePath);
            } catch (Exception e) {
                System.out.println("Fail to open  test file " + testFilePath + e.toString());
                LOG.error("Fail to open  test file " + testFilePath + e.toString());
                isFailed = true;
            }

            float[] data = new float[vectorSize * numRows];
            int q = 0;
            int index = 0;
            while(true){
                String line = in.readLine();
                if (line == null) break;
                String[] lineData = line.split(",");
                q++;
                for(int t =0 ; t< lineData.length; t++){
                    data[index] = Float.parseFloat(lineData[t]);
                    index++;                                                          
                }
            }
            long[] dims = {numRows, vectorSize};
            Tensor predictionData = new HomogenTensor(daal_Context, dims, data);
            return predictionData;
        }

        private Tensor getTensorHDFS(DaalContext daal_Context, Configuration conf, String inputFiles, int vectorSize, int numRows) 
        throws IOException{
            Path inputFilePaths = new Path(inputFiles);
            List<String> inputFileList = new LinkedList<>();

            try {
                FileSystem fs =
                inputFilePaths.getFileSystem(conf);
                RemoteIterator<LocatedFileStatus> iterator =
                fs.listFiles(inputFilePaths, true);

                while (iterator.hasNext()) {
                    String name =
                    iterator.next().getPath().toUri()
                    .toString();
                    inputFileList.add(name);
                }

            } catch (IOException e) {
                LOG.error("Fail to get test files", e);
            }

            int dataSize = vectorSize*numRows;
            // float[] data = new float[dataSize];
            double[] data = new double[dataSize];
            long[] dims = {numRows, vectorSize};
            int index = 0;

            FSDataInputStream in = null;

            //loop over all the files in the list
            ListIterator<String> file_itr = inputFileList.listIterator();
            while (file_itr.hasNext())
            {
                String file_name = file_itr.next();
                LOG.info("read in file name: " + file_name);

                Path file_path = new Path(file_name);
                try {

                    FileSystem fs =
                    file_path.getFileSystem(conf);
                    in = fs.open(file_path);

                } catch (Exception e) {
                    LOG.error("Fail to open file "+ e.toString());
                    return null;
                }

                //read file content
                while(true)
                {
                    String line = in.readLine();
                    if (line == null) break;

                    String[] lineData = line.split(",");

                    for(int t =0 ; t< lineData.length; t++)
                    {
                        if (index < dataSize)
                        {
                            // data[index] = Float.parseFloat(lineData[t]);
                            data[index] = Double.parseDouble(lineData[t]);
                            index++;                                                          
                        }
                        else
                        {
                            LOG.error("Incorrect size of file: dataSize: " + dataSize + "; index val: " + index);
                            return null;
                        }
                        
                    }
                }

                in.close();

            }

            if ( index  != dataSize )
            {
                LOG.error("Incorrect total size of file: dataSize: " + dataSize + "; index val: " + index);
                return null;
            }

            //debug check the vals of data
            // for(int p=0;p<60;p++)
            //     LOG.info("data at: " + p + " is: " + data[p]);
            
            Tensor predictionData = new HomogenTensor(daal_Context, dims, data);
            return predictionData;

        }
    }
