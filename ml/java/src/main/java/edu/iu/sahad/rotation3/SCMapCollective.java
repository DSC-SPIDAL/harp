/**
 * subgraph counting for unlabeled graph; 
 * using key value abstraction
 * using rotation
 *
 * This version is based rotation2. But will use static scheduler instead of dynamic scheduler
 */
package edu.iu.sahad.rotation3;

import edu.iu.fileformat.MultiFileInputFormat;
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

import java.io.IOException;
import java.util.ArrayList;

public class SCMapCollective extends Configured implements Tool {

	int numMapTasks;
	String template;
	String graphDir;
	String outDir;
	int numThreads;
	boolean useLocalMultiThread;
	ArrayList<SCSubJob> subjoblist;
	/*
	 * a template is like:
	    i graph 5
		u5-1 u3-1 u2
		u3-1 i u2
		u2 i i
		final u5-1 5 2
	 */
	public int run(String[] args) throws Exception {
		if (args.length < 6) {
			System.err.println("Usage: edu.iu.sahad.rotation3.SCMapCollective <number of map tasks> <useLocalMultiThread> <template> <graphDir> <outDir> <num threads per node>");
			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}
		numMapTasks = Integer.parseInt(args[0]);
		useLocalMultiThread = (Integer.parseInt(args[1])) == 0? false: true;
		template = args[2];
		graphDir = args[3];
		outDir = args[4];
		numThreads = Integer.parseInt(args[5]);
		System.out.println("use Local MultiThread? "+useLocalMultiThread);
		System.out.println("set Number of Map Tasks = " + numMapTasks);
		System.out.println("set number of threads: "+numThreads);
		launch();
		return 0;
	}
	private Job configureSCJob( ) throws IOException  {
		Configuration configuration = getConf();
		Job job = Job.getInstance(configuration, "subgraph counting");
		Configuration jobConfig = job.getConfiguration();
		Path jobOutDir = new Path(outDir);
		FileSystem fs = FileSystem.get(configuration);
		if (fs.exists(jobOutDir)) {
			fs.delete(jobOutDir, true);
		}
		FileInputFormat.setInputPaths(job, graphDir);
		FileOutputFormat.setOutputPath(job, jobOutDir);

		//job.setInputFormatClass(KeyValueTextInputFormat.class);
		//use harp multifile input format to have a better control on num of map tasks
		job.setInputFormatClass(MultiFileInputFormat.class);

		job.setJarByClass(SCMapCollective.class);
		job.setMapperClass(SCCollectiveMapper.class);
		JobConf jobConf = (JobConf) job.getConfiguration();
		jobConf.set("mapreduce.framework.name", "map-collective");
		jobConf.setNumMapTasks(numMapTasks);
		jobConf.setInt("mapreduce.job.max.split.locations", 10000);
		job.setNumReduceTasks(0);
		jobConfig.setInt(SCConstants.NUM_MAPPERS, numMapTasks);
		jobConfig.set(SCConstants.TEMPLATE_PATH, template);
		jobConfig.set(SCConstants.OUTPUT_PATH, outDir);
		jobConfig.setBoolean(SCConstants.USE_LOCAL_MULTITHREAD, useLocalMultiThread);
		jobConfig.setInt(SCConstants.NUM_THREADS_PER_NODE,numThreads);

		return job;
	}
	
	public void launch() throws ClassNotFoundException, IOException, InterruptedException{
		boolean jobSuccess = true;
		int jobRetryCount = 0;
		Job scJob = configureSCJob();
		// ----------------------------------------------------------
		jobSuccess =scJob.waitForCompletion(true);
		// ----------------------------------------------------------
	}
	
	public static void main(String[] args) throws Exception {
		
		long begintime = System.currentTimeMillis();
		int res = ToolRunner.run(new Configuration(), new SCMapCollective(), args);
		long endtinme=System.currentTimeMillis();
		long costTime = (endtinme - begintime);
		System.out.println(costTime+"ms"); 
		
		System.exit(res);
	}
}	
