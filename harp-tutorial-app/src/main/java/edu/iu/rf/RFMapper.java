package edu.iu.rf;

import java.util.*;
import java.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.schdynamic.DynamicScheduler;

public class RFMapper extends CollectiveMapper<String, String, Object, Object> {

    private int numTrees;
    private int numMapTasks;
    private int numThreads;
    private String trainPath;
    private String testPath;
    private String outputPath;
    private Configuration configuration;

    private ArrayList<Sample> data;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        configuration = context.getConfiguration();
        numTrees = configuration.getInt("numTrees", 100);
        numMapTasks = configuration.getInt("numMapTasks", 4);
        numThreads = configuration.getInt("numThreads", 5);
        trainPath = configuration.get("trainPath");
        testPath = configuration.get("testPath");
        outputPath = configuration.get("outputPath");

    }

    protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    	ArrayList<Sample> trainData = new ArrayList<Sample>();
        ArrayList<Sample> testData = new ArrayList<Sample>();
        while (reader.nextKeyValue()) {
            String value = reader.getCurrentValue();
            Util.loadData(value, trainData, configuration);
        }
        Util.loadData(testPath, testData, configuration);
        Sample test = trainData.get(0);
        String output = "";
        for (Float f : test.features) {
            output += (Float.toString(f) + " ");
        }
        output += Integer.toString(test.label);
        context.write(output);
        test = testData.get(0);
        output = "";
        for (Float f : test.features) {
            output += (Float.toString(f) + " ");
        }
        output += Integer.toString(test.label);
        context.write(output);

    	initialThreads();
    }

    private void initialThreads() {
    }
}
