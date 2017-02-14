package edu.iu.randomforest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;


public class RandomForestMapper extends CollectiveMapper<String, String, Object, Object> {

	private int numTrees;
	private int numAttributes;
	private Log log;


	@Override
	protected void setup(Context context) throws IOException, InterruptedException {

		// Set up the configuration for the environment
		Configuration configuration = context.getConfiguration();

		// Get the number of trees in the forest that is to be learned locally
		numTrees = configuration.getInt(RandomForestConstants.NUM_TREES, 50);

		// Get the number of attributes that the random forest will need to operate
		// over
		numAttributes = RandomForestConstants.NUM_ATTRIBUTES;

		//Initialize log
		log = LogFactory.getLog(RandomForestMapper.class);
	}

	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {

		 // This is a list of the files that contain the data points that will be
		 // used to learn the forest at the mapper
		 List<String> pointFiles = new ArrayList<String>();
		 // Read all of the keys from the reader where the values are the data file paths
	    while (reader.nextKeyValue()) {
				// Get the current key
	    	String key = reader.getCurrentKey();
				// Get the current value which is a path to a data file
	    	String value = reader.getCurrentValue();
	    	log.info("Key: " + key + ", Value: " + value);
				// Add this file to the string list of file paths
	    	pointFiles.add(value);
	    }
			// Get the necessary configuration information
	    Configuration conf = context.getConfiguration();

			// Have each map task update its centroids estimate based on the data, and iterate
			runRandomForest(pointFiles, conf, context);}


  private void runRandomForest(List<String> fileNames, Configuration conf, Context context) throws IOException {

  	if(this.isMaster()) {
  		log.info("I am the master!!");
  	}

		// Initialize the arrays that will hold the train and test data
		Dataset trainDataPoints = new DefaultDataset();
		Dataset testDataPoints = new DefaultDataset();		

		// Load the train and test data from HD files passed to this mapper
		loadData(trainDataPoints, testDataPoints, fileNames, conf);

		// Get the number of test points
		int numTest = testDataPoints.size();

		// Create the Random Forest classifier that uses 5 neighbors to make decisions
		Classifier rf = new RandomForest(numTrees, false, numAttributes, new Random());
		// Learn the forest
		rf.buildClassifier(trainDataPoints);

		// Flush out the training points now that they are do to limit the amount
		// of space required
		trainDataPoints = null;

		log.info("Starting to populate predTable\n");
		// Populate Partition table with prediction to Push to master
		// where prediction is populated by vote
		Table<IntArray> predTable = new Table<>(0, new IntArrPlus());
		// This is used to track which data point is being classified
		int dp = 0;

		// Pass each of the test points through the Random Forest classifier
		for (Instance inst : testDataPoints) {
			// This will hold this random forest's class vote for this data point
			//IntArray votes = IntArray.create(C, false);
			IntArray votes = IntArray.create(RandomForestConstants.NUM_CLASSES, false);
			// Get the prediction class from this Random Forest
			Object predictedClassValue = rf.classify(inst);
			// Get the true value
			Object realClassValue = inst.classValue();
			//int predIndex = Integer.parseInt(preds.get(dp));
			// Check which class was predicted
			for (int i = 0; i < RandomForestConstants.NUM_CLASSES; i++) {
				// Check to see if this index matches the class that was predicted
				if (predictedClassValue.equals(Integer.toString(i))) {
					// log.info("i: " + i + "; predictedClassValue: " + predictedClassValue + "; condition: " + predictedClassValue.equals(Integer.toString(i)));
					votes.get()[i] = 1;
				} else {
					votes.get()[i] = 0;
				}
			}
			// log.info("Populating - Predicted class: " + predictedClassValue + "; Real class: " + realClassValue);
			/*if (predictedClassValue.equals(realClassValue)) {
				// Set the item at index 0 to 0 and the item at index 1 to 1
				// in order to indicate that this forest is one that correctly voted
				votes.get()[0] = 1;
				votes.get()[1] = 0;

				// Check whether this is a true positive or a true negative
                if (predictedClassValue.equals("1")) {
					// This is a true positive
					votes.get()[2] = 1;
					votes.get()[3] = 0;
					votes.get()[4] = 0;
					votes.get()[5] = 0;
				} else {
					// This is a true negative
					votes.get()[2] = 0;
					votes.get()[3] = 1;
					votes.get()[4] = 0;
					votes.get()[5] = 0;
				}
			}
			else {
				// Set the item at index 0 to 1 and the item at index 1 to 0
				// in order to indicate that this forest is one that incorrectly voted
				votes.get()[0] = 0;
				votes.get()[1] = 1;
				// Check whether this is a false positive or a false negative
				if (predictedClassValue.equals("1")) {
					// This is a false positive
					votes.get()[2] = 0;
					votes.get()[3] = 0;
					votes.get()[4] = 1;
					votes.get()[5] = 0;
				} else {
					// This is a false negative
					votes.get()[2] = 0;
					votes.get()[3] = 0;
					votes.get()[4] = 0;
					votes.get()[5] = 1;
				}
			}*/

			// Add the voting results to the partition
			Partition<IntArray> dpP = new Partition<IntArray>(dp, votes);
			predTable.addPartition(dpP);

			// Move onto the next data point
			dp++;
		}

		log.info("Done populating predTable\n");

		// All Reduce from all Mappers
		log.info("Before allreduce!!!!");
		allreduce("main", "allreduce", predTable);
		log.info("After allreduce!!!!");

		// Flush out the testing points now that they are do to limit the amount
		// of space required
		//testDataPoints = null;

		if(this.isMaster()) {
			// Get the overall prediction for each test data points by taking the
			// mode of the predictions from each random forest and compare with the
			// true labels for each data point
			// This tracks the number of correctly predicted values
			double correct = 0.0;
			double wrong = 0.0;
			double TP = 0.0;
			double TN = 0.0;
			double FP = 0.0;
			double FN = 0.0;


			// Used to index into the true labels for the test data points

			for( Partition<IntArray> ap: predTable.getPartitions()){				
				// Get the true label
				Object trueLabel = testDataPoints.get(ap.id()).classValue();

				// Get the global vote for this data point
				IntArray votes = ap.get();

				Random random = new Random();
				// Get the predicted value by finding the class with the most votes
				String predLabel = "0";
				int maxCount = votes.get()[0];
				//log.info("Num votes 0: " + maxCount);
				// Check each class to see if it is the most popular one
				for (int j = 1; j < RandomForestConstants.NUM_CLASSES; j++) {
					//log.info("Num votes 1: " + votes.get()[j]);
					if (votes.get()[j] > maxCount) {
						maxCount = votes.get()[j];
						predLabel = Integer.toString(j);
					}
					else if (votes.get()[j] == maxCount) {
						if (random.nextDouble() < 0.5) {
							predLabel = Integer.toString(j);
						} 
					}
				}
				// Check to see if the class label was correctly predicted
				if (trueLabel.toString().equals(predLabel)) {
					// Set the item at index 0 to 0 and the item at index 1 to 1
					// in order to indicate that this forest is one that correctly voted
					correct++;

					// Check whether this is a true positive or a true negative
					if (predLabel.equals("1")) {
						// This is a true positive
						TP++;
					} else {
						// This is a true negative
						TN++;
					}
				}
				else {
					// log.info("Num votes 0: " + votes.get()[0]);
					// log.info("Num votes 1: " + votes.get()[1]);					
					// Set the item at index 0 to 1 and the item at index 1 to 0
					// in order to indicate that this forest is one that incorrectly voted
					wrong++;
					// Check whether this is a false positive or a false negative
					if (predLabel.equals("1")) {
						// This is a false positive
						FP++;
					} else {
						// This is a false negative
						FN++;
					}
				}
				// log.info("datapoint: " + k + ";trueLabel: " + trueLabel + "predLabel: " + predLabel + "\n");			
			}
			log.info("----------------------------------");
			log.info("----------------------------------");
			log.info("Correct predictions  " + correct);
			log.info("Wrong predictions " + wrong);
			log.info("True positives " + TP);
			log.info("True negatives " + TN);
			log.info("False positives " + FP);
			log.info("False negatives " + FN);			
			log.info("----------------------------------");
			log.info("Bye now!!!!!");

			outputResults(context, correct, wrong, TP, TN, FP, FN);
		} 
		else {
			testDataPoints = null;
		}

 }

	/*
	* This function writes the current results to the distributed file system context and
	* environment.
	*/
	private void outputResults(Context context, double correct, double wrong, double TP, double TN, double FP, double FN){
		try {

			context.write("Accuracy: " , new Text(Double.toString(((double)correct)/(correct+wrong))));
			context.write("Correct predictions:  " , new Text(Double.toString(correct)));
			context.write("Wrong predictions: " , new Text(Double.toString(wrong)));
			context.write("True positives: " , new Text(Double.toString(TP)));
			context.write("True negatives: " , new Text(Double.toString(TN)));
			context.write("False positives: " , new Text(Double.toString(FP)));
			context.write("False negatives: " , new Text(Double.toString(FN)));

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	* This function takes a String list, the size of each data vector, and the configuration obejct for
	* the distributed file system sent it. It reads the data from each of the files on the distributed
	* file system into an array list. The data point values are represented as doubles.
	*/
	private void loadData(Dataset train, Dataset test, List<String> fileNames, Configuration conf) throws IOException{
	  // Process each file
	  for (String filename: fileNames) {
	    // Get the information about how the file system is configured
	    FileSystem fs = FileSystem.get(conf);
	    // Get the location of the data file in the distrubted file system
	    Path dPath = new Path(filename);
	    // Create the objects that are needed to read in the data
	    FSDataInputStream in = fs.open(dPath);

	    try {
	      InputStreamReader inIn = new InputStreamReader(in);
	      BufferedReader br = new BufferedReader(inIn);

	      // This line is used to more through each data file
	      String sCurrentLine;

	      // This flag tracks whether or not the transition point indicating that
	      // the data points are now test points has been seen
	      boolean seenTest = false;

	      // Read through the file until all lines have been seen
	      while ((sCurrentLine = br.readLine()) != null) {
	        // Check if the lines are split by commas
	        if (sCurrentLine.indexOf(",") >= 0) {
	          // Split the line at the spaces
	          String[] arr = sCurrentLine.split(",");
	          // Get the number of values in this line
	          int nvals = arr.length;
	          // Holder for array of the values in the file
	          double[] values = new double[nvals - 1];
	          // This is the class value for this data point
	          String classValue = arr[nvals - 1];
	          // Loop through each value in the current data point
	          for (int i = 0; i < (nvals - 1); i++) {
	            // Get the feature value as a double
	            // This is the current value at index i
	            double cval;
	            try {
	              // Parse the value as a double
	              cval = Double.parseDouble(arr[i]);
	            } catch (NumberFormatException e) {cval = Double.NaN;}
	            // Add the double value to the list of values
	            values[i] = cval;
	          }
	          // Check if these data points represent the train or the test
	          // sets
	          if (seenTest) {
	            test.add(new DenseInstance(values, classValue));
	          } else {train.add(new DenseInstance(values, classValue));}
	        }
	        // Check if this is a data point that is split by spaces
	        else if (sCurrentLine.indexOf(" ") >= 0) {
	          // Split the line at the spaces
	          String[] arr = sCurrentLine.split(" ");
	          // Get the number of values in this line
	          int nvals = arr.length;
	          // Holder for array of the values in the file
	          double[] values = new double[nvals - 1];
	          // This is the class value for this data point
	          String classValue = arr[nvals - 1];
	          // Loop through each value in the current data point
	          for (int i = 0; i < (nvals - 1); i++) {
	            // Get the feature value as a double
	            // This is the current value at index i
	            double cval;
	            try {
	              // Parse the value as a double
	              cval = Double.parseDouble(arr[i]);
	            } catch (NumberFormatException e) {cval = Double.NaN;}
	            // Add the double value to the list of values
	            values[i] = cval;
	          }
	          // Check if these data points represent the train or the test
	          // sets
	          if (seenTest) {
	            test.add(new DenseInstance(values, classValue));
	          } else {train.add(new DenseInstance(values, classValue));}
	        }
	        // This is the line indicating that we are switching from the train to
	        // the test data
	        else {seenTest = true;}
	      }

	    } finally {
	      // Close the input stream
	      in.close();
	    }
	  }
	}

}
