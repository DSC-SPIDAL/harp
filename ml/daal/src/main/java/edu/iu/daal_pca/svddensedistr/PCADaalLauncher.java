/*
 * Copyright 2013-2016 Indiana University
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

package edu.iu.daal_pca.svddensedistr;

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

import org.apache.hadoop.filecache.DistributedCache;
import java.net.URI;

import edu.iu.fileformat.MultiFileInputFormat;
import edu.iu.datasource.*;
import edu.iu.data_aux.*;
import edu.iu.data_comm.*;
import edu.iu.data_gen.*;

public class PCADaalLauncher extends Configured
  implements Tool {

  public static void main(String[] argv) throws Exception
  {
    int res = ToolRunner.run(new Configuration(), new PCADaalLauncher(), argv);
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

	  /* Put shared libraries into the distributed cache */
	  init.loadDistributedLibs();

	  // load args
	  init.loadSysArgs();

	  //load app args
	  conf.setInt(HarpDAALConstants.FILE_DIM, Integer.parseInt(args[init.getSysArgNum()]));
	  conf.setInt(HarpDAALConstants.FEATURE_DIM, Integer.parseInt(args[init.getSysArgNum()+1]));

	  // config job
	  System.out.println("Starting Job");
	  long perJobSubmitTime = System.currentTimeMillis();
	  System.out.println("Start Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
	  Job pcaJob = init.createJob("pcaJob", PCADaalLauncher.class, PCADaalCollectiveMapper.class); 

	  // initialize centroids data
	  JobConf thisjobConf = (JobConf) pcaJob.getConfiguration();
	  FileSystem fs = FileSystem.get(conf);
	  int nFeatures = Integer.parseInt(args[init.getSysArgNum()+1]);
	  Path workPath = init.getWorkPath();

	  //generate Data if required
	  boolean generateData = Boolean.parseBoolean(args[init.getSysArgNum()+2]); 
	  if (generateData)
	  {
		  Path inputPath = init.getInputPath();
		  int total_points = Integer.parseInt(args[init.getSysArgNum()+3]);
		  int total_files = Integer.parseInt(args[init.getSysArgNum()+4]);
		  String tmpDirPathName = args[init.getSysArgNum()+5];

		  DataGenerator.generateDenseDataMulti(total_points, nFeatures, total_files, 2, 1, ",", inputPath, tmpDirPathName, fs);
	  }

	  // finish job
	  boolean jobSuccess = pcaJob.waitForCompletion(true);
	  System.out.println("End Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
	  System.out.println("| Job#"  + " Finished in " + (System.currentTimeMillis() - perJobSubmitTime)+ " miliseconds |");
	  if (!jobSuccess) {
		  pcaJob.killJob();
		  System.out.println("pcaJob failed");
	  }

	  return 0;
  }


}
