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

package edu.iu.daal_dforest.RegTraverseDenseBatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.DoubleBuffer;
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

import edu.iu.datasource.*;
import edu.iu.data_aux.*;

// intel daal algorithms 
import com.intel.daal.algorithms.regression.TreeNodeVisitor;
import com.intel.daal.algorithms.decision_forest.regression.*;
import com.intel.daal.algorithms.decision_forest.regression.training.*;
import com.intel.daal.algorithms.decision_forest.*;

// intel daal data structures and services
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.MergedNumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import com.intel.daal.data_management.data.*;

class DfRegPrintNodeVisitor extends TreeNodeVisitor {
    @Override
    public boolean onLeafNode(long level, double response) {
        if(level != 0)
            printTab(level);
        System.out.println("Level " + level + ", leaf node. Response value = " + response);
        return true;
    }

    public boolean onSplitNode(long level, long featureIndex, double featureValue){
        if(level != 0)
            printTab(level);
        System.out.println("Level " + level + ", split node. Feature index = " + featureIndex + ", feature value = " + featureValue);
        return true;
    }

    private void printTab(long level) {
        String s = "";
        for (long i = 0; i < level; i++) {
            s += "  ";
        }
        System.out.print(s);
    }
}

/**
 * @brief the Harp mapper for running K-means
 */
public class DFREGTAVDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int numMappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;

	private int nFeatures;
    	private int nTrees;

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

	private static HarpDAALDataSource datasource;
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

	this.nFeatures = configuration.getInt(HarpDAALConstants.FEATURE_DIM, 13);
	this.fileDim = configuration.getInt(HarpDAALConstants.FILE_DIM, 14);
	this.nTrees = configuration.getInt(Constants.NUM_TREES, 2);

        this.numMappers = configuration.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = configuration.getInt(HarpDAALConstants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();

        LOG.info("File Dim " + this.fileDim);
        LOG.info("Num Mappers " + this.numMappers);
        LOG.info("Num Threads " + this.numThreads);
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

	    // read data file names from HDFS
            List<String> dataFiles =
                new LinkedList<String>();
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                        + value);
                LOG.info("file name: " + value);
                dataFiles.add(value);
            }
            
            Configuration conf = context.getConfiguration();

	    // ----------------------- runtime settings -----------------------
            //set thread number used in DAAL
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	    this.datasource = new HarpDAALDataSource(dataFiles, fileDim, harpThreads, conf);

	    // ----------------------- start the execution -----------------------
            runDFREGTAV(conf, context);
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
        private void runDFREGTAV(Configuration conf, Context context) throws IOException 
	{
		// ---------- load data ----------
		this.datasource.loadFiles();
		// ---------- training and testing ----------
		TrainingResult trainingResult = trainModel();
        	printModel(trainingResult);

		daal_Context.dispose();
	}

	private TrainingResult trainModel() {

        /* Create Numeric Tables for training data and labels */
        NumericTable trainData = new HomogenNumericTable(daal_Context, Double.class, nFeatures, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
        NumericTable trainGroundTruth = new HomogenNumericTable(daal_Context, Double.class, 1, this.datasource.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
        MergedNumericTable mergedData = new MergedNumericTable(daal_Context);
        mergedData.addNumericTable(trainData);
        mergedData.addNumericTable(trainGroundTruth);

        /* Retrieve the data from an input file */
	this.datasource.loadDataBlock(mergedData);

        /* Set feature as categorical */
        DataFeature categoricalFeature = trainData.getDictionary().getFeature(3);
        categoricalFeature.setFeatureType(DataFeatureUtils.FeatureType.DAAL_CATEGORICAL);

        /* Create algorithm objects to train the decision forest classification model */
        TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense);
        algorithm.parameter.setNTrees(nTrees);

        /* Pass a training data set and dependent values to the algorithm */
        algorithm.input.set(InputId.data, trainData);
        algorithm.input.set(InputId.dependentVariable, trainGroundTruth);

        /* Train the decision forest classification model */
        TrainingResult trainingResult = algorithm.compute();

        
        return trainingResult;
    }


    private void printModel(TrainingResult trainingResult) {
        Model m = trainingResult.get(TrainingResultId.model);
        long nTrees = m.getNumberOfTrees();
        System.out.println("Number of trees: " + nTrees);
        DfRegPrintNodeVisitor visitor = new DfRegPrintNodeVisitor();
        for (long i = 0; i < nTrees; i++) {
            System.out.println("Tree #" + i);
            m.traverseDF(i, visitor);
        }
    }
    
 
      
}
