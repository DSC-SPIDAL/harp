package edu.iu.svm;

import java.io.IOException;
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

public class IterativeSVM extends Configured implements Tool {

	public static void main(String[] argv) throws Exception {
		int res = ToolRunner.run(new Configuration(), new IterativeSVM(), argv);
		System.exit(res);
	}
	
	@Override
	public int run(String[] args) throws Exception {
		//check the arguments length
		if (args.length < 4) {
			System.err.println("Usage: IterativeSVM <number of map tasks> <number of iteration> <work path> <local path>");
			return -1;
		}

		//parse arguments
		int numOfTasks = Integer.parseInt(args[0]);
		int numOfIteration = Integer.parseInt(args[1]);
		String workPathString = args[2];
		String localPathString = args[3];

		launch(numOfTasks, numOfIteration, workPathString, localPathString);

		//svm finished
		System.out.println("Harp SVM Completed!");
		return 0;
	}

	void launch(int numOfTasks, int numOfIteration, String workPathString, String localPathString) throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException {
		Configuration configuration = getConf();
		Path workPath = new Path(workPathString);
		FileSystem fs = FileSystem.get(configuration);
		Path dataPath = new Path(workPath, "data");
		Path outputPath = new Path(workPath, "out");

		//split the whole data set
		Utils.generateData(numOfTasks, fs, localPathString, dataPath);
		
		long startTime = System.currentTimeMillis();

		runSVMAllReduce(configuration, numOfTasks, numOfIteration, workPath, dataPath, outputPath);

		long endTime = System.currentTimeMillis();
		
		//print running time
		System.out.println("Total Harp SVM Execution Time: " + (endTime - startTime));
	}

	private void runSVMAllReduce(Configuration configuration, int numOfTasks, int numOfIteration, Path workPath, Path dataPath, Path outputPath) throws IOException, URISyntaxException, InterruptedException, ClassNotFoundException {
		boolean jobSuccess = true;
		int jobRetryCount = 0;
		while (true) {
			Job svmJob = configureSVMJob(configuration, numOfTasks, numOfIteration, workPath, dataPath, outputPath);
    		jobSuccess = svmJob.waitForCompletion(true);
    		if (!jobSuccess) {
    			jobRetryCount++;
    			if (jobRetryCount == 3) {
    				System.err.println("SVM job failed.");
    				System.exit(-1);
    			}
    		}
    		else {
    			break;
    		}
		}
	}

	private Job configureSVMJob(Configuration configuration, int numOfTasks, int numOfIteration, Path workPath, Path dataPath, Path outputPath) throws IOException, URISyntaxException {
		Job job = Job.getInstance(configuration, "svm_job");
    	Configuration jobConfig = job.getConfiguration();
    	FileSystem fs = FileSystem.get(configuration);
    	if (fs.exists(outputPath)) {
    		fs.delete(outputPath, true);
    	}
    	FileInputFormat.setInputPaths(job, dataPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		job.setInputFormatClass(MultiFileInputFormat.class);
		job.setJarByClass(IterativeSVM.class);
		job.setMapperClass(SVMMapper.class);
		JobConf jobConf = (JobConf)job.getConfiguration();
		jobConf.set("mapreduce.framework.name", "map-collective");
		jobConf.setNumMapTasks(numOfTasks);
		jobConf.setInt("mapreduce.job.max.split.locations", 10000);
		job.setNumReduceTasks(0);
		jobConfig.setInt(SVMConstants.NUM_OF_ITERATON, numOfIteration);
		return job;
	}
}
