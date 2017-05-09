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

package edu.iu.daal_als;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.PrintWriter;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.Thread;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.dymoro.RotationUtil;
import edu.iu.dymoro.Rotator;
import edu.iu.dymoro.Scheduler;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.example.LongArrPlus;

import edu.iu.daal.*;

// packages from Daal 
import com.intel.daal.algorithms.implicit_als.PartialModel;
import com.intel.daal.algorithms.implicit_als.prediction.ratings.*;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.KeyValueDataCollection;
// import com.intel.daal.examples.utils.Service;

import com.intel.daal.algorithms.mf_sgd.*;
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

public class ALSDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

        //feature dimension
        private int r;
        private double oneOverSqrtR;
        //lambda parameter in update
        private double lambda;
        //learning rate
        private double epsilon;
        //num of training iterations
        private int numIterations;
        //num of threads in computation
        private int numThreads;
        private int numThreads_harp = 60;
        private String modelDirPath;
        //time value used by timer
        private double time;
        //use or not use timer tuning
        private boolean enableTuning;
        //number of pipelines in model rotation
        private int numModelSlices;
        //iteration interval of doing rmse test
        private int rmseIteInterval;
        //iteration interval of freeing cached data in model rotation
        private int freeInterval;
        //final RMSE value of test
        private double rmse;
        private String testFilePath;
        //time of total trainining 
        private long trainTime = 0;
        //time of computing time of each step 
        private long computeTime_step1 = 0;
        private long computeTime_step2 = 0;
        private long computeTime_step3 = 0;
        private long computeTime_step4 = 0;

        private long commTime_step1 = 0;
        private long commTime_step2 = 0;
        private long commTime_step3 = 0;
        private long commTime_step4 = 0;

        //time of parallel tasks 
        private long computeTaskTime = 0;
        //time spent in waiting rotated model data in each itr
        private long waitTime = 0;
        //timestamp of each training iteration
        private long itrTimeStamp;
        //num of trained training points 
        private long numVTrained;
        
        //random generator 
        private Random random;
        //total num of training data from all the workers
        private long totalNumTrain;
        //total num of columns in training dataset from all the workers
        private long totalNumCols;
        //computed test points in test process
        private long effectiveTestV;
        //training time per iteration
        public double trainTimePerIter = 0;

        //DAAL related
        private static DaalContext daal_Context = new DaalContext();
        //daal table to hold row ids of local model W 
        private HomogenBMNumericTable wMat_rowid_daal;  
        //num of local rows of training set
        private long wMat_size;
        //max global row ids
        private long maxRowID = 0;
        private double alpha = 40.0;
        private double lambda_als = 0.06;
        // private double alpha = 20.0;

        //daal table to hold row ids of local training set
        private HomogenBMNumericTable train_wPos_daal;
        //daal table to hold cols ids of local training set
        private HomogenBMNumericTable train_hPos_daal;
        //daal table to hold values of local training set
        private HomogenBMNumericTable train_val_daal;

        //daal table to hold row ids of test set
        private HomogenBMNumericTable test_wPos_daal;
        //daal table to hold col ids of test set
        private HomogenBMNumericTable test_hPos_daal;
        //daal table to hold values of test set
        private HomogenBMNumericTable test_val_daal;
        //daal table to hold rotated H model
        private NumericTable hTableMap_daal;
        
        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context) {
            LOG
                .info("start setup: "
                        + new SimpleDateFormat("yyyyMMdd_HHmmss")
                        .format(Calendar.getInstance()
                            .getTime()));
            long startTime = System.currentTimeMillis();
            Configuration configuration =
                context.getConfiguration();
            r = configuration.getInt(Constants.R, 100);
            lambda =
                configuration.getDouble(Constants.LAMBDA,
                        0.001);
            epsilon =
                configuration.getDouble(Constants.EPSILON,
                        0.001);
            numIterations =
                configuration.getInt(
                        Constants.NUM_ITERATIONS, 100);
            numThreads =
                configuration.getInt(Constants.NUM_THREADS,
                        16);
            modelDirPath =
                configuration.get(Constants.MODEL_DIR, "");

            enableTuning = configuration
                    .getBoolean(Constants.ENABLE_TUNING, true);

            time = enableTuning ? 1000L : 1000000000L;
           
            numModelSlices = 2;
           
            testFilePath =
                configuration.get(Constants.TEST_FILE_PATH,
                        "");
            String javaOpts =
                configuration.get(
                        "mapreduce.map.collective.java.opts", "");
            rmseIteInterval = 1;
            freeInterval = 20;
            rmse = 0.0;
            // computeTime = 0L;
            computeTaskTime = 0L;
            itrTimeStamp = 0L;
            waitTime = 0L;
            numVTrained = 0L;
            totalNumTrain = 0L;
            totalNumCols = 0L;
            effectiveTestV = 0L;
            oneOverSqrtR = 1.0 / Math.sqrt(r);
            random =
                new Random(System.currentTimeMillis());
            long endTime = System.currentTimeMillis();
            LOG.info("config (ms): "
                    + (endTime - startTime));
            LOG.info("R " + r);
            LOG.info("Lambda " + lambda);
            LOG.info("Epsilon " + epsilon);
            LOG.info("Num Iterations " + numIterations);
            LOG.info("Num Threads " + numThreads);
            LOG.info("Timer " + time);
            LOG.info("Model Slices " + numModelSlices);
            LOG.info("Model Dir Path " + modelDirPath);
            LOG.info("TEST FILE PATH " + testFilePath);
            LOG.info("JAVA OPTS " + javaOpts);
            LOG.info("Time Tuning " + enableTuning);
        }

        /**
         * @brief harp's mapper function
         *
         * @param reader
         * @param context
         *
         * @return 
         */
        protected void mapCollective(
                KeyValReader reader, Context context)
            throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            LinkedList<String> vFiles = getVFiles(reader);
            try {
                runALS(vFiles, context.getConfiguration(),
                        context);
            } catch (Exception e) {
                LOG.error("Fail to run SGD.", e);
            }

            LOG.info("Total execution time: "
                    + (System.currentTimeMillis() - startTime));
        }

        /**
         * @brief readin dataset from HDFS
         *
         * @param reader
         *
         * @return 
         */
        private LinkedList<String> getVFiles(
                final KeyValReader reader)
            throws IOException, InterruptedException {
            final LinkedList<String> vFiles =
                new LinkedList<>();
            while (reader.nextKeyValue()) {
                final String value =
                    reader.getCurrentValue();
                LOG.info("File: " + value);
                vFiles.add(value);
            }
            return vFiles;
        }


        /**
         * @brief running ALS 
         *
         * @param vFilePaths
         * @param configuration
         * @param context
         *
         * @return 
         */
        private void runALS(
                final LinkedList<String> vFilePaths,
                final Configuration configuration,
                final Context context) throws Exception 
        {//{{{

            LOG.info("Load ALS training points in COO format");

            //----------------------- load the train dataset-----------------------
            // grouped by row ids
            Int2ObjectOpenHashMap<VRowCol> trainDataMap = SGDUtil.loadVMapRow(vFilePaths, numThreads_harp, configuration);

            // grouped by row ids
            Int2ObjectOpenHashMap<VRowCol> trainDataMapTran = SGDUtil.loadVMapTran(vFilePaths, numThreads_harp, configuration);

            //sync the number of rows/columns and row/col ids among all the mappers
            int[] rowIds = new int[trainDataMap.size()];
            int[] colIds = new int[trainDataMapTran.size()];

            rowIds = trainDataMap.keySet().toArray(rowIds); 
            colIds = trainDataMapTran.keySet().toArray(colIds); 

            //remapping row ids
            ReMapRowColID remapper_row = new ReMapRowColID(rowIds, this.getSelfID(), this.getNumWorkers(), this);
            int[] row_mapping = remapper_row.getRemapping();

            //remapping col ids
            ReMapRowColID remapper_col = new ReMapRowColID(colIds, this.getSelfID(), this.getNumWorkers(), this);
            int[] col_mapping = remapper_col.getRemapping();

            //regroup among all the mappers by row
            Table<VSet> trainDataTable = TrainDataRGroupByRow(trainDataMap, row_mapping, numThreads_harp, false);

            //regroup among all the mappers by col
            Table<VSet> trainDataTableTran = TrainDataRGroupByRow(trainDataMapTran, col_mapping, numThreads_harp, true);

            // ------------------------ load test dataset ------------------------
            // -------------------------- each node has the whole dataset --------------------------
            Int2ObjectOpenHashMap<VRowCol> testDataMap = SGDUtil.loadTMapRow(testFilePath, numThreads_harp, configuration);

            long start_spmat = System.currentTimeMillis();
            //convert coo format of training dataset to CSR format
            COOToCSR converter = new COOToCSR(trainDataTable, col_mapping);
            CSRNumericTable trainDaalTable = converter.convert();

            COOToCSR converter_tran = new COOToCSR(trainDataTableTran, row_mapping);
            CSRNumericTable trainDaalTableTran = converter_tran.convert();

            long end_spmat = System.currentTimeMillis();

            //check the size of trainDaalTable
            LOG.info("CSR table rows: " + trainDaalTable.getNumberOfRows() + " CSR table cols: " 
                    + trainDaalTable.getNumberOfColumns() + " CSR datasize: " 
                    + trainDaalTable.getDataSize() 
                    + "CSR table tran rows: " + trainDaalTableTran.getNumberOfRows() + " CSR table tran cols: " 
                    + trainDaalTableTran.getNumberOfColumns() + " CSRi tran datasize: " 
                    + trainDaalTableTran.getDataSize()
                    + " conversion time: " + (end_spmat - start_spmat));

            //create the ALS daal containers
            int workerNum = this.getNumWorkers();

            //global var broadcast
            long[] usersPartition = new long[workerNum + 1];  

            //global var broadcast
            long[] itemsPartition = new long[workerNum + 1]; 

            //local var
            KeyValueDataCollection usersOutBlocks; 
            //local var
            KeyValueDataCollection itemsOutBlocks;
            //local var, sync on master node
            DistributedPartialResultStep1 step1LocalResult;
            //global var
            NumericTable step2MasterResult = null;
            //local var
            KeyValueDataCollection step3LocalResult;
            //global vars 
            KeyValueDataCollection step4LocalInput;
            //local vars
            DistributedPartialResultStep4 itemsPartialResultLocal = null;
            //local vars
            DistributedPartialResultStep4 usersPartialResultLocal = null;

            // ------------------------------ initialization start ------------------------------

            InitDistributed initAlgorithm = new InitDistributed(daal_Context, Double.class, InitMethod.fastCSR);
            initAlgorithm.parameter.setFullNUsers(this.maxRowID + 1);
            initAlgorithm.parameter.setNFactors(r);
            initAlgorithm.parameter.setNumThreads(numThreads);
            initAlgorithm.parameter.setSeed(initAlgorithm.parameter.getSeed() + this.getSelfID());
            initAlgorithm.input.set(InitInputId.data, trainDaalTableTran);

            // Initialize the implicit ALS model 
            InitPartialResult initPartialResult = initAlgorithm.compute();

            // partialModel is local on each node
            PartialModel partialModel = initPartialResult.get(InitPartialResultId.partialModel);
            itemsPartialResultLocal = new DistributedPartialResultStep4(daal_Context);

            //store the partialModel on local slave node
            itemsPartialResultLocal.set(DistributedPartialResultStep4Id.outputOfStep4ForStep1, partialModel);
            step4LocalInput = new KeyValueDataCollection(daal_Context);

            // ------------------------------ initialization end ------------------------------

            // ------------------------------ computePartialModelBlocksToNode start ------------------------------
            long dataTableRows = trainDaalTable.getNumberOfRows();
            long dataTableTranRows = trainDaalTableTran.getNumberOfRows();

            //allreduce to get the users and items partition table
            Table<LongArray> dataTable_partition = new Table<>(0, new LongArrPlus());

            LongArray partition_array = LongArray.create(2, false);
            partition_array.get()[0] = dataTableRows;
            partition_array.get()[1] = dataTableTranRows;

            dataTable_partition.addPartition(new Partition<>(this.getSelfID(), partition_array));

            this.allgather("als", "get-partition-info", dataTable_partition);

            usersPartition[0] = 0;
            itemsPartition[0] = 0;

            for (int j=0;j<workerNum;j++) 
            {
                usersPartition[j+1] = usersPartition[j] + dataTable_partition.getPartition(j).get().get()[0];  
                itemsPartition[j+1] = itemsPartition[j] + dataTable_partition.getPartition(j).get().get()[1];
            }

            //compute out blocks
            usersOutBlocks = computeOutBlocks(daal_Context, workerNum, trainDaalTable, itemsPartition);
            itemsOutBlocks = computeOutBlocks(daal_Context, workerNum, trainDaalTableTran, usersPartition);

            // ------------------------------ compute initial RMSE from test dataset ------------------------------
            testModelInitRMSEMulti(usersPartition, itemsPartition, dataTableRows, testDataMap, row_mapping, col_mapping);

            // ------------------------------ Training Model Start ------------------------------
            for (int iteration=0; iteration < numIterations; iteration++) 
            {
                long compute_time_itr_step1 = 0;
                long comm_time_itr_step1 = 0;
                long compute_time_itr_step2 = 0;
                long comm_time_itr_step2 = 0;
                long compute_time_itr_step3 = 0;
                long comm_time_itr_step3 = 0;
                long compute_time_itr_step4 = 0;
                long comm_time_itr_step4 = 0;
                long train_time_itr = 0; 

                long start_train = System.currentTimeMillis();

                long start = System.currentTimeMillis();

                //step1 on local slave nodes 
                ALSTrainStep1 algo_step1 = new ALSTrainStep1(r, numThreads, itemsPartialResultLocal, this);

                //compute step 1
                step1LocalResult = algo_step1.compute();
                long end = System.currentTimeMillis();
                compute_time_itr_step1 += (end - start);

                //communication step 1
                start = System.currentTimeMillis();
                Table<ByteArray> step1LocalResult_table = algo_step1.communicate(step1LocalResult);
                end = System.currentTimeMillis();
                comm_time_itr_step1 += (end - start);
                
                //step 2 on master node
                ALSTrainStep2 algo_step2 = new ALSTrainStep2(r, numThreads, step1LocalResult_table, this);

                if (this.getSelfID() == 0)
                {
                    start = System.currentTimeMillis();
                    step2MasterResult = algo_step2.compute();                
                    end = System.currentTimeMillis();
                    compute_time_itr_step2 += (end - start);
                }

                //free up memory 
                step1LocalResult_table = null;
                step1LocalResult = null;

                //broadcast step2MasterResult to other slave nodes step2MasterResult is HomogenNumericTable
                start = System.currentTimeMillis();
                step2MasterResult = algo_step2.communicate(step2MasterResult);
                end = System.currentTimeMillis();
                comm_time_itr_step2 += (end - start);

                // ----------------------------------------- step3 on local node -----------------------------------------
                ALSTrainStep3 algo_step3 = new ALSTrainStep3(r, numThreads, itemsPartition, itemsOutBlocks, itemsPartialResultLocal, this);
                start = System.currentTimeMillis();

                DistributedPartialResultStep3 partialResult_step3 = algo_step3.compute();

                end = System.currentTimeMillis();
                compute_time_itr_step3 += (end - start);

                //broadcast step3LocalResult
                start = System.currentTimeMillis();
                // Prepare input objects for the fourth step of the distributed algorithm 
                step3LocalResult = partialResult_step3.get(DistributedPartialResultStep3Id.outputOfStep3ForStep4);
                Table<ByteArray> step3LocalResult_table = new Table<>(0, new ByteArrPlus());

                step4LocalInput = algo_step3.communicate(step3LocalResult, step4LocalInput, step3LocalResult_table);

                end = System.currentTimeMillis();
                comm_time_itr_step3 += (end - start);

                // // ----------------------------------------- step4 on local node -----------------------------------------
                start = System.currentTimeMillis();

                ALSTrainStep4 algo_step4 = new ALSTrainStep4(r, numThreads, alpha, lambda_als, step4LocalInput, trainDaalTable, 
                        step2MasterResult, this);

                usersPartialResultLocal = algo_step4.compute();

                //free up memory 
                step2MasterResult = null;
                step3LocalResult_table = null;

                end = System.currentTimeMillis();
                compute_time_itr_step4 += (end - start);

                //-------- clean the algo containers
                algo_step1 = null;
                algo_step2 = null;
                algo_step3 = null;
                algo_step4 = null;

                System.gc();
                // ----------------------------------------- finish updating partial users factors  -----------------------------------------
                // ----------------------------------------- Start updating partial item factors  -----------------------------------------

                //step1 on local slave nodes 
                // Create an algorithm object to perform first step of the implicit ALS training algorithm on local-node data 
                start = System.currentTimeMillis();
                algo_step1 = new ALSTrainStep1(r, numThreads, usersPartialResultLocal, this);
                //compute step 1
                step1LocalResult = algo_step1.compute();
                end = System.currentTimeMillis();
                compute_time_itr_step1 += (end - start);

                start = System.currentTimeMillis();
                step1LocalResult_table = algo_step1.communicate(step1LocalResult);
                end = System.currentTimeMillis();
                comm_time_itr_step1 += (end - start);

                algo_step2 = new ALSTrainStep2(r, numThreads, step1LocalResult_table, this);
                //step 2 on master node
                if (this.getSelfID() == 0)
                {
                    start = System.currentTimeMillis();
                    step2MasterResult = algo_step2.compute();                
                    end = System.currentTimeMillis();
                    compute_time_itr_step2 += (end - start);
                }

                //free up memory 
                // step1LocalResult_table_item = null;
                step1LocalResult_table = null;
                step1LocalResult = null;

                //broadcast step2MasterResult to other slave nodes step2MasterResult is HomogenNumericTable
                start = System.currentTimeMillis();
                step2MasterResult = algo_step2.communicate(step2MasterResult);
                end = System.currentTimeMillis();
                comm_time_itr_step2 += (end - start);

                // ----------------------------------------- step3 on local node update item  -----------------------------------------
                algo_step3 = new ALSTrainStep3(r, numThreads, usersPartition, usersOutBlocks, usersPartialResultLocal, this);
                start = System.currentTimeMillis();
                
                if (partialResult_step3 != null)
                    partialResult_step3 = null;

                partialResult_step3 = algo_step3.compute();
                end = System.currentTimeMillis();
                compute_time_itr_step3 += (end - start);

                //broadcast step3LocalResult
                start = System.currentTimeMillis();
                step3LocalResult = partialResult_step3.get(DistributedPartialResultStep3Id.outputOfStep3ForStep4);

                if (step3LocalResult_table != null)
                    step3LocalResult_table = null;

                step3LocalResult_table = new Table<>(0, new ByteArrPlus());
                step4LocalInput = algo_step3.communicate(step3LocalResult, step4LocalInput, step3LocalResult_table);
                end = System.currentTimeMillis();
                comm_time_itr_step3 += (end - start);
                

                // // ----------------------------------------- step4 on local node to update items-----------------------------------------
                start = System.currentTimeMillis();
                algo_step4 = new ALSTrainStep4(r, numThreads, alpha, lambda_als, step4LocalInput, trainDaalTableTran, 
                        step2MasterResult, this);

                itemsPartialResultLocal = algo_step4.compute();

                end = System.currentTimeMillis();
                compute_time_itr_step4 += (end - start);

                //free up memory 
                step2MasterResult = null;
                step3LocalResult_table = null;

                algo_step1 = null;
                algo_step2 = null;
                algo_step3 = null;
                algo_step4 = null;

                System.gc();

                long end_train = System.currentTimeMillis();
                LOG.info("Training Itr " + iteration + " Complete, Training time total: " + (end_train - start_train) + 
                        " compute time step1: " + compute_time_itr_step1 + 
                        " compute time step2: " + compute_time_itr_step2 + 
                        " compute time step3: " + compute_time_itr_step3 + 
                        " compute time step4: " + compute_time_itr_step4 + 
                        " comm time step1: " + comm_time_itr_step1 + 
                        " comm time step2: " + comm_time_itr_step2 + 
                        " comm time step3: " + comm_time_itr_step3 + 
                        " comm time step4: " + comm_time_itr_step4); 

                trainTime += (end_train - start_train);
                computeTime_step1 += compute_time_itr_step1;
                computeTime_step2 += compute_time_itr_step2;
                computeTime_step3 += compute_time_itr_step3;
                computeTime_step4 += compute_time_itr_step4;

                commTime_step1 += comm_time_itr_step1;
                commTime_step2 += comm_time_itr_step2;
                commTime_step3 += comm_time_itr_step3;
                commTime_step4 += comm_time_itr_step4;

                this.freeMemory();
                this.freeConn();
                System.gc();

                //test model after this iteration
                testModelMulti(iteration, usersPartition, itemsPartition, usersPartialResultLocal, itemsPartialResultLocal, testDataMap, row_mapping, col_mapping);

            }

            LOG.info("Training Time per iteration: " + (double)trainTime/numIterations 
                    + ", Compute Time Step 1: "+ ((double)computeTime_step1)/numIterations 
                    + ", Compute Time Step 2: "+ ((double)computeTime_step2)/numIterations 
                    + ", Compute Time Step 3: "+ ((double)computeTime_step3)/numIterations 
                    + ", Compute Time Step 4: "+ ((double)computeTime_step4)/numIterations 
                    + ", Comm Time Step 1: "+ ((double)commTime_step1)/numIterations 
                    + ", Comm Time Step 2: "+ ((double)commTime_step2)/numIterations 
                    + ", Comm Time Step 3: "+ ((double)commTime_step3)/numIterations 
                    + ", Comm Time Step 4: "+ ((double)commTime_step4)/numIterations );

            // ------------------------------ Training Model end ------------------------------
            //
            this.freeMemory();
            this.freeConn();

            context.progress();
            daal_Context.dispose();

        }//}}}


        /**
         * @brief computing RMSE values in sequential order
         *
         * @param itr
         * @param usersPartition
         * @param itemsPartition
         * @param usersPartialResultLocal
         * @param itemsPartialResultLocal
         * @param testDataMap
         * @param row_mapping
         * @param col_mapping
         *
         * @return 
         */
        private void testModel(int itr,
                             long[] usersPartition,
                             long[] itemsPartition,
                             DistributedPartialResultStep4 usersPartialResultLocal,
                             DistributedPartialResultStep4 itemsPartialResultLocal, 
                             Int2ObjectOpenHashMap<VRowCol> testDataMap,
                             int[] row_mapping, int[] col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
            //broadcast itemsPartialResult 
            byte[] serialItemsPartial = serializeItemRes(itemsPartialResultLocal);

            //deserialize itemsPartialResultLocal
            itemsPartialResultLocal.unpack(daal_Context);

            ByteArray itemsPartialResultLocal_harp = new ByteArray(serialItemsPartial, 0, serialItemsPartial.length);
            Table<ByteArray> itemsPartialResultLocal_table = new Table<>(0, new ByteArrPlus());
            itemsPartialResultLocal_table.addPartition(new Partition<>(this.getSelfID(), itemsPartialResultLocal_harp));
            this.allgather("als", "sync-partial-step4-res", itemsPartialResultLocal_table);

            LOG.info("Start Predicting");
            long start = System.currentTimeMillis();

            DistributedPartialResultStep4[] itemsRemoteResults = new DistributedPartialResultStep4[this.getNumWorkers()]; 
            for (int j=0; j<this.getNumWorkers();j++ ) 
                itemsRemoteResults[j] = deserializeItemRes(itemsPartialResultLocal_table.getPartition(j).get().get());

            
            //itemsPartition records the offset of item indices
            long startRowIndex = usersPartition[this.getSelfID()]; 
            long endRowIndex = usersPartition[this.getSelfID() + 1];
            
            //retrieve user model data
            HomogenNumericTable userModelTest = (HomogenNumericTable)(usersPartialResultLocal.get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
            long userModelTestRowNum = userModelTest.getNumberOfRows();
            long userModelTestColNum = userModelTest.getNumberOfColumns();
            double[] userModelTestData = userModelTest.getDoubleArray();

            //retrieve item model data
            long[][] itemRowColNum = new long[this.getNumWorkers()][];
            double[][] itemModelTestData = new double[this.getNumWorkers()][];

            for (int j=0; j<this.getNumWorkers(); j++) 
            {
                HomogenNumericTable itemModelTest = (HomogenNumericTable)(itemsRemoteResults[j].get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
                itemRowColNum[j] = new long[2];
                itemRowColNum[j][0] = itemModelTest.getNumberOfRows();
                itemRowColNum[j][1] = itemModelTest.getNumberOfColumns();
                itemModelTestData[j] = itemModelTest.getDoubleArray();
            }

            //row id and col id in test data in COO format starts from 0
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testDataMap.int2ObjectEntrySet().fastIterator();

            int testVLocalNum = 0;
            double rmse_local = 0;
            double rmse_effect_num = 0;

            while (iterator.hasNext()) {

                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();

                VRowCol vRowCol = entry.getValue();

                int row_idx_map = row_mapping[vRowCol.id] - 1; //starts from 0 

                if (row_idx_map >= startRowIndex && row_idx_map < endRowIndex)
                {
                    int index_row = row_idx_map - (int)startRowIndex;
                    int index_col = 0;

                    for(int j=0;j<vRowCol.numV;j++)
                    {

                        int item_buck = -1;
                        if (vRowCol.ids[j] >= col_mapping.length || col_mapping[vRowCol.ids[j]] == 0)
                            continue;

                        index_col = col_mapping[vRowCol.ids[j]] - 1;

                        double rating = vRowCol.v[j];

                        //get the relative itemblock that contains index_col
                        for(int k=0;k<this.getNumWorkers();k++)
                        {
                            if (index_col < (int)itemsPartition[k+1])
                            {
                                item_buck = k;
                                break;
                            }
                        }

                        if (item_buck >= 0)
                        {
                            index_col = index_col - (int)itemsPartition[item_buck];
                            //get the item buck data
                            double[] item_calc = itemModelTestData[item_buck]; 
                            double[] user_calc = userModelTestData;

                            double predict = 0;
                            for (int k=0;k<r;k++)
                            {
                                predict += (user_calc[index_row*r + k]*item_calc[index_col*r + k]); 
                            }

                            rmse_local += (((1.0 - predict)*(1.0 - predict))*(1+alpha*rating));
                            rmse_effect_num++;

                        }
                        
                    }

                    // testVLocalNum++;
                }

            }

            // LOG.info("Test Points on this worker: " + rmse_effect_num + ", rmse value: " + rmse_local);

            //allreduce to get the RMSE value 
            Table<DoubleArray> rmse_table = new Table<>(0, new DoubleArrPlus());
            DoubleArray rmse_array = DoubleArray.create(2, false);

            rmse_array.get()[0] = rmse_local;
            rmse_array.get()[1] = rmse_effect_num;
            rmse_table.addPartition(new Partition<>(this.getSelfID(), rmse_array));
            this.allgather("als", "get-rmse-info", rmse_table);

            //calculate out the RMSE of all the nodes
            double total_rmse = 0;
            double total_rmse_num = 0;

            for(int j=0;j<this.getNumWorkers();j++)
            {
                total_rmse += rmse_table.getPartition(j).get().get()[0];
                total_rmse_num += rmse_table.getPartition(j).get().get()[1];
            }
            
            long end = System.currentTimeMillis();
            LOG.info("Test model takes: " + (end - start));
            LOG.info("RMSE after Iteration: " + itr + " is : " + Math.sqrt(total_rmse/total_rmse_num));

        }//}}}

        /**
         * @brief compute RMSE values in parallel (multi-threading)
         *
         * @param itr
         * @param usersPartition
         * @param itemsPartition
         * @param usersPartialResultLocal
         * @param itemsPartialResultLocal
         * @param testDataMap
         * @param row_mapping
         * @param col_mapping
         *
         * @return 
         */
        private void testModelMulti(int itr,
                             long[] usersPartition,
                             long[] itemsPartition,
                             DistributedPartialResultStep4 usersPartialResultLocal,
                             DistributedPartialResultStep4 itemsPartialResultLocal, 
                             Int2ObjectOpenHashMap<VRowCol> testDataMap,
                             int[] row_mapping, int[] col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
            //broadcast itemsPartialResult 
            byte[] serialItemsPartial = serializeItemRes(itemsPartialResultLocal);

            //deserialize itemsPartialResultLocal
            itemsPartialResultLocal.unpack(daal_Context);

            ByteArray itemsPartialResultLocal_harp = new ByteArray(serialItemsPartial, 0, serialItemsPartial.length);
            Table<ByteArray> itemsPartialResultLocal_table = new Table<>(0, new ByteArrPlus());
            itemsPartialResultLocal_table.addPartition(new Partition<>(this.getSelfID(), itemsPartialResultLocal_harp));
            this.allgather("als", "sync-partial-step4-res", itemsPartialResultLocal_table);

            LOG.info("Start Predicting");
            long start = System.currentTimeMillis();

            DistributedPartialResultStep4[] itemsRemoteResults = new DistributedPartialResultStep4[this.getNumWorkers()]; 
            for (int j=0; j<this.getNumWorkers();j++ ) 
                itemsRemoteResults[j] = deserializeItemRes(itemsPartialResultLocal_table.getPartition(j).get().get());

            
            //itemsPartition records the offset of item indices
            long startRowIndex = usersPartition[this.getSelfID()]; 
            long endRowIndex = usersPartition[this.getSelfID() + 1];
            
            //retrieve user model data
            HomogenNumericTable userModelTest = (HomogenNumericTable)(usersPartialResultLocal.get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
            long userModelTestRowNum = userModelTest.getNumberOfRows();
            long userModelTestColNum = userModelTest.getNumberOfColumns();
            double[] userModelTestData = userModelTest.getDoubleArray();

            //retrieve item model data
            long[][] itemRowColNum = new long[this.getNumWorkers()][];
            double[][] itemModelTestData = new double[this.getNumWorkers()][];

            for (int j=0; j<this.getNumWorkers(); j++) 
            {
                HomogenNumericTable itemModelTest = (HomogenNumericTable)(itemsRemoteResults[j].get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
                itemRowColNum[j] = new long[2];
                itemRowColNum[j][0] = itemModelTest.getNumberOfRows();
                itemRowColNum[j][1] = itemModelTest.getNumberOfColumns();
                itemModelTestData[j] = itemModelTest.getDoubleArray();
            }

            //create the tasks for computing local RMSE
            double rmse_local = 0;
            long rmse_effect_num = 0;

            LinkedList<ComputeRMSE> rmse_compute_tasks = new LinkedList<>();
            double[][] rmse_vals  = new double[numThreads][];
            for (int j = 0; j < numThreads; j++) {
                //rmse_vals[0] is rmse val, and rmse_vals[1] is test point counts
                rmse_vals[j] = new double[2];
                rmse_compute_tasks.add(new ComputeRMSE(this.getNumWorkers(), this.getSelfID(), row_mapping, col_mapping, startRowIndex, endRowIndex, itemsPartition, 
                            userModelTestData, itemModelTestData, alpha, r, rmse_vals[j]));
            }

            DynamicScheduler<VRowCol, Object, ComputeRMSE> rmse_schedule =
                new DynamicScheduler<>(rmse_compute_tasks);

            //row id and col id in test data in COO format starts from 0
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testDataMap.int2ObjectEntrySet().fastIterator();

            // int testVLocalNum = 0;
            
            while (iterator.hasNext()) {

                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();
                // VRowCol vRowCol = entry.getValue();
                rmse_schedule.submit(entry.getValue());
            }

            rmse_schedule.start();
            rmse_schedule.stop();

            while (rmse_schedule.hasOutput()) {
                rmse_schedule.waitForOutput();
            }

            //add up all the local rmse values and counts
            for(int j=0;j<numThreads;j++)
            {
                rmse_local += rmse_vals[j][0]; 
                rmse_effect_num += rmse_vals[j][1];
            }

            // LOG.info("Test Points on this worker: " + rmse_effect_num + ", rmse value: " + rmse_local);

            //allreduce to get the RMSE value 
            Table<DoubleArray> rmse_table = new Table<>(0, new DoubleArrPlus());
            DoubleArray rmse_array = DoubleArray.create(2, false);

            rmse_array.get()[0] = rmse_local;
            rmse_array.get()[1] = rmse_effect_num;
            rmse_table.addPartition(new Partition<>(this.getSelfID(), rmse_array));
            this.allgather("als", "get-rmse-info", rmse_table);

            //calculate out the RMSE of all the nodes
            double total_rmse = 0;
            double total_rmse_num = 0;

            for(int j=0;j<this.getNumWorkers();j++)
            {
                total_rmse += rmse_table.getPartition(j).get().get()[0];
                total_rmse_num += rmse_table.getPartition(j).get().get()[1];
            }
            
            long end = System.currentTimeMillis();
            LOG.info("Test model takes: " + (end - start));
            LOG.info("RMSE after Iteration: " + itr + " is : " + Math.sqrt(total_rmse/total_rmse_num));

        }//}}}


        /**
         * @brief compute RMSE values prior to training process in 
         * sequential order
         *
         * @param usersPartition
         * @param itemsPartition
         * @param userNum
         * @param testDataMap
         * @param row_mapping
         * @param col_mapping
         *
         * @return 
         */
        private void testModelInitRMSE(long[] usersPartition,
                                       long[] itemsPartition,
                                       long userNum,
                                       Int2ObjectOpenHashMap<VRowCol> testDataMap,
                                       int[] row_mapping, int[] col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
            
            long start = System.currentTimeMillis();
            Random rand = new Random(System.currentTimeMillis());

            //itemsPartition records the offset of item indices
            long startRowIndex = usersPartition[this.getSelfID()];
            long endRowIndex = usersPartition[this.getSelfID() + 1];
            
            //retrieve user model data
            // HomogenNumericTable userModelTest = (HomogenNumericTable)(usersPartialResultLocal.get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
            // long userModelTestRowNum = userModelTest.getNumberOfRows();
            // long userModelTestColNum = userModelTest.getNumberOfColumns();
            double[] userModelTestData = new double[(int)userNum*r];
            //randomize user model values
            SGDUtil.randomize(rand, userModelTestData, (int)userNum*r, oneOverSqrtR);

            //retrieve item model data
            long[][] itemRowColNum = new long[this.getNumWorkers()][];
            double[][] itemModelTestData = new double[this.getNumWorkers()][];

            for (int j=0; j<this.getNumWorkers(); j++) 
            {
                // HomogenNumericTable itemModelTest = (HomogenNumericTable)(itemsRemoteResults[j].get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
                itemRowColNum[j] = new long[2];
                itemRowColNum[j][0] = itemsPartition[j+1] - itemsPartition[j]; //num of items on each worker
                itemRowColNum[j][1] = r;
                itemModelTestData[j] = new double[(int)itemRowColNum[j][0]*r];
                //randomize items model values
                SGDUtil.randomize(rand, itemModelTestData[j], (int)itemRowColNum[j][0]*r, oneOverSqrtR);

            }

            //row id and col id in test data in COO format starts from 0
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testDataMap.int2ObjectEntrySet().fastIterator();

            int testVLocalNum = 0;
            double rmse_local = 0;
            long rmse_effect_num = 0;

            while (iterator.hasNext()) {

                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();

                VRowCol vRowCol = entry.getValue();
                int row_idx_map = row_mapping[vRowCol.id] - 1;

                if (row_idx_map >= startRowIndex && row_idx_map < endRowIndex)
                {
                    int index_row = row_idx_map - (int)startRowIndex;
                    int index_col = 0;

                    for(int j=0;j<vRowCol.numV;j++)
                    {
                        int item_buck = -1;

                        if (vRowCol.ids[j] >= col_mapping.length || col_mapping[vRowCol.ids[j]] == 0)
                            continue;

                        index_col = col_mapping[vRowCol.ids[j]] - 1;

                        double rating = vRowCol.v[j];

                        //get the relative itemblock that contains index_col
                        for(int k=0;k<this.getNumWorkers();k++)
                        {
                            if (index_col < (int)itemsPartition[k+1])
                            {
                                item_buck = k;
                                break;
                            }
                        }

                        if (item_buck >= 0)
                        {
                            index_col = index_col - (int)itemsPartition[item_buck];
                            //get the item buck data
                            double[] item_calc = itemModelTestData[item_buck]; 
                            double[] user_calc = userModelTestData;

                            double predict = 0;
                            for (int k=0;k<r;k++)
                            {
                                predict += (user_calc[index_row*r + k]*item_calc[index_col*r + k]); 
                            }

                            rmse_local += (((1.0 - predict)*(1.0 - predict))*(1+alpha*rating));
                            rmse_effect_num++;

                        }
                        
                    }

                }

            }

            // LOG.info("Test Points on this worker: " + rmse_effect_num + ", rmse value: " + rmse_local);

            //allreduce to get the RMSE value 
            Table<DoubleArray> rmse_table = new Table<>(0, new DoubleArrPlus());
            DoubleArray rmse_array = DoubleArray.create(2, false);

            rmse_array.get()[0] = rmse_local;
            rmse_array.get()[1] = (double)rmse_effect_num;
            rmse_table.addPartition(new Partition<>(this.getSelfID(), rmse_array));
            this.allgather("als", "get-rmse-info", rmse_table);

            //calculate out the RMSE of all the nodes
            double total_rmse = 0;
            double total_rmse_num = 0;

            for(int j=0;j<this.getNumWorkers();j++)
            {
                total_rmse += rmse_table.getPartition(j).get().get()[0];
                total_rmse_num += rmse_table.getPartition(j).get().get()[1];
            }
            
            long end = System.currentTimeMillis();
            LOG.info("Test model takes: " + (end - start));
            LOG.info("RMSE before Iteration is : " + Math.sqrt(total_rmse/total_rmse_num));

        }//}}}

        /**
         * @brief compute RMSE values prior to training process 
         * in parallel (multi-threading)
         *
         * @param usersPartition
         * @param itemsPartition
         * @param userNum
         * @param testDataMap
         * @param row_mapping
         * @param col_mapping
         *
         * @return 
         */
        private void testModelInitRMSEMulti(long[] usersPartition,
                                       long[] itemsPartition,
                                       long userNum,
                                       Int2ObjectOpenHashMap<VRowCol> testDataMap,
                                       int[] row_mapping, int[] col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
            
            long start = System.currentTimeMillis();
            Random rand = new Random(System.currentTimeMillis());

            //itemsPartition records the offset of item indices
            long startRowIndex = usersPartition[this.getSelfID()];
            long endRowIndex = usersPartition[this.getSelfID() + 1];
            
            //retrieve user model data
            // HomogenNumericTable userModelTest = (HomogenNumericTable)(usersPartialResultLocal.get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
            // long userModelTestRowNum = userModelTest.getNumberOfRows();
            // long userModelTestColNum = userModelTest.getNumberOfColumns();
            double[] userModelTestData = new double[(int)userNum*r];
            //randomize user model values
            SGDUtil.randomize(rand, userModelTestData, (int)userNum*r, oneOverSqrtR);

            //retrieve item model data
            long[][] itemRowColNum = new long[this.getNumWorkers()][];
            double[][] itemModelTestData = new double[this.getNumWorkers()][];

            for (int j=0; j<this.getNumWorkers(); j++) 
            {
                // HomogenNumericTable itemModelTest = (HomogenNumericTable)(itemsRemoteResults[j].get(DistributedPartialResultStep4Id.outputOfStep4).getFactors());
                itemRowColNum[j] = new long[2];
                itemRowColNum[j][0] = itemsPartition[j+1] - itemsPartition[j]; //num of items on each worker
                itemRowColNum[j][1] = r;
                itemModelTestData[j] = new double[(int)itemRowColNum[j][0]*r];
                //randomize items model values
                SGDUtil.randomize(rand, itemModelTestData[j], (int)itemRowColNum[j][0]*r, oneOverSqrtR);

            }

           
            //create the tasks for computing local RMSE
            double rmse_local = 0;
            long rmse_effect_num = 0;

            LinkedList<ComputeRMSE> rmse_compute_tasks = new LinkedList<>();
            double[][] rmse_vals  = new double[numThreads][];
            for (int j = 0; j < numThreads; j++) {
                //rmse_vals[0] is rmse val, and rmse_vals[1] is test point counts
                rmse_vals[j] = new double[2];
                rmse_compute_tasks.add(new ComputeRMSE(this.getNumWorkers(), this.getSelfID(), row_mapping, col_mapping, startRowIndex, endRowIndex, itemsPartition, 
                            userModelTestData, itemModelTestData, alpha, r, rmse_vals[j]));
            }

            DynamicScheduler<VRowCol, Object, ComputeRMSE> rmse_schedule =
                new DynamicScheduler<>(rmse_compute_tasks);

            //row id and col id in test data in COO format starts from 0
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testDataMap.int2ObjectEntrySet().fastIterator();

            while (iterator.hasNext()) {

                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();

                rmse_schedule.submit(entry.getValue());

            }

            rmse_schedule.start();
            rmse_schedule.stop();

            while (rmse_schedule.hasOutput()) {
                rmse_schedule.waitForOutput();
            }

            //add up all the local rmse values and counts
            for(int j=0;j<numThreads;j++)
            {
                rmse_local += rmse_vals[j][0]; 
                rmse_effect_num += rmse_vals[j][1];
            }

            // LOG.info("Test Points on this worker: " + rmse_effect_num + ", rmse value: " + rmse_local);

            //allreduce to get the RMSE value 
            Table<DoubleArray> rmse_table = new Table<>(0, new DoubleArrPlus());
            DoubleArray rmse_array = DoubleArray.create(2, false);

            rmse_array.get()[0] = rmse_local;
            rmse_array.get()[1] = (double)rmse_effect_num;
            rmse_table.addPartition(new Partition<>(this.getSelfID(), rmse_array));
            this.allgather("als", "get-rmse-info", rmse_table);

            //calculate out the RMSE of all the nodes
            double total_rmse = 0;
            double total_rmse_num = 0;

            for(int j=0;j<this.getNumWorkers();j++)
            {
                total_rmse += rmse_table.getPartition(j).get().get()[0];
                total_rmse_num += rmse_table.getPartition(j).get().get()[1];
            }
            
            long end = System.currentTimeMillis();
            LOG.info("Test model takes: " + (end - start));
            LOG.info("RMSE before Iteration is : " + Math.sqrt(total_rmse/total_rmse_num));

        }//}}}

        /**
         * @brief serialization of model data for broadcasting to other nodes
         *
         * @param res
         *
         * @return 
         */
        private byte[] serializeItemRes(DistributedPartialResultStep4 res) throws IOException 
        {//{{{
            // Create an output stream to serialize the numeric table 
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

            // Serialize the partialResult table into the output stream 
            res.pack();
            outputStream.writeObject(res);

            // Store the serialized data in an array 
            byte[] buffer = outputByteStream.toByteArray();
            return buffer;
        }//}}}

        /**
         * @brief deserialization after receiving model data from other nodes 
         *
         * @param buffer
         *
         * @return 
         */
        private DistributedPartialResultStep4 deserializeItemRes(byte[] buffer) throws IOException, ClassNotFoundException 
        {//{{{

            // Create an input stream to deserialize the numeric table from the array 
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

            // Create a numeric table object 
            DistributedPartialResultStep4 restoredRes = (DistributedPartialResultStep4)inputStream.readObject();
            restoredRes.unpack(daal_Context);

            return restoredRes;
        }//}}}

        /**
         * @brief compute the indices of columns/rows in training dataset that
         * shall be sent to other nodes
         *
         * @param context
         * @param nNodes
         * @param dataBlock
         * @param dataBlockPartition
         *
         * @return 
         */
        private KeyValueDataCollection computeOutBlocks(DaalContext context,
                int nNodes, CSRNumericTable dataBlock,
                long[] dataBlockPartition) 
        {//{{{

            long nRows = dataBlock.getNumberOfRows();
            int iNRows = (int)nRows;
            boolean[] blockIdFlags = new boolean[iNRows * nNodes];
            for (int i = 0; i < iNRows * nNodes; i++) {
                blockIdFlags[i] = false;
            }

            long[] rowOffsets = dataBlock.getRowOffsetsArray();
            long[] colIndices = dataBlock.getColIndicesArray();

            for (long i = 0; i < nRows; i++) {
                for (long j = rowOffsets[(int)i] - 1; j < rowOffsets[(int)i+1] - 1; j++) {
                    for (int k = 1; k < nNodes + 1; k++) {
                        if (dataBlockPartition[k-1] <= colIndices[(int)j] - 1 && colIndices[(int)j] - 1 < dataBlockPartition[k]) {
                            blockIdFlags[(k-1) * iNRows + (int)i] = true;
                        }
                    }
                }
            }

            long[] nNotNull = new long[nNodes];
            for (int i = 0; i < nNodes; i++) {
                nNotNull[i] = 0;
                for (int j = 0; j < iNRows; j++) {
                    if (blockIdFlags[i * iNRows + j]) {
                        nNotNull[i] += 1;
                    }
                }
            }

            KeyValueDataCollection result = new KeyValueDataCollection(context);

            for (int i = 0; i < nNodes; i++) {
                HomogenNumericTable indicesTable = new HomogenNumericTable(context, Integer.class, 1, nNotNull[i],
                        NumericTable.AllocationFlag.DoAllocate);
                IntBuffer indicesBuffer = IntBuffer.allocate((int)nNotNull[i]);
                indicesBuffer = indicesTable.getBlockOfRows(0, nNotNull[i], indicesBuffer);
                int indexId = 0;
                for (int j = 0; j < iNRows; j++) {
                    if (blockIdFlags[i * iNRows + j]) {
                        indicesBuffer.put(indexId, j);
                        indexId++;
                    }
                }
                indicesTable.releaseBlockOfRows(0, nNotNull[i], indicesBuffer);
                result.set(i, indicesTable);
            }
            return result;
        }//}}}

        /**
         * @brief regroup the ALS training data by rows
         *
         * @param vRowMap
         * @param numThreads
         *
         * @return 
         */
        private Table<VSet> TrainDataRGroupByRow(Int2ObjectOpenHashMap<VRowCol> vRowMap, int[] mapping, int numThreads, boolean isTran)
        {//{{{

            // maximum row ids 
            int maxRowID = Integer.MIN_VALUE;

            VRowCol[] values = vRowMap.values().toArray(new VRowCol[0]);

            // Clean the data
            vRowMap.clear();
            vRowMap.trim();

            //export data from hashmap to daal table
            Table<VSet> vSetTable = new Table<>(0, new VSetCombiner(), values.length);
            for (int i = 0; i < values.length; i++) 
            {
                VRowCol vRowCol = values[i];
                vSetTable.addPartition(new Partition<>(
                            mapping[vRowCol.id], new VSet(vRowCol.id,
                                vRowCol.ids, vRowCol.v, vRowCol.numV)));
                if ((i + 1) % 1000000 == 0) {
                    LOG.info("Processed " + (i + 1));
                }

                if (mapping[vRowCol.id] > maxRowID) {
                    maxRowID = mapping[vRowCol.id];
                }
            }

            values = null;

            //regroup by row ids, partition in a range
            int oldNumRows = vSetTable.getNumPartitions();
            Table<LongArray> maxRowTable =
                new Table<>(0, new LongArrMax());

            LongArray maxrowArray = LongArray.create(1, false);
            maxrowArray.get()[0] = (long) maxRowID;
            maxRowTable.addPartition(new Partition<>(0, maxrowArray));

            //get the global maxRow id
            this.allreduce("als", "get-max-rowID", maxRowTable);

            maxRowID = (int) maxRowTable.getPartition(0).get().get()[0];
            // seed = seedTable.getPartition(0).get().get()[1];
            maxRowTable.release();
            maxRowTable = null;

            LOG.info("Regroup data by rows " + oldNumRows
                    + ", new compact maxRowID " + maxRowID);

            long start = System.currentTimeMillis();

            if (isTran == false)
                this.maxRowID = maxRowID;

            regroup("sgd", "regroup-vw", vSetTable,
                    new ALSRowPartitioner(maxRowID, this.getNumWorkers()));

            long end = System.currentTimeMillis();

            int newNumRows = vSetTable.getNumPartitions();

            LOG.info("Regroup data by rows took: "
                    + (end - start)
                    + ", old number of rows: "
                    + oldNumRows + " new num of rows: " + newNumRows);

            return vSetTable;

        }//}}}

             
        /**
         * @brief function to printout values of DAAL CSRNumericTable for debugging
         *
         * @param header
         * @param nt
         * @param nPrintedRows
         *
         * @return 
         */
        public void printNumericTable(String header, CSRNumericTable nt, long nPrintedRows) 
        {//{{{

            long[] rowOffsets = nt.getRowOffsetsArray();
            long[] colIndices = nt.getColIndicesArray();
            double[] values = nt.getDoubleArray();

            long nNtCols = nt.getNumberOfColumns();
            long nNtRows = nt.getNumberOfRows();
            long nRows = nNtRows;
            long nCols = nNtCols;

            if (nPrintedRows > 0) {
                nRows = Math.min(nNtRows, nPrintedRows);
            }

            // if (nPrintedCols > 0) {
            //     nCols = Math.min(nNtCols, nPrintedCols);
            // }

            StringBuilder builder = new StringBuilder();
            builder.append(header);
            builder.append("\n");

            // double[] oneDenseRow = new double[(int) nCols];
            // double[] oneDenseRow = new double[(int) nNtCols];
            for (int i = 0; i < nRows; i++) {

                // for (int j = 0; j < nCols; j++) {
                //     oneDenseRow[j] = 0;
                // }

                int nElementsInRow = (int) (rowOffsets[i + 1] - rowOffsets[i]);
                for (int k = 0; k < nElementsInRow; k++) {
                    // oneDenseRow[(int) (colIndices[(int) (rowOffsets[i] - 1 + k)] - 1)] = values[(int) (rowOffsets[i] - 1
                            // + k)];
                    String tmp = String.format("(%d, %d, %-6.3f) ", i, (int)(colIndices[(int) (rowOffsets[i] - 1 + k)]), values[(int) (rowOffsets[i] - 1 + k)]);
                    builder.append(tmp);
                }

                builder.append("\n");
                // for (int j = 0; j < nCols; j++) {
                //     String tmp = String.format("%-6.3f   ", oneDenseRow[j]);
                //     builder.append(tmp);
                // }

            }

            System.out.println(builder.toString());
        }//}}}

        /**
         * @brief function to printout daal NumericTable for debugging
         *
         * @param header
         * @param nt
         * @param nPrintedRows
         * @param nPrintedCols
         *
         * @return 
         */
        public void printNumericTable(String header, NumericTable nt, long nPrintedRows, long nPrintedCols) 
        {//{{{

            long nNtCols = nt.getNumberOfColumns();
            long nNtRows = nt.getNumberOfRows();
            long nRows = nNtRows;
            long nCols = nNtCols;

            if (nPrintedRows > 0) {
                nRows = Math.min(nNtRows, nPrintedRows);
            }

            DoubleBuffer result = DoubleBuffer.allocate((int) (nNtCols * nRows));
            result = nt.getBlockOfRows(0, nRows, result);

            if (nPrintedCols > 0) {
                nCols = Math.min(nNtCols, nPrintedCols);
            }

            StringBuilder builder = new StringBuilder();
            builder.append(header);
            builder.append("\n");
            for (long i = 0; i < nRows; i++) {
                for (long j = 0; j < nCols; j++) {
                    String tmp = String.format("%-6.3f   ", result.get((int) (i * nNtCols + j)));
                    builder.append(tmp);
                }
                builder.append("\n");
            }
            System.out.println(builder.toString());
        }//}}}

}
