/*
 * Copyright 2013-2017 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.daal_subgraph;

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

import org.apache.hadoop.filecache.DistributedCache;
import java.net.URI;

import java.io.IOException;
import java.util.ArrayList;

public class SCDaalLauncher extends Configured implements Tool {

	// int numMapTasks;
	// String template;
	// String graphDir;
	// String outDir;
	// int numThreads;
	// boolean useLocalMultiThread;
	// ArrayList<SCSubJob> subjoblist;

    public static void main(String[] args) throws Exception {
		
		long begintime = System.currentTimeMillis();
		int res = ToolRunner.run(new Configuration(), new SCDaalLauncher(), args);
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

		Configuration configuration = getConf();
        //load in dynamic libs for harp-daal modules
        DistributedCache.createSymlink(configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libJavaAPI.so#libJavaAPI.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so.2#libtbb.so.2"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so#libtbb.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so.2#libtbbmalloc.so.2"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so#libtbbmalloc.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libiomp5.so#libiomp5.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so#libhdfs.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so.0.0.0#libhdfs.so.0.0.0"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libautohbw.so#libautohbw.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libmemkind.so#libmemkind.so"), configuration);
        DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libmemkind.so.0#libmemkind.so.0"), configuration);

		if (args.length < 16) {
			System.err.println("Usage: edu.iu.subgraph.SCDaalLauncher" +
                    " <number of map tasks> "+
                    "<useLocalMultiThread> "+
                    "<template> "+
                    "<graphDir> "+
                    " <outDir> "+
                    "<num threads per node> "+
                    "<num cores per node> "+
                    "<thread affinity> "+
                    "<omp schedule>" +
                    "<thread per core> "+
                    "<mem per mapper>"+
                    "<mem java ratio>"+
                    "<mem per regroup array (MB)>"+
                    "<len per nbr split task>"+
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
        String omp_opt = args[8];
        int tpc = Integer.parseInt(args[9]);
        int mem = Integer.parseInt(args[10]);
        double memjavaratio = Double.parseDouble(args[11]);
        int send_array_limit = Integer.parseInt(args[12]);
        int nbr_split_len = Integer.parseInt(args[13]);
        boolean rotation_pipeline = Boolean.parseBoolean(args[14]);
        int numIteration = Integer.parseInt(args[15]);

		System.out.println("use Local MultiThread? "+useLocalMultiThread);
		System.out.println("set Number of Map Tasks = " + numMapTasks);
		System.out.println("set number of threads: "+numThreads);
		System.out.println("set threads affinity: "+affinity);
		System.out.println("set omp scheduler: "+omp_opt);
		System.out.println("set number of cores per node: "+numCores);
		System.out.println("set number of thd per core: "+ tpc);
		System.out.println("set mem size per regroup array (MB): "+ send_array_limit);
		System.out.println("set len of split nbr task: "+ nbr_split_len);

		launch(graphDir, template, outDir, numMapTasks, useLocalMultiThread, numThreads, numCores, affinity, 
                omp_opt, tpc,  mem, memjavaratio, send_array_limit, nbr_split_len, rotation_pipeline, numIteration);
		return 0;
	}

    public void launch(String graphDir, String template, String outDir, int numMapTasks, 
            boolean useLocalMultiThread, int numThreads, int numCores, String affinity, String omp_opt, int tpc, int mem, double memjavaratio, int send_array_limit, int nbr_split_len, boolean rotation_pipeline, int numIteration) 
        throws ClassNotFoundException, IOException, InterruptedException{

		boolean jobSuccess = true;
		int jobRetryCount = 0;
		Job scJob = configureSCJob(graphDir, template, outDir, 
                numMapTasks, useLocalMultiThread, numThreads, numCores, affinity, omp_opt, tpc, mem, memjavaratio, send_array_limit, nbr_split_len, rotation_pipeline, numIteration);
		// ----------------------------------------------------------
		jobSuccess =scJob.waitForCompletion(true);
		// ----------------------------------------------------------
	}

	private Job configureSCJob(String graphDir, String template, String outDir, int numMapTasks, 
            boolean useLocalMultiThread, int numThreads, int numCores, String affinity, 
            String omp_opt, int tpc, int mem, double memjavaratio,int send_array_limit, int nbr_split_len, 
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

		job.setJarByClass(SCDaalLauncher.class);
		job.setMapperClass(SCDaalCollectiveMapper.class);
		JobConf jobConf = (JobConf) job.getConfiguration();

		jobConf.set("mapreduce.framework.name", "map-collective");

        // mapreduce.map.collective.memory.mb
        // 125000
        jobConf.setInt("mapreduce.map.collective.memory.mb", mem);
        // mapreduce.map.collective.java.opts
        // -Xmx120000m -Xms120000m
        // int xmx = (mem - 5000) > (mem * 0.9)
        //     ? (mem - 5000) : (int) Math.ceil(mem * 0.5);
        // int xmx = (int) Math.ceil((mem - 5000)*0.2);
        int xmx = (int) Math.ceil((mem - 5000)*memjavaratio);
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
		jobConfig.set(SCConstants.OMPSCHEDULE,omp_opt);
		jobConfig.setInt(SCConstants.TPC, tpc);
		jobConfig.setInt(SCConstants.SENDLIMIT, send_array_limit);
		jobConfig.setInt(SCConstants.NBRTASKLEN, nbr_split_len);

		jobConfig.setBoolean(SCConstants.ROTATION_PIPELINE, rotation_pipeline);
		jobConfig.setInt(SCConstants.NUM_ITERATION, numIteration);

		return job;
	}
	
	
	
	
}	
