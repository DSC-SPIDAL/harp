package edu.iu.mlr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.*;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class Util {
  public static ArrayList<String> LoadTopicList(
    String filepath, Configuration conf)
    throws IOException {
    ArrayList<String> topics =
      new ArrayList<String>();
    Path path = new Path(filepath);
    FileSystem fs = path.getFileSystem(conf);
    FSDataInputStream in = fs.open(path);
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(in));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        topics.add(line);
      }
    } finally {
      in.close();
    }

    return topics;
  }

  public static
    HashMap<Integer, ArrayList<String>>
    LoadQrels(String filepath, Configuration conf)
      throws IOException {
    HashMap<Integer, ArrayList<String>> qrels =
      new HashMap<Integer, ArrayList<String>>();
    Path path = new Path(filepath);
    FileSystem fs = path.getFileSystem(conf);
    FSDataInputStream in = fs.open(path);
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(in));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(" ");
        int id = Integer.parseInt(parts[1]);

        if (qrels.containsKey(id))
          qrels.get(id).add(parts[0]);
        else {
          ArrayList<String> lst =
            new ArrayList<String>();
          lst.add(parts[0]);
          qrels.put(id, lst);
        }
      }
    } finally {
      in.close();
    }

    return qrels;
  }

  public static void LoadData(String filepath,
    Configuration conf, ArrayList<Instance> data)
    throws IOException {
    Path path = new Path(filepath);
    FileSystem fs = path.getFileSystem(conf);
    FSDataInputStream in = fs.open(path);
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(in));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\\s+");
        int id = Integer.parseInt(parts[0]);
        HashMap<Integer, Double> pair =
          new HashMap<Integer, Double>();

        for (int i = 1; i < parts.length; i++) {
          String[] sub_parts =
            parts[i].split(":");
          pair.put(Integer.parseInt(sub_parts[0]),
            Double.parseDouble(sub_parts[1]));
        }

        data.add(new Instance(id, pair));
      }
    } finally {
      in.close();
    }
  }

  public static void outputData(String filename,
    ArrayList<String> topics,
    Table<DoubleArray> wTable, Configuration conf)
    throws IOException {
    Path path = new Path(filename);
    FileSystem fs = path.getFileSystem(conf);
    FSDataOutputStream output =
      fs.create(path, true);
    BufferedWriter writer = new BufferedWriter(
      new OutputStreamWriter(output));

    for (Partition par : wTable.getPartitions()) {
      String cat = topics.get(par.id());
      double[] W =
        ((DoubleArray) par.get()).get();

      writer.write(cat);
      for (int j = 0; j < W.length; ++j)
        writer.write(" " + W[j]);
      writer.write("\n");
    }
    writer.close();
  }

  public static void outputEval(String filename,
    ArrayList<String> topics,
    Table<DoubleArray> evTable, Configuration conf)
    throws IOException {
        double tp = 0.;
        double fn = 0.;
        double fp = 0.;
        double sump = 0.;
        double sumr = 0.;
        double count = 0.;

        Path path = new Path(filename);
        FileSystem fs = path.getFileSystem(conf);
        FSDataOutputStream output =
          fs.create(path, true);
        BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(output));

        writer.write(" cat\ttp\tfn\tfp\ttn\tf1\n");
        for (Partition par : evTable.getPartitions()) {
          //System.out.print(" " + par.id());
          
          String cat = topics.get(par.id());
          // tp, fn, fp, tn
          double[] cm = ((DoubleArray) par.get()).get();
          tp += cm[0];
          fp += cm[2];
          fn += cm[1];
          sump += cm[0] / (cm[0] + cm[2] + 1e-9);
          sumr += cm[0] / (cm[0] + cm[1] + 1e-9);
          count ++;

          writer.write(" " + cat + ":" + "\t" + 
                  cm[0] + "\t" + cm[1] + "\t" +cm[2] + "\t" + cm[3] + "\t" + 
                  2*cm[0]/(2*cm[0] + cm[1] +cm[2] + 1e-9) + "\n");
        }

        //microF1
        double microF1 = 2*tp/(2*tp + fp +fn);
        double macroF1 = 2*sump*sumr/count/(sump + sumr);
        writer.write("microF1 : " + microF1 + "\n");
        writer.write("macroF1 : " + macroF1 + "\n");
        
        writer.close();

    }

}
