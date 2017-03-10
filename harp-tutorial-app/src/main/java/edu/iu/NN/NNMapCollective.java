package edu.iu.NN;

import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

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

public class NNMapCollective extends Configured implements Tool {
    //configurations
	int numMapTasks;
	int epochs;
	int syncIterNum;
    int miniBatchSize;
    double lambda;
	String hiddenLayers;
	String workDir;

	public static void main(String[] argv) throws Exception {
		int res = ToolRunner.run(new Configuration(), new NNMapCollective(), argv);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		//keep this unchanged.
		if (args.length < 7) {
			System.err.println("Usage: NNMapCollective "
					+ "<number of map tasks> <epochs> <syncIterNum> <hiddenLayers> <minibatchsize> <lambda> <workDir>");
			ToolRunner.printGenericCommandUsage(System.err);
				return -1;
		}

        //set the class global variables
		numMapTasks = Integer.parseInt(args[0]);
		epochs = Integer.parseInt(args[1]);
		syncIterNum = Integer.parseInt(args[2]);
		hiddenLayers = args[3];
		miniBatchSize= Integer.parseInt(args[4]);
		lambda = Double.parseDouble(args[5]);
		workDir = args[6];

		for(String arg: args){
			System.out.print(arg+";");
		}
		System.out.println();
		
		launch();
		System.out.println("NN Completed"); 
		return 0;
	}
	void launch()
			throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException {
		
		Configuration configuration = getConf();
		Path workDirPath = new Path(workDir);
		FileSystem fs = FileSystem.get(configuration);
		Path dataDir = new Path(workDirPath, "batch");
		Path wDir = new Path(workDirPath, "weights_file");
		Path outDir = new Path(workDirPath, "out");
		if (fs.exists(outDir)) {
			fs.delete(outDir, true);
		}
		fs.mkdirs(outDir);

		//prepare the training dataset before hand
		//System.out.println("Generate data and weights.");
		//File lDir= Utils.readData(numMapTasks, fs, localDirStr, dataDir, configuration);
		
		long startTime = System.currentTimeMillis();

		runNNAllReduce(configuration, workDirPath,
				dataDir, outDir, wDir);
		long endTime = System.currentTimeMillis();
		System.out.println("Total NN Execution Time: "+ (endTime - startTime));
	}
	
	
	private void runNNAllReduce(Configuration configuration, Path workDirPath, Path dataDir, Path outDir, Path wDir) throws IOException,URISyntaxException, InterruptedException,ClassNotFoundException 
	{
			
		System.out.println("Starting Job");
		boolean jobSuccess = true;
		int jobRetryCount = 0;
		
		do 
		{
			Job nnJob = configureNNJob(configuration, workDirPath, dataDir, outDir, wDir);
			
			jobSuccess = nnJob.waitForCompletion(true);
			
			if (!jobSuccess) 
			{
				System.out.println("NN Job failed. ");
				jobRetryCount++;
				if (jobRetryCount == 1) 
				{
					break;
				}
			}
			else
			{
				break;
			}
		} while (true);
	}
	
	private Job configureNNJob(Configuration configuration,Path workDirPath, Path dataDir, Path outDir, Path wDir) throws IOException, URISyntaxException 
	{
		
		Job job = Job.getInstance(configuration, "NN_job");
		Configuration jobConfig = job.getConfiguration();
		Path jobOutDir = new Path(outDir, "NN_out");
		
		FileSystem fs = FileSystem.get(configuration);
		
		if (fs.exists(jobOutDir)) 
		{
			fs.delete(jobOutDir, true);
		}

		FileInputFormat.setInputPaths(job, dataDir);
		FileOutputFormat.setOutputPath(job, jobOutDir);

		String filename = "weights_file";
		Path weightsPath = new Path(wDir, filename);

		job.setInputFormatClass(MultiFileInputFormat.class);
		job.setJarByClass(NNMapCollective.class);
		job.setMapperClass(NNMapper.class);

		org.apache.hadoop.mapred.JobConf jobConf = (JobConf) job.getConfiguration();
		jobConf.set("mapreduce.framework.name", "map-collective");

		long milliSeconds = 1000*60*60;
		jobConf.setLong("mapred.task.timeout", milliSeconds);
		jobConf.setNumMapTasks(numMapTasks);
		jobConf.setInt("mapreduce.job.max.split.locations", 10000);
		job.setNumReduceTasks(0);

		jobConfig.set(NNConstants.WEIGHTS_FILE, weightsPath.toString());
		jobConfig.setInt(NNConstants.NUM_TASKS, numMapTasks);
		jobConfig.setInt(NNConstants.EPOCHS, epochs);
		jobConfig.setInt(NNConstants.SYNC_ITERNUM, syncIterNum);
		jobConfig.set(NNConstants.HIDDEN_LAYERS, hiddenLayers);
        if (miniBatchSize > 0){
		    jobConfig.setInt(NNConstants.MINIBATCH_SIZE, miniBatchSize);
        }

        if (lambda > 0){
		    jobConfig.setDouble(NNConstants.LAMBDA, lambda);
        }

		return job;
	}
}

