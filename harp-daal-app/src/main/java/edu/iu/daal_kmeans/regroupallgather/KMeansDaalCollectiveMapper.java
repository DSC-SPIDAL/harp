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

package edu.iu.daal_kmeans.regroupallgather;

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
import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.services.DaalContext;

/**
 * @brief the Harp mapper for running K-means
 */
public class KMeansDaalCollectiveMapper extends
    CollectiveMapper<String, String, Object, Object>
{
  private int pointsPerFile;
  private int vectorSize;
  private int numMappers;
  private int numThreads;

  //to measure the time
  private long convert_time = 0;
  private long train_time = 0;
  private long compute_time = 0;
  private long comm_time = 0;

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
    LOG.info("Points Per File " + pointsPerFile);
    LOG.info("Vector Size " + vectorSize);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Threads " + numThreads);
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
   * @brief run K-means by invoking DAAL Java API
   *
   * @param fileNames
   * @param conf
   * @param context
   *
   * @return
   */
  private void runPCA(List<String> fileNames, Configuration conf, Context context) throws IOException
  {
    /*creating an object to store the partial results on each node*/
    PartialCorrelationResult pres = new PartialCorrelationResult(daal_Context);

    System.out.println("File taken by this:"+ fileNames.get(0));

    FileDataSource dataSource = new FileDataSource(daal_Context, "/N/u/pgangwar/pca_"+this.getSelfID()+".csv",
                                                   DataSource.DictionaryCreationFlag.DoDictionaryFromContext,
                                                   DataSource.NumericTableAllocationFlag.DoAllocateNumericTable);

    /* Retrieve the data from the input file */
    dataSource.loadDataBlock();

    /* Set the input data on local nodes */
    NumericTable pointsArray_daal = dataSource.getNumericTable();

    /* Create an algorithm to compute PCA decomposition using the correlation method on local nodes */
    DistributedStep1Local pcaLocal = new DistributedStep1Local(daal_Context, Double.class, Method.correlationDense);

    /* Set the input data on local nodes */
    pcaLocal.input.set(InputId.data, pointsArray_daal);

    /*Compute the partial results on the local data nodes*/
    pres = (PartialCorrelationResult)pcaLocal.compute();

    /*Do an reduce to send all the data to the master node*/
    Table<ByteArray> step1LocalResult_table = communicate(pres);

    /*Start the Step 2 on the master node*/
    if(this.isMaster())
    {
      /*create a new algorithm for the master node computations*/
      DistributedStep2Master pcaMaster = new DistributedStep2Master(daal_Context, Double.class, Method.correlationDense);
      try
     {
        for (int i = 0; i < this.getNumWorkers(); i++)
        {
          /*get the partial results from the local nodes and deserialize*/
          PartialCorrelationResult step1LocalResultNew = deserializeStep1Result(step1LocalResult_table.getPartition(i).get().get());

          /*add the partial results from the loacl nodes to the master node input*/
          pcaMaster.input.add(MasterInputId.partialResults, step1LocalResultNew);
        }
        /*compute the results on the master node*/
        pcaMaster.compute();
      }
      catch(Exception e)
      {
        System.out.println("Exception: + " + e);
      }

      /*get the results from master node*/
      Result res = pcaMaster.finalizeCompute();
      NumericTable eigenValues = res.get(ResultId.eigenValues);
      NumericTable eigenVectors = res.get(ResultId.eigenVectors);

      /*printing the results*/
      Service.printNumericTable("Eigenvalues:", eigenValues);
      Service.printNumericTable("Eigenvectors:", eigenVectors);

      /*free the memory*/
      daal_Context.dispose();
    }
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