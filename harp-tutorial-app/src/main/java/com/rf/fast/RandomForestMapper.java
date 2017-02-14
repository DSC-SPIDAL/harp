package com.rf.fast;

import java.lang.Math;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;

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


public class RandomForestMapper extends CollectiveMapper<String, String, Object, Object> {

	private int numTrees;
	private String DataInfo;
	private DescribeTrees DT;
	private Log log;


	@Override
	protected void setup(Context context) throws IOException, InterruptedException {

		// Set up the configuration for the environment
		Configuration configuration = context.getConfiguration();

		// Get the configuration of the data points
		DataInfo = RandomForestConstants.DATA_CONFIG;

		// Get the number of trees in the forest that is to be learned locally
		numTrees =configuration.getInt(RandomForestConstants.NUM_TREES, 50);

		// This function contains a number of decision tree untilities that is used
		// in a couple of different places
		DT = new DescribeTrees();

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
	    //runKMean(pointFiles, cenTable, conf, context, numIterations);
			runRandomForest(pointFiles, conf, context);

	}


	  private void runRandomForest(List<String> fileNames, Configuration conf, Context context) throws IOException {

	  	if(this.isMaster()) {
	  		log.info("I am the master!!");
	  	}

			// Create the data structure that tracks the format of the data points in
			// terms of each attribute's data type
			ArrayList<Character> DataLayout = DT.CreateFinalLayout(DataInfo);
			// Initialize the arrays that will hold the train and test data
			ArrayList<ArrayList<String>> trainDataPoints = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> testDataPoints = new ArrayList<ArrayList<String>>();
			// Load the train and test data from hdfs files passed to this mapper
			loadData(trainDataPoints, testDataPoints, fileNames, conf);

			// The number of distinct class labels in the dataset
			int C = RandomForestConstants.NUM_TARGETS;

			// The number of attributes in each data point
			int M=DataLayout.size()-1;
			
			// The number of attributes to randomly subselect at each split in a given
	    	// tree. These randomly subselected attributes are the ones that are considered
	    	// as candidate split decision points
			// int Ms = (int)Math.round(Math.log(M)/Math.log(2)+1);
			int Ms = 3;


			// Create the Random Forest object instance
			RandomForest RFC = new RandomForest(DataLayout,numTrees,1, M, Ms, C,trainDataPoints,testDataPoints);

			trainDataPoints = null;

			// Start learning and testing the Random Forest classifier
			ArrayList<String> preds = RFC.Start(true, false);

			log.info("Starting to populate predTable\n");
			// Populate Partition table with prediction to Push to master
			// where prediction is populated by vote
			Table<IntArray> predTable = new Table<>(0, new IntArrPlus());
			for (int dp = 0; dp < testDataPoints.size(); dp++ ) {
				IntArray votes = IntArray.create(C, false);
				int predIndex = Integer.parseInt(preds.get(dp));
				for(int j = 0; j < C; j ++) {
					if ( j == predIndex) {
						votes.get()[j] = 1;	
					} else {
						votes.get()[j] = 0;	
					}					
				}
				Partition<IntArray> dpP = new Partition<IntArray>(dp, votes);
				predTable.addPartition(dpP);
			}

			log.info("Done populating predTable\n");

			// All Reduce from all Mappers
			log.info("Before allreduce!!!!");
			allreduce("main", "allreduce", predTable);
			log.info("After allreduce!!!!");

			preds = null;

			if(this.isMaster()) {
				int i, j, k;

				/*
				// This is the array of string arrays that will hold the prediction for
				// each test point from the forest at each mapper
				ArrayList<String>[] allpreds = new ArrayList<String>[preds.size()];
				// Add the predictions from the Master worker and create the array lists
				// Populate the hash map
				for (int j=0; j < preds.size(); j++) {
					// Add the arraylist of strings to the array
					allpreds[j] = new ArrayList<String>();
					// Add the predicted value to the array list of strings at index j
					allpreds[j].add(preds.get(j));
				}
				*/				

				// Get the overall prediction for each test data points by taking the
				// mode of the predictions from each random forest and compare with the
				// true labels for each data point
				// This tracks the number of correctly predicted values
				double correct = 0.0;
				// Evaluate each prediction
				/*
				for (i=1; i < allpreds.length; i++) {
					// Get the mode of the predicted values for this data point
					String pred = RFC.ModeofList(allpreds[i]);
					// Get the true label
					String tlabel = testDataPoints.get(i).get(testDataPoints.get(i)-1);
					// Check to see if the predicted and the true label match
					if (tlabel.equals(pred)) {
						// Increment the number correct tracker
						correct++;
					}
				}
				*/

				k = 0;
				for( Partition<IntArray> ap: predTable.getPartitions()){
					IntArray votes = ap.get();
					int maxCount = votes.get()[0], maxCountPos = 0;
					for(j=1;j < votes.size(); j++){
						if(votes.get()[j] > maxCount) {
							maxCount =votes.get()[j];
							maxCountPos = j;
						}
					}
					int tlabel = Integer.parseInt(testDataPoints.get(k).get(testDataPoints.get(k).size()-1));
					// Check to see if the predicted and the true label match
					if (tlabel == maxCountPos) {
						// Increment the number correct tracker
						correct++;
					}
					k++;
				}

				/*
				for (i=0; i < testDataPoints.size(); i++) {
				
					// Iterate over classes, and pick class with max votes

					// Get the mode of the predicted values for this data point
					String pred = RFC.ModeofList(allpreds[i]);
					// Get the true label
					String tlabel = testDataPoints.get(i).get(testDataPoints.get(i)-1);
					// Check to see if the predicted and the true label match
					if (tlabel.equals(pred)) {
						// Increment the number correct tracker
						correct++;
					}
				}
				*/
				// Get the accuracy by dividing the number of correctly predicted labels
				// by the total number of predicted values
				double accuracy = correct / testDataPoints.size();
				// Print the accuracy to file system
				outputResults(context,accuracy);
				log.info("Accuracy: " + accuracy);
				log.info("Bye now!!!!!");

			}

	 }

	/*
	* This function writes the current results to the distributed file system context and
	* environment.
	*/
	private void outputResults(Context context, double accuracy){	  

		String output  = Double.toString(accuracy);		
		try {
			context.write("accuracy", new Text(output));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}/*
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	  /*
		* This function takes a String list, the size of each data vector, and the configuration obejct for
		* the distributed file system sent it. It reads the data from each of the files on the distributed
		* file system into an array list. The data point values are represented as doubles.
		*/
	  private void loadData(ArrayList<ArrayList<String>> train, ArrayList<ArrayList<String>> test, List<String> fileNames, Configuration conf) throws IOException{
			// Process each file
		  for(String filename: fileNames){
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

					while((sCurrentLine = br.readLine()) != null){
						// This holds the index of all commas in the data line from the input
						// file
						ArrayList<Integer> Sp = new ArrayList<Integer>(); int i;

						// Check to see if the line contains commas indicating that the values
						// are comma separated
						if(sCurrentLine.indexOf(",") >= 0){
							// Pad the data line with commas on both ends and then find the location
							// of each comma so that feature values can be grabbed based on the
							// characters that fall between the commas. Within this function, the
							// data is represented as a list of strings. Even the number based
							// features are represented as strings.
							// Read each line in the data file

							// Pad the line with commas at the start and the end of the comma
							// separated line of values
							sCurrentLine=","+sCurrentLine+",";
							// Convert type so able to iterate through
							char[] c =sCurrentLine.toCharArray();
							// Find the indices of all of the comma characters
							for(i=0;i<sCurrentLine.length();i++){
								if(c[i]==',')
									Sp.add(i);
							}
							ArrayList<String> DataPoint=new ArrayList<String>();
							// Move through the data string using the location of the commas
							// as guides to where individual pieces of attribute data start
							// and end
							for(i=0;i<Sp.size()-1;i++){
								// Get the data that falls between this comma and the next comma
								DataPoint.add(sCurrentLine.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
							}
							// Check to see if the file has transitioned to test data points
							if (seenTest) {
								// Add the data point to the list of train data strings.
								// All features are treated as strings here, even the number of
								// features
								test.add(DataPoint);
							} else {
								// Add the data point to the list of test data strings.
								// All features are treated as strings here, even the number of
								// features
								train.add(DataPoint);
							}

						}
						// Split each data line in the data file where there are white spaces.
						// This is the same method used above for commas.
						// The location of all white spaces are found, and data elements are
						// add to the list of string elements based on what falls between
						// two subsequent white spaces.
						else if (sCurrentLine.indexOf(" ")>=0){
							//has spaces
							sCurrentLine=" "+sCurrentLine+" ";
							for(i=0;i<sCurrentLine.length();i++){
								if(Character.isWhitespace(sCurrentLine.charAt(i)))
									Sp.add(i);
							}ArrayList<String> DataPoint=new ArrayList<String>();
							for(i=0;i<Sp.size()-1;i++){
								DataPoint.add(sCurrentLine.substring(Sp.get(i), Sp.get(i+1)).trim());
							}
							// Check to see if the file has transitioned to test data points
							if (seenTest) {
								// Add the data point to the list of train data strings.
								// All features are treated as strings here, even the number of
								// features
								test.add(DataPoint);
							} else {
								// Add the data point to the list of test data strings.
								// All features are treated as strings here, even the number of
								// features
								train.add(DataPoint);
							}
						}

						// This is the line indicating that the data points
						// are now test data points
						else {
							// The data points are now part of the test set
							seenTest = true;
						}
					}
			  } finally {
					// Close the input stream
					in.close();
			  }
		  }

			// Check to make sure that the size of the trainig data matches that of
			// the specified data layout
			char[] datalayout = DT.CreateLayout(DataInfo);
			if(datalayout.length!=train.get(0).size()){
				System.out.print("Data Layout is incorrect. "+datalayout.length+" "+train.get(0).size());
			}else{
				ArrayList<Character> FinalPin = new ArrayList<Character>();
				for(char c:datalayout)
					FinalPin.add(c);
				for(int i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='I'){
						FinalPin.remove(i);
						for(ArrayList<String>DP:train)
							DP.remove(i);
						i=i-1;
					}
				}
				for(int i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='L'){
						for(ArrayList<String> DP:train){
							String swap = DP.get(i);
							DP.set(i, DP.get(DP.size()-1));
							DP.set(DP.size()-1, swap);
						}
					}break;
				}
			}

	  }
}
