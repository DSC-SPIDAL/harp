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

//import daa.jar API
//import com.intel.daal.algorithms.kmeans.*;
//import com.intel.daal.algorithms.kmeans.init.*;

//import PCA from DAAL library
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.algorithms.pca.*;
// import com.intel.daal.algorithms.implicit_als..*;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
//import com.intel.daal.examples.utils.Service;
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
  private int numCentroids;
  private int vectorSize;
  private int numCenPars;
  private int cenVecSize;
  private int numMappers;
  private int numThreads;
  private int numIterations;
  private String cenDir;

  //to measure the time
  private long convert_time = 0;
  private long train_time = 0;
  private long compute_time = 0;
  private long comm_time = 0;

  private static DaalContext daal_Context = new DaalContext();
//  private static DaalContext master_context = new DaalContext();

  /**
   * Mapper configuration.
   */
  @Override
  protected void setup(Context context)
  throws IOException, InterruptedException
  {
    System.out.println("setup function");
    long startTime = System.currentTimeMillis();
    Configuration configuration = context.getConfiguration();
    pointsPerFile = configuration.getInt(Constants.POINTS_PER_FILE, 20);
    numCentroids = configuration.getInt(Constants.NUM_CENTROIDS, 20);
    vectorSize = configuration.getInt(Constants.VECTOR_SIZE, 20);
    numMappers = configuration.getInt(Constants.NUM_MAPPERS, 10);
    // numCenPars = numMappers;
    cenVecSize = vectorSize + 1;
    numThreads = configuration.getInt(Constants.NUM_THREADS, 10);
    numCenPars = numThreads;
    numIterations = configuration.getInt(Constants.NUM_ITERATIONS, 10);
    cenDir = configuration.get(Constants.CEN_DIR);
    LOG.info("Points Per File " + pointsPerFile);
    LOG.info("Num Centroids " + numCentroids);
    LOG.info("Vector Size " + vectorSize);
    LOG.info("Num Mappers " + numMappers);
    LOG.info("Num Threads " + numThreads);
    LOG.info("Num Iterations " + numIterations);
    LOG.info("Cen Dir " + cenDir);
    long endTime = System.currentTimeMillis();
    LOG.info("config (ms) :" + (endTime - startTime));
  }

  protected void mapCollective(KeyValReader reader, Context context)
      throws IOException, InterruptedException
  {
    System.out.println("mapCollective function");
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
    runKmeans(pointFiles, conf, context);
    LOG.info("Total iterations in master view: "
            + (System.currentTimeMillis() - startTime));
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
  private void runKmeans(List<String> fileNames, Configuration conf, Context context)
      throws IOException
  {
    // Table<DoubleArray> points_harp = new Table<>(0, new DoubleArrPlus());
    PartialCorrelationResult pres = new PartialCorrelationResult(daal_Context);
    System.out.println("This is some node. runKmeans");

    //pointArrays are used in daal table with feature dimension to be vectorSize instead of cenVecSize
    List<double[]> pointArrays = KMUtil.loadPoints(fileNames, pointsPerFile,
                vectorSize, conf, numThreads);

    //---------------- convert cenTable and pointArrays to Daal table ------------------
    //create the daal table for pointsArrays
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
    System.out.println("tableSize="+tableSize);
    NumericTable pointsArray_daal = new HomogenBMNumericTable(daal_Context, Double.class, nFeature, tableSize, NumericTable.AllocationFlag.DoAllocate);

    int row_idx = 0;
    int row_len = 0;
    for (int k=0; k<pointArrays.size(); k++)
    {
      row_len = (array_data[k].length)/(int)nFeature;
      //release data from Java side to native side
      ((HomogenBMNumericTable)pointsArray_daal).releaseBlockOfRowsByte(row_idx, row_len, array_data[k]);
      row_idx += row_len;
    }

    //Service.printNumericTable("pointsArray_daal:", pointsArray_daal);
    //create the algorithm DistributedStep1Local
    DistributedStep1Local pcaLocal = new DistributedStep1Local(daal_Context, Double.class, Method.correlationDense);
    pcaLocal.input.set(InputId.data, pointsArray_daal);
    pres = (PartialCorrelationResult)pcaLocal.compute();
    // pres1 = (PartialResult)pcaLocal.compute();
    //Service.printNumericTable("step1LocalResultNew:", pres.get(PartialCorrelationResultID.crossProductCorrelation));
    System.out.println("checking on the original");
    pointsArray_daal.pack();
    pointsArray_daal.unpack(daal_Context);
    System.out.println("checking on the original done");

    // System.out.println("checking on the original pres");
    // pres.pack();
    // pres.unpack(daal_Context);
    // System.out.println("checking on the original pres done");
    Table<ByteArray> step1LocalResult_table_NoObservation = communicate(pres.get(PartialCorrelationResultID.crossProductCorrelation), "corr");
    Table<ByteArray> step1LocalResult_table_crossPro = communicate(pres.get(PartialCorrelationResultID.nObservations), "obser");
    Table<ByteArray> step1LocalResult_table_sumCorr = communicate(pres.get(PartialCorrelationResultID.sumCorrelation), "sum");


    if(this.isMaster())
    {
      System.out.println("This is the master node and yolo");
      DistributedStep2Master pcaMaster = new DistributedStep2Master(daal_Context, Double.class, Method.correlationDense);

     try
     {
        for (int i = 0; i < this.getNumWorkers(); i++)
        {
          // System.out.println("This is the"+i+"th iteration"+"in"+this.getNumWorkers());
          // for (int j = 0; j < step1LocalResult_table.getPartition(0).get().get().length; j++)
          // {
          //   if (j > 0)
          //   {
          //     System.out.print(", ");
          //   }
          //   System.out.print(step1LocalResult_table.getPartition(0).get().get()[j]);
          // }
          // System.out.println("THe sesond part");
          // for (int j = 0; j < step1LocalResult_table.getPartition(1).get().get().length; j++)
          // {
          //   if (j > 0)
          //   {
          //       System.out.print(", ");
          //   }
          //   System.out.print(step1LocalResult_table.getPartition(1).get().get()[j]);
          // }
          NumericTable step1LocalResultNew_NoObservation = deserializeStep1Result(step1LocalResult_table_NoObservation.getPartition(i).get().get());
          NumericTable step1LocalResultNew_crossPro      = deserializeStep1Result(step1LocalResult_table_crossPro.getPartition(i).get().get());
          NumericTable step1LocalResultNew_sumCorr       = deserializeStep1Result(step1LocalResult_table_sumCorr.getPartition(i).get().get());

          PartialCorrelationResult step1LocalResultNew = new PartialCorrelationResult(daal_Context);
          step1LocalResultNew.set(PartialCorrelationResultID.crossProductCorrelation, step1LocalResultNew_crossPro);
          step1LocalResultNew.set(PartialCorrelationResultID.nObservations, step1LocalResultNew_NoObservation);
          step1LocalResultNew.set(PartialCorrelationResultID.sumCorrelation, step1LocalResultNew_sumCorr);

          System.out.println("This is the"+i+this.getNumWorkers());
          // Service.printNumericTable("step1LocalResultNew:", pres.get(PartialCorrelationResultID.crossProductCorrelation));
          pcaMaster.input.add(MasterInputId.partialResults, step1LocalResultNew);
        }
        System.out.println("Going to compute now");
        pcaMaster.compute();
        System.out.println("Computation done");
      }
      catch (Exception e)
      {
        System.out.println("Throws Exception: "+e);
        LOG.error("Failed to serialize.", e);
      }
      /* Finalize computations and retrieve the results */
      // Result res = pcaMaster.finalizeCompute();

      // NumericTable eigenValues = res.get(ResultId.eigenValues);
      // NumericTable eigenVectors = res.get(ResultId.eigenVectors);
      // Service.printNumericTable("Eigenvalues:", eigenValues);
      // Service.printNumericTable("Eigenvectors:", eigenVectors);
      daal_Context.dispose();
    }
  }

  public Table<ByteArray> communicate(NumericTable res, String stri) throws IOException
  {
    try
    {
      byte[] serialStep1LocalResult = serializeStep1Result(res);
      System.out.println("Size of step1out: "+serialStep1LocalResult.length);
      // for (int i = 0; i < serialStep1LocalResult.length; i++)
      // {
      //   if (i > 0)
      //   {
      //     System.out.print(", ");
      //   }
      //   System.out.print(serialStep1LocalResult[i]);
      // }
      ByteArray step1LocalResult_harp = new ByteArray(serialStep1LocalResult, 0, serialStep1LocalResult.length);
      Table<ByteArray> step1LocalResult_table = new Table<>(0, new ByteArrPlus());
      step1LocalResult_table.addPartition(new Partition<>(this.getSelfID(), step1LocalResult_harp));

      //reduce to master node with id 0
      System.out.println("Before: "+ step1LocalResult_table.getNumPartitions());
      this.allgather("pca", "sync-partial-res-"+stri, step1LocalResult_table);
      System.out.println("After: "+ step1LocalResult_table.getNumPartitions());
      // System.out.println("Size of step1out: "+ step1LocalResult_table.length);

      return step1LocalResult_table;
    }
    catch (Exception e)
    {
      System.out.println("Threw Exception" + e);
      LOG.error("Fail to serilization.", e);
      return null;
    }
  }

  private byte[] serializeStep1Result(NumericTable res) throws IOException
  {
    // Create an output stream to serialize the numeric table
    ByteArrayOutputStream outputByteStream = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(outputByteStream);

    // Serialize the partialResult table into the output stream
    res.pack();
    System.out.println("it works here");
    outputStream.writeObject(res);

    // Store the serialized data in an array
    byte[] buffer = outputByteStream.toByteArray();
    return buffer;
  }

  private NumericTable deserializeStep1Result(byte[] buffer) throws IOException, ClassNotFoundException
  {
    // Create an input stream to deserialize the numeric table from the array
    System.out.println("Size of step1out bytearr[]:");
    System.out.println("Size of step1out bytearr[]: "+ buffer.length);
    ByteArrayInputStream inputByteStream = new ByteArrayInputStream(buffer);
    System.out.println("Size of step1out bytearr[]: "+ buffer.length);
    for (int i = 0; i < buffer.length; i++)
    {
      if (i > 0)
      {
        System.out.print(", ");
      }
      System.out.print(buffer[i]);
    }
    System.out.println("created a new ByteArray");
    ObjectInputStream inputStream = new ObjectInputStream(inputByteStream);
    System.out.println("new inputStream");
    // Create a numeric table object
    NumericTable restoredRes = (NumericTable)inputStream.readObject();
    // PartialCorrelationResult restoredRes;
    System.out.println("read the object");
    restoredRes.unpack(daal_Context);
    System.out.println("done everything. now returning");
    return restoredRes;
  }
}