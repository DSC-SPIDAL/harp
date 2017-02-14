package edu.iu.mlr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.util.*;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

public class Util {
    public static ArrayList<String> LoadTopicList(String filepath, Configuration conf) throws IOException {
        ArrayList<String> topics = new ArrayList<String>();
        Path path = new Path(filepath);
        FileSystem fs = path.getFileSystem(conf);
        FSDataInputStream in = fs.open(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));        
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                topics.add(line);
            }
        }
        finally {
            in.close();
        }
        
        return topics;
    }

    public static HashMap<Integer, ArrayList<String>> LoadQrels(String filepath, Configuration conf) throws IOException{
        HashMap<Integer, ArrayList<String>> qrels = new HashMap<Integer, ArrayList<String>>();
        Path path = new Path(filepath);
        FileSystem fs = path.getFileSystem(conf);
        FSDataInputStream in = fs.open(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                int id = Integer.parseInt(parts[1]);
            
                if(qrels.containsKey(id))
                    qrels.get(id).add(parts[0]);
                else {
                    ArrayList<String> lst = new ArrayList<String>();
                    lst.add(parts[0]);
                    qrels.put(id, lst);
                }
            }
        }
        finally {
            in.close();
        }

        return qrels;
    }

    public static void LoadData(String filepath, Configuration conf,
                                ArrayList<Instance> data) throws IOException{
        Path path = new Path(filepath);
        FileSystem fs = path.getFileSystem(conf);
        FSDataInputStream in = fs.open(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                int id = Integer.parseInt(parts[0]);
                HashMap<Integer, Double> pair = new HashMap<Integer, Double>();
            
                for(int i = 1; i < parts.length; i++) {
                    String[] sub_parts = parts[i].split(":");
                    pair.put(Integer.parseInt(sub_parts[0]),
                             Double.parseDouble(sub_parts[1]));
                }
            
                data.add(new Instance(id, pair));
            }
        }
        finally {
            in.close();
        }
    }

    public static void outputData(String filename,
                                  ArrayList<String> topics,
                                  Table<DoubleArray> wTable,
                                  Configuration conf) throws IOException{
        Path path = new Path(filename);
        FileSystem fs = path.getFileSystem(conf);
        FSDataOutputStream output = fs.create(path, true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

        for (Partition par : wTable.getPartitions()) {
            String cat = topics.get(par.id());
            double[] W = ((DoubleArray)par.get()).get();
            
            writer.write(cat);
            for (int j = 0; j < W.length; ++j)
                writer.write(" " + W[j]);
            writer.write("\n");
        }
        writer.close();
    }
}
