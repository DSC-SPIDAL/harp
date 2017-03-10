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
	public static void loadDataset(String pathString, Dataset dataset, Configuration configuration) throws IOException {
		Path path = new Path(pathString);
		FileSystem fs = path.getFileSystem(configuration);
		FSDataInputStream in = fs.open(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] numbers = line.split(" ");
				double[] features = new double[numbers.length - 1];
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

	public static Dataset doBagging(Dataset dataset) throws IOException {
		Dataset baggingDataset = new DefaultDataset();
		Random rand = new Random();
		for (int i = 0; i < dataset.size(); i++) {
			baggingDataset.add(dataset.get(rand.nextInt(dataset.size())));
		}
		return baggingDataset;
	}
}
