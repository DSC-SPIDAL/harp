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

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        configuration = context.getConfiguration();
        numTrees = conf.getInt("numTrees", 100);
        numMapTasks = conf.getInt("numMapTasks", 4);
        numThreads = conf.getInt("numThreads", 5);
        trainPath = conf.get("trainPath");
        testPath = conf.get("testPath");
        outputPath = conf.get("outputPath");

        System.out.println("just for test.");
    }

    protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    	//loadData

    	initialThreads();
    }

    private void initialThreads() {
    }
}