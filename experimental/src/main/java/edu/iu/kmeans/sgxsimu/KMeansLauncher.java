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

package edu.iu.kmeans.sgxsimu;

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

import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class KMeansLauncher extends Configured implements Tool 
{

	public static void main(String[] argv) throws Exception 
	{
		int res = ToolRunner.run(new Configuration(), new KMeansLauncher(), argv);
		System.exit(res);
	}

	/**
	 * Launches all the tasks in order.
	 */
	@Override
	public int run(String[] args) throws Exception 
	{

		/* Put shared libraries into the distributed cache */
		Configuration conf = this.getConf();

		Initialize init = new Initialize(conf, args);

		// load args
		init.loadSysArgs();

      		init.loadDistributedLibs();

		//load app args
		conf.setInt(HarpDAALConstants.FILE_DIM, Integer.parseInt(args[init.getSysArgNum()]));
		conf.setInt(HarpDAALConstants.FEATURE_DIM, Integer.parseInt(args[init.getSysArgNum()+1]));
		conf.setInt(HarpDAALConstants.NUM_CENTROIDS, Integer.parseInt(args[init.getSysArgNum()+2]));
		conf.setInt(Constants.ENCLAVE_TOTAL, Integer.parseInt(args[init.getSysArgNum()+3]));
		conf.setInt(Constants.ENCLAVE_PER_THD, Integer.parseInt(args[init.getSysArgNum()+4]));
		conf.setInt(Constants.ENCLAVE_TASK, Integer.parseInt(args[init.getSysArgNum()+5]));
		conf.setBoolean(Constants.ENABLE_SIMU, Boolean.parseBoolean(args[init.getSysArgNum()+6]));

		// config job
		System.out.println("Starting Job");
		long perJobSubmitTime = System.currentTimeMillis();
		System.out.println("Start Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
		Job kmeansJob = init.createJob("kmeansJob", KMeansLauncher.class, KMeansCollectiveMapper.class); 

		// initialize centroids data
		JobConf thisjobConf = (JobConf) kmeansJob.getConfiguration();
		FileSystem fs = FileSystem.get(conf);
		int nFeatures = Integer.parseInt(args[init.getSysArgNum()+1]);
		int numCentroids = Integer.parseInt(args[init.getSysArgNum()+2]);
		Path workPath = init.getWorkPath();
		Path cenDir = new Path(workPath, "centroids");
		fs.mkdirs(cenDir);
		if (fs.exists(cenDir)) {
			fs.delete(cenDir, true);
		}

		Path initCenDir = new Path(cenDir, "init_centroids");
		DataGenerator.generateDenseDataSingle(numCentroids, nFeatures, 1000, 0, " ", initCenDir, fs);
		thisjobConf.set(HarpDAALConstants.CEN_DIR, cenDir.toString());
		thisjobConf.set(HarpDAALConstants.CENTROID_FILE_NAME, "init_centroids");

		//generate Data if required
		boolean generateData = Boolean.parseBoolean(args[init.getSysArgNum()+7]); 
		if (generateData)
		{
			Path inputPath = init.getInputPath();
			int total_points = Integer.parseInt(args[init.getSysArgNum()+8]);
			int total_files = Integer.parseInt(args[init.getSysArgNum()+9]);
			String tmpDirPathName = args[init.getSysArgNum()+10];

			DataGenerator.generateDenseDataMulti(total_points, nFeatures, total_files, 2, 1, ",", inputPath, tmpDirPathName, fs);
		}

		// finish job
		boolean jobSuccess = kmeansJob.waitForCompletion(true);
		System.out.println("End Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
		System.out.println("| Job#"  + " Finished in " + (System.currentTimeMillis() - perJobSubmitTime)+ " miliseconds |");
		if (!jobSuccess) {
			kmeansJob.killJob();
			System.out.println("kmeansJob failed");
		}
		
		return 0;
	}

}
