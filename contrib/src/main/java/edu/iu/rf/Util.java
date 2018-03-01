package edu.iu.rf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;

public class Util {
  public static void loadDataset(
    String pathString, Dataset dataset,
    Configuration configuration)
    throws IOException {
    Path path = new Path(pathString);
    FileSystem fs =
      path.getFileSystem(configuration);
    FSDataInputStream in = fs.open(path);
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(in));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] numbers = line.split(" ");
        double[] features =
          new double[numbers.length - 1];
        for (int i =
          0; i < (numbers.length - 1); i++) {
          features[i] =
            Double.parseDouble(numbers[i]);
        }
        dataset.add(new DenseInstance(features,
          numbers[numbers.length - 1]));
      }
    } finally {
      in.close();
    }
  }

  public static Dataset doBagging(Dataset dataset)
    throws IOException {
    Dataset baggingDataset = new DefaultDataset();
    Random rand = new Random();
    for (int i = 0; i < dataset.size(); i++) {
      baggingDataset.add(dataset
        .get(rand.nextInt(dataset.size())));
    }
    return baggingDataset;
  }
}
