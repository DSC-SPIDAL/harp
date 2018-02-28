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

package edu.iu.daal_mom;

import com.intel.daal.algorithms.low_order_moments.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.ByteArray;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.*;
import java.nio.DoubleBuffer;
import java.util.LinkedList;
import java.util.List;

//import daal.jar API


/**
 * @brief the Harp mapper for running Neural Network
 */


public class MOMDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{



  private PartialResult partialResult;
  private Result result;
  private int pointsPerFile = 50;                             //change
  private int vectorSize = 10;
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

      runMOM(trainingDataFiles, conf, context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }



    private void runMOM(List<String> trainingDataFiles, Configuration conf, Context context) throws IOException {

        //set thread number used in DAAL
        LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
        Environment.setNumberOfThreads(numThreads);
        LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

        ts_start = System.currentTimeMillis();

        ts1 = System.currentTimeMillis();
        // extracting points from csv files
        List<double[]> pointArrays = MOMUtil.loadPoints(trainingDataFiles, pointsPerFile,
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

        Table<ByteArray> partialResultTable = new Table<>(0, new ByteArrPlus());

        computeOnLocalNode(featureArray_daal, partialResultTable);
        if(this.isMaster()){
            computeOnMasterNode(partialResultTable);
            printResults(result);

        }

        daal_Context.dispose();

        ts_end = System.currentTimeMillis();
        total_time = (ts_end - ts_start);

        LOG.info("Total Execution Time of MOM: "+ total_time);
        LOG.info("Loading Data Time of MOM: "+ load_time);
        LOG.info("Computation Time of MOM: "+ compute_time);
        LOG.info("Comm Time of MOM: "+ comm_time);
        LOG.info("DataType Convert Time of MOM: "+ convert_time);
        LOG.info("Misc Time of MOM: "+ (total_time - load_time - compute_time - comm_time - convert_time));
    }

  private void computeOnLocalNode(NumericTable featureArray_daal, Table<ByteArray> partialResultTable) throws IOException {

    ts1 = System.currentTimeMillis();
    /* Create algorithm objects to compute a variance-covariance matrix in the distributed processing mode using the default method */
    DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Float.class, Method.defaultDense);
    /* Set input objects for the algorithm */
    algorithm.input.set(InputId.data, featureArray_daal);

    /* Compute partial estimates on nodes */
    partialResult = algorithm.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    partialResultTable.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(partialResult)));
    boolean reduceStatus = false;
    reduceStatus = this.reduce("mom", "sync-partialresult", partialResultTable, this.getMasterID());
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    if(!reduceStatus){
      System.out.println("reduce not successful");
    }
    else{
      System.out.println("reduce successful");
    }
  }

  private void computeOnMasterNode(Table<ByteArray> partialResultTable){
    int[] pid = partialResultTable.getPartitionIDs().toIntArray();
    DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Float.class, Method.defaultDense);
    ts1 = System.currentTimeMillis();
    for(int j = 0; j< pid.length; j++){
      try {
        algorithm.input.add(DistributedStep2MasterInputId.partialResults,
          deserializePartialResult(partialResultTable.getPartition(pid[j]).get())); 
      } catch (Exception e) 
      {  
        System.out.println("Fail to deserilize partialResultTable" + e.toString());
        e.printStackTrace();
      }
    }
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    algorithm.compute();
    result = algorithm.finalizeCompute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);
  }

  private void printResults(Result result){
    NumericTable minimum = result.get(ResultId.minimum);
    NumericTable maximum = result.get(ResultId.maximum);
    NumericTable sum = result.get(ResultId.sum);
    NumericTable sumSquares = result.get(ResultId.sumSquares);
    NumericTable sumSquaresCentered = result.get(ResultId.sumSquaresCentered);
    NumericTable mean = result.get(ResultId.mean);
    NumericTable secondOrderRawMoment = result.get(ResultId.secondOrderRawMoment);
    NumericTable variance = result.get(ResultId.variance);
    NumericTable standardDeviation = result.get(ResultId.standardDeviation);
    NumericTable variation = result.get(ResultId.variation);

    System.out.println("Low order moments:");
    Service.printNumericTable("Min:", minimum);
    Service.printNumericTable("Max:", maximum);
    Service.printNumericTable("Sum:", sum);
    Service.printNumericTable("SumSquares:", sumSquares);
    Service.printNumericTable("SumSquaredDiffFromMean:", sumSquaresCentered);
    Service.printNumericTable("Mean:", mean);
    Service.printNumericTable("SecondOrderRawMoment:", secondOrderRawMoment);
    Service.printNumericTable("Variance:", variance);
    Service.printNumericTable("StandartDeviation:", standardDeviation);
    Service.printNumericTable("Variation:", variation);

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

}
