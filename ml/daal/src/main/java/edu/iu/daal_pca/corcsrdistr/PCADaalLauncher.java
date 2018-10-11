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

package edu.iu.daal_pca.corcsrdistr;

import edu.iu.data_aux.Initialize;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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

	  // launch job
	  System.out.println("Starting Job");
	  long perJobSubmitTime = System.currentTimeMillis();
	  System.out.println("Start Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));

	  Job pcaJob = init.createJob("pcaJob", PCADaalLauncher.class, PCADaalCollectiveMapper.class); 

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
