package edu.iu.mlr;

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

public class MLRMapper extends CollectiveMapper<String, String, Object, Object> {
    private double alpha;
    private int ITER;
    private int TERM;
    private int numMapTask;
    private int numThread;
    private String topicPath;
    private String qrelsPath;
    private String dataPath;
    private String outputPath;
    private Configuration conf;
    private ArrayList<String> topics;
    private HashMap<Integer, ArrayList<String>> qrels;
    private ArrayList<Instance> data;
    private Table<DoubleArray> wTable;
    private List<GDtask> GDthread;
    private DynamicScheduler<Partition, Object, GDtask> GDsch;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        conf = context.getConfiguration();

        alpha = conf.getDouble("alpha", 1.0);
        ITER  = conf.getInt("ITER", 100);
        TERM  = conf.getInt("TERM", 47236);
        numMapTask = conf.getInt("numMapTask", 2);
        numThread  = conf.getInt("numThread", 8);
        System.out.println("Worker " + this.getSelfID() + ": " + numThread + " threads");
        topicPath = conf.get("topicPath");
        qrelsPath = conf.get("qrelsPath");
        dataPath = conf.get("dataPath");
        outputPath = conf.get("outputPath");
    }

    protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
        LoadAll(reader);
        initTable();
        initThread();

        regroup("mlr", "regroup_wTable", wTable, new Partitioner(getNumWorkers()));
        //printPar();

        GDsch.start();        
        for (int iter = 0; iter < ITER * numMapTask; ++iter) {
            // submit job
            for (Partition par : wTable.getPartitions()) {
                GDsch.submit(par);
            }
            // wait until all job completed
            while (GDsch.hasOutput()) {
                GDsch.waitForOutput();
            }
            
            rotate("mlr", "rotate_" + iter, wTable, null);
            //printPar();

            context.progress();
        }
        GDsch.stop();
        allgather("mlr", "allgather_wTable", wTable);

        if (isMaster()) {
            Util.outputData(outputPath, topics, wTable, conf);
        }

        wTable.release();
    }

    private void printPar() {
        System.out.print(this.getSelfID() + ":");
        for (Partition par : wTable.getPartitions()) {
            System.out.print(" " + par.id());
        }
        System.out.println();
    }

    private void LoadAll(KeyValReader reader) throws IOException, InterruptedException {
        topics = Util.LoadTopicList(topicPath, conf);
        //System.out.print(topics);
        qrels = Util.LoadQrels(qrelsPath, conf);
        data = new ArrayList<Instance>();

        while (reader.nextKeyValue()) {
            String value = reader.getCurrentValue();
            //System.out.println("Worker " + this.getSelfID() + ": load data from " + value);
            Util.LoadData(value, conf, data);
        }
    }

    private void initTable() {
        wTable = new Table(0, new DoubleArrPlus());
        for (int i = 0; i < topics.size(); ++i) {
            wTable.addPartition(new Partition(i, DoubleArray.create(TERM + 1, false)));
        }
    }

    private void initThread() {
        GDthread = new LinkedList<>();
        for (int i = 0; i < numThread; i++) {
            GDthread.add(new GDtask(alpha, data, topics, qrels));
        }
        GDsch = new DynamicScheduler<>(GDthread);
    }
}
