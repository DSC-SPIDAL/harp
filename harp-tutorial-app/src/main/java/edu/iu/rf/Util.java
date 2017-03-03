package edu.iu.rf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.*;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

public class Util {
	public static void loadData(String pathString, ArrayList<Sample> data, Configuration configuration) throws IOException {
		Path path = new Path(pathString);
		FileSystem fs = path.getFileSystem(configuration);
		FSDataInputStream in = fs.open(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] numbers = line.split(" ");
				int label = Integer.parseInt(numbers[numbers.length - 1]);
				ArrayList<Float> features = new ArrayList<Float>();
				for (int i = 0;i < numbers.length - 2;i++) {
					features.add(Float.parseFloat(numbers[i]));
				}
				data.add(new Sample(label, features));
			}
		}
		finally {
			in.close();
		}
	}

	public static void loadDataset(String pathString, Dataset dataset, Configuration configuration) throws IOException {
		Path path = new Path(pathString);
		FileSystem fs = path.getFileSystem(configuration);
		FSDataInputStream in = fs.open(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] numbers = line.split(" ");
				Double[] features = new Double[numbers.length - 1];
				for (int i = 0; i < (numbers.length - 1); i++) {
					features[i] = Double.parseDouble(numbers[i]);
				}
				dataset.add(new DenseInstance(features, numbers[numbers.length - 1]));
			}
		}
		finally {
			in.close();
		}
	}
}
