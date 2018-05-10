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

package edu.iu.daal_mom.densedistri;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;


public class MOMUtil{
	protected static final Log LOG = LogFactory
    	.getLog(MOMUtil.class);

    public static List<double[]> loadPoints(
    List<String> fileNames, int pointsPerFile,
    int cenVecSize, Configuration conf,
    int numThreads) {
    long startTime = System.currentTimeMillis();
    List<PointLoadTask> tasks =
      new LinkedList<>();
    List<double[]> arrays = new LinkedList<double[]>();
    for (int i = 0; i < numThreads; i++) {
      tasks.add(new PointLoadTask(pointsPerFile,
        cenVecSize, conf));
    }
    DynamicScheduler<String, double[], PointLoadTask> compute =
      new DynamicScheduler<>(tasks);
    for (String fileName : fileNames) {
      compute.submit(fileName);
    }
    compute.start();
    compute.stop();
    while (compute.hasOutput()) {
      double[] output = compute.waitForOutput();
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

















































