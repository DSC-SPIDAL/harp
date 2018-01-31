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

package edu.iu.daal_naive;

import java.io.IOException;
import java.lang.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.util.LinkedList;
import java.util.List;

import edu.iu.harp.schdynamic.Task;

public class PointLoadTask implements
  Task<String, List<double[]>> {

  protected static final Log LOG = LogFactory
    .getLog(PointLoadTask.class);

  // private int pointsPerFile;
  private int cenVecSize;
  private Configuration conf;

  public PointLoadTask(int cenVecSize, Configuration conf) {
    // this.pointsPerFile = pointsPerFile;
    this.cenVecSize = cenVecSize;
    this.conf = conf;
  }

  @Override
  public List<double[]> run(String fileName)
    throws Exception {
    long threadId = Thread.currentThread().getId();
    int count = 0;
    boolean isSuccess = false;
    do {
      try {
        List<double[]> array = loadPoints(fileName, cenVecSize, conf);
        return array;
      } catch (Exception e) {
        LOG.error("load " + fileName
          + " fails. Count=" + count, e);
        Thread.sleep(100);
        isSuccess = false;
        count++;
      }
    } while (!isSuccess && count < 100);
    LOG.error("Fail to load files.");
    return null;
  }

  // create a loadPoints that ignores the points per file
  public static List<double[]> loadPoints(String file, int cenVecSize,
    Configuration conf) throws Exception {

    System.out.println("filename: "+file );
    List<double[]> points = new LinkedList<double[]>();

	List<Double> labelData = new LinkedList<Double>();

    Path pointFilePath = new Path(file);
    FileSystem fs =
      pointFilePath.getFileSystem(conf);
    FSDataInputStream in = fs.open(pointFilePath);
	String readline = null;

    int k =0;
    try{

	  while ((readline = in.readLine()) != null)
	  {
        String[] line = readline.split(",");
		double[] trainpoint = new double[cenVecSize];
        for(int j = 0; j < cenVecSize; j++)
          trainpoint[j] = Double.parseDouble(line[j]);

		points.add(trainpoint);
		labelData.add(Double.parseDouble(line[cenVecSize]));

	  }

	  //convert labelData to primitive array
	  double[] labelDataArray = new double[labelData.size()];
	  for(int j=0;j<labelData.size();j++)
		  labelDataArray[j] = labelData.get(j);

      points.add(labelDataArray);

    } finally{
      in.close();
    }
    return points;
  }

  /**
   * @brief deprecated load in function
   *
   * @param file
   * @param pointsPerFile
   * @param cenVecSize
   * @param conf
   *
   * @return 
   */
  public static List<double[]> loadPointsOld(String file,
    int pointsPerFile, int cenVecSize,
    Configuration conf) throws Exception {
    System.out.println("filename: "+file );
    List<double[]> points = new LinkedList<double[]>();
    double[] trainingData =
      new double[pointsPerFile * cenVecSize];
      double[] labelData =
      new double[pointsPerFile * 1]; 
    Path pointFilePath = new Path(file);
    FileSystem fs =
      pointFilePath.getFileSystem(conf);
    FSDataInputStream in = fs.open(pointFilePath);
    int k =0;
    try{
      for(int i = 0; i < pointsPerFile;i++){
        String[] line = in.readLine().split(",");
        for(int j = 0; j < cenVecSize; j++){
          trainingData[k] = Double.parseDouble(line[j]);
          k++;
        }
        labelData[i] = Double.parseDouble(line[cenVecSize]);
      }
    } finally{
      in.close();
      points.add(trainingData);
      points.add(labelData);
    }
    return points;
  }
}
