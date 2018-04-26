/*
 * Copyright 2013-2018 Indiana University
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

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStreamReader;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.ListIterator;

import edu.iu.dymoro.*;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Array;
import edu.iu.harp.schstatic.StaticScheduler;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.hadoop.conf.Configuration;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

public class HarpDAALDataSource
{
   private List<String> hdfs_filenames;
   private int harpthreads; // threads used in parallel data reading/transfer
   private List<double[]>[] datalist;
   private Configuration conf; 
   private int dim;
   private int totallines;
   private int totalPoints;
   private double[] testData;
   private int numTestRows;

   protected static final Log LOG = LogFactory
		.getLog(HarpDAALDataSource.class);

   public HarpDAALDataSource(List<String> hdfs_filenames, int dim, int harpthreads, Configuration conf)
   {
	this.hdfs_filenames = hdfs_filenames;
	this.dim = dim;
	this.harpthreads = harpthreads;
	this.conf = conf;
	this.datalist = null;
	this.totallines = 0;
	this.totalPoints = 0;
	this.testData = null;
	this.numTestRows = 0;
   }

   public void loadFiles()
   {
     	MTReader reader = new MTReader();
	this.datalist = reader.readfiles(this.hdfs_filenames, this.dim, this.conf, this.harpthreads); 
	this.totallines = reader.getTotalLines();
	this.totalPoints = reader.getTotalPoints();
   }

   public int getTotalLines()
   {
	   return this.totallines;
   }

   public int getTestRows()
   {
	   return this.numTestRows;
   }

   /**
    * @brief loading data from source to dst daal table
    *
    * @return 
    */
   public void loadDataBlock(NumericTable dst_table)
   {
      // check the datalist obj
      if (this.datalist == null)
      {
	 LOG.info("Error no hdfs data to load");
	 return;
      }

      //copy block of rows from this.datalist to DAAL NumericTable 
      int rowIdx = 0; 
      for(int i=0;i<this.datalist.length;i++)
      {
	 List<double[]> elemList = this.datalist[i];
	 double[] rowblocks = new double[elemList.size()*this.dim]; 
	 for (int j=0;j<elemList.size();j++)
 		System.arraycopy(elemList.get(j), 0, rowblocks, j*dim, dim);

	 //copy rowblock to NumericTable
	 dst_table.releaseBlockOfRows(rowIdx, elemList.size(), DoubleBuffer.wrap(rowblocks));
	 rowIdx += elemList.size();
      }

      this.datalist = null;
   }

   // public void loadTestFile(NumericTable testTable, String inputFiles, int vectorSize) throws IOException
   public void loadTestFile(String inputFiles, int vectorSize) throws IOException
   {

	   Path inputFilePaths = new Path(inputFiles);
	   List<String> inputFileList = new LinkedList<>();

	   try {
		   FileSystem fs =
			   inputFilePaths.getFileSystem(conf);
		   RemoteIterator<LocatedFileStatus> iterator =
			   fs.listFiles(inputFilePaths, true);

		   while (iterator.hasNext()) {
			   String name =
				   iterator.next().getPath().toUri()
				   .toString();
			   inputFileList.add(name);
		   }

	   } catch (IOException e) {
		   LOG.error("Fail to get test files", e);
	   }

	   List<double[]> points = new LinkedList<double[]>();
	   
	   FSDataInputStream in = null;

	   //loop over all the files in the list
	   ListIterator<String> file_itr = inputFileList.listIterator();
	   while (file_itr.hasNext())
	   {
		   String file_name = file_itr.next();
		   LOG.info("read in file name: " + file_name);

		   Path file_path = new Path(file_name);
		   try {

			   FileSystem fs =
				   file_path.getFileSystem(conf);
			   in = fs.open(file_path);

		   } catch (Exception e) {
			   LOG.error("Fail to open file "+ e.toString());
			   return;
		   }

		   //read file content
		   while(true)
		   {
			   String line = in.readLine();
			   if (line == null) break;

			   String[] lineData = line.split(",");
			   double[] cell = new double[vectorSize];

			   for(int t =0 ; t< vectorSize; t++)
			      cell[t] = Double.parseDouble(lineData[t]);

			   points.add(cell);
		   }

		   in.close();
	   }

	   //copy points to block of data
	   int dataSize = vectorSize*points.size();
	   double[] data = new double[dataSize];

	   for(int i=0; i< points.size(); i++)
		System.arraycopy(points.get(i), 0, data, i*vectorSize, vectorSize);

	   this.testData = data;
	   this.numTestRows = points.size();
	   //copy data to daal table
	   // testTable.releaseBlockOfRows(0, points.size(), DoubleBuffer.wrap(data));
	   points = null;

   }

   public void loadTestTable(NumericTable testTable)
   {
      testTable.releaseBlockOfRows(0, this.numTestRows, DoubleBuffer.wrap(this.testData));
   }

}

