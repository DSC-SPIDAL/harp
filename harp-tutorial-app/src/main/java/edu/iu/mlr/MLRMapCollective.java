package edu.iu.mlr;

import java.io.*;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import edu.iu.fileformat.MultiFileInputFormat;

public class MLRMapCollective extends Configured implements Tool {

    public static void main(String[] argv) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MLRMapCollective(), argv);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 9) {
            System.err.println("Usage: MLRMapCollective <alpha> <#iter> <#terms> " +
                               "<#Map Task> <#thread> <topics> <qrels> " +
                               " <training data> <output file>");
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        double alpha      = Double.parseDouble(args[0]);
        int    ITER       = Integer.parseInt(args[1]);
        int    TERM       = Integer.parseInt(args[2]);
        int    numMapTask = Integer.parseInt(args[3]);
        int    numThread  = Integer.parseInt(args[4]);
        String topicsPath = args[5];
        String qrelsPath  = args[6];
        String trainPath  = args[7];
        String outputPath = args[8];

        Configuration configuration = this.getConf();
        FileSystem fs = FileSystem.get((Configuration)configuration);

        Path outDirPath = new Path(outputPath);
        if (fs.exists(outDirPath)) {
            fs.delete(outDirPath, true);
        }

        // Configure jobs
        Job job = configureMLRJob(alpha, ITER, TERM, numMapTask, numThread,
                                  topicsPath, qrelsPath, trainPath,
                                  outputPath, configuration);
        
        // Launch job
        boolean jobSuccess = job.waitForCompletion(true);
        if (!jobSuccess) {
            System.out.println("mlr Job fails.");
        }

        return 0;
    }

    private Job configureMLRJob(double alpha, int ITER, int TERM, int numMapTask, int numThread,
                                String topicPath, String qrelsPath,
                                String dataPath, String outputPath,
                                Configuration configuration) throws IOException, URISyntaxException {
        Job job = Job.getInstance(configuration, "MLR_job");
        JobConf jobConf = (JobConf) job.getConfiguration();
        Configuration jobConfig = job.getConfiguration();

        FileInputFormat.setInputPaths(job, new Path(dataPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        job.setInputFormatClass(MultiFileInputFormat.class);
        job.setJarByClass(MLRMapCollective.class);
        job.setMapperClass(MLRMapper.class);

        jobConf.set("mapreduce.framework.name", "map-collective");
        jobConf.setNumMapTasks(numMapTask);
        jobConf.setInt("mapreduce.job.max.split.locations", 10000);
        job.setNumReduceTasks(0);

        jobConfig.setDouble("alpha", alpha);
        jobConfig.setInt("ITER", ITER);
        jobConfig.setInt("TERM", TERM);
        jobConfig.setInt("numMapTask", numMapTask);
        jobConfig.setInt("numThread", numThread);
        jobConfig.set("topicPath", topicPath);
        jobConfig.set("qrelsPath", qrelsPath);
        jobConfig.set("dataPath", dataPath);
        jobConfig.set("outputPath", outputPath + File.separator + "weights");

        return job;
    }
}
