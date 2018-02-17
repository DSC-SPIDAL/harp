/**
 * subgraph counting for unlabeled graph; 
 * using key value abstraction
 * using rotation
 *
 * This version is based rotation2. But will use static scheduler instead of dynamic scheduler
 */
package edu.iu.subgraph;

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

public class SCLauncher extends Configured implements Tool {

	// int numMapTasks;
	// String template;
	// String graphDir;
	// String outDir;
	// int numThreads;
	// boolean useLocalMultiThread;
	// ArrayList<SCSubJob> subjoblist;

    public static void main(String[] args) throws Exception {
		
		long begintime = System.currentTimeMillis();
		int res = ToolRunner.run(new Configuration(), new SCLauncher(), args);
		long endtinme=System.currentTimeMillis();
		long costTime = (endtinme - begintime);
		System.out.println(costTime+"ms"); 
		
		System.exit(res);
	}

	/*
	 * a template is like:
	    i graph 5
		u5-1 u3-1 u2
		u3-1 i u2
		u2 i i
		final u5-1 5 2
	 */
    @Override
	public int run(String[] args) throws Exception {
		if (args.length < 13) {
			System.err.println("Usage: edu.iu.subgraph.SCLauncher" +
                    " <number of map tasks> "+
                    "<useLocalMultiThread> "+
                    "<template> "+
                    "<graphDir> "+
                    " <outDir> "+
                    "<num threads per node> "+
                    "<num cores per node> "+
                    "<thread affinity> "+
                    "<thread per core> "+
                    "<mem per mapper>"+
                    "<mem per regroup array (MB)>"+
                    "<rotation-pipeline>" +
                    "<num iteration>");

			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}

		int numMapTasks = Integer.parseInt(args[0]);
		boolean useLocalMultiThread = Boolean.parseBoolean(args[1]);
		String template = args[2];
		String graphDir = args[3];
		String outDir = args[4];
		int numThreads = Integer.parseInt(args[5]);
		int numCores = Integer.parseInt(args[6]);
        String affinity = args[7];
        int tpc = Integer.parseInt(args[8]);
        int mem = Integer.parseInt(args[9]);
        int send_array_limit = Integer.parseInt(args[10]);
        boolean rotation_pipeline = Boolean.parseBoolean(args[11]);
        int numIteration = Integer.parseInt(args[12]);

		System.out.println("use Local MultiThread? "+useLocalMultiThread);
		System.out.println("set Number of Map Tasks = " + numMapTasks);
		System.out.println("set number of threads: "+numThreads);
		System.out.println("set threads affinity: "+affinity);
		System.out.println("set number of cores per node: "+numCores);
		System.out.println("set number of thd per core: "+ tpc);
		System.out.println("set mem size per regroup array (MB): "+ send_array_limit);

		launch(graphDir, template, outDir, numMapTasks, useLocalMultiThread, numThreads, numCores, affinity, tpc,  mem, send_array_limit, rotation_pipeline, numIteration);
		return 0;
	}

    public void launch(String graphDir, String template, String outDir, int numMapTasks, 
            boolean useLocalMultiThread, int numThreads, int numCores, String affinity, int tpc, int mem, int send_array_limit, boolean rotation_pipeline, int numIteration) 
        throws ClassNotFoundException, IOException, InterruptedException{

		boolean jobSuccess = true;
		int jobRetryCount = 0;
		Job scJob = configureSCJob(graphDir, template, outDir, 
                numMapTasks, useLocalMultiThread, numThreads, numCores, affinity, tpc, mem, send_array_limit, rotation_pipeline, numIteration);
		// ----------------------------------------------------------
		jobSuccess =scJob.waitForCompletion(true);
		// ----------------------------------------------------------
	}

	private Job configureSCJob(String graphDir, String template, String outDir, int numMapTasks, 
            boolean useLocalMultiThread, int numThreads, int numCores, String affinity, int tpc, int mem, int send_array_limit, 
            boolean rotation_pipeline, int numIteration) throws IOException  
    {

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

		job.setJarByClass(SCLauncher.class);
		job.setMapperClass(SCCollectiveMapper.class);
		JobConf jobConf = (JobConf) job.getConfiguration();

		jobConf.set("mapreduce.framework.name", "map-collective");

        // mapreduce.map.collective.memory.mb
        // 125000
        jobConf.setInt("mapreduce.map.collective.memory.mb", mem);
        // mapreduce.map.collective.java.opts
        // -Xmx120000m -Xms120000m
        int xmx = (mem - 5000) > (mem * 0.9)
            ? (mem - 5000) : (int) Math.ceil(mem * 0.9);
        int xmn = (int) Math.ceil(0.25 * xmx);
        jobConf.set(
                "mapreduce.map.collective.java.opts",
                "-Xmx" + xmx + "m -Xms" + xmx + "m"
                + " -Xmn" + xmn + "m");

        jobConf.setNumMapTasks(numMapTasks);
		jobConf.setInt("mapreduce.job.max.split.locations", 10000);

        jobConf.setInt("mapreduce.task.timeout", 60000000);

		job.setNumReduceTasks(0);

		jobConfig.setInt(SCConstants.NUM_MAPPERS, numMapTasks);

		jobConfig.set(SCConstants.TEMPLATE_PATH, template);

		jobConfig.set(SCConstants.OUTPUT_PATH, outDir);

		jobConfig.setBoolean(SCConstants.USE_LOCAL_MULTITHREAD, useLocalMultiThread);

		jobConfig.setInt(SCConstants.NUM_THREADS_PER_NODE,numThreads);

		jobConfig.setInt(SCConstants.THREAD_NUM,numThreads);
		jobConfig.setInt(SCConstants.CORE_NUM, numCores);
		jobConfig.set(SCConstants.THD_AFFINITY,affinity);
		jobConfig.setInt(SCConstants.TPC, tpc);
		jobConfig.setInt(SCConstants.SENDLIMIT, send_array_limit);

		jobConfig.setBoolean(SCConstants.ROTATION_PIPELINE, rotation_pipeline);
		jobConfig.setInt(SCConstants.NUM_ITERATION, numIteration);

		return job;
	}
	
	
	
	
}	
