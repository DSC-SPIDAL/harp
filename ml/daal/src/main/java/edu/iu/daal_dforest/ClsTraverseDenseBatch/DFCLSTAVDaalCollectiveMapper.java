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

package edu.iu.daal_dforest.ClsTraverseDenseBatch;

import com.intel.daal.algorithms.classifier.TreeNodeVisitor;
import com.intel.daal.algorithms.classifier.training.InputId;
import com.intel.daal.algorithms.classifier.training.TrainingResultId;
import com.intel.daal.algorithms.decision_forest.classification.Model;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingBatch;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingMethod;
import com.intel.daal.algorithms.decision_forest.classification.training.TrainingResult;
import com.intel.daal.data_management.data.DataFeature;
import com.intel.daal.data_management.data.DataFeatureUtils;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;
import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.datasource.HarpDAALDataSource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

// intel daal algorithms
// intel daal data structures and services

class DfClsPrintNodeVisitor extends TreeNodeVisitor {
    @Override
    public boolean onLeafNode(long level, long response) {
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
public class DFCLSTAVDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {

	//cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads; 
	private int fileDim;
	private int nFeatures;
    	private int nClasses;
    	private int nTrees;
    	private int minObservationsInLeafNode;
    	private int maxTreeDepth;
	private List<String> inputFiles;
	private Configuration conf;

    	private static NumericTable testGroundTruth;

	private static HarpDAALDataSource datasource;
	private static DaalContext daal_Context = new DaalContext();

        /**
         * Mapper configuration.
         */
        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        this.conf = context.getConfiguration();

	this.nFeatures = this.conf.getInt(HarpDAALConstants.FEATURE_DIM, 3);
	this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 4);
	this.nClasses = this.conf.getInt(HarpDAALConstants.NUM_CLASS, 5);
	this.nTrees = this.conf.getInt(Constants.NUM_TREES, 2);
	this.minObservationsInLeafNode = this.conf.getInt(Constants.MIN_OBS_LEAFNODE, 8);
	this.maxTreeDepth = this.conf.getInt(Constants.MAX_TREE_DEPTH, 15);

        this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
        this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
        //always use the maximum hardware threads to load in data and convert data 
        this.harpThreads = Runtime.getRuntime().availableProcessors();

	//set thread number used in DAAL
	LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
	Environment.setNumberOfThreads(numThreads);
	LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());

	LOG.info("File Dim " + this.fileDim);
        LOG.info("Num Mappers " + this.num_mappers);
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
            this.inputFiles =
                new LinkedList<String>();
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                        + value);
                LOG.info("file name: " + value);
                this.inputFiles.add(value);
            }
            
	    this.datasource = new HarpDAALDataSource(harpThreads, conf);

	    // ----------------------- start the execution -----------------------
            runDFCLSTAV(context);
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
        private void runDFCLSTAV(Context context) throws IOException 
	{
		// ---------- training and testing ----------
		TrainingResult trainingResult = trainModel();
        	printModel(trainingResult);
		daal_Context.dispose();
	}

	private TrainingResult trainModel() 
	{

		//load training data
		NumericTable[] load_table = this.datasource.createDenseNumericTableSplit(this.inputFiles, nFeatures, 1, ",", this.daal_Context);
		NumericTable trainData = load_table[0];
		NumericTable trainGroundTruth = load_table[1];


		/* Set feature as categorical */
		DataFeature categoricalFeature = trainData.getDictionary().getFeature(2);
		categoricalFeature.setFeatureType(DataFeatureUtils.FeatureType.DAAL_CATEGORICAL);

		/* Create algorithm objects to train the decision forest classification model */
		TrainingBatch algorithm = new TrainingBatch(daal_Context, Double.class, TrainingMethod.defaultDense, nClasses);
		algorithm.parameter.setNTrees(nTrees);
		algorithm.parameter.setFeaturesPerNode(nFeatures);
		algorithm.parameter.setMinObservationsInLeafNode(minObservationsInLeafNode);
		algorithm.parameter.setMaxTreeDepth(maxTreeDepth);

		/* Pass a training data set and dependent values to the algorithm */
		algorithm.input.set(InputId.data, trainData);
		algorithm.input.set(InputId.labels, trainGroundTruth);

		/* Train the decision forest classification model */
		TrainingResult trainingResult = algorithm.compute();

		return trainingResult;
	}

    private static void printModel(TrainingResult trainingResult) {
        Model m = trainingResult.get(TrainingResultId.model);
        long nTrees = m.getNumberOfTrees();
        System.out.println("Number of trees: " + nTrees);
        DfClsPrintNodeVisitor visitor = new DfClsPrintNodeVisitor();
        for (long i = 0; i < nTrees; i++) {
            System.out.println("Tree #" + i);
            m.traverseDF(i, visitor);
        }
    }
      
}
