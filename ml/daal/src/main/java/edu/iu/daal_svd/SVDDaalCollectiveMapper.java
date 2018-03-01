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

package edu.iu.daal_svd;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import edu.iu.data_transfer.*;
import java.nio.DoubleBuffer;

//import daa.jar API
import com.intel.daal.algorithms.svd.*;
// import com.intel.daal.algorithms.kmeans.init.*;

import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
// import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.DataCollection;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.services.Environment;


/**
 * @brief the Harp mapper for running K-means
 */
public class SVDDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

        private int pointsPerFile;
        private int vectorSize;
        private int numMappers;
        private int numThreads;
        private int harpThreads; 

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

        private static NumericTable   S;
        private static NumericTable   V;
        private static NumericTable   U;      

        // SVD declarations
        private static DistributedStep1Local  svdStep1Local;
        private static DistributedStep2Master svdStep2Master;
        private static DistributedStep3Local  svdStep3Local;

        
        // In Psuedo Distributed (Intel -- daal) below three are arrays of
        // Datacollection object. But here we want only one object on each
        // node. We will gather the values on this using harp's collective 
        // communication method.


        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {


        long startTime = System.currentTimeMillis();
        Configuration configuration =
            context.getConfiguration();
        pointsPerFile =
            configuration.getInt(
                    Constants.POINTS_PER_FILE, 20);
        vectorSize =
            configuration.getInt(Constants.VECTOR_SIZE,
                    20);
        numMappers =
            configuration.getInt(Constants.NUM_MAPPERS,
                    10);
        numThreads =
            configuration.getInt(Constants.NUM_THREADS,
                    10);

        //always use the maximum hardware threads to load in data and convert data 
        harpThreads = Runtime.getRuntime().availableProcessors();

        LOG.info("Points Per File " + pointsPerFile);
        // LOG.info("Num Centroids " + numCentroids);
        LOG.info("Vector Size " + vectorSize);
        LOG.info("Num Mappers " + numMappers);
        LOG.info("Num Threads " + numThreads);
        LOG.info("Num harp load data threads " + harpThreads);

        long endTime = System.currentTimeMillis();
        LOG.info("config (ms) :"
                + (endTime - startTime));

        }

        // Assigns the reader to different nodes
        protected void mapCollective(
                KeyValReader reader, Context context)
            throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            List<String> pointFiles =
                new LinkedList<String>();
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                        + value);
                // System.out.println("file name: " + value);
                LOG.info("file name: " + value);
                pointFiles.add(value);
            }
            
            Configuration conf = context.getConfiguration();
            
            runSVD(pointFiles, conf, context);
            this.freeMemory();
            this.freeConn();
            System.gc();
        }

        /**
         * @brief run SVD by invoking DAAL Java API
         *
         * @param fileNames
         * @param conf
         * @param context
         *
         * @return 
         */
        private void runSVD(List<String> fileNames,
                Configuration conf, Context context)
            throws IOException {

            ts_start = System.currentTimeMillis();

            //set thread number used in DAAL
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

            ts1 = System.currentTimeMillis();
            // Collect data on slave nodes
            List<double[]> pointArrays =
                SVDUtil.loadPoints(fileNames, pointsPerFile,
                        vectorSize, conf, harpThreads);

            ts2 = System.currentTimeMillis();
            load_time += (ts2 - ts1);

            //---------------- convert pointArrays to Daal table ------------------
            ts1 = System.currentTimeMillis();

            //create the daal table for pointsArrays
            long nFeature = vectorSize;
            long totalLength = 0;

            // System.out.println("vectorSize is  " + vectorSize);
            LOG.info("vectorSize is  " + vectorSize);

            long[] array_startP = new long[pointArrays.size()];
            double[][] array_data = new double[pointArrays.size()][];

            for(int k=0;k<pointArrays.size();k++)
            {
                array_data[k] = pointArrays.get(k);

                array_startP[k] = totalLength;
                totalLength += pointArrays.get(k).length;
            }


            long tableSize = totalLength/nFeature;
            // NumericTable pointsArray_daal = new HomogenBMNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);
            NumericTable pointsArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);

            int row_idx = 0;
            int row_len = 0;
            for (int k=0; k<pointArrays.size(); k++) 
            {
                row_len = (array_data[k].length)/(int)nFeature;

                //release data from Java side to native side
                // ((HomogenBMNumericTable)pointsArray_daal).releaseBlockOfRowsByte(row_idx, row_len, array_data[k]);
                DoubleBuffer array_data_buf = DoubleBuffer.wrap(array_data[k]);
                pointsArray_daal.releaseBlockOfRows(row_idx, row_len, array_data_buf);
                row_idx += row_len;
            }

            ts2 = System.currentTimeMillis();
            convert_time += (ts2 - ts1);

            // Compute the results
            printNumericTable("Model pointsArray_daal: ", pointsArray_daal, 10,10);

            svdStep1Local = new DistributedStep1Local(daal_Context, Double.class, Method.defaultDense);
            /* Set the input data on local nodes */

            NumericTable input = pointsArray_daal;
            svdStep1Local.input.set(InputId.data, input);

            /* Compute SVD */
            ts1 = System.currentTimeMillis();
            DistributedStep1LocalPartialResult pres = svdStep1Local.compute();
            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);

            /* Get the results for next steps */
            DataCollection dataFromStep1ForStep2 = pres.get(PartialResultId.outputOfStep1ForStep2);
            DataCollection dataFromStep1ForStep3 = pres.get(PartialResultId.outputOfStep1ForStep3);

            // Communicate the results

            ts1 = System.currentTimeMillis();
            // Serializing the results to communicate.
            byte[] serialStep1LocalResultForStep2 = serializeStep1LResult(dataFromStep1ForStep2);
            // byte[] serialStep1LocalResultForStep3 = serializeStep1LResult(dataFromStep1ForStep3);

            // Convert to harp table and communicate for step 2
            ByteArray step1LocalResultForStep2_harp = new ByteArray(serialStep1LocalResultForStep2, 0, serialStep1LocalResultForStep2.length);
            Table<ByteArray> step1LocalResultForStep2_table = new Table<>(0, new ByteArrPlus());

            step1LocalResultForStep2_table.addPartition(new Partition<>(this.getSelfID(), step1LocalResultForStep2_harp));

            //reduce to master node with id 0
            this.allgather("svd", "sync-partial-res", step1LocalResultForStep2_table);

            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);

            Table<ByteArray> step2Result_table = new Table<>(0, new ByteArrPlus());
            KeyValueDataCollection inputForStep3FromStep2;
            DataCollection dataFromStep2ForStep3;


            if (this.isMaster()) {
                // This is for SVD
                svdStep2Master = new DistributedStep2Master(daal_Context, Double.class, Method.defaultDense);
                try{

                    ts1 = System.currentTimeMillis();
                    for (int j = 0; j < this.getNumWorkers(); j++)
                    {
                        DataCollection step1LocalResultNew = deserializeStep1Result(step1LocalResultForStep2_table.getPartition(j).get().get());
                        svdStep2Master.input.add(DistributedStep2MasterInputId.inputOfStep2FromStep1, j , step1LocalResultNew);
                    }

                    ts2 = System.currentTimeMillis();
                    comm_time += (ts2 - ts1);

                }catch (Exception e) 
                {
                    LOG.error("Fail to data collect from local nodes in step 1 and deserialize", e);
                }

                ts1 = System.currentTimeMillis();
                DistributedStep2MasterPartialResult pres2master = svdStep2Master.compute();
                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);

                /* Get the result for step 3 */
                inputForStep3FromStep2 = pres2master.get(DistributedPartialResultCollectionId.outputOfStep2ForStep3);

                //dataFromStep2ForStep3 = (DataCollection)inputForStep3FromStep2.get(iNode);

                // byte[] serialStep2MasterResult = serializeStep2MResult((KeyValueDataCollection)inputForStep3FromStep2);
                ts1 = System.currentTimeMillis();
                byte[] serialStep2MasterResult = serializeStep2MResult(inputForStep3FromStep2);
                ByteArray step2MasterResult_harp = new ByteArray(serialStep2MasterResult, 0, serialStep2MasterResult.length);
                step2Result_table.addPartition(new Partition<>(this.getSelfID(), step2MasterResult_harp));

                ts2 = System.currentTimeMillis();
                comm_time += (ts2 - ts1);

                ts1 = System.currentTimeMillis();
                Result result = svdStep2Master.finalizeCompute();
                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);

                /* Get final singular values and a matrix of right singular vectors */
                S = result.get(ResultId.singularValues);
                V = result.get(ResultId.rightSingularMatrix);

                printNumericTable("Model S:", S, 10,10);

                printNumericTable("Model V:", V, 10,10);
            }
            else
            {
                byte[] dummyRes = new byte[1];
                ByteArray step2MasterResult_harp = new ByteArray(dummyRes, 0, 1);
                step2Result_table.addPartition(new Partition<>(this.getSelfID(), step2MasterResult_harp));
            }   

            ts1 = System.currentTimeMillis();
            this.allgather("svd2", "sync-step2-master", step2Result_table);
            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);

            ts1 = System.currentTimeMillis();
            // System.out.println("Communicated data from step 2 at master to all slaves");
            LOG.info("Communicated data from step 2 at master to all slaves");
            /* Create an algorithm to compute SVD on local nodes */

            svdStep3Local = new DistributedStep3Local(daal_Context, Double.class, Method.defaultDense);
            svdStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep1, dataFromStep1ForStep3);

            KeyValueDataCollection dataFromStep2ForStep3_keyValue = new KeyValueDataCollection(daal_Context);
            try {
                dataFromStep2ForStep3_keyValue =  deserializeStep2MResult(step2Result_table.getPartition(0).get().get());
            }catch (Exception e) 
            {
                LOG.error("Fail to serilization.", e);
            }

            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);

            dataFromStep2ForStep3 = (DataCollection)dataFromStep2ForStep3_keyValue.get(this.getSelfID());
            svdStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep2, dataFromStep2ForStep3);

            /* Compute SVD */
            ts1 = System.currentTimeMillis();
            svdStep3Local.compute();
            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);

            Result result = svdStep3Local.finalizeCompute();

            /* Get final matrices of left singular vectors */
            U = result.get(ResultId.leftSingularMatrix);


            pointsArray_daal.freeDataMemory();
            printNumericTable("Model U:", U, 10,10);
            // System.out.println("Final factorization ");
            LOG.info("Final factorization ");
            printNumericTable("Left orthogonal matrix U (10 first vectors):", U, 10,10);

            ts_end = System.currentTimeMillis();
            total_time = (ts_end - ts_start);
            LOG.info("Total Execution Time of SVD: "+ total_time);
            LOG.info("Loading Data Time of SVD: "+ load_time);
            LOG.info("Computation Time of SVD: "+ compute_time);
            LOG.info("Comm Time of SVD: "+ comm_time);
            LOG.info("DataType Convert Time of SVD: "+ convert_time);
            LOG.info("Misc Time of SVD: "+ (total_time - load_time - compute_time - comm_time - convert_time));

        }


        private DataCollection deserializeStep1Result(byte[] buffer) throws IOException, ClassNotFoundException 
        {
            // Create an input stream to deserialize the numeric table from the array 
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

            // Create a numeric table object 
            DataCollection restoredRes = (DataCollection)inputStream.readObject();
            restoredRes.unpack(daal_Context);

            return restoredRes;
        }

        private byte[] serializeStep1LResult(DataCollection res) throws IOException
        {
            // Create an output stream to serialize the numeric table 
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

            // Serialize the partialResult table into the output stream 
            res.pack();
            outputStream.writeObject(res);

            // Store the serialized data in an array 
            byte[] buffer = outputByteStream.toByteArray();
            return buffer;
        }

        private byte[] serializeStep2MResult(KeyValueDataCollection res) throws IOException
        {
            // Create an output stream to serialize the numeric table 
            ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

            // Serialize the partialResult table into the output stream 
            res.pack();
            outputStream.writeObject(res);

            // Store the serialized data in an array 
            byte[] buffer = outputByteStream.toByteArray();
            return buffer;
        }

        private KeyValueDataCollection deserializeStep2MResult(byte[] buffer) throws IOException, ClassNotFoundException 
        {
        // Create an input stream to deserialize the numeric table from the array 
        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        // Create a numeric table object 
        KeyValueDataCollection restoredRes = (KeyValueDataCollection)inputStream.readObject();
        restoredRes.unpack(daal_Context);

        return restoredRes;
        }

        private DataCollection deserializeStep1LResult(byte[] buffer) throws IOException, ClassNotFoundException 
        {
        // Create an input stream to deserialize the numeric table from the array 
        ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

        // Create a numeric table object 
        DataCollection restoredRes = (DataCollection)inputStream.readObject();
        restoredRes.unpack(daal_Context);

        return restoredRes;
        }
    

    private void printNumericTable(String header, NumericTable nt, long nPrintedRows, long nPrintedCols) {
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
                String tmp = String.format("%-6.3f ", result.get((int) (i * nNtCols + j)));
                builder.append(tmp);
            }
            builder.append("\n");
        }
        System.out.println(builder.toString());
        // LOG.info(builder.toString());
    }


}
