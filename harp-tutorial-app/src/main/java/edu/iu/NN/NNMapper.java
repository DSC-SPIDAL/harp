package edu.iu.NN;

import org.apache.hadoop.mapred.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
//import org.dvincent1337.neuralNet.NeuralNetwork;


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
	private int epochs;
	private int syncIterNum;
	private int numMapTasks;
	private String hiddenLayers;
	private int miniBatchSize;
    private double lambda;
	private Log log;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
	{
		Configuration configuration = context.getConfiguration();

		// Parameter initilization 
		weightsFileName = configuration.get(NNConstants.WEIGHTS_FILE);
		epochs = configuration.getInt(NNConstants.EPOCHS, 20);
		syncIterNum = configuration.getInt(NNConstants.SYNC_ITERNUM, 1);
		numMapTasks = configuration.getInt(NNConstants.NUM_TASKS, 20);
		hiddenLayers = configuration.get(NNConstants.HIDDEN_LAYERS);

		miniBatchSize = configuration.getInt(NNConstants.MINIBATCH_SIZE, 1000);
		lambda = configuration.getDouble(NNConstants.LAMBDA, 0.6987);

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

		// Construct A NN  
		//int[] topology = {784,100,32,10};
        // build the topology from X.shape and hiddenLayers setting
        int inputLayerSize = dataX.columns;
        int outputLayerSize = dataY.columns;
        String[] tokens = hiddenLayers.split("[,]");
        int[] topology = new int[2 + tokens.length];
        {
            int i;
            topology[0] = inputLayerSize;
            for (i = 0; i < tokens.length; i++){
                topology[i+1] = Integer.parseInt(tokens[i]);
            }
            topology[i+1] = outputLayerSize;
        }
        //log
        String msg = "Topology: ";
        for (int i = 0; i < tokens.length +2 ; i++){
            msg += topology[i] + ",";
        }
        log.info(msg);

        
		HarpNeuralNetwork localNN = new HarpNeuralNetwork(topology, true);
        Table<DoubleArray> weightTable = localNN.reshapeListToTable(localNN.getTheta());

		Vector shuffledSeq = new Vector();

		for (int i = 0; i < dataX.rows; ++i)
			shuffledSeq.add(i);

		Random random = new Random();

		int trainSize = (int) (dataX.rows * 1);

		// initialize weights
		random.setSeed(System.currentTimeMillis());
		Collections.shuffle(shuffledSeq, random);

		// Split to train and test
		List trainSeq = shuffledSeq.subList(0, trainSize);

		//get the initial WeightTable
		// reduce and broadcast
		allreduce("main", "allreduce", weightTable);
        // Average the weight table by the numMapTasks
        weightTable = localNN.modelAveraging(weightTable, numMapTasks);

        //start iteration
		int itr = epochs*trainSize/miniBatchSize/syncIterNum;
        log.info("Total Iteration number: " + itr + ", trainSize: " + trainSize);

		for(int i = 0; i < itr; ++i)
		{

            long t0 = System.currentTimeMillis();

			Collections.shuffle(trainSeq, random);
			List shuffledList = trainSeq.subList(0, miniBatchSize);

			DoubleMatrix X = getMiniBatch(dataX, shuffledList);
			DoubleMatrix Y = getMiniBatch(dataY, shuffledList);


            long t1 = System.currentTimeMillis();

			// Calculate the new weights 
			weightTable = localNN.train(X, Y, weightTable, syncIterNum, numMapTasks, lambda);

            long t2 = System.currentTimeMillis();

			// reduce and broadcast
			allreduce("main", "allreduce" + i, weightTable);
            // Average the weight table by the numMapTasks
            weightTable = localNN.modelAveraging(weightTable, numMapTasks);
    
            long t3 = System.currentTimeMillis();

            log.info("Iteration " + i + ": traintime " + (t2-t1) + ", commtime: " + (t3-t2) + ", misc: " + (t1-t0));

        }

		//if(this.isMaster())
		{
			DoubleMatrix testX = getMiniBatch(dataX, trainSeq);
			DoubleMatrix testY = getMiniBatch(dataY, trainSeq);

			DoubleMatrix predictions = localNN.predictFP(testX, weightTable, numMapTasks);
	 
			double accuracy = localNN.computeAccuracy(predictions,testY);

			log.info("Accuracy: " + accuracy);

			outputResults(accuracy, conf, context);
		}

	}

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

		// code from NeuralNetwork:getMatrixFromTextFile()
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
