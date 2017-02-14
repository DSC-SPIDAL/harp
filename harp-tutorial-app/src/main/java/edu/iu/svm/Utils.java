package edu.iu.svm;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.partition.Table;
import edu.iu.harp.partition.Partition;

public class Utils {
	private static void randomizeInPlace(int[] input) { 
        int n = input.length; 
        for (int i = 0; i < n; i++) { 
            int index = (int)(Math.random() * (n - i - 1) + i); 
            int temp = input[index]; 
            input[index] = input[i]; 
            input[i] = temp; 
        } 
    }

	static void generateData(int numOfTasks, FileSystem fs,  String localPathString, Path dataPath) throws IOException, InterruptedException, ExecutionException {
		File localFile = new File(localPathString);

		//check if localFile exists
		if (localFile.isFile() && localFile.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(localFile));
			String line = "";

			//get number of lines
			int lineCount = 0;
			while ((line = br.readLine()) != null) {
				lineCount++;
			}
			br.close();

			//randomize the index of lines
			int[] randomLineIndex = new int[lineCount];
			for (int i = 0;i < lineCount;i++) {
				randomLineIndex[i] = i;
			}
			randomizeInPlace(randomLineIndex);

			//generate sub data sets
			File[] subLocalFile = new File[numOfTasks];
			for (int i = 0;i < numOfTasks;i++) {
				subLocalFile[i] = new File(localPathString.substring(0, 10) + File.separator + "data_" + Integer.toString(i));
			}

			//split the data set
			FileWriter[] fw = new FileWriter[numOfTasks];
			BufferedWriter[] bw = new BufferedWriter[numOfTasks];
			for (int i = 0;i < numOfTasks;i++) {
				fw[i] = new FileWriter(subLocalFile[i].getAbsoluteFile());
				bw[i] = new BufferedWriter(fw[i]);
			}
			br = new BufferedReader(new FileReader(localFile));
			line = "";
			for (int i = 0;i < lineCount;i++) {
				line = br.readLine();
				randomLineIndex[i] %= numOfTasks;
				bw[randomLineIndex[i]].write(line);
	    		bw[randomLineIndex[i]].newLine();
			}
			br.close();
			for (int i = 0;i < numOfTasks;i++) {
				bw[i].close();
			}

			//copy to HDFS
			Path localPath = new Path(localPathString.substring(0, 10));
			if (fs.exists(dataPath)) {
	    		fs.delete(dataPath, true);
	    	}
			fs.copyFromLocalFile(localPath, dataPath);
			fs.delete(new Path(dataPath, localPathString.substring(11)), false);
		}
		else {
			System.err.println("Error: Data set doesn't exists!");
			System.exit(-1);
		}
	}
}
