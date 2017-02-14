package com.rf.fast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RandomForest {
  /** the number of threads to use when generating the forest */
	private static int NUM_THREADS;//=Runtime.getRuntime().availableProcessors();
	//private static final int NUM_THREADS=1;

	/** the number of categorical responses of the data (the classes, the "Y" values) - set this before beginning the forest creation */
	public static int C;

	/** the number of attributes in the data - set this before beginning the forest creation */
	public static int M;

	/** Of the M total attributes, the random forest computation requires a subset of them
	 * to be used and picked via random selection. "Ms" is the number of attributes in this
	 * subset. The formula used to generate Ms was recommended on Breiman's website.
	 */
	public static int Ms;//recommended by Breiman: =(int)Math.round(Math.log(M)/Math.log(2)+1);

	/** the collection of the forest's decision trees */
	private ArrayList<DecisionTree> trees2;

	/** the starting time when timing random forest creation */
	private long time_o;

	/** the number of trees in this random tree */
	private int numTrees;

	/** For progress bar display for the creation of this random forest, this is the amount to update by when one tree is completed */
	private double update;

	/** For progress bar display for the creation of this random forest, this records the total progress */
	private double progress;

	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;

	/** This maps from a data record to an array that records the classifications by the trees where it was a "left out" record (the indices are the class and the values are the counts) */
	private HashMap<int[],int[]> estimateOOB;

	/** the total forest-wide error */
	private double error;

	/** the thread pool that controls the generation of the decision trees */
	private ExecutorService treePool;

	/** the original training data matrix that will be used to generate the random forest classifier */
	private ArrayList<ArrayList<String>> data;

	/** the data on which produced random forest will be tested*/
	private ArrayList<ArrayList<String>> testdata;

	/** This holds all of the predictions of trees in a Forest */
	private ArrayList<ArrayList<String>> Prediction;

	/** This holds the user specified layout for training data */
	public ArrayList<Character> DataAttributes;

	/** This holds all of the predictions of trees in a Forest Vs Actual Value */
	public HashMap<ArrayList<String>, String> FinalPredictions;

	/** This holds the number of correct predictions*/
	public int corretsPredictions;


	/**
	 * Initializes a Breiman random forest creation
	 *
	 * @param numTrees			the number of trees in the forest
	 * @param data				the training data used to generate the forest
	 * @param buildProgress		records the progress of the random forest creation
	 */
	@SuppressWarnings("static-access")
	public RandomForest(ArrayList<Character> dataLayout,int numTrees,int numThreads,int M,int Ms,int C, ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test) {
		// TODO Auto-generated constructor stub
		StartTimer();
    // Set up all of the class values for this instances of the random forest
		this.numTrees=numTrees;
		this.NUM_THREADS=numThreads;
		this.data=train;
		this.testdata=test;
		this.M=M;
		this.Ms=Ms;
		this.C=C;
		this.DataAttributes = dataLayout;

    // This holds all of the trees that make up the random forest
    System.out.println("The number of trees " + numTrees);
		trees2 = new ArrayList<DecisionTree>(numTrees);
    // Some type of interval that describes when something is updated
		update=100/((double)numTrees);
		progress=0;
    // Tracks the performances of the random forest for a given period of its
    // growth
		corretsPredictions =0;
		System.out.println("(RandomForest.java ln 101) creating "+numTrees+" trees in a random Forest. . . ");
		System.out.println("(RandomForest.java ln 102) total data size is "+train.size());
		System.out.println("(RandomForest.java ln 103) number of attributes "+M);
		System.out.println("(RandomForest.java ln 104) number of selected attributes "+Ms);

		estimateOOB=new HashMap<int[],int[]>(data.size());
		Prediction = new ArrayList<ArrayList<String>>();
		FinalPredictions = new HashMap<ArrayList<String>, String>();

	}

	/**
	 * Begins the random forest creation
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> Start(boolean forAccuracy,boolean withThreads) {
    // The predicted values for this forest
    ArrayList<String> predVals = new ArrayList<>();
		// TODO Auto-generated method stub
		System.out.println("(RandomForest.java ln 118) Number of threads started : "+NUM_THREADS);
		System.out.println("(RandomForest.java ln 119) Starting trees");
		treePool=Executors.newFixedThreadPool(NUM_THREADS);
    System.out.println("(RandomForest.java ln 121) treepool created");
    // Create numTrees trees. These trees are what will make up the forest
		for (int t=0;t<numTrees;t++){
      // Creating the t-th tree
      System.out.println("Creating a tree " + t);
			treePool.execute(new CreateTree(data,this,t+1));
      //CreateTree DT = new CreateTree(data,this,t+1);
      System.out.println("Tree created!");
		}treePool.shutdown();
    System.out.println("(RandomForest.java ln 130) All trees have been pushed to the tree pool.");
    System.out.println("(RandomForest.java ln 130) trees2 " + trees2.size());
    System.out.println("(RandomForest.java ln 134) Trying to set hold...");
    try {
      System.out.println("(RandomForest.java ln 134) Trying to set hold...");
      // Not able to come back out of the lock to wait until the threads have
      // finished terminating
			treePool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
      System.out.println("(RandomForest.java ln 136) The time limit has been set.");
	    } catch (InterruptedException ignored){
	    	System.out.println("interrupted exception in Random Forests");
	    }
		System.out.println("(RandomForest.java ln 131) Trees Production completed in "+TimeElapsed(time_o));

		if(forAccuracy){
			if(withThreads){
				System.out.println("(RandomForest.java ln 135) Testing Forest for Accuracy with threads");
				ArrayList<DecisionTree> Tree1 = (ArrayList<DecisionTree>) trees2.clone();
				TestforAccuracy(Tree1,testdata,data);
			}else{
				System.out.println("(RandomForest.java ln 139) Testing Forest for Accuracy without threads");
				ArrayList<DecisionTree> Tree2 = (ArrayList<DecisionTree>) trees2.clone();
        System.out.println("(RandomForest.java ln 143) the trees (Tree2) " + Tree2);
				predVals = TestForestForAccuracy(Tree2, data, testdata);
			}

		}else{
			if(withThreads){
				System.out.println("(RandomForest.java ln 146) Testing Forest for Labels with threads");
				ArrayList<DecisionTree> Tree3 = (ArrayList<DecisionTree>) trees2.clone();
				TestForestForLabelWT(Tree3, data, testdata);
			}else{
				System.out.println("(RandomForest.java ln 150) Testing Forest for Labels without threads");
				ArrayList<DecisionTree> Tree4 = (ArrayList<DecisionTree>) trees2.clone();
				TestForestForLabel(Tree4, data, testdata);
			}
		}
    return predVals;
  }

	/**
	 * Running testing on concurrent threads
	 *
	 * @param trees			DesicionTrees
	 * @param Testdata
	 * @param TrainData
	 */
	private void TestforAccuracy(ArrayList<DecisionTree> trees,ArrayList<ArrayList<String>> Testdata,ArrayList<ArrayList<String>> TrainData) {
		long time2 = System.currentTimeMillis();
		ExecutorService TestthreadPool = Executors.newFixedThreadPool(NUM_THREADS);

		for(ArrayList<String> TP:Testdata){
			TestthreadPool.execute(new TestTree(TP,trees,TrainData));
		}TestthreadPool.shutdown();
		try{
			TestthreadPool.awaitTermination(10, TimeUnit.SECONDS);
		}catch(InterruptedException ignored){
			System.out.print("(RandomForest.java ln 174) Interuption in testing");
		}System.out.println("(RandomForest.java ln 175) Testing Complete");

		System.out.println("(RandomForest.java ln 177) Results are ...");
		System.out.println("(RandomForest.java ln 178) Forest Accuracy is "+((corretsPredictions*100)/Testdata.size())+"%");
		System.out.println("(RandomForest.java ln 179) this test was done in "+TimeElapsed(time2));
		System.out.println("");System.out.println("");

	}

	/**
	 * Predicting unlabeled data
	 *
	 * @param trees22
	 * @param data2
	 * @param testdata2
	 */
	private void TestForestForLabel(ArrayList<DecisionTree> trees,ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata) {
		// TODO Auto-generated method stub
		long time = System.currentTimeMillis();
		int treee=1;
		System.out.println("(RandomForest.java ln 195) Predicting Labels now");
		for(DecisionTree DTC : trees){
			DTC.CalculateClasses(traindata, testdata, treee);treee++;
			if(DTC.predictions!=null)
			Prediction.add(DTC.predictions);
		}
		for(int i = 0;i<testdata.size();i++){
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(Prediction.get(j).get(i));
			}
			String pred = ModeofList(Val);
			System.out.println("(RandomForest.java ln 207) ["+pred+"]: Class predicted for data point: "+i+1);
		}
		System.out.println("(RandomForest.java ln 209) this test was done in "+TimeElapsed(time));
	}

	/**
	 * Predicting unlabeled data with threads
	 *
	 * @param tree
	 * @param traindata
	 * @param testdata
	 */
	private void TestForestForLabelWT(ArrayList<DecisionTree> tree,ArrayList<ArrayList<String>> traindata,ArrayList<ArrayList<String>> testdata) {
		long time = System.currentTimeMillis();
		ExecutorService TestthreadPool = Executors.newFixedThreadPool(NUM_THREADS);int i=1;
		for(ArrayList<String> TP:testdata){
			TestthreadPool.execute(new TestTreeforLabel(TP,tree,traindata,i));i++;
		}TestthreadPool.shutdown();
		try{
			TestthreadPool.awaitTermination(10, TimeUnit.SECONDS);
		}catch(InterruptedException ignored){
			System.out.print("(RandomForest.java ln 228) Interuption in testing");
		}
		System.out.println("(RandomForest.java ln 230) Testing Complete");
		System.out.println("(RandomForest.java ln 231) this test was done in "+TimeElapsed(time));
	}

	/**
	 * Testing the forest using the test-data
	 *
	 * @param DecisionTree
	 * @param TrainData
	 * @param TestData
	 *
	 */
	public ArrayList<String> TestForestForAccuracy(ArrayList<DecisionTree> trees,ArrayList<ArrayList<String>> train,ArrayList<ArrayList<String>> test){
		long time = System.currentTimeMillis();
		int correctness=0;ArrayList<String> ActualValues = new ArrayList<String>();
    ArrayList<String> PredValues = new ArrayList<String>();
    System.out.println(trees);
		for(ArrayList<String> s:test){
			ActualValues.add(s.get(s.size()-1));
		}int treee=1;
		System.out.println("Testing forest now (RandomForest.java ln 249)");

		for(DecisionTree DTC : trees){
			DTC.CalculateClasses(train, test, treee);treee++;
			if(DTC.predictions!=null)
      System.out.println("(RandomForest.java ln 254) Predicted value from tree " + DTC.predictions);
			Prediction.add(DTC.predictions);
		}
    System.out.println("(RandomForest.java ln 256) The number of trees " + trees.size());
		for(int i = 0;i<test.size();i++){
			ArrayList<String> Val = new ArrayList<String>();
			for(int j=0;j<trees.size();j++){
				Val.add(Prediction.get(j).get(i));
			}
      System.out.println("(RandomForest.java ln 261) Predicted values " + Val);
			String pred = ModeofList(Val);
      // Add the predicted value to the array of predicted values
      PredValues.add(pred);
      System.out.println(pred);
      System.out.println("(RandomForest.java ln 262) Actual Values @ " + i);
      System.out.println("(RandomForest.java ln 263) Actual Value " + ActualValues.get(i));
      System.out.println("(RandomForest.java ln 264) Predicted Value " + pred);
			if(pred.equalsIgnoreCase(ActualValues.get(i))){
				correctness = correctness +1;
			}
		}
		System.out.println("(RandomForest.java ln 269) The Result of Predictions :-");
		System.out.println("(RandomForest.java ln 270) Total Cases : "+test.size());
		System.out.println("(RandomForest.java ln 271) Total CorrectPredicitions  : "+correctness);
		System.out.println("(RandomForest.java ln 271) Forest Accuracy :"+(correctness*100/test.size())+"%");
		System.out.println("(RandomForest.java ln 273) this test was done in "+TimeElapsed(time));

    return PredValues;
	}

	/**
	 * To find the final prediction of the trees
	 *
	 * @param predictions
	 * @return the mode of the list
	 */
	public String ModeofList(ArrayList<String> predictions) {
		// TODO Auto-generated method stub
		String MaxValue = null; int MaxCount = 0;
		for(int i=0;i<predictions.size();i++){
			int count=0;
			for(int j=0;j<predictions.size();j++){
				if(predictions.get(j).trim().equalsIgnoreCase(predictions.get(i).trim()))
					count++;
				if(count>MaxCount){
					MaxValue=predictions.get(i);
					MaxCount=count;
				}
			}
		}return MaxValue;
	}

	/**
	 * This class houses the machinery to generate one decision tree in a thread pool environment.
	 *
	 */
	private class CreateTree implements Runnable{
		/** the training data to generate the decision tree (same for all trees) */
		private ArrayList<ArrayList<String>> data;
		/** the current forest */
		private RandomForest forest;
		/** the Tree number */
		private int treenum;

		/**
		 * A default constructor
		 */
		public CreateTree(ArrayList<ArrayList<String>> data,RandomForest forest,int num){
			this.data=data;
			this.forest=forest;
			this.treenum=num;
		}
		/**
		 * Creates the decision tree
		 */
		public void run() {
      System.out.println("(RandomForest.java ln 326) Progress " + progress);
			//trees.add(new DTreeCateg(data,forest,treenum));
			trees2.add(new DecisionTree(data, forest, treenum));
      System.out.println("(RandomForest.java ln 332) trees2 " + trees2);
			progress+=update;
      System.out.println("(RandomForest.java ln 332) trees2 " + trees2.size());
		}
	}

	/**
	 * This class houses the machinery to test decision trees in a thread pool environment.
	 *
	 */
	public class TestTree implements Runnable{

		public ArrayList<String> testrecord;
		public ArrayList<DecisionTree> Trees;
		public ArrayList<ArrayList<String>> trainData;

		public TestTree(ArrayList<String> testpoint, ArrayList<DecisionTree> Dtrees, ArrayList<ArrayList<String>> train){

			this.testrecord = testpoint;
			this.Trees = Dtrees;
			this.trainData = train;
		}

		@Override
		public void run() {
			//System.out.print("Testing...");
			ArrayList<String> predictions = new ArrayList<String>();

			for(DecisionTree DT:Trees){
				String Class = DT.Evaluate(testrecord, trainData);
				if(Class == null)
					predictions.add("n/a");
				else
					predictions.add(Class);
			}

			String finalClass = ModeofList(predictions);
			if(finalClass.equalsIgnoreCase(testrecord.get(M)))
				corretsPredictions++;
			System.out.println(finalClass);
			FinalPredictions.put(testrecord,finalClass);
		}
	}

	/**
	 * This class houses the machinery to predict class from decision trees in a thread pool environment.
	 *
	 */
	public class TestTreeforLabel implements Runnable{

		public ArrayList<String> testrecord;
		public ArrayList<DecisionTree> Trees;
		public ArrayList<ArrayList<String>> trainData;
		public int point;

		public TestTreeforLabel(ArrayList<String> dp, ArrayList<DecisionTree> dtree, ArrayList<ArrayList<String>> data,int i){
			this.testrecord = dp;
			this.Trees = dtree;
			this.trainData = data;
			this.point =i;
		}

		@Override
		public void run() {
			ArrayList<String> predictions = new ArrayList<String>();

			for(DecisionTree DT:Trees){
				String Class = DT.Evaluate(testrecord, trainData);
				if(Class == null)
					predictions.add("n/a");
				else
					predictions.add(Class);
			}

			String finalClass = ModeofList(predictions);
			System.out.println("["+finalClass+"]: Class predicted for data point: "+point);
		}
	}
	/** Start the timer when beginning forest creation */
	private void StartTimer(){
		time_o=System.currentTimeMillis();
	}

	/**
	 * Given a certain time that's elapsed, return a string
	 * representation of that time in hr,min,s
	 *
	 * @param timeinms	the beginning time in milliseconds
	 * @return			the hr,min,s formatted string representation of the time
	 */
	private static String TimeElapsed(long timeinms){
		double s=(double)(System.currentTimeMillis()-timeinms)/1000;
		int h=(int)Math.floor(s/((double)3600));
		s-=(h*3600);
		int m=(int)Math.floor(s/((double)60));
		s-=(m*60);
		return ""+h+"hr "+m+"m "+s+"sec";
	}

}
