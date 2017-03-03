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

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;

public class RFMapper extends CollectiveMapper<String, String, Object, Object> {

    private int numTrees;
    private int numMapTasks;
    private int numThreads;
    private int numFeatures;
    private String trainPath;
    private String testPath;
    private String outputPath;
    private Configuration configuration;

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
        Dataset trainDataset = new DefaultDataset();
        Dataset testDataset = new DefaultDataset();
        while (reader.nextKeyValue()) {
            String value = reader.getCurrentValue();
            Util.loadDataset(value, trainDataset, configuration);
        }
        Util.loadDataset(testPath, testDataset, configuration);

    	initialThreads();

        numFeatures = trainDataset.noAttributes();
        System.out.println(numFeatures);

        Classifier rf = new RandomForest(numTrees, false, numFeatures, new Random());
        rf.buildClassifier(trainDataset);



    }

    private void initialThreads() {
    }
}
