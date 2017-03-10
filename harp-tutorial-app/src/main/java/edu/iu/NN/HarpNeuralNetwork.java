/*
 * 	wrapper class for NeuralNetwork
 */

package edu.iu.NN;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import org.dvincent1337.neuralNet.NeuralNetwork;


public class HarpNeuralNetwork  extends NeuralNetwork
{
	// private int weightVectorLength;
	// private int mini_epochs;
	// private int sizeOfLayer;
	// private DoubleMatrix X;
	// private DoubleMatrix Y;
	/*
	 * Constructs a new neural network with given topology;
	 * 	Initializes weight matrices with random values if initWeights == true
	 */
	public HarpNeuralNetwork(int [] newTopology, boolean initWeights)
	{
		this.setTopology(newTopology);
		if (initWeights)
			this.initWeights();
	}

	public Table<DoubleArray> train(DoubleMatrix X, DoubleMatrix Y, Table<DoubleArray> localWeightTable, int mini_epochs, int numMapTasks, double lambda) throws IOException
	{
		// check this
		//double lambda = 0.6987;
		boolean verbose = true;

		Vector<DoubleMatrix> weightMatrix = reshapeTableToList(localWeightTable);

		setTheta(weightMatrix);

		trainBP(X, Y, lambda, mini_epochs, verbose);

		Table<DoubleArray> newWeightTable = reshapeListToTable(this.getTheta());

		return newWeightTable;

	}

	public Table<DoubleArray> reshapeListToTable( Vector<DoubleMatrix> weightMatrix) throws IOException
	{
		DoubleMatrix newFlatWeightMatrix = reshapeToVector(weightMatrix);
		int weightVectorLength = newFlatWeightMatrix.getRows();

		Table<DoubleArray> newWeightTable = new Table<>(0, new DoubleArrPlus());
		DoubleArray arr2 = DoubleArray.create(weightVectorLength, false);
		double[] newWeightArr = arr2.get();

		for(int i=0; i<weightVectorLength; i++)
		{
			newWeightArr[i] = newFlatWeightMatrix.get(i,0);
		}

		Partition<DoubleArray> ap = new Partition<DoubleArray>(0, arr2);
		newWeightTable.addPartition(ap);
		return newWeightTable;

	}

	private Vector<DoubleMatrix> reshapeTableToList( Table<DoubleArray> weightTable) throws IOException
	{
		DoubleArray arr1 = weightTable.getPartition(0).get();
		double[] oldWeightArr = arr1.get();
		int weightVectorLength = oldWeightArr.length;

		DoubleMatrix flatWeightMatrix = new DoubleMatrix(weightVectorLength, 1);
		for(int i = 0; i < weightVectorLength; ++i)
		{
			flatWeightMatrix.put(i, 0, oldWeightArr[i]);
		}

		Vector<DoubleMatrix> weightMatrix = reshapeToList(flatWeightMatrix, this.getTopology());

		return weightMatrix;

	}

	// Averge weights
	public Table<DoubleArray> modelAveraging( Table<DoubleArray> weightTable, int numMapTasks) throws IOException
	{
		Table<DoubleArray> weightMeanTable = new Table<>(0, new DoubleArrPlus());
		
		DoubleArray arr1 = weightTable.getPartition(0).get();
		double[] weightArr = arr1.get();
		int weightVectorLength = weightArr.length;

		DoubleArray arr2 = DoubleArray.create(weightVectorLength, false);
		double[] weightMeanArr = arr2.get();

		for(int i = 0; i < weightVectorLength; ++i)
			weightMeanArr[i] = weightArr[i]/(float) numMapTasks;

		Partition<DoubleArray> ap = new Partition<DoubleArray>(0, arr2);
		weightMeanTable.addPartition(ap);
		return weightMeanTable;
	}
 


	/**
	 * Runs forward prop to find the prediction (all elements of resulting matrix are either 0 or 1)
	 */
	public DoubleMatrix predictFP(DoubleMatrix inputs, Table<DoubleArray> weightTable, int numMapTasks) throws IOException
	{
		Vector<DoubleMatrix> weightMatrix = reshapeTableToList(weightTable);

		setTheta(weightMatrix);

		return this.predictFP(inputs);
	}

}
