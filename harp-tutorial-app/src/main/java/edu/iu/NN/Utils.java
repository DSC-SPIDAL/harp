/*
 * generate data and initial centroids
 */

package edu.iu.NN;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.ExecutionException;


import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


public class Utils 
{
	public static int inputVecSize;
	public static int outputVecSize;
	public static int weightVectorLength;

	/**
	* Gets a matrix from a ascii text file.
	* The format of the text file should have each file containing only one matrix.
	* The format of the text file should have each row separated by a newline character
	* The format of the text file should have each column separated by a space character (' ').
	*/
	
	public static DoubleMatrix getMatrixFromTextFile(String filename, Configuration conf) throws NumberFormatException, IOException
	{
		
		FileSystem fs = FileSystem.get(conf);
		Path dPath = new Path("hdfs://j-045:9010/" + filename);
		FSDataInputStream in = fs.open(dPath);
		BufferedReader reader = new BufferedReader( new InputStreamReader(in));

		DoubleMatrix result =null;
		String line = null;
		Vector<Double> example = new Vector<Double>();
		Vector<double[]> examples = new Vector<double[]>();

		while((line = reader.readLine())!=null)
		{
			while(true)
			{
				//seperate by space
				int index =line.indexOf(' ');
				//seperate by comma
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
			
		}// While

		result = new DoubleMatrix(examples.size(),examples.firstElement().length);
		for (int i = 0; i<examples.size();i++)
		{
			result.putRow(i, new DoubleMatrix(examples.get(i)));
		}

		return result;
	}
	
	
	static File readData(int numMapTasks, FileSystem fs,  String localDirStr, Path dataDir, Configuration conf) throws IOException, InterruptedException, ExecutionException 
	{

		DoubleMatrix X = getMatrixFromTextFile("/digitInput.txt", conf);
		DoubleMatrix Y = getMatrixFromTextFile("/digitOutput.txt", conf);

		int miniBatchSize = X.rows;
		inputVecSize =  X.columns;
		outputVecSize = Y.columns;

	    System.out.println("Writing " + miniBatchSize + " vectors to "+ numMapTasks +" file evenly");
	    
	    // Check data directory
	    if (fs.exists(dataDir)) {
	    	fs.delete(dataDir, true);
	    }
	    
	    // Check local directory
	    File localDir = new File(localDirStr);
	    // If existed, regenerate data
	    if (localDir.exists() && localDir.isDirectory()) {
	    	for (File file : localDir.listFiles()) {
	    		file.delete();
	    	}
	    	localDir.delete();
	    }
	    
	    boolean success = localDir.mkdir();
	    if (success) {
	    	System.out.println("Directory: " + localDirStr + " created");
	    }
	    if (miniBatchSize == 0) {
	    	throw new IOException("No point to write.");
	    }

	    int kk = 0;
	    for (int k = 0; k < numMapTasks; k++) 
	    {
	    	try 
	    	{
	    		String filename = Integer.toString(k);
	    		File fileX = new File(localDirStr + File.separator + "batch_" + Integer.toString(kk++) + filename);
	    		File fileY = new File(localDirStr + File.separator + "batch_" + Integer.toString(kk++) + filename);
	    		FileWriter fwX = new FileWriter(fileX.getAbsoluteFile());
	    		FileWriter fwY = new FileWriter(fileY.getAbsoluteFile());
				BufferedWriter bwX = new BufferedWriter(fwX);
				BufferedWriter bwY = new BufferedWriter(fwY);
	    			    		
	    		for (int i = 0; i < miniBatchSize; i++) 
	    		{
	    			for (int j = 0; j < X.columns; j++) 
	    			{
	    				if(j == X.columns-1)
	    				{
	    					bwX.write(X.get(i, j)+" ");
	    					bwX.newLine();
	    				}
	    				else
	    				{
	    					bwX.write(X.get(i, j)+" ");
	    				}
	    			}

	    			for (int j = 0; j < Y.columns; j++) 
	    			{
	    				if(j == Y.columns-1)
	    				{
	    					bwY.write(Y.get(i, j)+" ");
	    					bwY.newLine();
	    				}
	    				else
	    				{
	    					bwY.write(Y.get(i, j)+" ");
	    				}
	    			}
	    		}

	    		bwX.close();
	    		bwY.close();
	    		System.out.println("Done written "+ miniBatchSize + " points " +" to file "+ filename);
	    	} 
	    	catch (FileNotFoundException e) 
	    	{
	    		e.printStackTrace();
	    	} catch (IOException e) 
	    	{
	    		e.printStackTrace();
	    	}
	    } // For k
	
	    Path localPath = new Path(localDirStr);
	    fs.copyFromLocalFile(localPath, dataDir); 
	    return localDir; 
	} // generateData

} // Utils
