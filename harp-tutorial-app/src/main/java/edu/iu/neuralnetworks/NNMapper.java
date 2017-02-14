package edu.iu.neuralnetworks;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import java.io.*;
import java.util.*;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class NNMapper extends CollectiveMapper<String, String, Object, Object> 
{	
	private String weightsFileName;
	private int weightVectorLength;
	private int inputVecSize;
	private int outputVecSize;
	private int epochs;
	private int numMapTasks;
	private int sizeOfLayer;
	private Log log;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
	{
		Configuration configuration = context.getConfiguration();

		// Parameter initilization 
		weightsFileName = configuration.get(NNConstants.WEIGHTS_FILE);
		weightVectorLength = configuration.getInt(NNConstants.WEIGHTS_VEC_SIZE, 20);
		inputVecSize = configuration.getInt(NNConstants.INPUT_VEC_SIZE, 20);
		outputVecSize = configuration.getInt(NNConstants.OUTPUT_VEC_SIZE, 20);
		epochs = configuration.getInt(NNConstants.EPOCHS, 20);
		numMapTasks = configuration.getInt(NNConstants.NUM_TASKS, 20);
		sizeOfLayer = configuration.getInt(NNConstants.NUM_OF_UNITS, 20);

		log = LogFactory.getLog(CollectiveMapper.class);  	
	}

	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException 
	{

		List<String> pointFiles = new ArrayList<String>();
	    while (reader.nextKeyValue()) {
	    	String key = reader.getCurrentKey();
	    	String value = reader.getCurrentValue();
	    	System.out.println("Key: " + key + ", Value: " + value);
	    	pointFiles.add(value);
	    }
	    Configuration conf = context.getConfiguration();
	    runNN(pointFiles, conf, context);

	}

	private void runNN(List<String> fileNames, Configuration conf, Context context) throws IOException 
	{
		// load data 
		for(int i=0; i < fileNames.size(); ++i)
		{
			System.out.println("fileNames: " + fileNames.get(i));
		}
		DoubleMatrix dataX = loadMatrixFromFile(fileNames.get(0), conf);
		DoubleMatrix dataY = loadMatrixFromFile(fileNames.get(1), conf);

		Table<DoubleArray> weightTable = new Table<>(0, new DoubleArrPlus());

		// Construct A NN  
		NeuralNetwork localNN = constructLocalNN(sizeOfLayer);

		int n = 5;
		int itr = epochs/n;

		Vector shuffledSeq = new Vector();

		for (int i = 0; i < dataX.rows; ++i)
			shuffledSeq.add(i);

		Random random = new Random();

		int trainSize = (int) (dataX.rows * 0.7);
		int miniBatchSize = (int) (trainSize * 0.9);

		for(int s = 0; s < 10 ; s++)
		{
			// initialize weights
			weightTable = generateInitialWeights(1, sizeOfLayer, s);
			random.setSeed(s);
			Collections.shuffle(shuffledSeq, random);

			// Split to train and test
			List trainSeq = shuffledSeq.subList(0, trainSize);
			List testSeq = shuffledSeq.subList(trainSize, dataX.rows);
			
			// reduce and broadcast
			allreduce("main", "allreduce" , weightTable);

			for(int i = 0; i < itr; ++i)
			{
				Collections.shuffle(trainSeq, random);
				List shuffledList = trainSeq.subList(0, miniBatchSize);

				DoubleMatrix X = getMiniBatch(dataX, shuffledList);
				DoubleMatrix Y = getMiniBatch(dataY, shuffledList);

				// Calculate the new weights 
				weightTable = localNN.train(X, Y, weightTable, weightVectorLength, n, numMapTasks);

				// reduce and broadcast
				allreduce("main", "allreduce" + i, weightTable);
			}

			if(this.isMaster())
			{
				DoubleMatrix testX = getMiniBatch(dataX, testSeq);
				DoubleMatrix testY = getMiniBatch(dataY, testSeq);

				DoubleMatrix predictions = localNN.predictFP(testX, weightTable, weightVectorLength, numMapTasks);
	 
				double accuracy = localNN.computeAccuracy(predictions,testY);

				System.out.println("- Accuracy: " + accuracy);

				outputResults(accuracy, conf, context);
			}

		}
	}

 	private Table<DoubleArray> generateInitialWeights(int numOfLayers, int sizeOfLayer, int s) throws IOException
	{		
		Random random = new Random(s);

		int weightVectorLength = (inputVecSize*sizeOfLayer) + sizeOfLayer;

		for(int i = 0; i <numOfLayers-1; ++i)
		{
			weightVectorLength += (sizeOfLayer*sizeOfLayer) + sizeOfLayer;
		}

		weightVectorLength += (sizeOfLayer*outputVecSize) + outputVecSize;

		Table<DoubleArray> weightMeanTable = new Table<>(0, new DoubleArrPlus());

		DoubleArray array = DoubleArray.create(weightVectorLength, false);
		double[] weightsFlatArr = array.get();
		
		for(int i=0; i<weightVectorLength; i++)
		{
			weightsFlatArr[i] = random.nextDouble();
		}
		
		Partition<DoubleArray> ap = new Partition<DoubleArray>(0, array);
		weightMeanTable.addPartition(ap);
		return weightMeanTable;
	} // generateInitialWeights

	private DoubleMatrix getMiniBatch(DoubleMatrix dataMat, List shuffledList) throws IOException
	{
		DoubleMatrix miniBatch = new DoubleMatrix(shuffledList.size(), dataMat.columns);

		for(int i =0; i < shuffledList.size(); ++i)
			miniBatch.putRow(i, dataMat.getRow((int)shuffledList.get(i)));

		return miniBatch;
	}

	//load data form HDFS
	private DoubleMatrix loadMatrixFromFile(String fileName, Configuration conf) throws IOException
	{
		FileSystem fs = FileSystem.get(conf);
		Path dPath = new Path(fileName);
		FSDataInputStream in = fs.open(dPath);
		BufferedReader reader = new BufferedReader( new InputStreamReader(in));
		
		Vector<Double> example = new Vector<Double>();
		Vector<double[]> examples = new Vector<double[]>();

		DoubleMatrix result =null;
		String line = null;

		while((line = reader.readLine())!=null)
		{
			while(true)
			{
				//seperate by space
				int index = line.indexOf(' ');
				// seperate by comma
				if (index < 0)
				{
					index = line.indexOf(',');
				}
				
				if (index>=0)
				{
					example.add(new Double(Double.parseDouble(line.substring(0,index))));
					line = line.substring(index+1,line.length());
				}
				else
				{
					break;
				}
			}
			double [] darray = new double[example.size()];
			for (int i = 0; i<example.size();i++)
			{
				darray[i]  = example.get(i).doubleValue();
			}
			examples.add(darray);
			example.removeAllElements();
			
		}
		result = new DoubleMatrix(examples.size(),examples.firstElement().length);
		
		for (int i = 0; i<examples.size();i++)
		{
			result.putRow(i, new DoubleMatrix(examples.get(i)));
		}
		return result;

	}

	private NeuralNetwork constructLocalNN(int sizeOfLayer) throws IOException
	{
		
		//-Train the neural network with backprop
		double lambda = 0.6987; 	//used for regularization 
		int iters = 10;		//number of iterations to run fmincg
		boolean verbose = true;  //monitor the iterations and cost of each update

		//-Create the neural network and initialize the weights randomly
		int [] topology = {inputVecSize, sizeOfLayer, outputVecSize};

		NeuralNetwork localNetwork = new NeuralNetwork(topology, false);
 		
 		return localNetwork;
	}

	private void outputResults(double accuracy, Configuration conf, Context context)
	{
		int numTasks = conf.getInt(NNConstants.NUM_TASKS, 20);
		String output="Accuracy: ";

		output += accuracy;
	
		output+="\n";

		try 
		{
			context.write(null, new Text(output));
		} catch (IOException e) {
			e.printStackTrace();
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

}
