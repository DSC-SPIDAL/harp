package edu.iu.rf;

import java.util.*;
import java.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.hadoop.mapreduce.Mapper.Context;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.tree.RandomForest;
import net.sf.javaml.classification.tree.RandomTree;

public class RFTask implements Task<Dataset, Classifier> {
    private int numFeatures;
    private Context context;

    public RFTask(int numFeatures, Context context) {
        this.numFeatures = numFeatures;
        this.context = context;
    }

    @Override
    public Classifier run(Dataset dataset) throws Exception {
    	Classifier rf = new RandomForest(1, false, numFeatures, new Random());
    	rf.buildClassifier(dataset);
        context.progress();
    	return rf;
    }
}
