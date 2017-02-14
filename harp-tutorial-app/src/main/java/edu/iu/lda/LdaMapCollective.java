package edu.iu.lda;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.iu.fileformat.MultiFileInputFormat;

public class LdaMapCollective extends Configured implements Tool {
	
	Path inputDir;
	Path outputDir;
	Path metafile;
	int numOfTerms;
	int numOfTopics;
	int numOfDocs;
	int numOfMapTasks;
	int numOfIterations;
	int numOfThreads;
	int mode;
	
	public static void main(String[] args) throws Exception {
	    int res = ToolRunner.run(new Configuration(), new LdaMapCollective(), args);
	    System.exit(res);
	}
	
	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 10) {
			System.err.println("Usage: LdaMapCollective "
					+ "<input dir> "
					+ "<metafile> "
					+ "<output dir> "
					+ "<number of terms> "
					+ "<number of topics> "
					+ "<number of docs> "
					+ "<number of MapTasks> "
					+ "<number of iterations> "
					+ "<number of threads> "
					+ "<mode, 1=multithreading> ");
			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}
		
		inputDir = new Path(args[0]);
		metafile = new Path(args[1]);
		outputDir = new Path(args[2]);
		numOfTerms = Integer.parseInt(args[3]);
		numOfTopics = Integer.parseInt(args[4]);
		numOfDocs = Integer.parseInt(args[5]);
		numOfMapTasks = Integer.parseInt(args[6]);
		numOfIterations = Integer.parseInt(args[7]);
		numOfThreads = Integer.parseInt(args[8]);
		mode = Integer.parseInt(args[9]);
		launch();
		
		return 0;
	}
	
	void launch(){
		Configuration configuration = getConf();
		long beginTime = System.currentTimeMillis();
		try {
			Job job = configureJob(configuration);
			job.waitForCompletion(true);
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		}
		long endOfOneIteration = System.currentTimeMillis();
		System.out.println("total running time(ms): "+(endOfOneIteration-beginTime));

	}
	
	public Job configureJob(Configuration configuration) throws IOException{
		Job job = Job.getInstance(configuration, "lda_job");
		FileSystem fs = FileSystem.get(configuration);
		
		if (fs.exists(outputDir)) {
			fs.delete(outputDir, true);
		}
		
		FileInputFormat.setInputPaths(job,  inputDir);
		FileOutputFormat.setOutputPath(job, outputDir);
		
		job.setInputFormatClass(MultiFileInputFormat.class);
		job.setJarByClass(LdaMapCollective.class);
		if(mode == 1)
			job.setMapperClass(LDAMapperDyn.class);
		else{
			job.setMapperClass(LDAMapper.class);
		}
		
		if(!fs.exists(metafile))
		{
			throw new IOException();
		}
		
		org.apache.hadoop.mapred.JobConf jobConf = (JobConf) job.getConfiguration();
		jobConf.setNumReduceTasks(0);
		jobConf.set("mapreduce.framework.name", "map-collective");
		jobConf.setNumMapTasks(numOfMapTasks);
		
		jobConf.setInt(Constants.NUM_OF_ITERATIONS, numOfIterations);
		jobConf.setInt(Constants.NUM_OF_TOPICS, numOfTopics);
		jobConf.setInt(Constants.NUM_OF_TERMS, numOfTerms);
		jobConf.setInt(Constants.NUM_OF_DOCS, numOfDocs);
		jobConf.setInt(Constants.NUM_OF_THREADS, numOfThreads);
		jobConf.setStrings(Constants.META_DATA_FILE_NAME, metafile.getName());
		
		return job;
	}
	
 
}
