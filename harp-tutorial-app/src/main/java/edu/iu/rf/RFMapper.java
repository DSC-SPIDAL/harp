package edu.iu.rf;

import java.util.*;
import java.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.hadoop.fs.*;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.example.IntArrPlus;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;
import net.sf.javaml.classification.tree.RandomTree;

public class RFMapper extends CollectiveMapper<String, String, Object, Object> {

    private int numTrees;
    private int numMapTasks;
    private int numThreads;
    private int numFeatures;
    private String trainPath;
    private String testPath;
    private String outputPath;
    private Configuration configuration;
    private List<RFTask> rfThreads;
    private DynamicScheduler<Dataset, Classifier, RFTask> rfScheduler;

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
        ArrayList<Classifier> rfClassifier = new ArrayList<Classifier>();
        while (reader.nextKeyValue()) {
            String value = reader.getCurrentValue();
            Util.loadDataset(value, trainDataset, configuration);
        }
        Util.loadDataset(testPath, testDataset, configuration);

    	initialThreads(context);

        numFeatures = trainDataset.noAttributes();

        rfScheduler.start();

        for (int i = 0; i < numTrees / numMapTasks; i++) {
            Dataset baggingDataset = Util.doBagging(trainDataset);
            rfScheduler.submit(trainDataset);
        }
        
        while (rfScheduler.hasOutput()) {
            rfClassifier.add(rfScheduler.waitForOutput());
        }
        context.progress();

        rfScheduler.stop();

        Table<IntArray> predictTable = new Table<>(0, new IntArrPlus());
        int partitionId = 0;

        for (Instance testData : testDataset) {
            IntArray votes = IntArray.create(2, false);
            for (Classifier rf : rfClassifier) {
                Object classValue = rf.classify(testData);
                if (classValue.toString().equals("0")) {
                    votes.get()[0] += 1;
                }
                else {
                    votes.get()[1] += 1;
                }
            }
            Partition<IntArray> partition = new Partition<IntArray>(partitionId, votes);
            predictTable.addPartition(partition);
            partitionId += 1;
        }

        reduce("main", "reduce", predictTable, 0);

        if (this.isMaster()) {
            printResults(predictTable, testDataset);
        }

        predictTable.release();
    }

    private void initialThreads(Context context) {
        rfThreads = new LinkedList<>();
        for (int i = 0; i < numThreads; i++) {
            rfThreads.add(new RFTask(numFeatures, context));
        }
        rfScheduler = new DynamicScheduler<>(rfThreads);
    }

    private void printResults(Table<IntArray> predictTable, Dataset testDataset) throws IOException {
        int correct = 0;
        int total = 0;
        for (Partition<IntArray> partition : predictTable.getPartitions()) {
            Object label = testDataset.get(partition.id()).classValue();
            int predictLabel;
            IntArray votes = partition.get();
            int label0 = votes.get()[0];
            int label1 = votes.get()[1];
            if (label0 > label1) {
                predictLabel = 0;
            }
            else if (label0 < label1) {
                predictLabel = 1;
            }
            else {
                Random random = new Random();
                if (random.nextDouble() < 0.5) {
                    predictLabel = 0;
                }
                else {
                    predictLabel = 1;
                }
            }
            if (label.toString().equals(Integer.toString(predictLabel))) {
                correct += 1;
            }
            total += 1;
        }

        Path path = new Path(outputPath);
        FileSystem fs = path.getFileSystem(configuration);
        FSDataOutputStream out = fs.create(path, true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        writer.write(Double.toString(correct * 1.0 / total));

        writer.close();
    }
}
