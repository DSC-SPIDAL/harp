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

import com.intel.daal.algorithms.implicit_als.PartialModel;
import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.algorithms.implicit_als.training.init.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.data_aux.Service;
import edu.iu.data_comm.HarpDAALComm;
import edu.iu.datasource.COO;
import edu.iu.datasource.COOGroup;
import edu.iu.datasource.HarpDAALDataSource;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.LongArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

// packages from Daal

public class ALSDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//command line args
        private int numIterations;
	private int numThreads;
        private int numThreads_harp;
        private int nFactors; 
        private double alpha;
        private double lambda_als;
        private String testFilePath;
	private List<String> inputFiles;
	private Configuration conf;

             
        //max global row ids
        private long maxRowID = 0;
	// app paras
        private double oneOverSqrtR;
        //final RMSE value of test
        private double rmse;
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

	//DAAL related
  	private static HarpDAALComm harpcomm;	
        private static DaalContext daal_Context = new DaalContext();
	private static HarpDAALDataSource datasource;

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
        //training time per iteration
        public double trainTimePerIter = 0;

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context) {

            LOG.info("start setup: " + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
            long startTime = System.currentTimeMillis();
            this.conf = context.getConfiguration();

            this.nFactors = this.conf.getInt(HarpDAALConstants.NUM_FACTOR, 100);
            this.numIterations = this.conf.getInt(HarpDAALConstants.NUM_ITERATIONS, 100);
            this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 16);
            this.testFilePath = this.conf.get(HarpDAALConstants.TEST_FILE_PATH,"");
	    this.alpha = this.conf.getDouble(Constants.ALPHA, 40.0);
	    this.lambda_als = this.conf.getDouble(Constants.LAMBDA, 0.06);
            this.numThreads_harp = Runtime.getRuntime().availableProcessors();

            rmse = 0.0;
            computeTaskTime = 0L;
            itrTimeStamp = 0L;
            waitTime = 0L;
            numVTrained = 0L;
            totalNumTrain = 0L;
            totalNumCols = 0L;
            effectiveTestV = 0L;
            oneOverSqrtR = 1.0/Math.sqrt(nFactors);
            random = new Random(System.currentTimeMillis());

            long endTime = System.currentTimeMillis();
            LOG.info("config (ms): "+ (endTime - startTime));
            LOG.info("nFactors " + nFactors);
            LOG.info("Num Iterations " + numIterations);
            LOG.info("Num Threads " + numThreads);
            LOG.info("TEST FILE PATH " + testFilePath);
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
            this.inputFiles = getVFiles(reader);

	    this.datasource = new HarpDAALDataSource(this.numThreads_harp, this.conf);
	    // create communicator
            this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.getNumWorkers(), this.daal_Context, this);

            try {
                runALS(context);
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
        private void runALS(final Context context) throws Exception 
        {//{{{

	    // load COO files from HDFS
            LOG.info("Load ALS training points in COO format");
	    List<COO> coo_data = this.datasource.loadCOOFiles(this.inputFiles," ");
	    LOG.info("loaded coo elem number: " + coo_data.size());

	    // group local COO data points by rows and cols 
	    HashMap<Long, COOGroup> coo_group = this.datasource.groupCOOByIDs(coo_data, true);
	    HashMap<Long, COOGroup> coo_group_tran = this.datasource.groupCOOByIDs(coo_data, false);

	    // remapping row and col ids into a compact form
	    HashMap<Long, Integer> gid_remap = this.datasource.remapCOOIDs(coo_group, this.getSelfID(), this.getNumWorkers(), this);
	    HashMap<Long, Integer> gid_remap_tran = this.datasource.remapCOOIDs(coo_group_tran, this.getSelfID(), this.getNumWorkers(), this);

	    // regroup data points according to the remapped compact IDs 
	    int[] maxCompactID = new int[1];
	    Table<COOGroup> regrouped_table = this.datasource.regroupCOOList(coo_group, gid_remap, this, maxCompactID); 

	    int[] maxCompactIDTran = new int[1];
	    Table<COOGroup> regrouped_table_tran = this.datasource.regroupCOOList(coo_group_tran, gid_remap_tran, this, maxCompactIDTran); 

	    this.maxRowID = maxCompactID[0];
	    LOG.info("MAX ROW ID is: " + this.maxRowID);

	    // ------------------------ load test dataset ------------------------
            // -------------------------- each node has the whole dataset --------------------------
	    List<COO> coo_data_test = this.datasource.loadCOOFiles(testFilePath, " ");
	    HashMap<Long, COOGroup> testDataMap = this.datasource.groupCOOByIDs(coo_data_test, true);

	    //create the CSRNumericTable
            long start_spmat = System.currentTimeMillis();

	    CSRNumericTable trainDaalTable = this.datasource.COOToCSR(regrouped_table, gid_remap_tran, daal_Context);
	    CSRNumericTable trainDaalTableTran = this.datasource.COOToCSR(regrouped_table_tran, gid_remap, daal_Context);

            long end_spmat = System.currentTimeMillis();

            //check the size of trainDaalTable
            LOG.info("CSR table rows: " + trainDaalTable.getNumberOfRows() + " CSR table cols: " 
                    + trainDaalTable.getNumberOfColumns() + " CSR datasize: " 
                    + trainDaalTable.getDataSize() 
                    + "CSR table tran rows: " + trainDaalTableTran.getNumberOfRows() + " CSR table tran cols: " 
                    + trainDaalTableTran.getNumberOfColumns() + " CSR tran datasize: " 
                    + trainDaalTableTran.getDataSize()
                    + " conversion time: " + (end_spmat - start_spmat));

            //create the ALS daal containers
            int workerNum = this.getNumWorkers();

            //global var broadcast
            long[] usersPartition = { workerNum };

            long[] usersPartition_test = new long[workerNum + 1];  
            long[] itemsPartition_test = new long[workerNum + 1]; 

            NumericTable userOffsets;
            NumericTable itemOffsets;

            // for Step 1
            //local var, sync on master node
            KeyValueDataCollection initStep1LocalResult = null;
            DistributedPartialResultStep1 step1LocalResult;

            // for Step 2
            //global var
            KeyValueDataCollection initStep2LocalInput = null;
            NumericTable step2MasterResult = null;

            // for Step 3
            KeyValueDataCollection userStep3LocalInput = null;
            KeyValueDataCollection itemStep3LocalInput = null;
            //local var
            KeyValueDataCollection step3LocalResult;

            // for Step 4
            //global vars 
            KeyValueDataCollection step4LocalInput = new KeyValueDataCollection(daal_Context);
            //local vars
            DistributedPartialResultStep4 itemsPartialResultLocal = null;
            //local vars
            DistributedPartialResultStep4 usersPartialResultLocal = null;

            // ------------------------------ initialization start ------------------------------

            InitDistributedStep1Local initAlgorithm = new  InitDistributedStep1Local(daal_Context, Double.class, InitMethod.fastCSR);
            initAlgorithm.parameter.setFullNUsers(this.maxRowID + 1);
            initAlgorithm.parameter.setNFactors(nFactors);
            initAlgorithm.parameter.setSeed(initAlgorithm.parameter.getSeed() + this.getSelfID());
            initAlgorithm.parameter.setPartition(new HomogenNumericTable(daal_Context, usersPartition, 1, usersPartition.length));

            initAlgorithm.input.set(InitInputId.data, trainDaalTableTran);

            // Initialize the implicit ALS model 
            InitPartialResult initPartialResult = initAlgorithm.compute();
            itemStep3LocalInput = initPartialResult.get(InitPartialResultBaseId.outputOfInitForComputeStep3);
            userOffsets = initPartialResult.get(InitPartialResultBaseId.offsets, this.getSelfID());
            // partialModel is local on each node
            PartialModel partialModel  = initPartialResult.get(InitPartialResultId.partialModel);

            itemsPartialResultLocal = new DistributedPartialResultStep4(daal_Context);
            //store the partialModel on local slave node
            itemsPartialResultLocal.set(DistributedPartialResultStep4Id.outputOfStep4ForStep1, partialModel);

            //initStep1LocalResult shall be broadcasted to each node
            initStep1LocalResult = initPartialResult.get(InitPartialResultCollectionId.outputOfStep1ForStep2);
            initStep2LocalInput = new KeyValueDataCollection(daal_Context);
            ALSInitResult init_res = new ALSInitResult(initStep1LocalResult, initStep2LocalInput, this.getSelfID(), this.getNumWorkers(), this.harpcomm);
            init_res.communicate();

            //initialize step 2 
            InitDistributedStep2Local initAlgorithm_step2 = new InitDistributedStep2Local(daal_Context, Double.class, InitMethod.fastCSR);
            initAlgorithm_step2.input.set(InitStep2LocalInputId.inputOfStep2FromStep1, initStep2LocalInput);

            // Compute partial results of the second step on local nodes 
            InitDistributedPartialResultStep2 initPartialResult_step2 = initAlgorithm_step2.compute();
            trainDaalTable = (CSRNumericTable)(initPartialResult_step2.get(InitDistributedPartialResultStep2Id.transposedData));
            userStep3LocalInput = initPartialResult_step2.get(InitPartialResultBaseId.outputOfInitForComputeStep3);
            itemOffsets = initPartialResult_step2.get(InitPartialResultBaseId.offsets, this.getSelfID());

            // ------------------------------ initialization end ------------------------------
            // ------------------------------ compute initial RMSE from test dataset ------------------------------
            long dataTableRows = trainDaalTable.getNumberOfRows();
            long dataTableTranRows = trainDaalTableTran.getNumberOfRows();
            
            //allreduce to get the users and items partition table
            Table<LongArray> dataTable_partition = new Table<>(0, new LongArrPlus());
            LongArray partition_array = LongArray.create(2, false);
            partition_array.get()[0] = dataTableRows;
            partition_array.get()[1] = dataTableTranRows;
            dataTable_partition.addPartition(new Partition<>(this.getSelfID(), partition_array));
            this.allgather("als", "get-partition-info", dataTable_partition);
            
            usersPartition_test[0] = 0;
            itemsPartition_test[0] = 0;
            
            for (int j=0;j<workerNum;j++) 
            {
                usersPartition_test[j+1] = usersPartition_test[j] + dataTable_partition.getPartition(j).get().get()[0];  
                itemsPartition_test[j+1] = itemsPartition_test[j] + dataTable_partition.getPartition(j).get().get()[1];
            }
            
            testModelInitRMSEMulti(usersPartition_test, itemsPartition_test, dataTableRows, testDataMap, gid_remap, gid_remap_tran);

            // ------------------------------ Training Model Start ------------------------------
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

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
                ALSTrainStep1 algo_step1 = new ALSTrainStep1(nFactors, numThreads, this.getNumWorkers(), itemsPartialResultLocal, this.harpcomm);
                //compute step 1
                step1LocalResult = algo_step1.compute();
                long end = System.currentTimeMillis();
                compute_time_itr_step1 += (end - start);

                //communication step 1
                start = System.currentTimeMillis();
                DistributedPartialResultStep1[] step1LocalResult_table = algo_step1.communicate(step1LocalResult);
                end = System.currentTimeMillis();
                comm_time_itr_step1 += (end - start);
                
                //step 2 on master node
                ALSTrainStep2 algo_step2 = new ALSTrainStep2(nFactors, numThreads, this.getNumWorkers(), step1LocalResult_table, this.harpcomm);
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
                ALSTrainStep3 algo_step3 = new ALSTrainStep3(nFactors, numThreads, this.getSelfID(), this.getNumWorkers(), 
				itemOffsets, itemsPartialResultLocal, itemStep3LocalInput, this.harpcomm);
                start = System.currentTimeMillis();
                DistributedPartialResultStep3 partialResult_step3 = algo_step3.compute();
                end = System.currentTimeMillis();
                compute_time_itr_step3 += (end - start);

                //broadcast step3LocalResult
                start = System.currentTimeMillis();
                // Prepare input objects for the fourth step of the distributed algorithm 
                step3LocalResult = partialResult_step3.get(DistributedPartialResultStep3Id.outputOfStep3ForStep4);
                step4LocalInput = algo_step3.communicate(step3LocalResult, step4LocalInput);

                end = System.currentTimeMillis();
                comm_time_itr_step3 += (end - start);

                // ----------------------------------------- step4 on local node -----------------------------------------
                start = System.currentTimeMillis();
                ALSTrainStep4 algo_step4 = new ALSTrainStep4(nFactors, numThreads, alpha, lambda_als, step4LocalInput, trainDaalTable, 
                        step2MasterResult, this);

                usersPartialResultLocal = algo_step4.compute();

                //free up memory 
                step2MasterResult = null;

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
                algo_step1 = new ALSTrainStep1(nFactors, numThreads, this.getNumWorkers(), usersPartialResultLocal, this.harpcomm);
                //compute step 1
                step1LocalResult = algo_step1.compute();
                end = System.currentTimeMillis();
                compute_time_itr_step1 += (end - start);

                start = System.currentTimeMillis();
                step1LocalResult_table = algo_step1.communicate(step1LocalResult);
                end = System.currentTimeMillis();
                comm_time_itr_step1 += (end - start);

                algo_step2 = new ALSTrainStep2(nFactors, numThreads, this.getNumWorkers(), step1LocalResult_table, this.harpcomm);
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
                algo_step3 = new ALSTrainStep3(nFactors, numThreads, this.getSelfID(), this.getNumWorkers(), userOffsets, 
				usersPartialResultLocal, userStep3LocalInput, this.harpcomm);

                start = System.currentTimeMillis();
                
                if (partialResult_step3 != null)
                    partialResult_step3 = null;

                partialResult_step3 = algo_step3.compute();
                end = System.currentTimeMillis();
                compute_time_itr_step3 += (end - start);

                //broadcast step3LocalResult
                start = System.currentTimeMillis();
                step3LocalResult = partialResult_step3.get(DistributedPartialResultStep3Id.outputOfStep3ForStep4);

                step4LocalInput = algo_step3.communicate(step3LocalResult, step4LocalInput);
                end = System.currentTimeMillis();
                comm_time_itr_step3 += (end - start);
                
                // // ----------------------------------------- step4 on local node to update items-----------------------------------------
                start = System.currentTimeMillis();
                algo_step4 = new ALSTrainStep4(nFactors, numThreads, alpha, lambda_als, step4LocalInput, trainDaalTableTran, 
                        step2MasterResult, this);

                itemsPartialResultLocal = algo_step4.compute();

                end = System.currentTimeMillis();
                compute_time_itr_step4 += (end - start);

                //free up memory 
                step2MasterResult = null;

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
                testModelMulti(iteration, usersPartition_test, itemsPartition_test, usersPartialResultLocal, itemsPartialResultLocal, testDataMap, gid_remap, gid_remap_tran);

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
                             HashMap<Long, COOGroup> testDataMap,
                             HashMap<Long, Integer> row_mapping, HashMap<Long, Integer> col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
	    // collect local partial results
	    SerializableBase[] des_ouput = this.harpcomm.harpdaal_allgather(itemsPartialResultLocal, "als", "allgather_itemsPartialRes");
            itemsPartialResultLocal.unpack(daal_Context);

	    LOG.info("Start Predicting");
            long start = System.currentTimeMillis();

            DistributedPartialResultStep4[] itemsRemoteResults = new DistributedPartialResultStep4[this.getNumWorkers()]; 
 	    for (int j=0; j<this.getNumWorkers();j++ ) 
                itemsRemoteResults[j] = (DistributedPartialResultStep4)(des_ouput[j]); 

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
                            userModelTestData, itemModelTestData, alpha, nFactors, rmse_vals[j]));
            }

            DynamicScheduler<COOGroup, Object, ComputeRMSE> rmse_schedule =
                new DynamicScheduler<>(rmse_compute_tasks);

	    for(COOGroup entry : testDataMap.values())
                rmse_schedule.submit(entry);

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
                                       HashMap<Long, COOGroup> testDataMap,
                                       HashMap<Long, Integer> row_mapping, HashMap<Long, Integer> col_mapping) throws Exception
        {//{{{

            // ------------------------------ Predicting Model Start ------------------------------
            
            long start = System.currentTimeMillis();
            Random rand = new Random(System.currentTimeMillis());

            //itemsPartition records the offset of item indices
            long startRowIndex = usersPartition[this.getSelfID()];
            long endRowIndex = usersPartition[this.getSelfID() + 1];
            
            //retrieve user model data
            double[] userModelTestData = new double[(int)userNum*nFactors];
            //randomize user model values
            Service.randomize(rand, userModelTestData, (int)userNum*nFactors, oneOverSqrtR);

            //retrieve item model data
            long[][] itemRowColNum = new long[this.getNumWorkers()][];
            double[][] itemModelTestData = new double[this.getNumWorkers()][];

            for (int j=0; j<this.getNumWorkers(); j++) 
            {
                itemRowColNum[j] = new long[2];
                itemRowColNum[j][0] = itemsPartition[j+1] - itemsPartition[j]; //num of items on each worker
                itemRowColNum[j][1] = nFactors;
                itemModelTestData[j] = new double[(int)itemRowColNum[j][0]*nFactors];
                //randomize items model values
                Service.randomize(rand, itemModelTestData[j], (int)itemRowColNum[j][0]*nFactors, oneOverSqrtR);

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
                            userModelTestData, itemModelTestData, alpha, nFactors, rmse_vals[j]));
            }

            DynamicScheduler<COOGroup, Object, ComputeRMSE> rmse_schedule =
                new DynamicScheduler<>(rmse_compute_tasks);

	    for(COOGroup entry : testDataMap.values())
		rmse_schedule.submit(entry);

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
