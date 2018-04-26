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

package edu.iu.daal_qr;

import org.apache.commons.io.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import java.nio.DoubleBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
import com.intel.daal.algorithms.qr.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

public class QRDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object>{



        private DataCollection dataFromStep1ForStep2;
        private DataCollection dataFromStep1ForStep3;
        private DataCollection dataFromStep2ForStep3;
        private static KeyValueDataCollection inputForStep3FromStep2;

        private NumericTable R;
        private NumericTable Qi;

        private DistributedStep1Local qrStep1Local;
        private DistributedStep2Master qrStep2Master;
        private DistributedStep3Local qrStep3Local;
        private int pointsPerFile = 4000;                             //change
        private int vectorSize = 18;
        private static DaalContext daal_Context = new DaalContext();
        private int numMappers;
        private int numThreads;
        private int harpThreads; 

        //to measure the time
        private long total_time = 0;
        private long load_time = 0;
        private long convert_time = 0;
        private long compute_time = 0;
        private long comm_time = 0;
        private long ts_start = 0;
        private long ts_end = 0;
        private long ts1 = 0;
        private long ts2 = 0;

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

        //always use the maximum hardware threads to load in data and convert data 
        harpThreads = Runtime.getRuntime().availableProcessors();

        LOG.info("Num Mappers " + numMappers);
        LOG.info("Num Threads " + numThreads);
        LOG.info("Num harp load data threads " + harpThreads);

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

            runQR(trainingDataFiles, conf, context);
            LOG.info("Total iterations in master view: "
                    + (System.currentTimeMillis() - startTime));
            this.freeMemory();
            this.freeConn();
            System.gc();
                
        }



        private void runQR(List<String> trainingDataFiles, Configuration conf, Context context) throws IOException 
        {

            ts_start = System.currentTimeMillis();

            //set thread number used in DAAL
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());


            ts1 = System.currentTimeMillis();
            // extracting points from csv files
            List<double[]> pointArrays = QRUtil.loadPoints(trainingDataFiles, pointsPerFile,
                    vectorSize, conf, harpThreads);

            ts2 = System.currentTimeMillis();
            load_time += (ts2 - ts1);

            // converting data to Numeric Table
            ts1 = System.currentTimeMillis();

            long nFeature = vectorSize;
            long nLabel = 1;
            long totalLengthFeature = 0;

            long[] array_startP_feature = new long[pointArrays.size()];
            double[][] array_data_feature = new double[pointArrays.size()][];

            for(int k=0;k<pointArrays.size();k++)
            {
                array_data_feature[k] = pointArrays.get(k);
                array_startP_feature[k] = totalLengthFeature;
                totalLengthFeature += pointArrays.get(k).length;
            }

            long featuretableSize = totalLengthFeature/nFeature;

            //initializing Numeric Table

            NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);

            int row_idx_feature = 0;
            int row_len_feature = 0;

            for (int k=0; k<pointArrays.size(); k++) 
            {
                row_len_feature = (array_data_feature[k].length)/(int)nFeature;
                //release data from Java side to native side
                ((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, DoubleBuffer.wrap(array_data_feature[k]));
                row_idx_feature += row_len_feature;
            }

            ts2 = System.currentTimeMillis();
            convert_time += (ts2 - ts1);

            qrStep1Local = new DistributedStep1Local(daal_Context, Float.class, Method.defaultDense);
            qrStep1Local.input.set(InputId.data, featureArray_daal);
            DistributedStep1LocalPartialResult pres = qrStep1Local.compute();
            dataFromStep1ForStep2 = pres.get(PartialResultId.outputOfStep1ForStep2);
            dataFromStep1ForStep3 = pres.get(PartialResultId.outputOfStep1ForStep3);

            ts1 = System.currentTimeMillis();

            Table<ByteArray> partialStep12 = new Table<>(0, new ByteArrPlus());
            partialStep12.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(dataFromStep1ForStep2)));
            System.out.println("number of partition in partialresult before reduce :" + partialStep12.getNumPartitions());
            boolean reduceStatus = false;
            reduceStatus = this.reduce("nn", "sync-partialresult", partialStep12, this.getMasterID()); 

            if(!reduceStatus){
                System.out.println("reduce not successful");
            }
            else{
                System.out.println("reduce successful");
            }

            System.out.println("number of partition in partialresult after reduce :" + partialStep12.getNumPartitions());

            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);

            Table<ByteArray> partialStep32 = new Table<>(0, new ByteArrPlus());
            System.out.println("number of partition in partialstep32 before broadcast :" + partialStep32.getNumPartitions());

            System.out.println("self id : " + this.getSelfID());

            if(this.isMaster())
            {

                qrStep2Master = new DistributedStep2Master(daal_Context, Float.class, Method.defaultDense);

                System.out.println("this is a master node");
                int[] pid = partialStep12.getPartitionIDs().toIntArray();

                ts1 = System.currentTimeMillis();
                for(int j = 0; j< pid.length; j++){
                    try {
                        System.out.println("pid : "+pid[j]);
                        qrStep2Master.input.add(DistributedStep2MasterInputId.inputOfStep2FromStep1, pid[j],
                                deserializePartialResult(partialStep12.getPartition(pid[j]).get())); 
                    } catch (Exception e) 
                    {  
                        System.out.println("Fail to deserilize partialResultTable" + e.toString());
                        e.printStackTrace();
                    }
                }

                ts2 = System.currentTimeMillis();
                comm_time += (ts2 - ts1);
                
                ts1 = System.currentTimeMillis();
                DistributedStep2MasterPartialResult presStep2 = qrStep2Master.compute();
                

                inputForStep3FromStep2 = presStep2.get(DistributedPartialResultCollectionId.outputOfStep2ForStep3);

                for(int j = 0; j< pid.length; j++){
                    partialStep32.addPartition(new Partition<>(j,serializePartialResult
                                ((DataCollection)inputForStep3FromStep2.get(j))));
                }

                Result result = qrStep2Master.finalizeCompute();
                R = result.get(ResultId.matrixR);

                ts2 = System.currentTimeMillis();
                compute_time += (ts2 - ts1);
            }

            boolean isSuccess = broadcast("main","broadcast-partialStep32", partialStep32, 0,false);
            if(isSuccess){
                System.out.println("broadcast successful");
            }
            else{
                System.out.println("broadcast not successful");
            }


            System.out.println("number of partition in partialstep32 after broadcast :" + partialStep32.getNumPartitions());


            qrStep3Local = new DistributedStep3Local(daal_Context, Float.class, Method.defaultDense);
            qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep1,dataFromStep1ForStep3); 

            ts1 = System.currentTimeMillis();

            try{
                qrStep3Local.input.set(DistributedStep3LocalInputId.inputOfStep3FromStep2, 
                        deserializePartialResult(partialStep32.getPartition(this.getSelfID()).get()));
            } catch (Exception e) 
            {  
                System.out.println("Fail to deserilize partialResultTable" + e.toString());
                e.printStackTrace();
            }

            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);

            ts1 = System.currentTimeMillis();
            qrStep3Local.compute();
            Result result = qrStep3Local.finalizeCompute();

            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);

            Qi = result.get(ResultId.matrixQ);
            System.out.println("number of rows" + Qi.getNumberOfRows());
            System.out.println("number of columns" + Qi.getNumberOfColumns());

            Table<ByteArray> resultNT = new Table<>(0, new ByteArrPlus());

            Service.printNumericTable("Orthogonal matrix Q (10 first vectors):", Qi, 10);
            if(this.isMaster()){
                Service.printNumericTable("Triangular matrix R:", R);
            }

            ts_end = System.currentTimeMillis();
            total_time = (ts_end - ts_start);

            LOG.info("Total Execution Time of QR: "+ total_time);
            LOG.info("Loading Data Time of QR: "+ load_time);
            LOG.info("Computation Time of QR: "+ compute_time);
            LOG.info("Comm Time of QR: "+ comm_time);
            LOG.info("DataType Convert Time of QR: "+ convert_time);
            LOG.info("Misc Time of QR: "+ (total_time - load_time - compute_time - comm_time - convert_time));

        }


        private static ByteArray serializePartialResult(DataCollection partialResult) throws IOException {
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

        private static DataCollection deserializePartialResult(ByteArray byteArray) throws IOException, ClassNotFoundException {
            /* Create an input stream to deserialize the numeric table from the array */
            byte[] buffer = byteArray.get();
            ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
            ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);

            /* Create a numeric table object */
            DataCollection restoredDataTable = (DataCollection) inputStream.readObject();
            restoredDataTable.unpack(daal_Context);

            return restoredDataTable;
        } 

    }
