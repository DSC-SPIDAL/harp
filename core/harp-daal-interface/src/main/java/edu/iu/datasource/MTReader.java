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

package edu.iu.datasource;

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

public class MTReader{

	protected static final Log LOG = LogFactory
		.getLog(MTReader.class);

	private int totalLine;
	private int totalPoints;

	public MTReader()
	{
	   this.totalLine = 0;
	   this.totalPoints = 0;
	}

	/**
	 * @brief read files from HDFS in parallel (multi-threading)
	 * file format is dense vector (matrix)
	 *
	 * @param fileNames: filenames in HDFS
	 * @param dim  
	 * @param conf
	 * @param numThreads
	 *
	 * @return 
	 */
	public List<double[]>[] readfiles(
			List<String> fileNames,
			int dim, Configuration conf,
			int numThreads) {

		List<ReadParaTask> tasks = new LinkedList<>();
		List<double[]>[] arrays = new List[fileNames.size()];

		for (int i = 0; i < numThreads; i++) {
		   tasks.add(new ReadParaTask(dim, conf));
		}

		DynamicScheduler<String, List<double[]>, ReadParaTask> compute =
			new DynamicScheduler<>(tasks);

		for (String fileName : fileNames) {
			compute.submit(fileName);
		}

		compute.start();
		compute.stop();

		int addidex = 0;
		this.totalLine = 0;
		this.totalPoints = 0;
		while (compute.hasOutput()) {

			List<double[]> output = compute.waitForOutput();
			if (output != null) {
				arrays[addidex++] = output;
				totalLine += output.size();
				totalPoints += output.size()*dim;
			}
		}

		return arrays;
	} 

	public int getTotalLines() {return this.totalLine; }
	public int getTotalPoints() {return this.totalPoints; }

}
