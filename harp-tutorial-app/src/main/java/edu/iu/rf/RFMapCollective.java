package edu.iu.rf;

import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.iu.fileformat.MultiFileInputFormat;

public class RFMapCollective extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new RFMapCollective(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Usage: edu.iu.rf.RFMapCollective <numTrees> <numMapTasks> <numThreads> <trainPath> <testPath> <outputPath>");
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        int numTrees = Integer.parseInt(args[0]);
        int numMapTasks = Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        String trainPath = args[3];
        String testPath = args[4];
        String outputPath = args[5];

        Configuration configuration = this.getConf();
        FileSystem fs = FileSystem.get((Configuration)configuration);

        Path outDirPath = new Path(outputPath);
        if (fs.exists(outDirPath)) {
            fs.delete(outDirPath, true);
        }

        Job job = configureRFJob(numTrees, numMapTasks, numThreads, trainPath, testPath, outputPath, configuration);
        boolean jobSuccess = job.waitForCompletion(true);
        if (!jobSuccess) {
            System.out.println("Random Forests job fails.");
        }
        else {
            System.out.println("Random Forests job suceeds!");
        }
        return 0;
    }

    private Job configureRFJob(int numTrees, int numMapTasks, int numThreads, String trainPath, String testPath, String outputPath, Configuration configuration)
        throws IOException, URISyntaxException {
        Job job = Job.getInstance(configuration, "RF_job");
        JobConf jobConf = (JobConf)job.getConfiguration();
        Configuration jobConfiguration = job.getConfiguration();

        FileInputFormat.setInputPaths(job, new Path(trainPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        job.setInputFormatClass(MultiFileInputFormat.class);
        job.setJarByClass(RFMapCollective.class);
        job.setMapperClass(RFMapper.class);

        jobConf.set("mapreduce.framework.name", "map-collective");
        jobConf.setNumMapTasks(numMapTasks);
        jobConf.setInt("mapreduce.job.max.split.locations", 10000);
        job.setNumReduceTasks(0);

        jobConfiguration.setInt("numTrees", numTrees);
        jobConfiguration.setInt("numMapTasks", numMapTasks);
        jobConfiguration.setInt("numThreads", numThreads);
        jobConfiguration.set("trainPath", trainPath);
        jobConfiguration.set("testPath", testPath);
        jobConfiguration.set("outputPath", outputPath + File.separator + "output");

        return job;
    }
}
