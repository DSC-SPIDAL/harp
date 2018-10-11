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

package edu.iu.daal_naive.densedistri;

import edu.iu.data_aux.HarpDAALConstants;
import edu.iu.data_aux.Initialize;
import edu.iu.data_gen.DataGenerator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class NaiveDaalLauncher extends Configured
implements Tool {

  public static void main(String[] argv)
  throws Exception {
    int res =
    ToolRunner.run(new Configuration(),
      new NaiveDaalLauncher(), argv);
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
	  conf.setInt(HarpDAALConstants.NUM_CLASS, Integer.parseInt(args[init.getSysArgNum()+2]));
    	  conf.set(HarpDAALConstants.TEST_FILE_PATH, args[init.getSysArgNum()+3]);
    	  conf.set(HarpDAALConstants.TEST_TRUTH_PATH, args[init.getSysArgNum()+4]);

	  // config job
	  System.out.println("Starting Job");
	  long perJobSubmitTime = System.currentTimeMillis();
	  System.out.println("Start Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
	  Job naiveJob = init.createJob("naiveJob", NaiveDaalLauncher.class, NaiveDaalCollectiveMapper.class); 

	  // initialize centroids data
	  JobConf thisjobConf = (JobConf) naiveJob.getConfiguration();
	  FileSystem fs = FileSystem.get(conf);
	  int nFeatures = Integer.parseInt(args[init.getSysArgNum()+1]);

	  //generate Data if required
	  boolean generateData = Boolean.parseBoolean(args[init.getSysArgNum()+5]); 
	  if (generateData)
	  {
		  Path inputPath = init.getInputPath();
		  int nClass = Integer.parseInt(args[init.getSysArgNum()+2]);
		  int total_points = Integer.parseInt(args[init.getSysArgNum()+6]);
		  int total_files = Integer.parseInt(args[init.getSysArgNum()+7]);

		  Path testPath = new Path(args[init.getSysArgNum()+3]); 
		  int total_test_points = Integer.parseInt(args[init.getSysArgNum()+8]);
		  Path testGroundTruthPath = new Path(args[init.getSysArgNum()+4]); 
		  String tmpDirPathName = args[init.getSysArgNum()+9];

		  // replace it with naive specific data generator
		  // generate training data
		  DataGenerator.generateDenseDataAndIntLabelMulti(total_points, nFeatures, total_files, 2, 1, nClass, ",", inputPath, tmpDirPathName, fs);
		  // generate test data a single file
		  DataGenerator.generateDenseDataAndIntLabelMulti(total_test_points, nFeatures, 1, 2, 1, nClass, ",", testPath, tmpDirPathName, fs);
		  // generate test groundtruth data a single file nFeature==1
		  DataGenerator.generateDenseLabelMulti(total_test_points, 1, 1, nClass, ",", testGroundTruthPath, tmpDirPathName, fs);
	  }

	  // finish job
	  boolean jobSuccess = naiveJob.waitForCompletion(true);
	  System.out.println("End Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
	  System.out.println("| Job#"  + " Finished in " + (System.currentTimeMillis() - perJobSubmitTime)+ " miliseconds |");
	  if (!jobSuccess) {
		  naiveJob.killJob();
		  System.out.println("naiveJob failed");
	  }

	  return 0;
  }


}
