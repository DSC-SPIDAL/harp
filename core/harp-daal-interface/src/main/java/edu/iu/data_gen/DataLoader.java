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

package edu.iu.data_gen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.util.LinkedList;
import java.util.List;

public final class DataLoader
{

	private DataLoader() {
	}

	/**
	 * Load data points from a file.
	 * deprecated
	 * To be removed
	 * 
	 * @param file
	 * @param conf
	 * @return
	 * @throws IOException
	 */
	public static double[] loadPoints(String file,
			int pointsPerFile, int cenVecSize,
			Configuration conf) throws Exception {
		double[] points =
			new double[pointsPerFile * cenVecSize];
		Path pointFilePath = new Path(file);
		FileSystem fs =
			pointFilePath.getFileSystem(conf);
		FSDataInputStream in = fs.open(pointFilePath);
		try {
			for (int i = 0; i < points.length;) {
				for (int j = 0; j < cenVecSize; j++) {
					points[i++] = in.readDouble();
				}
			}
		} finally {
			in.close();
		}
		return points;
	}

	/**
	 * @brief read in data points with seperator ","
	 * each line contains a dense vector with length cenVecSize
	 *
	 * @param file
	 * @param cenVecSize
	 * @param conf
	 *
	 * @return 
	 */
	public static double[] loadPointsMMDense(String file, int cenVecSize,
			Configuration conf) throws Exception {

		System.out.println("filename: "+file );
		List<double[]> points = new LinkedList<double[]>();

		Path pointFilePath = new Path(file);
		FileSystem fs =
			pointFilePath.getFileSystem(conf);
		FSDataInputStream in = fs.open(pointFilePath);
		String readline = null;
		double[] data_points = null;

		try{

			while ((readline = in.readLine()) != null)
			{
				String[] line = readline.split(",");
				double[] trainpoint = new double[cenVecSize];
				for(int j = 0; j < cenVecSize; j++)
					trainpoint[j] = Double.parseDouble(line[j]);

				points.add(trainpoint);
			}

			int num_array = points.size();
			data_points = new double[num_array*cenVecSize];
			for(int i=0; i<num_array; i++)
				System.arraycopy(points.get(i), 0, data_points, i*cenVecSize, cenVecSize);


			points = null;

		} finally{
			in.close();
		}

		return data_points;
	}

	public static List<double[][]> loadPointsMMDense2(String file, int cenVecSize, int shadsize,
			Configuration conf) throws Exception 
	{

		System.out.println("filename: "+file );
		System.out.println("Shad size: "+ shadsize);

		int shadptr = 0;
		double[][] points = new double[shadsize][]; 

		List<double[][]> outputlist = new LinkedList<double[][]>();

		Path pointFilePath = new Path(file);
		FileSystem fs =
			pointFilePath.getFileSystem(conf);
		FSDataInputStream in = fs.open(pointFilePath);
		String readline = null;

		try{

			while ((readline = in.readLine()) != null)
			{
				String[] line = readline.split(",");
				double[] trainpoint = new double[cenVecSize];
				for(int j = 0; j < cenVecSize; j++)
					trainpoint[j] = Double.parseDouble(line[j]);

				// points.add(trainpoint);
				points[shadptr++] = trainpoint;

				if (shadptr == shadsize)
				{
					outputlist.add(points);
					points = new double[shadsize][];
					shadptr = 0;
				}

			}

			

			// int num_array = points.size();
			// data_points = new double[num_array*cenVecSize];
			// for(int i=0; i<num_array; i++)
			// 	System.arraycopy(points.get(i), 0, data_points, i*cenVecSize, cenVecSize);


			// points = null;

		} finally{
			in.close();
		}

		if (shadptr > 0)
		{
		   //compress the last shad
		   double[][] lastshad = new double[shadptr][];
		   for(int j=0;j<shadptr;j++)
			   lastshad[j] = points[j];

		   outputlist.add(lastshad);
		   points = null;
		}

		return outputlist;
	}
}
