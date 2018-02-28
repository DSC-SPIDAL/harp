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

package edu.iu.daal_linreg;

import com.intel.daal.algorithms.linear_regression.Model;
import com.intel.daal.algorithms.linear_regression.prediction.*;
import com.intel.daal.algorithms.linear_regression.training.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.ByteArray;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.*;
import java.nio.DoubleBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

//import daal.jar API


/**
 * @brief the Harp mapper for running Linear Regression
 */


public class LinRegDaalCollectiveMapper
extends
CollectiveMapper<String, String, Object, Object>{

  private int pointsPerFile = 250;                             //change
  private int vectorSize = 10;
  private int nDependentVariables  = 2;
  private int numMappers;
  private int numThreads;
  private int harpThreads; 
  private TrainingResult trainingResult;
  private PredictionResult predictionResult;
  private String testFilePath;
  private String testGroundTruth;
  private Model model;
  private NumericTable results;

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
      testFilePath =
            configuration.get(Constants.TEST_FILE_PATH,"");
      testGroundTruth =
      configuration.get(Constants.TEST_TRUTH_PATH,"");

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

      runLinReg(trainingDataFiles, conf, context);
      LOG.info("Total iterations in master view: "
        + (System.currentTimeMillis() - startTime));
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

  private void runLinReg(List<String> trainingDataFiles, Configuration conf, Context context) throws IOException {

      ts_start = System.currentTimeMillis();

      ts1 = System.currentTimeMillis();
    // extracting points from csv files
      List<List<double[]>> pointArrays = LinRegUtil.loadPoints(trainingDataFiles, pointsPerFile,
                    vectorSize, nDependentVariables, conf, harpThreads);
      List<double[]> featurePoints = new LinkedList<>();
      for(int i = 0; i<pointArrays.size(); i++){
          featurePoints.add(pointArrays.get(i).get(0));
      }
      List<double[]> labelPoints = new LinkedList<>();
      for(int i = 0; i<pointArrays.size(); i++){
          labelPoints.add(pointArrays.get(i).get(1));
      }

      ts2 = System.currentTimeMillis();
      load_time += (ts2 - ts1);



    // converting data to Numeric Table
      ts1 = System.currentTimeMillis();

      long nFeature = vectorSize;
      long nLabel = nDependentVariables;
      long totalLengthFeature = 0;
      long totalLengthLabel = 0;

      long[] array_startP_feature = new long[pointArrays.size()];
      double[][] array_data_feature = new double[pointArrays.size()][];
      long[] array_startP_label = new long[labelPoints.size()];
      double[][] array_data_label = new double[labelPoints.size()][];

      for(int k=0;k<featurePoints.size();k++)
            {
                array_data_feature[k] = featurePoints.get(k);
                array_startP_feature[k] = totalLengthFeature;
                totalLengthFeature += featurePoints.get(k).length;
            }

      for(int k=0;k<labelPoints.size();k++)
      {
          array_data_label[k] = labelPoints.get(k);
          array_startP_label[k] = totalLengthLabel;
          totalLengthLabel += labelPoints.get(k).length;
      }

    long featuretableSize = totalLengthFeature/nFeature;
    long labeltableSize = totalLengthLabel/nLabel;

   //initializing Numeric Table


    NumericTable featureArray_daal = new HomogenNumericTable(daal_Context, Double.class, nFeature, featuretableSize, NumericTable.AllocationFlag.DoAllocate);
    NumericTable labelArray_daal = new HomogenNumericTable(daal_Context, Double.class, nLabel, labeltableSize, NumericTable.AllocationFlag.DoAllocate);


    int row_idx_feature = 0;
    int row_len_feature = 0;

    for (int k=0; k<featurePoints.size(); k++) 
      {
          row_len_feature = (array_data_feature[k].length)/(int)nFeature;
          //release data from Java side to native side
          ((HomogenNumericTable)featureArray_daal).releaseBlockOfRows(row_idx_feature, row_len_feature, DoubleBuffer.wrap(array_data_feature[k]));
          row_idx_feature += row_len_feature;
      }

    int row_idx_label = 0;
    int row_len_label = 0;

    for (int k=0; k<labelPoints.size(); k++) 
    {
        row_len_label = (array_data_label[k].length)/(int)nLabel;
        //release data from Java side to native side
        ((HomogenNumericTable)labelArray_daal).releaseBlockOfRows(row_idx_label, row_len_label, DoubleBuffer.wrap(array_data_label[k]));
        row_idx_label += row_len_label;
    }

    ts2 = System.currentTimeMillis();
    convert_time += (ts2 - ts1);

    Service.printNumericTable("featureArray_daal", featureArray_daal, 5, featureArray_daal.getNumberOfColumns());
    Service.printNumericTable("labelArray_daal", labelArray_daal, 5, labelArray_daal.getNumberOfColumns());

    Table<ByteArray> partialResultTable = new Table<>(0, new ByteArrPlus());

    trainModel(featureArray_daal, labelArray_daal, partialResultTable);
    if(this.isMaster()){
      testModel(testFilePath, conf);
      printResults(testGroundTruth, predictionResult, conf);
    }
    
    daal_Context.dispose();

    ts_end = System.currentTimeMillis();
    total_time = (ts_end - ts_start);
    
    LOG.info("Total Execution Time of LinReg: "+ total_time);
    LOG.info("Loading Data Time of LinReg: "+ load_time);
    LOG.info("Computation Time of LinReg: "+ compute_time);
    LOG.info("Comm Time of LinReg: "+ comm_time);
    LOG.info("DataType Convert Time of LinReg: "+ convert_time);
    LOG.info("Misc Time of LinReg: "+ (total_time - load_time - compute_time - comm_time - convert_time));
}
  
  private void trainModel(NumericTable trainData, NumericTable trainDependentVariables, Table<ByteArray> partialResultTable) throws IOException {

    LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
    Environment.setNumberOfThreads(numThreads);
    LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

    ts1 = System.currentTimeMillis();
    TrainingDistributedStep1Local linearRegressionTraining = new TrainingDistributedStep1Local(daal_Context, Float.class,
                    TrainingMethod.qrDense);
    linearRegressionTraining.input.set(TrainingInputId.data, trainData);
    linearRegressionTraining.input.set(TrainingInputId.dependentVariable, trainDependentVariables);

    PartialResult pres = linearRegressionTraining.compute();
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

    ts1 = System.currentTimeMillis();
    partialResultTable.addPartition(new Partition<>(this.getSelfID(), serializePartialResult(pres)));
    boolean reduceStatus = false;
    reduceStatus = this.reduce("linreg", "sync-partialresult", partialResultTable, this.getMasterID());
    ts2 = System.currentTimeMillis();
    comm_time += (ts2 - ts1);

    if(!reduceStatus){
      System.out.println("reduce not successful");
    }
    else{
      System.out.println("reduce successful");
    }

    if(this.isMaster()){
      TrainingDistributedStep2Master linearRegressionTrainingMaster = new TrainingDistributedStep2Master(daal_Context, Float.class,
                TrainingMethod.qrDense);
      ts1 = System.currentTimeMillis();
      int[] pid = partialResultTable.getPartitionIDs().toIntArray();
      for(int j = 0; j< pid.length; j++){
        try {
          linearRegressionTrainingMaster.input.add(MasterInputId.partialModels,
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
      linearRegressionTrainingMaster.compute();
      trainingResult = linearRegressionTrainingMaster.finalizeCompute();
      ts2 = System.currentTimeMillis();
      compute_time += (ts2 - ts1);
      model = trainingResult.get(TrainingResultId.model);
    }

  }

  private void testModel(String testFilePath, Configuration conf) throws java.io.FileNotFoundException, IOException {
    PredictionBatch linearRegressionPredict = new PredictionBatch(daal_Context, Float.class, PredictionMethod.defaultDense);
    NumericTable testData = getNumericTableHDFS(daal_Context, conf, testFilePath, 10, 250);
    linearRegressionPredict.input.set(PredictionInputId.data, testData);
    linearRegressionPredict.input.set(PredictionInputId.model, model);

    /* Compute the prediction results */
    ts1 = System.currentTimeMillis();
    predictionResult = linearRegressionPredict.compute();
    results = predictionResult.get(PredictionResultId.prediction);
    ts2 = System.currentTimeMillis();
    compute_time += (ts2 - ts1);

  }

  private void printResults(String testGroundTruth, PredictionResult predictionResult, Configuration conf) throws java.io.FileNotFoundException, IOException {
        NumericTable beta = model.getBeta();
        NumericTable expected = getNumericTableHDFS(daal_Context, conf, testGroundTruth, 2, 250);
        Service.printNumericTable("Coefficients: ", beta);
        Service.printNumericTable("First 10 rows of results (obtained): ", results, 10);
        Service.printNumericTable("First 10 rows of results (expected): ", expected, 10);
    }

private NumericTable getNumericTableHDFS(DaalContext daal_Context, Configuration conf, String inputFiles, int vectorSize, int numRows) 
        throws IOException{
            Path inputFilePaths = new Path(inputFiles);
            List<String> inputFileList = new LinkedList<>();

            try {
                FileSystem fs =
                inputFilePaths.getFileSystem(conf);
                RemoteIterator<LocatedFileStatus> iterator =
                fs.listFiles(inputFilePaths, true);

                while (iterator.hasNext()) {
                    String name =
                    iterator.next().getPath().toUri()
                    .toString();
                    inputFileList.add(name);
                }

            } catch (IOException e) {
                LOG.error("Fail to get test files", e);
            }
            int dataSize = vectorSize*numRows;
            // float[] data = new float[dataSize];
            double[] data = new double[dataSize];
            long[] dims = {numRows, vectorSize};
            int index = 0;

            FSDataInputStream in = null;

            //loop over all the files in the list
            ListIterator<String> file_itr = inputFileList.listIterator();
            while (file_itr.hasNext())
            {
                String file_name = file_itr.next();
                LOG.info("read in file name: " + file_name);

                Path file_path = new Path(file_name);
                try {

                    FileSystem fs =
                    file_path.getFileSystem(conf);
                    in = fs.open(file_path);

                } catch (Exception e) {
                    LOG.error("Fail to open file "+ e.toString());
                    return null;
                }

                //read file content
                while(true)
                {
                    String line = in.readLine();
                    if (line == null) break;

                    String[] lineData = line.split(",");

                    for(int t =0 ; t< vectorSize; t++)
                    {
                        if (index < dataSize)
                        {
                            // data[index] = Float.parseFloat(lineData[t]);
                            data[index] = Double.parseDouble(lineData[t]);
                            index++;                                                          
                        }
                        else
                        {
                            LOG.error("Incorrect size of file: dataSize: " + dataSize + "; index val: " + index);
                            return null;
                        }
                        
                    }
                }

                in.close();

            }

            if ( index  != dataSize )
            {
                LOG.error("Incorrect total size of file: dataSize: " + dataSize + "; index val: " + index);
                return null;
            }
            //debug check the vals of data
            // for(int p=0;p<60;p++)
            //     LOG.info("data at: " + p + " is: " + data[p]);
            
            NumericTable predictionData = new HomogenNumericTable(daal_Context, data, vectorSize, numRows);
            return predictionData;

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
