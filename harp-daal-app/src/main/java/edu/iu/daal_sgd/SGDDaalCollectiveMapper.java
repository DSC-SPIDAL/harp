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
import edu.iu.harp.schdynamic.DynamicScheduler;


import edu.iu.daal.*;

// packages from Daal 
import com.intel.daal.algorithms.mf_sgd.*;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.AOSNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

public class SGDDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {
        private int r;
        private double oneOverSqrtR;
        private double lambda;
        private double epsilon;
        private int numIterations;
        private int numThreads;
        private String modelDirPath;
        private double time;
        private int numModelSlices;
        private int rmseIteInterval;
        private int freeInterval;
        private double rmse;
        private double testRMSE;
        private String testFilePath;
        private boolean timerTuning;
        private long computeTime;
        private long numVTrained;
        private long waitTime;
        private Random random;

        private long totalNumV;
        private long totalNumCols;
        private int absentTestNum;

        //daal Context
        private static DaalContext daal_Context = new DaalContext();

        //for test trainTime per iteration
        public double trainTimePerIter = 0;

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
                .getDouble(Constants.Time, 0);
            numModelSlices =
                configuration.getInt(
                        Constants.NUM_MODEL_SLICES, 2);
            testFilePath =
                configuration.get(Constants.TEST_FILE_PATH,
                        "");
            String javaOpts =
                configuration.get(
                        "mapreduce.map.collective.java.opts", "");
            // rmseIteInterval = 5;
            rmseIteInterval = 1;
            freeInterval = 20;
            rmse = 0.0;
            testRMSE = 0.0;
            computeTime = 0L;
            numVTrained = 0L;
            waitTime = 0L;
            timerTuning =
                configuration.getBoolean(
                        Constants.ENABLE_TUNING, true);
            totalNumV = 0L;
            totalNumCols = 0L;
            absentTestNum = 0;
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

            //create the algorithm object
            // Distri sgdAlgorithm = new Distri(daal_Context, Double.class, Method.defaultSGD);
            //
            // ArrayList<VPointD> testtask = new ArrayList<>();
            // for(int p = 0;p<1000;p++)
            //  testtask.add(new VPointD(p,p,5.0));
            //
            // //load the model data matrix W
            // PartialResult model_data = new PartialResult(daal_Context);
            //
            // NumericTable matrixW = new HomogenNumericTable(daal_Context, Double.class, 128, 5000, NumericTable.AllocationFlag.DoAllocate, 0.5);
            // NumericTable matrixH = new HomogenNumericTable(daal_Context, Double.class, 128, 3000, NumericTable.AllocationFlag.DoAllocate, 0.5);
            //
            // model_data.set(PartialResultId.presWMat, matrixW);
            // model_data.set(PartialResultId.presHMat, matrixH);
            //
            // sgdAlgorithm.setPartialResult(model_data);
            //
            // AOSNumericTable daal_task_table = new AOSNumericTable(daal_Context, testtask.toArray());
            //
            // sgdAlgorithm.input.set(InputId.dataTrain, daal_task_table);
            //
            // sgdAlgorithm.parameter.set(0.005, 0.003, 128, 5000, 3000, 1, 0, 0, 0);
            //
            // sgdAlgorithm.compute();

            LOG.info("Total execution time: "
                    + (System.currentTimeMillis() - startTime));
                }

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

        private void runSGD(
                final LinkedList<String> vFilePaths,
                final Configuration configuration,
                final Context context) throws Exception {//{{{

            LOG.info("Use Model Parallelism");

            //load the train dataset
            Int2ObjectOpenHashMap<VRowCol> vRowMap =
                SGDUtil.loadVWMap(vFilePaths, numThreads,
                        configuration);

            //load the test dataset
            Int2ObjectOpenHashMap<VRowCol> testVRowMap =
                SGDUtil.loadTestVWMap(testFilePath, numThreads,
                        configuration);

            // int totalNumTestV = 0;
            // for (VRowCol vRowCol : testVRowMap.values()) {
            //     totalNumTestV += vRowCol.numV;
            // }

            //this is the original test vs, not the ones after regroups
            // LOG.info("Total num of test V: " 
            //         + totalNumTestV);
            // ---------------------------------------------
            // Create vHMap and W model
            int numSplits =
                ((int) Math.round(numThreads / 20.0) + 1) * 20;

            final int numRowSplits = numSplits;
            final int numColSplits = numRowSplits;

            LOG.info("numRowSplits: " + numRowSplits
                    + " " + " numColSplits: " + numColSplits);

            final Int2ObjectOpenHashMap<double[]> wMap =
                new Int2ObjectOpenHashMap<>();

            Int2ObjectOpenHashMap<VRowCol>[] vWHMap =
                new Int2ObjectOpenHashMap[numRowSplits];

            //create wMap according to points in training dataset vWHMap
            final long workerNumV =
                createVWHMapAndWModel(vWHMap, wMap,
                        vRowMap, r, oneOverSqrtR, numThreads,
                        random);

            Int2ObjectOpenHashMap<VRowCol>[] testWHMap =
                new Int2ObjectOpenHashMap[numRowSplits];

            //creating testWHMap, also filtering test points whose rows not shown in wMap
            long totalNumTestV = createTestWHMap(testWHMap, wMap,
                    testVRowMap, r, oneOverSqrtR, numThreads,
                    random);

            // ----------------- load W model data from Harp into DAAL's data structure -----------------

            int wMap_size = wMap.size(); //row num of local W model
            //an index hashmap for W model data in DAAL
            final Int2ObjectOpenHashMap<Integer> wMap_index =
                new Int2ObjectOpenHashMap<>();
            //create W Model data in DAAL as a homogenNumericTable
            NumericTable wMap_daal = new HomogenNumericTable(daal_Context, Double.class, r, wMap_size, NumericTable.AllocationFlag.DoAllocate);

            long LoadWMapStart = 0;
            long LoadWMapEnd = 0;
            long LoadWMapTime = 0;

            LoadWMapStart = System.currentTimeMillis();

            HomogenTableHarpMap<double[]> convert_wTable = new HomogenTableHarpMap<double[]>(wMap, wMap_index, wMap_daal, wMap_size, r, numThreads);
            convert_wTable.HarpToDaalDouble();

            LoadWMapEnd = System.currentTimeMillis();
            LoadWMapTime += LoadWMapEnd - LoadWMapStart; 
            LOG.info("Time in loading wMap_daal: "+LoadWMapTime);

            // ----------------- End of load W model data from Harp into DAAL's data structure -----------------

            vRowMap = null;
            testVRowMap = null;

            // Create H model
            Table<DoubleArray>[] hTableMap =
                new Table[numModelSlices];

            createHModel(hTableMap, numModelSlices,
                    vWHMap, oneOverSqrtR, random);

            this.freeMemory();
            this.freeConn();

            System.gc();

            // ----------------------------------------------
            // Create rotator
            final int numWorkers = this.getNumWorkers();

            RotatorDaal<double[], DoubleArray> rotator =
                new RotatorDaal<>(hTableMap, r, 20, this, null, "sgd");
            rotator.start();

            //create DAAL algorithm object, using distributed version of DAAL-MF-SGD  
            Distri sgdAlgorithm = new Distri(daal_Context, Double.class, Method.defaultSGD);
            //first set up W Model, for all the iterations
            PartialResult model_data = new PartialResult(daal_Context);
            sgdAlgorithm.setPartialResult(model_data);
            model_data.set(PartialResultId.presWMat, wMap_daal);

            //collect tasks from all the numRowSplits
            Int2ObjectOpenHashMap<ArrayList<VPointD>>[] taskMap_daal = new Int2ObjectOpenHashMap[numRowSplits];
            for (int p=0; p<numRowSplits; p++) 
            {
                taskMap_daal[p] = new Int2ObjectOpenHashMap<>();
            }

            //preload training data from Harp into DAAL's data structure
            ArrayList<NumericTable> train_data_wPos = new ArrayList<>();
            ArrayList<NumericTable> train_data_hPos = new ArrayList<>();
            ArrayList<NumericTable> train_data_val = new ArrayList<>();

            //preload test data from Harp into DAAL's data structure
            ArrayList<NumericTable> test_data_wPos = new ArrayList<>();
            ArrayList<NumericTable> test_data_hPos = new ArrayList<>();
            ArrayList<NumericTable> test_data_val = new ArrayList<>();

            
            totalNumTestV = loadDataDaal(numWorkers, numRowSplits, rotator, vWHMap, testWHMap, hTableMap, wMap_index, taskMap_daal,train_data_wPos, train_data_hPos, train_data_val, 
                    test_data_wPos, test_data_hPos, test_data_val);

            LOG.info("Effective Total Num of Test Points: " + totalNumTestV);

            vWHMap = null;
            testWHMap = null;

            //computeRMSE before iteration
            printRMSEbyDAAL(sgdAlgorithm, model_data,test_data_wPos,test_data_hPos,test_data_val, rotator, numWorkers, totalNumTestV, wMap_size, 0, configuration);

            LOG.info("Iteration Starts.");

            long iterationAccu = 0;

            long interfaceStart = 0;
            long interfaceEnd = 0;
            long interfaceTime = 0;

            long jniStart = 0;
            long jniEnd = 0;
            long jniTime = 0;

            long ComputeStart = 0;
            long ComputeEnd = 0;
            long ComputeTime = 0;

            int innerItr = 0;


            // -----------------------------------------
            // For iteration
            for (int i = 1; i <= numIterations; i++) {

                long iteStart = System.currentTimeMillis();

                for (int j = 0; j < numWorkers; j++) {

                    for (int k = 0; k < numModelSlices; k++) {

                        long t1 = System.currentTimeMillis();

                        //if printRMSE is not used, start the rotation 
                        // int sliceID = k;
                        // if (i> 1 || j>0)
                        //   sliceID = rotation.take(k);

                        //size numColSplits 
                        // List<Partition<DoubleArray>>[] hMap = rotator.getSplitMap(k);
                        NumericTable hTableMap_daal = rotator.getDaal_Table(k);

                        long t2 = System.currentTimeMillis();
                        waitTime += (t2 - t1);

                        //-----load H data from Harp into DAAL's data structure
                        int hPartitionMapSize = hTableMap[k].getNumPartitions();
                        LOG.info(" hPartition Size: "+ hPartitionMapSize);

                        // NumericTable hTableMap_daal = new HomogenNumericTable(daal_Context, Double.class, r, 
                        //         hPartitionMapSize, NumericTable.AllocationFlag.DoAllocate);
                        //
                        // HomogenTableHarpTable<double[], DoubleArray, Table<DoubleArray> > convert_hTable 
                        //     = new HomogenTableHarpTable<double[], DoubleArray, Table<DoubleArray> >(hTableMap[k], hTableMap_daal,hPartitionMapSize, r, numThreads);

                        interfaceStart = System.currentTimeMillis();
                        //load data from Harp table to Daal table in parallel
                        // convert_hTable.HarpToDaalDouble();
                        interfaceEnd = System.currentTimeMillis();
                        interfaceTime += (interfaceEnd - interfaceStart);

                        model_data.set(PartialResultId.presHMat, hTableMap_daal);

                        //----------- set up the training model data for DAAL -----------
                        int totalSlices = numModelSlices*numWorkers;
                        int slice_index = innerItr%totalSlices; 

                        NumericTable daal_task_wPos = train_data_wPos.get(slice_index); 
                        NumericTable daal_task_hPos = train_data_hPos.get(slice_index); 
                        NumericTable daal_task_val = train_data_val.get(slice_index); 

                        if (daal_task_val.getNumberOfRows() > 0)
                        {
                            //--------------- start of computation by DAAL ---------------

                            ComputeStart = System.currentTimeMillis();
                            sgdAlgorithm.input.set(InputId.dataWPos, daal_task_wPos);
                            sgdAlgorithm.input.set(InputId.dataHPos, daal_task_hPos);
                            sgdAlgorithm.input.set(InputId.dataVal, daal_task_val);

                            sgdAlgorithm.parameter.set(epsilon,lambda, r, wMap_size, hPartitionMapSize, 1, numThreads, 0, 1);
                            // sgdAlgorithm.parameter.set(epsilon,lambda, r, wMap_size, hPartitionMapSize, 1, 0, 0, 0);
                            sgdAlgorithm.parameter.setRatio(1.0);
                            sgdAlgorithm.parameter.setIteration(i-1);
                            sgdAlgorithm.parameter.setTimer(time);
                            sgdAlgorithm.compute();

                            ComputeEnd = System.currentTimeMillis();
                            ComputeTime += (ComputeEnd - ComputeStart);

                        }

                        //---------------------- update the H model Data ----------------------
                        t1 = System.currentTimeMillis();
                        rotator.rotate(k);
                        t2 = System.currentTimeMillis();
                        waitTime += (t2 - t1);
                        innerItr++;

                    }

                }

                long iteEnd = System.currentTimeMillis();
                long iteTime = iteEnd - iteStart;
                iterationAccu += iteTime;

                //update the wMap by DAAL
                convert_wTable.DaalToHarpDouble();

                // Calculate RMSE
                if (i == 1 || i % rmseIteInterval == 0) {
                    // printRMSE(rotatorRMSE, rmseCompute, numWorkers, totalNumV, totalNumTestV, i, configuration);
                    printRMSEbyDAAL(sgdAlgorithm, model_data, test_data_wPos, test_data_hPos,test_data_val, rotator, numWorkers, totalNumTestV, wMap_size, i, configuration);
                }
                if (i % freeInterval == 0) {
                    this.freeMemory();
                    this.freeConn();
                }
                context.progress();
            }

            // Stop sgdCompute and rotation
            trainTimePerIter = ((double)iterationAccu)/numIterations;

            LOG.info("Total Training Time: " + iterationAccu + " average time perinteration: " + trainTimePerIter);
            System.out.println("Training Time per iteration: " + trainTimePerIter 
                    + ", Compute Time: " 
                    + ((double)ComputeTime)/numIterations 
                    + ", Interfacing time per itr: "
                    + ((double)interfaceTime)/numIterations 
                    + ", JNI data transfer time: "
                    + ((double)jniTime)/numIterations
                    + ", wait rotation time per itr: "
                    + ((double)waitTime)/numIterations 
                    + ", misc time: " 
                    + ((double)(iterationAccu - ComputeTime - interfaceTime - jniTime - waitTime))/numIterations);

            // wMap_daal.freeDataMemory();
            wMap_daal = null;
            int daal_table_size = train_data_wPos.size();

            for(int p=0;p< daal_table_size;p++)
            {
                train_data_wPos.get(p).freeDataMemory();
                train_data_hPos.get(p).freeDataMemory();
                train_data_val.get(p).freeDataMemory();

                // train_data_wPos.get(p) = null;
                // train_data_hPos.get(p) = null;
                // train_data_val.get(p) = null;

            }

            daal_Context.dispose();
            rotator.stop();

            //Initialize RMSE compute
            // LinkedList<RMSETask> rmseTasks =
            //     new LinkedList<>();
            // for (int i = 0; i < numThreads; i++) {
            //     rmseTasks.add(new RMSETask(r, vWHMap,
            //                 testVColMap));
            // }
            // DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute =
            //     new DynamicScheduler<>(rmseTasks);
            // rmseCompute.start();
            //
            // Rotator<DoubleArray> rotatorRMSE =
            //     new Rotator<>(hTableMap, numColSplits,
            //             true, this, null, "sgd");
            //
            // rotatorRMSE.start();
            //
            // printRMSE(rotatorRMSE, rmseCompute, numWorkers,
            //         totalNumV, totalNumTestV, 0, configuration);
            //
            // rmseCompute.stop();
            // rotatorRMSE.stop();

        }//}}}

        private long loadDataDaal(int numWorkers, int numRowSplits, 
                RotatorDaal<double[], DoubleArray> rotator,
                Int2ObjectOpenHashMap<VRowCol>[] vWHMap, 
                Int2ObjectOpenHashMap<VRowCol>[] testWHMap, 
                Table<DoubleArray>[] hTableMap,
                Int2ObjectOpenHashMap<Integer> wMap_index, 
                Int2ObjectOpenHashMap<ArrayList<VPointD>>[] taskMap_daal,
                ArrayList<NumericTable> train_data_wPos, 
                ArrayList<NumericTable> train_data_hPos, 
                ArrayList<NumericTable> train_data_val,
                ArrayList<NumericTable> test_data_wPos, 
                ArrayList<NumericTable> test_data_hPos, 
                ArrayList<NumericTable> test_data_val)

        {//{{{

            long effectiveTestPointsNum = 0;

            for (int j = 0; j < numWorkers; j++) {

                for (int k = 0; k < numModelSlices; k++) {

                    // List<Partition<DoubleArray>>[] hMap = rotator.getSplitMap(k);
                    rotator.getDaal_Table(k);
                    int hPartitionMapSize = hTableMap[k].getNumPartitions();

                    //create the hMap_index map
                    final Int2ObjectOpenHashMap<Integer> hMap_index = new Int2ObjectOpenHashMap<>();
                    int hTableMap_pos = 0;
                    for(Partition<DoubleArray> p : hTableMap[k].getPartitions())
                    {
                        hMap_index.put(p.id(), new Integer(hTableMap_pos));
                        hTableMap_pos++;
                    }

                    //loading Training Data from Harp into DAAL 
                    ArrayList<VPointD> DaalTask_List = new ArrayList<>();

                    IntAVLTreeSet ColIDsSet = new IntAVLTreeSet(hTableMap[k].getPartitionIDs());
                    int[] ColIDsSetArray = new int[hPartitionMapSize];
                    ColIDsSetArray = ColIDsSet.toIntArray(ColIDsSetArray);

                    ArrayList<VPointD>[] DiaSelectMap = new ArrayList[numRowSplits];
                    for(int p = 0;p<numRowSplits;p++)
                        DiaSelectMap[p] = new ArrayList<>();

                    Thread[] threads = new Thread[numThreads];
                    int[] absentTrainNum = new int[numThreads];

                    for (int q = 0; q<numThreads; q++) 
                    {
                        threads[q] = new Thread(new TaskIndex(q, numThreads, numRowSplits, vWHMap, taskMap_daal, DiaSelectMap, wMap_index, hMap_index, 
                                    ColIDsSetArray, ColIDsSetArray.length, absentTrainNum));
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

                    for (int p = 0; p<numRowSplits; p++) 
                        DaalTask_List.addAll(DiaSelectMap[p]);

                    int daal_list_size = DaalTask_List.size();

                    NumericTable points_wPos = new HomogenNumericTable(daal_Context, Integer.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);
                    NumericTable points_hPos = new HomogenNumericTable(daal_Context, Integer.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);
                    NumericTable points_val = new HomogenNumericTable(daal_Context, Double.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);

                    int[] wPos_array = new int[daal_list_size];
                    int[] hPos_array = new int[daal_list_size];
                    double[] val_array = new double[daal_list_size];

                    for(int p=0;p<daal_list_size;p++)
                    {
                        VPointD elem = DaalTask_List.get(p);
                        wPos_array[p] = elem._wPos;
                        hPos_array[p] = elem._hPos;
                        val_array[p] = elem._val;
                    }

                    IntBuffer wPos_array_buf = IntBuffer.wrap(wPos_array);
                    points_wPos.releaseBlockOfColumnValues(0, 0, daal_list_size, wPos_array_buf);

                    IntBuffer hPos_array_buf = IntBuffer.wrap(hPos_array);
                    points_hPos.releaseBlockOfColumnValues(0, 0, daal_list_size, hPos_array_buf);

                    DoubleBuffer val_array_buf = DoubleBuffer.wrap(val_array);
                    points_val.releaseBlockOfColumnValues(0, 0, daal_list_size, val_array_buf);

                    LOG.info(" Computed Tasks: "+DaalTask_List.size());
                    train_data_wPos.add(points_wPos);
                    train_data_hPos.add(points_hPos);
                    train_data_val.add(points_val);

                    //loading Test Data from Harp into DAAL 
                    DaalTask_List.clear();
                    for(int p = 0;p<numRowSplits;p++)
                    {
                        taskMap_daal[p].clear();
                        DiaSelectMap[p].clear();
                    }

                    threads = null;
                    threads = new Thread[numThreads];

                    int[] absentTestNumArray = new int[numThreads];
                    
                    for (int q = 0; q<numThreads; q++) 
                    {
                        threads[q] = new Thread(new TaskIndex(q, numThreads, numRowSplits, testWHMap, taskMap_daal, DiaSelectMap, wMap_index, hMap_index, 
                                    ColIDsSetArray, ColIDsSetArray.length, absentTestNumArray));
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

                        absentTestNum += absentTestNumArray[q];

                    }


                    for (int p = 0; p<numRowSplits; p++) 
                        DaalTask_List.addAll(DiaSelectMap[p]);

                    daal_list_size = DaalTask_List.size();
                    effectiveTestPointsNum += daal_list_size;

                    NumericTable testpoints_wPos = new HomogenNumericTable(daal_Context, Integer.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);
                    NumericTable testpoints_hPos = new HomogenNumericTable(daal_Context, Integer.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);
                    NumericTable testpoints_val = new HomogenNumericTable(daal_Context, Double.class, 1, daal_list_size, NumericTable.AllocationFlag.DoAllocate);

                    int[] testwPos_array = new int[daal_list_size];
                    int[] testhPos_array = new int[daal_list_size];
                    double[] testval_array = new double[daal_list_size];

                    for(int p=0;p<daal_list_size;p++)
                    {
                        VPointD elem = DaalTask_List.get(p);
                        testwPos_array[p] = elem._wPos;
                        testhPos_array[p] = elem._hPos;
                        testval_array[p] = elem._val;
                    }

                    IntBuffer testwPos_array_buf = IntBuffer.wrap(testwPos_array);
                    testpoints_wPos.releaseBlockOfColumnValues(0, 0, daal_list_size, testwPos_array_buf);

                    IntBuffer testhPos_array_buf = IntBuffer.wrap(testhPos_array);
                    testpoints_hPos.releaseBlockOfColumnValues(0, 0, daal_list_size, testhPos_array_buf);

                    DoubleBuffer testval_array_buf = DoubleBuffer.wrap(testval_array);
                    testpoints_val.releaseBlockOfColumnValues(0, 0, daal_list_size, testval_array_buf);

                    LOG.info(" Test Tasks: "+DaalTask_List.size());
                    test_data_wPos.add(testpoints_wPos);
                    test_data_hPos.add(testpoints_hPos);
                    test_data_val.add(testpoints_val);

                    rotator.rotate(k);
                }

            }

            return effectiveTestPointsNum;

        }//}}}

        private long createVWHMapAndWModel(
                Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
                Int2ObjectOpenHashMap<double[]> wMap,
                Int2ObjectOpenHashMap<VRowCol> vRowMap,
                int r, double oneOverSqrtR, int numThreads,
                Random random) {

            //vRowMap sort by row ids
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
            long start = System.currentTimeMillis();
            regroup("sgd", "regroup-vw", vSetTable,
                    new Partitioner(this.getNumWorkers()));
            long end = System.currentTimeMillis();
            LOG.info("Regroup data by rows took: "
                    + (end - start)
                    + ", number of rows in local: "
                    + vSetTable.getNumPartitions());
            this.freeMemory();
            // Create a local V Map indexed by H columns
            // Create W model
            VSetSplit[] vSetList =
                new VSetSplit[vWHMap.length];
            for (int i = 0; i < vWHMap.length; i++) {
                vWHMap[i] = new Int2ObjectOpenHashMap<>();
                vSetList[i] = new VSetSplit(i);
            }
            long workerNumV = 0L;
            IntArray idArray =
                IntArray.create(
                        vSetTable.getNumPartitions(), false);
            int[] ids = idArray.get();
            vSetTable.getPartitionIDs().toArray(ids);
            IntArrays.quickSort(ids, 0, idArray.size());
            for (int i = 0; i < idArray.size(); i++) {
                Partition<VSet> partition =
                    vSetTable.getPartition(ids[i]);
                int rowID = partition.id();
                VSet vSet = partition.get();
                double[] rRow = wMap.get(rowID);
                if (rRow == null) {
                    rRow = new double[r];
                    SGDUtil.randomize(random, rRow, r,
                            oneOverSqrtR);
                    wMap.put(rowID, rRow);
                }
                int splitID = i % vWHMap.length;
                vSetList[splitID].list.add(vSet);
                workerNumV += vSet.getNumV();
            }
            idArray.release();
            idArray = null;
            LOG.info("Number of V on this worker "
                    + workerNumV + ". W model is created.");
            // Trim vHMap in vWHMap
            List<DataInitTask> tasks = new LinkedList<>();
            for (int i = 0; i < numThreads; i++) {
                tasks.add(new DataInitTask(vWHMap, wMap));
            }
            DynamicScheduler<VSetSplit, Object, DataInitTask> compute =
                new DynamicScheduler<>(tasks);
            compute.submitAll(vSetList);
            compute.start();
            compute.stop();
            while (compute.hasOutput()) {
                compute.waitForOutput();
            }
            vSetList = null;
            vSetTable.release();
            vSetTable = null;
            this.freeMemory();
            wMap.trim();
            return workerNumV;
                
        }

        private long createTestWHMap(
                Int2ObjectOpenHashMap<VRowCol>[] testWHMap,
                Int2ObjectOpenHashMap<double[]> wMap,
                Int2ObjectOpenHashMap<VRowCol> testVRowMap,
                int r, double oneOverSqrtR, int numThreads,
                Random random) {

            // Organize vWMap
            Table<VSet> vSetTable =
                new Table<>(0, new VSetCombiner());
            ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                testVRowMap.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Int2ObjectMap.Entry<VRowCol> entry =
                    iterator.next();
                int rowID = entry.getIntKey();
                VRowCol vRowCol = entry.getValue();
                vSetTable.addPartition(new Partition<>(
                            rowID, new VSet(vRowCol.id, vRowCol.ids,
                                vRowCol.v, vRowCol.numV)));
            }
            // Clean the data
            testVRowMap.clear();
            long start = System.currentTimeMillis();
            regroup("sgd", "regroup-testvw", vSetTable,
                    new Partitioner(this.getNumWorkers()));
            long end = System.currentTimeMillis();
            LOG.info("Regroup test data by rows took: "
                    + (end - start)
                    + ", number of rows in local: "
                    + vSetTable.getNumPartitions());
            this.freeMemory();
            // Create a local V Map indexed by H columns
            VSetSplit[] vSetList =
                new VSetSplit[testWHMap.length];
            for (int i = 0; i < testWHMap.length; i++) {
                testWHMap[i] = new Int2ObjectOpenHashMap<>();
                vSetList[i] = new VSetSplit(i);
            }
            long workerNumV = 0L;
            IntArray idArray =
                IntArray.create(
                        vSetTable.getNumPartitions(), false);
            int[] ids = idArray.get();
            vSetTable.getPartitionIDs().toArray(ids);
            IntArrays.quickSort(ids, 0, idArray.size());
            for (int i = 0; i < idArray.size(); i++) {
                Partition<VSet> partition =
                    vSetTable.getPartition(ids[i]);
                int rowID = partition.id();
                VSet vSet = partition.get();
                
                int splitID = i % testWHMap.length;
                vSetList[splitID].list.add(vSet);
                workerNumV += vSet.getNumV();
            }
            idArray.release();
            idArray = null;
            LOG.info("Number of testV on this worker "
                    + workerNumV);
            // Trim HMap in testWHMap
            List<DataInitTask> tasks = new LinkedList<>();
            for (int i = 0; i < numThreads; i++) {
                //also to filtering out the test points whose rows not shown in wMap
                tasks.add(new DataInitTask(testWHMap, wMap));
            }
            DynamicScheduler<VSetSplit, Object, DataInitTask> compute =
                new DynamicScheduler<>(tasks);
            compute.submitAll(vSetList);
            compute.start();
            compute.stop();
            while (compute.hasOutput()) {
                compute.waitForOutput();
            }
            vSetList = null;
            vSetTable.release();
            vSetTable = null;
            this.freeMemory();
            return workerNumV;
                
        }

        private long createHModel(
                Table<DoubleArray>[] hTableMap,
                int numModelSlices,
                Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
                double oneOverSqrtR, Random random)
            throws Exception {
            LOG.info("Start creating H model.");
            long t1 = System.currentTimeMillis();
            for (int i = 0; i < numModelSlices; i++) {
                hTableMap[i] =
                    new Table<>(i, new DoubleArrPlus());
            }
            Table<IntArray> vHSumTable =
                new Table<>(0, new IntArrPlus());
            for (int i = 0; i < vWHMap.length; i++) {
                ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
                    vWHMap[i].int2ObjectEntrySet()
                    .fastIterator();
                while (iterator.hasNext()) {
                    Int2ObjectMap.Entry<VRowCol> entry =
                        iterator.next();
                    IntArray array =
                        IntArray.create(1, false);
                    int partitionID = entry.getIntKey();
                    array.get()[0] = entry.getValue().numV;
                    PartitionStatus status =
                        vHSumTable
                        .addPartition(new Partition<>(
                                    partitionID, array));
                    if (status != PartitionStatus.ADDED) {
                        array.release();
                    }
                }
            }
            // Aggregate column index and the element
            // count on each column
            int numWorkers = this.getNumWorkers();
            this.regroup("sgd", "regroup-vhsum",
                    vHSumTable, new Partitioner(numWorkers));
            this.allgather("sgd", "allgather-vhsum",
                    vHSumTable);
            totalNumCols = vHSumTable.getNumPartitions();
            IntArray idArray =
                IntArray.create((int) totalNumCols, false);
            vHSumTable.getPartitionIDs().toArray(
                    idArray.get());
            IntArrays.quickSort(idArray.get(), 0,
                    idArray.size());
            int selfID = this.getSelfID();
            int workerIndex = 0;
            int sliceIndex = 0;
            int[] ids = idArray.get();
            for (int i = 0; i < idArray.size(); i++) {
                Partition<IntArray> partition =
                    vHSumTable.getPartition(ids[i]);
                totalNumV +=
                    (long) partition.get().get()[0];
                if (workerIndex % numWorkers == selfID) {
                    // This h column
                    // will be created by this worker
                    int colID = partition.id();
                    DoubleArray rCol =
                        DoubleArray.create(r, false);
                    SGDUtil.randomize(random, rCol.get(), r,
                            oneOverSqrtR);
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
            this.freeMemory();
            long t2 = System.currentTimeMillis();
            LOG.info("H model is created. "
                    + "Total number of training data "
                    + totalNumV + " " + totalNumCols + " "
                    + sliceIndex + " took " + (t2 - t1));
            return totalNumV;
                }

        private long adjustMiniBatch(int selfID,
                long computeTime, long numVTrained,
                int iteration, long miniBatch,
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
            double ratio =
                Constants.COMM_VS_COMPUTE
                * (double) numWorkers
                * (double) numThreads
                * (double) totalNumCols
                / (double) totalNumV / 2.0;
            if (ratio < Constants.MIN_RATIO) {
                double tmp = ratio;
                double delta = 0.0;
                while (tmp < Constants.MIN_RATIO) {
                    delta += 0.1;
                    tmp = (1.0 + delta) * ratio;
                }
                ratio = tmp;
            }
            if (ratio > Constants.MAX_RATIO) {
                ratio = Constants.MAX_RATIO;
            }
            double percentage =
                (double) totalNumVTrained
                / (double) totalNumV;
            double avgComputeTime =
                (double) totalComputeTime
                / (double) numWorkers
                / (double) numModelSlices
                / (double) numWorkers;
            miniBatch =
                (long) Math.round(ratio
                        / (double) percentage
                        * (double) avgComputeTime / 10.0) * 10L;
            LOG.info("new miniBatch " + miniBatch + " "
                    + ratio + " " + percentage);
            return miniBatch;
        }

        private void printRMSE(Rotator<DoubleArray> rotator,
                               DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute,
                               int numWorkers, long totalNumV,
                               int totalNumTestV, int iteration,
                               Configuration configuration) throws InterruptedException {

            computeRMSE(rotator, rmseCompute, numWorkers);
            DoubleArray array =
                DoubleArray.create(2, false);
            array.get()[0] = rmse;
            array.get()[1] = testRMSE;
            Table<DoubleArray> rmseTable =
                new Table<>(0, new DoubleArrPlus());
            rmseTable
                .addPartition(new Partition<DoubleArray>(0,
                            array));
            this.allreduce("sgd", "allreduce-rmse-"
                    + iteration, rmseTable);
            rmse =
                rmseTable.getPartition(0).get().get()[0];
            testRMSE =
                rmseTable.getPartition(0).get().get()[1];
            rmse = Math.sqrt(rmse / (double) totalNumV);
            testRMSE =
                Math
                .sqrt(testRMSE / (double) totalNumTestV);
            LOG.info("RMSE " + rmse + ", Test RMSE "
                    + testRMSE);
            rmseTable.release();
            rmse = 0.0;
            testRMSE = 0.0;
                    
        }

        private void computeRMSE(Rotator<DoubleArray> rotator,
                                 DynamicScheduler<List<Partition<DoubleArray>>, Object, RMSETask> rmseCompute,
                                 int numWorkers) {

            for (int j = 0; j < numWorkers; j++) {
                for (int k = 0; k < numModelSlices; k++) {
                    List<Partition<DoubleArray>>[] hMap =
                        rotator.getSplitMap(k);
                    rmseCompute.submitAll(hMap);
                    while (rmseCompute.hasOutput()) {
                        rmseCompute.waitForOutput();
                    }
                    rotator.rotate(k);
                }
            }

            for (RMSETask rmseTask : rmseCompute.getTasks()) {
                rmse += rmseTask.getRMSE();
                testRMSE += rmseTask.getTestRMSE();

            }

        }

        private void printRMSEbyDAAL(Distri Algo,
                                     PartialResult model_data,
                                     ArrayList<NumericTable> test_data_wPos,
                                     ArrayList<NumericTable> test_data_hPos,
                                     ArrayList<NumericTable> test_data_val,
                                     RotatorDaal<double[], DoubleArray> rotator,
                                     int numWorkers,
                                     long totalNumTestV, 
                                     int wMap_size,
                                     int iteration,
                                     Configuration configuration) throws InterruptedException {

            DoubleArray array = DoubleArray.create(2, false);

            array.get()[0] = computeRMSEbyDAAL(Algo, model_data, test_data_wPos, test_data_hPos, test_data_val, rotator, numWorkers, wMap_size);
            array.get()[1] = totalNumTestV - absentTestNum;
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



        private double computeRMSEbyDAAL( Distri Algo,
                                          PartialResult model_data,
                                          ArrayList<NumericTable> test_data_wPos,
                                          ArrayList<NumericTable> test_data_hPos,
                                          ArrayList<NumericTable> test_data_val,
                                          RotatorDaal<double[],DoubleArray> rotator,
                                          int numWorkers,
                                          int wMap_size) {

            double prmse = 0;
            int innerItr = 0;

            for (int j = 0; j < numWorkers; j++) {

                for (int k = 0; k < numModelSlices; k++) {

                    NumericTable hTableMap_daal = rotator.getDaal_Table(k);

                    long hPartitionMapSize = hTableMap_daal.getNumberOfRows();
                    LOG.info(" hPartition Size for test data: "+ hPartitionMapSize);
                    model_data.set(PartialResultId.presHMat, hTableMap_daal);
                    NumericTable matrixRMSE = new HomogenNumericTable(daal_Context, Double.class, 1, 1, NumericTable.AllocationFlag.DoAllocate, 0.0);
                    model_data.set(PartialResultId.presRMSE, matrixRMSE);

                    //----------- set up the training model data for DAAL -----------
                    int totalSlices = numModelSlices*numWorkers;
                    int slice_index = innerItr%totalSlices; 

                    NumericTable daal_task_wPos = test_data_wPos.get(slice_index); 
                    NumericTable daal_task_hPos = test_data_hPos.get(slice_index); 
                    NumericTable daal_task_val = test_data_val.get(slice_index); 

                    Algo.input.set(InputId.dataWPos, daal_task_wPos);
                    Algo.input.set(InputId.dataHPos, daal_task_hPos);
                    Algo.input.set(InputId.dataVal, daal_task_val);

                    Algo.parameter.set(epsilon,lambda, r, wMap_size, hPartitionMapSize, 1, numThreads, 0, 1);
                    Algo.parameter.setRatio(1.0);
                    Algo.parameter.setIsTrain(0);
                    Algo.compute();
                    Algo.parameter.setIsTrain(1);

                    //retrieve rmse value
                    double[] prmse_array = new double[1];
                    DoubleBuffer prmse_buf = DoubleBuffer.allocate(1);
                    prmse_buf = matrixRMSE.getBlockOfRows(0,1,prmse_buf);
                    prmse_buf.get(prmse_array, 0, 1);

                    prmse += prmse_array[0];
                    rotator.rotate(k);
                    innerItr++;
                }
            }

            return prmse;
        }

}
