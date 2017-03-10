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

package edu.iu.daal_sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.PrintWriter;
// import java.lang.reflect.Field;
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
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.daal.*;

// packages from Daal 
import com.intel.daal.algorithms.mf_sgd.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.HomogenBMNumericTable;
// import com.intel.daal.data_management.data.AOSNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
// import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

public class SGDDaalCollectiveMapper
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
        private String modelDirPath;
        //time value used by timer
        private double time;
        //use or not use timer tuning
        private boolean timerTuning;
        //number of pipelines in model rotation
        private int numModelSlices;
        //iteration interval of doing rmse test
        private int rmseIteInterval;
        //iteration interval of freeing cached data in model rotation
        private int freeInterval;
        //final RMSE value of test
        private double rmse;
        private String testFilePath;
        //time of total computation in each iteration
        private long computeTime;
        //time of parallel tasks in each iteration
        private long computeTaskTime;
        //timestamp of each training iteration
        private long itrTimeStamp;
        //num of trained training points 
        private long numVTrained;
        //time spent in waiting rotated model data in each itr
        private long waitTime;
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

            time =
                configuration
                .getDouble(Constants.Time, 1000);

            numModelSlices =
                configuration.getInt(
                        Constants.NUM_MODEL_SLICES, 2);
            testFilePath =
                configuration.get(Constants.TEST_FILE_PATH,
                        "");
            String javaOpts =
                configuration.get(
                        "mapreduce.map.collective.java.opts", "");
            rmseIteInterval = 1;
            freeInterval = 20;
            rmse = 0.0;
            computeTime = 0L;
            computeTaskTime = 0L;
            itrTimeStamp = 0L;
            waitTime = 0L;
            numVTrained = 0L;
            timerTuning =
                configuration.getBoolean(
                        Constants.ENABLE_TUNING, true);
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
            LOG.info("Time Tuning " + timerTuning);
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
                runSGD(vFiles, context.getConfiguration(),
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
         * @brief running MF-SGD 
         *
         * @param vFilePaths
         * @param configuration
         * @param context
         *
         * @return 
         */
        private void runSGD(
                final LinkedList<String> vFilePaths,
                final Configuration configuration,
                final Context context) throws Exception {//{{{

            LOG.info("Use Model Parallelism");

            //----------------------- load the train dataset-----------------------
            Int2ObjectOpenHashMap<VRowCol> vRowMap =
                SGDUtil.loadVWMap(vFilePaths, numThreads, configuration);

            //-----------------------load the test dataset-----------------------
            Int2ObjectOpenHashMap<VRowCol> testVColMap =
                SGDUtil.loadTestVHMap(testFilePath, configuration, numThreads);

            //-------------------- every colMap stores the number of points under a specific col_id --------------
            Int2ObjectOpenHashMap<int[]>[] colMaps = new Int2ObjectOpenHashMap[numThreads];
            for(int i=0;i< numThreads;i++)
                colMaps[i] = new Int2ObjectOpenHashMap<>();

            // --------------------- rowMap contains all the row ids on this local worker -------------------
            Int2ObjectOpenHashMap<int[]> rowMap = new Int2ObjectOpenHashMap<>();

            //-----------------------regrouping the training data points indexed by row ids -----------------------
            //create daal table for training set 
            //create daal table for row ids
            long totalNumTrainV = regroupLoadTrainData(vRowMap, rowMap, colMaps, numThreads);
            LOG.info("Total Train Points on this worker: "+totalNumTrainV);

            //-----------------------Load test data points indexed by row ids -----------------------
            //create daal table for testing set 
            long totalNumTestV = LoadTestData(testVColMap, rowMap,  numThreads);
            LOG.info("Total Test Points on this worker: "+totalNumTestV);

            // ----------------- creating H model within Harp  -----------------
            Table<DoubleArray>[] hTableMap = new Table[numModelSlices];
            createHModel(hTableMap, colMaps, numModelSlices, oneOverSqrtR, random);

            //free up java heap memory space
            vRowMap = null;
            testVColMap = null;
            colMaps = null;

            this.freeMemory();
            this.freeConn();

            System.gc();

            
            // ---------------------------------------------- Create rotator----------------------------------------------
            //
            // Use harp's rotator, order of columns is randomized in each rotation
            int numSplits = ((int) Math.round(numThreads / 20.0) + 1) * 20;
            final int numWorkers = this.getNumWorkers();
            int[] order = RotationUtil.getRotationSequences(random, numWorkers, (numIterations + 1) * 2, this);
            Rotator<DoubleArray> rotator = new Rotator<>(hTableMap, numSplits, true, this, order, "sgd");
            rotator.start();

            //create DAAL algorithm object, using distributed version of DAAL-MF-SGD  
            Distri sgdAlgorithm = new Distri(daal_Context, Double.class, Method.defaultSGD);

            //TO REMOVE!!
            sgdAlgorithm.parameter.setIsSGD2(1);

            // --------------------------loading training and test datasets into DAAL ------------------------------
            sgdAlgorithm.input.set(InputId.dataWPos, train_wPos_daal);
            sgdAlgorithm.input.set(InputId.dataHPos, train_hPos_daal);
            sgdAlgorithm.input.set(InputId.dataVal, train_val_daal);

            sgdAlgorithm.input.set(InputId.testWPos, test_wPos_daal);
            sgdAlgorithm.input.set(InputId.testHPos, test_hPos_daal);
            sgdAlgorithm.input.set(InputId.testVal, test_val_daal);

            PartialResult model_data = new PartialResult(daal_Context);
            sgdAlgorithm.setPartialResult(model_data);

            // ------------------------- loading W matrix into DAAL codes  -----------------
            // passing HomogenBMNumericTable wMat_rowid_daal into DAAL codes;
            // generate the W matrix scalable hashtable within DAAL
            model_data.set(PartialResultId.presWMat, wMat_rowid_daal);

            //create H matrix model
            //--- prepare the daal table --------------
            //the rotated num of columns in each itr shall be less than the size of table
            long htable_daal_size = (long)((totalNumCols + numWorkers)/numWorkers);
            htable_daal_size = ((htable_daal_size + numModelSlices)/numModelSlices);

            LOG.info("Create a htable_daal with a size of "+htable_daal_size);

            //hTableMap_daal has a dimension of feature dimension plus a sentinal value to remember 
            //the column id
            hTableMap_daal = new SOANumericTable(daal_Context, htable_daal_size, r+1);

            //initialize htable_daal_size with empty values
            for(int k=0;k<htable_daal_size;k++)
                ((SOANumericTable)hTableMap_daal).setArray(new double[r+1], k);

            //computeRMSE before iteration
            printRMSEbyDAAL(sgdAlgorithm, model_data, rotator, hTableMap, hTableMap_daal, numWorkers, totalNumTestV, wMat_size, 0, configuration);
            
            LOG.info("Iteration Starts.");

            long iterationAccu = 0;
            long jniTime = 0;

            long ComputeStart = 0;
            long ComputeEnd = 0;
            long ComputeTime = 0;
            // int innerItr = 0;

            // -----------------------------------------
            // For iteration
            for (int i = 1; i <= numIterations; i++) {

                long iteStart = System.currentTimeMillis();
                long compute_task_time_itr = 0;
                long jniDataConvertTime_itr = 0;
                long compute_time_itr = 0;
                long wait_time_itr = 0;

                //reset the trained num in each itr
                numVTrained = 0;
                
                //clear and reset task compute time
                sgdAlgorithm.parameter.ResetComputeTaskTime();
                sgdAlgorithm.parameter.ResetDataConvertTime();

                for (int j = 0; j < numWorkers; j++) {

                    for (int k = 0; k < numModelSlices; k++) {

                        long t1 = System.currentTimeMillis();
                        //finish rotation 
                        rotator.getRotation(k);
                        long t2 = System.currentTimeMillis();
                        waitTime += (t2 - t1);
                        wait_time_itr += (t2 - t1);

                        //-----load H data from Harp into DAAL's data structure
                        int hPartitionMapSize = hTableMap[k].getNumPartitions();
                        LOG.info(" hPartition Size: "+ hPartitionMapSize);
                        
                        //------------------ setup new columns into hTableMap_daal ----------
                        int table_entry = 0;
                        for(Partition<DoubleArray> p : hTableMap[k].getPartitions())
                        {
                            double[] data = (double[])p.get().get(); 
                            ((SOANumericTable)hTableMap_daal).setArrayOnly(data, table_entry);
                            table_entry++;
                        }

                        model_data.set(PartialResultId.presHMat, hTableMap_daal);

                        //--------------- start of computation by DAAL ---------------
                        ComputeStart = System.currentTimeMillis();

                        sgdAlgorithm.parameter.set(epsilon,lambda, r, wMat_size, hPartitionMapSize, 1, numThreads, 0, 1);
                        sgdAlgorithm.parameter.setTimer(time);
                        sgdAlgorithm.compute();
                        
                        numVTrained += sgdAlgorithm.parameter.GetTrainedNumV();
                        
                        ComputeEnd = System.currentTimeMillis();
                        ComputeTime += (ComputeEnd - ComputeStart);
                        compute_time_itr += (ComputeEnd - ComputeStart);
                        
                        //---------------------- update the H model Data ----------------------
                        t1 = System.currentTimeMillis();
                        rotator.rotate(k);
                        t2 = System.currentTimeMillis();
                        waitTime += (t2 - t1);
                        wait_time_itr += (t2 - t1);

                    }

                }

                long iteEnd = System.currentTimeMillis();
                long iteTime = iteEnd - iteStart;
                iterationAccu += iteTime;

                //adjust the timer
                int percentage = (int) Math.round((double) numVTrained / (double) totalNumTrainV*100.0);
                compute_task_time_itr =  sgdAlgorithm.parameter.GetComputeTaskTime();
                jniDataConvertTime_itr = sgdAlgorithm.parameter.GetDataConvertTime();
                jniTime += jniDataConvertTime_itr;

                if (i == 1 && timerTuning) {

                    //check
                    LOG.info("adjust minibatch: selfID: "+this.getSelfID()+" compute task time: "+compute_task_time_itr+" numVTrained: "+numVTrained+" time " + time);
                    double newMiniBatch = adjustMiniBatch(this.getSelfID(), compute_task_time_itr, numVTrained, i, time, numModelSlices, numWorkers);

                    if (time != newMiniBatch) {
                        time = newMiniBatch;
                        LOG.info("Set miniBatch to " + time);
                    }
                }

                // Calculate RMSE
                if (i == 1 || i % rmseIteInterval == 0) 
                {
                    printRMSEbyDAAL(sgdAlgorithm, model_data, rotator, hTableMap, hTableMap_daal, numWorkers, totalNumTestV, wMat_size, i, configuration);
                }

                //free up cached column values in rotation
                if (i % freeInterval == 0) {
                    this.freeMemory();
                    this.freeConn();
                }

                //printout summary for this iteration
                LOG.info("Summary for itr: "+i+" total: "+iteTime+" compute: "+ (compute_time_itr - jniDataConvertTime_itr) + 
                        " data convert: " + jniDataConvertTime_itr + "convert overhead: " + (jniDataConvertTime_itr*100/compute_time_itr)
                        + "%, wait: "+ wait_time_itr+" percentage: "+ percentage + " TimeStamp: " + iterationAccu + " s");
                
                context.progress();

            }

            // Stop sgdCompute and rotation
            trainTimePerIter = ((double)iterationAccu)/numIterations;

            LOG.info("Training Time per iteration: " + trainTimePerIter 
                    + ", Compute Time: " 
                    + ((double)ComputeTime - jniTime)/numIterations 
                    + ", JNI data transfer time: "
                    + ((double)jniTime)/numIterations
                    + ", wait rotation time per itr: "
                    + ((double)waitTime)/numIterations 
                    + ", misc time: " 
                    + ((double)(iterationAccu - ComputeTime - waitTime))/numIterations);

            //free up the native off-JVM heap memory allocated within DAAL codes
            sgdAlgorithm.parameter.freeData();

            //free up native memory of daal tables
            wMat_rowid_daal.freeDataMemory();
            train_wPos_daal.freeDataMemory();
            train_hPos_daal.freeDataMemory();
            train_val_daal.freeDataMemory();

            test_wPos_daal.freeDataMemory();
            test_hPos_daal.freeDataMemory();
            test_val_daal.freeDataMemory();

            daal_Context.dispose();
            rotator.stop();
            System.gc();

        }//}}}

        
        /**
         * @brief regroup the training dataset across all the mappers (workers)
         * load the training dataset to daal table
         *
         * @param vRowMap
         * @param rowMap
         * @param colMaps
         * @param numThreads
         *
         * @return 
         */
        private long regroupLoadTrainData(Int2ObjectOpenHashMap<VRowCol> vRowMap, Int2ObjectOpenHashMap<int[]> rowMap, Int2ObjectOpenHashMap<int[]>[] colMaps, int numThreads) 
        {//{{{

            //to convert VRowCol elements to three homogenNumericTables for DAAL
            //vRowMap sort by row ids
            long regroupTotalStart = System.currentTimeMillis();

            Table<VSet> vSetTable =
                new Table<>(0, new VSetCombiner());
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                vRowMap.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();
                int rowID = entry.getIntKey();
                VRowCol vRowCol = entry.getValue();
                //vRowCol.ids store columns ids
                vSetTable.addPartition(new Partition<>(
                            rowID, new VSet(vRowCol.id, vRowCol.ids,
                                vRowCol.v, vRowCol.numV)));
            }
            // Clean the data
            vRowMap.clear();

            // ------------- start regroup the data indexed by row ids-------------
            long start = System.currentTimeMillis();
            regroup("sgd", "regroup-vw", vSetTable,
                    new Partitioner(this.getNumWorkers()));
            long end = System.currentTimeMillis();

            LOG.info("Regroup data by rows took: "
                    + (end - start)
                    + " miliseconds, number of rows in local: "
                    + vSetTable.getNumPartitions());

            this.freeMemory();
            long regroupTotalEnd = System.currentTimeMillis();
            LOG.info("Time of Regrouping VRowMap: " + (regroupTotalEnd - regroupTotalStart));

            // ------------- finish regroup the data indexed by row ids-------------
            //
            long workerNumV = 0L;
            LinkedList<VSet> vSetList = new LinkedList<>();

            //obtain the row ids for local training points
            IntArray idArray = IntArray.create(vSetTable.getNumPartitions(), false);
            int[] ids = idArray.get();
            vSetTable.getPartitionIDs().toArray(ids);

            int local_row_num = idArray.size();
            IntArrays.quickSort(ids, 0, local_row_num);

            //---- load local wMap ids after regroup to hashmap -----------
            for(int k=0;k<local_row_num;k++)
            {
                rowMap.put(ids[k], new int[1]);
            }

            //----------------- create the daal table for local row ids -----------------
            wMat_size = idArray.size();
            wMat_rowid_daal = new HomogenBMNumericTable(daal_Context, Integer.class, 1, wMat_size, NumericTable.AllocationFlag.DoAllocate);
            wMat_rowid_daal.releaseBlockOfColumnValuesByte(0, 0, wMat_size, ids);

            //--------------------- redistribute the vsets onto different threads---------------------
            //--------------------- also calculate the number of col ids -----------------------------
            long prepareDaalCopyStart = System.currentTimeMillis();

            for (int i = 0; i < idArray.size(); i++) 
            {
                Partition<VSet> partition = vSetTable.getPartition(ids[i]);
                VSet vSet = partition.get();
                vSetList.add(vSet);
                workerNumV += vSet.getNumV();
            }

            idArray.release();
            idArray = null;
            LOG.info("Number of V on this worker " + workerNumV);

            LinkedList<ReGroupTask> reg_tasks = new LinkedList<>();
            for (int i = 0; i < numThreads; i++) {
                reg_tasks.add(new ReGroupTask(colMaps[i]));
            }

            DynamicScheduler<VSet, Object, ReGroupTask> reg_compute =
                new DynamicScheduler<>(reg_tasks);

            reg_compute.submitAll(vSetList);
            reg_compute.start();
            reg_compute.stop();

            while (reg_compute.hasOutput()) {
                reg_compute.waitForOutput();
            }

            //load points from Harp to DAAL side
            train_wPos_daal = new HomogenBMNumericTable(daal_Context, Integer.class, 1, workerNumV, NumericTable.AllocationFlag.DoAllocate);
            train_hPos_daal = new HomogenBMNumericTable(daal_Context, Integer.class, 1, workerNumV, NumericTable.AllocationFlag.DoAllocate);
            train_val_daal = new HomogenBMNumericTable(daal_Context, Double.class, 1, workerNumV, NumericTable.AllocationFlag.DoAllocate);

            Thread[] threads = new Thread[numThreads];

            LinkedList<int[]> train_wPos_daal_sets = new LinkedList<>();
            LinkedList<int[]> train_hPos_daal_sets = new LinkedList<>();
            LinkedList<double[]> train_val_daal_sets = new LinkedList<>();

            for(int i=0;i<numThreads;i++)
            {
                train_wPos_daal_sets.add(new int[reg_tasks.get(i).getNumPoint()]);
                train_hPos_daal_sets.add(new int[reg_tasks.get(i).getNumPoint()]);
                train_val_daal_sets.add(new double[reg_tasks.get(i).getNumPoint()]);
            }

            for (int q = 0; q<numThreads; q++) 
            {
                threads[q] = new Thread(new TaskLoadPoints(q, numThreads, reg_tasks.get(q).getSetList(), 
                            train_wPos_daal_sets.get(q),train_hPos_daal_sets.get(q), train_val_daal_sets.get(q)));

                threads[q].start();
            }

            for (int q=0; q< numThreads; q++) {

                try
                {
                    threads[q].join();
                }catch(InterruptedException e)
                {
                    System.out.println("Thread interrupted.");
                }

            }

            long prepareDaalCopyEnd = System.currentTimeMillis();
            LOG.info("Time of Preparing Train Data Daal Copy: " + (prepareDaalCopyEnd - prepareDaalCopyStart));

            long DaalCopyStart = System.currentTimeMillis();

            int itr_pos = 0;
            for (int i=0;i<numThreads; i++)
            {
                train_wPos_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), train_wPos_daal_sets.get(i));
                train_hPos_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), train_hPos_daal_sets.get(i));
                train_val_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), train_val_daal_sets.get(i));
                itr_pos += reg_tasks.get(i).getNumPoint();
            }

            long DaalCopyEnd = System.currentTimeMillis();
            LOG.info("Time of Train Data Daal Copy: " + (DaalCopyEnd - DaalCopyStart));

            vSetTable.release();
            vSetTable = null;
            train_wPos_daal_sets = null;
            train_hPos_daal_sets = null;
            train_val_daal_sets = null;
            reg_tasks = null;
            reg_compute = null;
            threads = null;

            System.gc();
            return workerNumV; //return the local num of training points

        }//}}}

        /**
         * @brief load test data into daal table
         *
         * @param testVColMap
         * @param rowMap
         * @param numThreads
         *
         * @return 
         */
        private long LoadTestData(Int2ObjectOpenHashMap<VRowCol> testVColMap, Int2ObjectOpenHashMap<int[]> rowMap, int numThreads) 
        {//{{{

            //testVRowMap is partitioned by columns
            long regroupTotalStart = System.currentTimeMillis();

            //filter out the test points whose row ids are not on this worker
            long workerTestV = 0L;

            LinkedList<VRowCol> test_set_list = new LinkedList<>();

            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testVColMap.int2ObjectEntrySet().fastIterator();

            while (iterator.hasNext()) {

                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();
                // Only record test V related to the local W
                // model
                VRowCol vRowCol = entry.getValue();
                double[] v = new double[vRowCol.numV];
                int[] ids = new int[vRowCol.numV];
                int index = 0;
                for (int i = 0; i < vRowCol.numV; i++) {
                    int[] wRow = rowMap.get(vRowCol.ids[i]);

                    if (wRow != null) {

                        v[index] = vRowCol.v[i];
                        ids[index] = vRowCol.ids[i];
                        index++;

                    }
                }

                double[] newV = new double[index];
                int[] newIds = new int[index];

                System.arraycopy(v, 0, newV, 0, index);
                System.arraycopy(ids, 0, newIds, 0, index);

                vRowCol.ids = newIds;
                vRowCol.v = newV;
                vRowCol.m1 = null;
                vRowCol.m2 = null;
                vRowCol.numV = index;

                workerTestV += index;
                test_set_list.add(vRowCol);
            }

            LOG.info("Test Points number on this worker: " + workerTestV);

            long prepareDaalCopyStart = System.currentTimeMillis();

            //redistribute the vsets onto different threads
            LinkedList<TestVDistriTask> reg_tasks = new LinkedList<>();
            for (int i = 0; i < numThreads; i++) {
                reg_tasks.add(new TestVDistriTask());
            }

            DynamicScheduler<VRowCol, Object, TestVDistriTask> reg_compute =
                new DynamicScheduler<>(reg_tasks);

            reg_compute.submitAll(test_set_list);
            reg_compute.start();
            reg_compute.stop();

            while (reg_compute.hasOutput()) {
                reg_compute.waitForOutput();
            }

            //load points from Harp to DAAL side
            test_wPos_daal = new HomogenBMNumericTable(daal_Context, Integer.class, 1, workerTestV, NumericTable.AllocationFlag.DoAllocate);
            test_hPos_daal = new HomogenBMNumericTable(daal_Context, Integer.class, 1, workerTestV, NumericTable.AllocationFlag.DoAllocate);
            test_val_daal = new HomogenBMNumericTable(daal_Context, Double.class, 1, workerTestV, NumericTable.AllocationFlag.DoAllocate);

            Thread[] threads = new Thread[numThreads];

            LinkedList<int[]> test_wPos_daal_sets = new LinkedList<>();
            LinkedList<int[]> test_hPos_daal_sets = new LinkedList<>();
            LinkedList<double[]> test_val_daal_sets = new LinkedList<>();

            for(int i=0;i<numThreads;i++)
            {
                test_wPos_daal_sets.add(new int[reg_tasks.get(i).getNumPoint()]);
                test_hPos_daal_sets.add(new int[reg_tasks.get(i).getNumPoint()]);
                test_val_daal_sets.add(new double[reg_tasks.get(i).getNumPoint()]);
            }

            for (int q = 0; q<numThreads; q++) 
            {
                threads[q] = new Thread(new TaskLoadTestPoints(q, numThreads, reg_tasks.get(q).getSetList(), 
                            test_wPos_daal_sets.get(q),test_hPos_daal_sets.get(q), test_val_daal_sets.get(q)));

                threads[q].start();
            }

            for (int q=0; q< numThreads; q++) {

                try
                {
                    threads[q].join();
                }catch(InterruptedException e)
                {
                    System.out.println("Thread interrupted.");
                }

            }

            long prepareDaalCopyEnd = System.currentTimeMillis();
            LOG.info("Time of Preparing Test Data Daal Copy: " + (prepareDaalCopyEnd - prepareDaalCopyStart));

            long DaalCopyStart = System.currentTimeMillis();

            int itr_pos = 0;
            for (int i=0;i<numThreads; i++)
            {

                test_wPos_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), test_wPos_daal_sets.get(i));
                test_hPos_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), test_hPos_daal_sets.get(i));
                test_val_daal.releaseBlockOfColumnValuesByte(0, itr_pos, reg_tasks.get(i).getNumPoint(), test_val_daal_sets.get(i));
                itr_pos += reg_tasks.get(i).getNumPoint();

            }

            long DaalCopyEnd = System.currentTimeMillis();
            LOG.info("Time of Test Data Daal Copy: " + (DaalCopyEnd - DaalCopyStart));

            testVColMap.clear();
            testVColMap = null;
            reg_tasks = null;
            reg_compute = null;
            threads = null;
            test_set_list = null;
            test_wPos_daal_sets = null;
            test_hPos_daal_sets = null;
            test_val_daal_sets = null;

            System.gc();
            return workerTestV; //return the local num of test points
           
        }//}}}

        /**
         * @brief create the H matrix for rotation
         *
         * @param hTableMap
         * @param colMaps
         * @param numModelSlices
         * @param oneOverSqrtR
         * @param random
         *
         * @return 
         */
        private void createHModel(
                Table<DoubleArray>[] hTableMap,
                Int2ObjectOpenHashMap<int[]>[] colMaps,
                int numModelSlices,
                double oneOverSqrtR, Random random) throws Exception 
        {//{{{

            LOG.info("Start creating H model.");

            for (int i = 0; i < numModelSlices; i++) {
                hTableMap[i] = new Table<>(i, new DoubleArrPlus());
            }

            Table<IntArray> vHSumTable = new Table<>(0, new IntArrPlus());

            for(int i = 0; i< numThreads; i++)
            {
                ObjectIterator<Int2ObjectMap.Entry<int[]>> iterator =
                    colMaps[i].int2ObjectEntrySet().fastIterator();

                    while (iterator.hasNext()) 
                    {

                        Int2ObjectMap.Entry<int[]> entry = iterator.next();
                        IntArray array = IntArray.create(1, false);
                        int partitionID = entry.getIntKey();
                        array.get()[0] = entry.getValue()[0];

                        PartitionStatus status = vHSumTable
                            .addPartition(new Partition<>(partitionID, array));

                    }

            }

            // Aggregate column index and the element
            // count on each column
            int numWorkers = this.getNumWorkers();

            this.regroup("sgd", "regroup-vhsum",
                    vHSumTable, new Partitioner(numWorkers));

            //add vHSumTable of each worker together
            this.allgather("sgd", "allgather-vhsum", vHSumTable);

            //totalNumCols is the number of cols from all the workers
            totalNumCols = vHSumTable.getNumPartitions();

            IntArray idArray = IntArray.create((int) totalNumCols, false);

            vHSumTable.getPartitionIDs().toArray(idArray.get());
            IntArrays.quickSort(idArray.get(), 0, idArray.size());
            int selfID = this.getSelfID();
            int workerIndex = 0;
            int sliceIndex = 0;
            int[] ids = idArray.get();

            //sum all the training points num over all the workers
            for (int i = 0; i < idArray.size(); i++) 
            {
                Partition<IntArray> partition = vHSumTable.getPartition(ids[i]);
                int[] numV_Col = partition.get().get();
                for(int k=0;k<numV_Col.length;k++)
                    totalNumTrain += (long) numV_Col[k];
            }

            LOG.info("Total Training Num V of all workers: "+ totalNumTrain);

            //evenly re-distributed the H model data onto each worker
            for (int i = 0; i < idArray.size(); i++) {

                if (workerIndex % numWorkers == selfID) {
                    // This h column
                    // will be created by this worker
                    int colID = ids[i];
                    DoubleArray rCol = DoubleArray.create(r+1, false);
                    SGDUtil.randomize(random, rCol.get(), r, oneOverSqrtR);
                    //the first element of rCol is its col id
                    rCol.get()[0] = (double)colID;

                    hTableMap[sliceIndex % numModelSlices]
                        .addPartition(new Partition<>(colID,
                                    rCol));
                    sliceIndex++;

                }

                workerIndex++;
            }

            vHSumTable.release();
            vHSumTable = null;
            idArray.release();
            idArray = null;

        }//}}}


        /**
         * @brief start a rotation to compute the test dataset
         *
         * @param Algo
         * @param model_data
         * @param rotator
         * @param hTableMap
         * @param hTableMap_daal
         * @param numWorkers
         * @param totalNumTestV
         * @param wMap_size
         * @param iteration
         * @param configuration
         *
         * @return 
         */
        private void printRMSEbyDAAL(Distri Algo,
                                     PartialResult model_data,
                                     Rotator<DoubleArray> rotator,
                                     Table<DoubleArray>[] hTableMap,
                                     NumericTable hTableMap_daal,
                                     int numWorkers,
                                     long totalNumTestV, 
                                     long wMap_size,
                                     int iteration,
                                     Configuration configuration) throws InterruptedException {

            DoubleArray array = DoubleArray.create(2, false);

            array.get()[0] = computeRMSEbyDAAL(Algo, model_data, rotator, hTableMap, hTableMap_daal, numWorkers, wMap_size);
            array.get()[1] = effectiveTestV;
            effectiveTestV = 0;
            Table<DoubleArray> rmseTable = new Table<>(0, new DoubleArrPlus());
            rmseTable.addPartition(new Partition<DoubleArray>(0,array));

            this.allreduce("sgd", "allreduce-rmse-" + iteration, rmseTable);

            rmse = rmseTable.getPartition(0).get().get()[0];
            double allNumTestV = rmseTable.getPartition(0).get().get()[1];
            rmse = Math.sqrt(rmse/allNumTestV);

            LOG.info("RMSE After Iteration " + iteration + ": " + rmse);
            rmseTable.release();
            rmse = 0.0;
                    
        }

        /**
         * @brief compute RMSE for test dataset
         *
         * @param Algo
         * @param model_data
         * @param rotator
         * @param hTableMap
         * @param hTableMap_daal
         * @param numWorkers
         * @param wMap_size
         *
         * @return 
         */
        private double computeRMSEbyDAAL( Distri Algo,
                                          PartialResult model_data,
                                          Rotator<DoubleArray> rotator,
                                          Table<DoubleArray>[] hTableMap,
                                          NumericTable hTableMap_daal,
                                          int numWorkers,
                                          long wMap_size) {

            double prmse = 0;
            // int innerItr = 0;
            int testV_count = 0;

            for (int j = 0; j < numWorkers; j++) {

                for (int k = 0; k < numModelSlices; k++) {

                    rotator.getRotation(k);

                    //-----load H data from Harp into DAAL's data structure
                    int hPartitionMapSize = hTableMap[k].getNumPartitions();
                    LOG.info(" Test hPartition Size: "+ hPartitionMapSize);

                    //--- prepare the daal table --------------
                    int table_entry = 0;
                    for(Partition<DoubleArray> p : hTableMap[k].getPartitions())
                    {
                        double[] data = (double[])p.get().get(); 
                        ((SOANumericTable)hTableMap_daal).setArrayOnly(data, table_entry);
                        table_entry++;
                    }

                    model_data.set(PartialResultId.presHMat, hTableMap_daal);

                    NumericTable matrixRMSE = new HomogenBMNumericTable(daal_Context, Double.class, 1, 1, NumericTable.AllocationFlag.DoAllocate, 0.0);
                    model_data.set(PartialResultId.presRMSE, matrixRMSE);

                    //----------- set up the training model data for DAAL -----------
                    Algo.parameter.set(epsilon,lambda, r, wMap_size, hPartitionMapSize, 1, numThreads, 0, 1);
                    Algo.parameter.setRatio(1.0);
                    Algo.parameter.setIsTrain(0);
                    Algo.compute();
                    effectiveTestV += Algo.parameter.GetTestV();
                    Algo.parameter.setIsTrain(1);

                    //retrieve rmse value
                    double[] prmse_array = new double[1];
                    ((HomogenBMNumericTable)matrixRMSE).getBlockOfRowsByte(0,1,prmse_array);

                    matrixRMSE.freeDataMemory();
                    prmse += prmse_array[0];
                    rotator.rotate(k);
                }
            }

            return prmse;
        }

        /**
         * @brief adjust the minibatch length
         *
         * @param selfID
         * @param computeTime
         * @param numVTrained
         * @param iteration
         * @param miniBatch
         * @param numModelSlices
         * @param numWorkers
         *
         * @return 
         */
        private double adjustMiniBatch(int selfID,
                long computeTime, long numVTrained,
                int iteration, double miniBatch,
                int numModelSlices, int numWorkers) {
            // Try to get worker ID
            // and related computation Time
            // and the percentage of
            // completion
            Table<IntArray> arrTable =
                new Table<>(0, new IntArrPlus());
            IntArray array = IntArray.create(2, false);
            array.get()[0] = (int) computeTime;
            array.get()[1] = (int) numVTrained;
            arrTable.addPartition(new Partition<>(selfID,
                        array));
            this.allgather("sgd",
                    "allgather-compute-status-" + iteration,
                    arrTable);
            long totalComputeTime = 0L;
            long totalNumVTrained = 0L;
            for (Partition<IntArray> partition : arrTable
                    .getPartitions()) {
                int[] recvArr = partition.get().get();
                totalComputeTime += (long) recvArr[0];
                totalNumVTrained += (long) recvArr[1];
                    }
            arrTable.release();
            arrTable = null;
            double ratio = Constants.GOLDEN_RATIO;
            double percentage =
                (double) totalNumVTrained
                / (double) totalNumTrain;
            double avgComputeTime =
                (double) totalComputeTime
                / (double) numWorkers
                / (double) numModelSlices
                / (double) numWorkers;
            miniBatch =
                (double) Math.round(ratio
                        / (double) percentage
                        * (double) avgComputeTime / 10.0) * 14L;
            LOG.info("new miniBatch " + miniBatch + " "
                    + ratio + " " + percentage);
            return miniBatch;
        }

}
