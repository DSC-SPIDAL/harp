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

package edu.iu.daal_pca;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.DynamicScheduler;

import edu.iu.harp.resource.ByteArray;

import edu.iu.daal.*;
import java.nio.DoubleBuffer;

//import PCA from DAAL library
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.algorithms.pca.*;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import edu.iu.harp.resource.ByteArray;

import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
// import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

/**
 * @brief the Harp mapper for running PCA
 */
public class PCADaalCollectiveMapper extends
    CollectiveMapper<String, String, Object, Object>
{
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

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context)
  throws IOException, InterruptedException
  {
    long startTime = System.currentTimeMillis();
    Configuration configuration = context.getConfiguration();
    pointsPerFile = configuration.getInt(Constants.POINTS_PER_FILE, 20);
    vectorSize = configuration.getInt(Constants.VECTOR_SIZE, 20);
    numMappers = configuration.getInt(Constants.NUM_MAPPERS, 10);
    numThreads = configuration.getInt(Constants.NUM_THREADS, 10);

    //always use the maximum hardware threads to load in data and convert data 
    harpThreads = Runtime.getRuntime().availableProcessors();

    LOG.info("Points Per File " + pointsPerFile);
    LOG.info("Vector Size " + vectorSize);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Num harp load data threads " + harpThreads);

    long endTime = System.currentTimeMillis();
    LOG.info("config (ms) :" + (endTime - startTime));
  }

  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException
  {
    long startTime = System.currentTimeMillis();
    List<String> pointFiles = new LinkedList<String>();
    while (reader.nextKeyValue())
    {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info("Key: " + key + ", Value: " + value);
      pointFiles.add(value);
    }
    Configuration conf = context.getConfiguration();
    runPCA(pointFiles, conf, context);
    LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
  }

  /**
   * @brief run PCA by invoking DAAL Java API
   *
   * @param fileNames
   * @param conf
   * @param context
   *
   * @return
   */
  private void runPCA(List<String> fileNames, Configuration conf, Context context) throws IOException
  {
    ts_start = System.currentTimeMillis();
    /*creating an object to store the partial results on each node*/
    PartialCorrelationResult pres = new PartialCorrelationResult(daal_Context);

    //set thread number used in DAAL
    LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
    Environment.setNumberOfThreads(numThreads);
    LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

    //load data
    ts1 = System.currentTimeMillis();
    // use maximum hardware threads to load data
    int loadThreads = Runtime.getRuntime().availableProcessors();
    List<double[]> pointArrays = PCAUtil.loadPoints(fileNames, pointsPerFile, vectorSize, conf, harpThreads);
    ts2 = System.currentTimeMillis();
    load_time += (ts2 - ts1);

    //create the daal table for pointsArrays
    ts1 = System.currentTimeMillis();

    long nFeature = vectorSize;
    long totalLength = 0;

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
    convert_time += (ts2 - ts2);

    /* Create an algorithm to compute PCA decomposition using the correlation method on local nodes */
    DistributedStep1Local pcaLocal = new DistributedStep1Local(daal_Context, Double.class, Method.correlationDense);

    /* Set the input data on local nodes */
    pcaLocal.input.set(InputId.data, pointsArray_daal);

    
    ts1 = System.currentTimeMillis();
    /*Compute the partial results on the local data nodes*/
    pres = (PartialCorrelationResult)pcaLocal.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    /*Do an reduce to send all the data to the master node*/
    Table<ByteArray> step1LocalResult_table = communicate(pres);
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    /*Start the Step 2 on the master node*/
    if(this.isMaster())
    {
        /*create a new algorithm for the master node computations*/
        DistributedStep2Master pcaMaster = new DistributedStep2Master(daal_Context, Double.class, Method.correlationDense);

        try
        {
            ts1 = System.currentTimeMillis();
            for (int i = 0; i < this.getNumWorkers(); i++)
            {
                /*get the partial results from the local nodes and deserialize*/
                PartialCorrelationResult step1LocalResultNew = deserializeStep1Result(step1LocalResult_table.getPartition(i).get().get());

                /*add the partial results from the loacl nodes to the master node input*/
                pcaMaster.input.add(MasterInputId.partialResults, step1LocalResultNew);
            }

            ts2 = System.currentTimeMillis();
            comm_time += (ts2 - ts1);
            /*compute the results on the master node*/
            ts1 = System.currentTimeMillis();
            pcaMaster.compute();
            ts2 = System.currentTimeMillis();
            compute_time += (ts2 - ts1);
        }
        catch(Exception e)
        {
            System.out.println("Exception: + " + e);
        }

        /*get the results from master node*/
        ts1 = System.currentTimeMillis();
        Result res = pcaMaster.finalizeCompute();
        ts2 = System.currentTimeMillis();
        compute_time += (ts2 - ts1);

        NumericTable eigenValues = res.get(ResultId.eigenValues);
        NumericTable eigenVectors = res.get(ResultId.eigenVectors);

        /*printing the results*/
        Service.printNumericTable("Eigenvalues:", eigenValues);
        Service.printNumericTable("Eigenvectors:", eigenVectors);

        /*free the memory*/
        daal_Context.dispose();
    }

    ts_end = System.currentTimeMillis();
    total_time = (ts_end - ts_start);
    LOG.info("Total Execution Time of PCA: "+ total_time);
    LOG.info("Loading Data Time of PCA: "+ load_time);
    LOG.info("Computation Time of PCA: "+ compute_time);
    LOG.info("Comm Time of PCA: "+ comm_time);
    LOG.info("DataType Convert Time of PCA: "+ convert_time);
    LOG.info("Misc Time of PCA: "+ (total_time - load_time - compute_time - comm_time - convert_time));

  }

  /**
   * @brief communicate via reduce by invoking Harp Java API
   *
   * @param res
   *
   * @return step1LocalResult_table
   */
  public Table<ByteArray> communicate(PartialCorrelationResult res) throws IOException
  {
    try
    {
      byte[] serialStep1LocalResult = serializeStep1Result(res);
      ByteArray step1LocalResult_harp = new ByteArray(serialStep1LocalResult, 0, serialStep1LocalResult.length);
      Table<ByteArray> step1LocalResult_table = new Table<>(0, new ByteArrPlus());
      step1LocalResult_table.addPartition(new Partition<>(this.getSelfID(), step1LocalResult_harp));
      this.reduce("pca", "sync-partial-res", step1LocalResult_table, this.getMasterID());
      return step1LocalResult_table;
    }
    catch (Exception e)
    {
      LOG.error("Fail to serilization.", e);
      return null;
    }
  }

  /**
   * @brief Serialize the PartialCorrelationResult by invoking Harp Java API
   *
   * @param res
   *
   * @return buffer
   */
  private byte[] serializeStep1Result(PartialCorrelationResult res) throws IOException
  {
    ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);
    res.pack();
    outputStream.writeObject(res);
    byte[] buffer = outputByteStream.toByteArray();
    return buffer;
  }


  /**
   * @brief deSerialize the dataStructure invoking DAAL Java API
   *
   * @param buffer
   *
   * @return restoredRes
   */
  private PartialCorrelationResult deserializeStep1Result(byte[] buffer) throws IOException, ClassNotFoundException
  {
    ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
    ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);
    PartialCorrelationResult restoredRes = (PartialCorrelationResult)inputStream.readObject();
    restoredRes.unpack(daal_Context);
    return restoredRes;
  }
}
