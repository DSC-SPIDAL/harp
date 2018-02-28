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

package edu.iu.daal_linreg;

import edu.iu.harp.schdynamic.DynamicScheduler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.util.LinkedList;
import java.util.List;


public class LinRegUtil{
	protected static final Log LOG = LogFactory
    	.getLog(LinRegUtil.class);

    public static List<List<double[]>> loadPoints(
    List<String> fileNames, int pointsPerFile,
    int cenVecSize, int labelSize, Configuration conf,
    int numThreads) {
    long startTime = System.currentTimeMillis();
    List<PointLoadTask> tasks =
      new LinkedList<>();
    List<List<double[]>> arrays = new LinkedList<List<double[]>>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new PointLoadTask(pointsPerFile,
        cenVecSize, labelSize, conf));
    }
    DynamicScheduler<String, List<double[]>, PointLoadTask> compute =
      new DynamicScheduler<>(tasks);
    for (String fileName : fileNames) {
      compute.submit(fileName);
    }
    compute.start();
    compute.stop();
    while (compute.hasOutput()) {
      List<double[]> output = compute.waitForOutput();
      if (output != null) {
        arrays.add(output);
      }
    }
    long endTime = System.currentTimeMillis();
    System.out.println("File read (ms): "
      + (endTime - startTime)
      + ", number of point arrays: "
      + arrays.size());
    return arrays;
  } 
}
