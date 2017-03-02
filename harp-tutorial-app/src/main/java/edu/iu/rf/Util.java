package edu.iu.rf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.*;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class Util {
	public static void loadData(String inputPath, ArrayList<Sample> data, Configuration configuration) throws IOException {
		Path path = new Path(inputPath);
		FileSystem fs = path.getFileSystem(configuration);
		FSDataInputStream in = fs.open(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] numbers = line.split(",");
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
}
