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
import java.util.HashMap;
import java.util.Map;

import edu.iu.dymoro.*;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.resource.Array;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.ByteArray;
import edu.iu.harp.schstatic.StaticScheduler;
import edu.iu.data_aux.*;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.*;
import org.apache.hadoop.mapred.CollectiveMapper;
import org.apache.hadoop.conf.Configuration;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.CSRNumericTable;
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
   private double[] csrlabels;

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
	this.csrlabels = null;
   }

   /**
    * @brief harpdaal data source with unspecified 
    * feature dimension
    *
    * @param hdfs_filenames
    * @param harpthreads
    * @param conf
    *
    * @return 
    */
   public HarpDAALDataSource(List<String> hdfs_filenames, int harpthreads, Configuration conf)
   {
	this.hdfs_filenames = hdfs_filenames;
	this.dim = 0;
	this.harpthreads = harpthreads;
	this.conf = conf;
	this.datalist = null;
	this.totallines = 0;
	this.totalPoints = 0;
	this.testData = null;
	this.numTestRows = 0;
	this.csrlabels = null;
   }

   public List<COO> loadCOOFiles(String regex)
   {//{{{
      	MTReader reader = new MTReader();
	List<COO> output = reader.readCOO(this.hdfs_filenames, regex, this.conf, this.harpthreads);
	this.totallines = reader.getTotalLines();
	this.totalPoints = reader.getTotalPoints();
	return output;
   }//}}}

   public List<COO> loadCOOFiles(String FilePath, String regex)
   {//{{{
	   List<String> FilePathsList = new LinkedList<>();
	   Path path = new Path(FilePath);
	   try {
		   FileSystem fs =
			   path.getFileSystem(this.conf);
		   RemoteIterator<LocatedFileStatus> iterator =
			   fs.listFiles(path, true);
		   while (iterator.hasNext()) {
			   String name =
				   iterator.next().getPath().toUri()
				   .toString();
			   FilePathsList.add(name);
		   }
	   } catch (IOException e) {
		   LOG.error("Fail to get test files", e);
	   }

	   MTReader reader = new MTReader();
	   List<COO> output = reader.readCOO(FilePathsList, regex, this.conf, this.harpthreads);
	   this.totallines = reader.getTotalLines();
	   this.totalPoints = reader.getTotalPoints();
	   return output;
   }//}}}

   public HashMap<Long, COOGroup> groupCOOByIDs(List<COO> inputData, boolean isRow)
   {//{{{
	   //regroup by multi-threading
      	   MTReader reader = new MTReader();
	   HashMap<Long, COOGroup> output_map = reader.regroupCOO(inputData, isRow, this.conf, this.harpthreads);
	   return output_map;
   }//}}}

   public HashMap<Long, Integer> remapCOOIDs(HashMap<Long, COOGroup> input_map, int mapper_id, int num_mappers, CollectiveMapper mapper)
   {//{{{

	   long[] inputIDs = new long[input_map.keySet().size()]; 
	   int id_itr = 0;
	   for(Long key : input_map.keySet())
		   inputIDs[id_itr++] = key;

	   Table<LongArray> RowInfo_Table = new Table<>(0, new LongArrPlus());
	   LongArray RowInfo_array = new LongArray(inputIDs, 0, inputIDs.length); 

	   RowInfo_Table.addPartition(new Partition<>(mapper_id, RowInfo_array));
	   mapper.allgather("coo", "remap_ids", RowInfo_Table);

	   long[][] row_ids_all = new long[num_mappers][];

	   for(int j=0;j<num_mappers; j++)
	   {
		   row_ids_all[j] = RowInfo_Table.getPartition(j).get().get();
	   }

	   HashMap<Long, Integer> row_mapping = new HashMap<>();
	   int row_pos = 1; //CSR pos start from 1
	   for(int j=0;j<num_mappers;j++)
	   {
		   for(int k=0;k<row_ids_all[j].length;k++)
		   {
			   if (row_mapping.get(row_ids_all[j][k]) == null)
			   {
				   row_mapping.put(row_ids_all[j][k], row_pos);
				   row_pos++;
			   }
			   
		   }
	   }

	   return row_mapping;
   }//}}}

   public Table<COOGroup> regroupCOOList(HashMap<Long,COOGroup> input_map, HashMap<Long, Integer> remap_idx, CollectiveMapper mapper, int[] maxCompactRowID)
   {//{{{
           int maxRowID = -1;
           Table<COOGroup> regroup_table = new Table<>(0, new COOGroupCombiner(), input_map.size());

	   for(Map.Entry<Long, COOGroup> entry : input_map.entrySet())
	   {
		   Long key = entry.getKey();
		   COOGroup val = entry.getValue();
		   int remapID = remap_idx.get(key);
		   regroup_table.addPartition(new Partition<>(remapID, val));
		   if (remapID > maxRowID)
			   maxRowID = remapID;

	   }

	   int num_par_prev = regroup_table.getNumPartitions();
	   Table<IntArray> maxRowTable = new Table<>(0, new IntArrMax());

	   IntArray maxrowArray = IntArray.create(1, false);
	   maxrowArray.get()[0] = maxRowID;
	   maxRowTable.addPartition(new Partition<>(0, maxrowArray));

	   mapper.allreduce("coo", "get-max-rowID", maxRowTable);

	   // from local max row id to global max row id
	   maxRowID = maxRowTable.getPartition(0).get().get()[0];
	   maxRowTable.release();
	   maxRowTable = null;
	   LOG.info("Num pars before regroup " + num_par_prev + ", global compact maxRowID " + maxRowID);
	   maxCompactRowID[0] = maxRowID;

           mapper.regroup("coo", "regroup-coo", regroup_table, new COORegroupPartitioner(maxRowID, mapper.getNumWorkers()));
	   mapper.barrier("coo", "finish-regroup");

	   int num_par_cur = regroup_table.getNumPartitions();
	   LOG.info("Num pars after regroup " + num_par_cur);
	   return regroup_table;
   }//}}}

   public CSRNumericTable COOToCSR(Table<COOGroup> inputTable, HashMap<Long, Integer> remapIDs, DaalContext daal_Context)
   {//{{{
	   int num_pars = inputTable.getNumPartitions();
	   int num_cols = 0;
	   int num_vals = 0;
	   long[] rowOffset = new long[num_pars+1];
	   rowOffset[0] = 1;

	   //here partition id starts from 1
	   IntArray idArray = IntArray.create(num_pars, false);
	   inputTable.getPartitionIDs().toArray(idArray.get());
	   Arrays.sort(idArray.get(), 0, idArray.size());
	   int[] ids = idArray.get();

	   // write the rowoffset
	   for (int i = 0; i < idArray.size(); i++) 
	   {
		   COOGroup elem = inputTable.getPartition(ids[i]).get();
		   rowOffset[i+1] = rowOffset[i] + elem.getNumEntry(); 
		   num_cols += elem.getNumEntry();
	   }

	   num_vals =  num_cols;

	   long[] colIndex = new long[num_cols];
	   double[] data = new double[num_vals];

	   int itr_pos = 0;
	   int itr_ids = 0;
	   long maxCol = 0;

	   //write colIndex and CSR data
	   for (int i = 0; i < idArray.size(); i++) 
	   {
		   COOGroup elem_g = inputTable.getPartition(ids[i]).get();
		   long[] Ids = elem_g.getIds();
		   int num_entry = elem_g.getNumEntry(); 

		   for(int j=0;j<num_entry;j++)
		   {
			   colIndex[itr_ids] = remapIDs.get(Ids[j]); //CSR format colIndex start from 1
			   if (colIndex[itr_ids] > maxCol)
				   maxCol = colIndex[itr_ids];

			   itr_ids++;
		   }

		   System.arraycopy(elem_g.getVals(), 0, data, itr_pos, num_entry);
		   itr_pos += num_entry;
	   }

	   long nFeatures = maxCol;
	   //check CSR table 
	   if ((rowOffset[num_pars] - 1) != num_vals || nFeatures == 0 || num_pars == 0) {
		   LOG.info("Wrong CSR format: ");
		   return null;
	   }
	   else
		   return new CSRNumericTable(daal_Context, data, colIndex, rowOffset, nFeatures, num_pars);

   }//}}}

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

   
   public NumericTable createDenseNumericTableInput(DaalContext context) throws IOException
   {
	  this.loadFiles();
	  NumericTable inputTable = new HomogenNumericTable(context, Double.class, this.dim, this.getTotalLines(), NumericTable.AllocationFlag.DoAllocate);
	  this.loadDataBlock(inputTable);

	  return inputTable;
   }

   public NumericTable createCSRNumericTableInput(DaalContext context) throws IOException
   {
	 if (this.hdfs_filenames.size() > 1)
	 {
		 LOG.info("CSR data shall be contained in a single file");
		 return null;
	 }

	 return  loadCSRNumericTableImpl(this.hdfs_filenames.get(0), context);
   }

   public NumericTable createDenseNumericTable(String filePath, int fileDim, DaalContext context) throws IOException
   {
	this.loadTestFile(filePath, fileDim);
	NumericTable table = new HomogenNumericTable(context, Double.class, fileDim, this.getTestRows(), NumericTable.AllocationFlag.DoAllocate);
	this.loadTestTable(table);
	return table;
   }
   
   public NumericTable createCSRNumericTable(List<String> filenames, DaalContext context) throws IOException
   {
	 if (filenames.size() > 1)
	 {
		 LOG.info("CSR data shall be contained in a single file");
		 return null;
	 }

	 return  loadCSRNumericTableImpl(filenames.get(0), context);
   }

   public NumericTable loadCSRNumericTable(DaalContext context) throws IOException
   {
	 if (this.hdfs_filenames.size() > 1)
	 {
		 LOG.info("CSR data shall be contained in a single file");
		 return null;
	 }

	 return  loadCSRNumericTableImpl(this.hdfs_filenames.get(0), context);
   }
   
   public NumericTable loadCSRNumericTableAndLabel(DaalContext context) throws IOException
   {
	 if (this.hdfs_filenames.size() > 1)
	 {
		 LOG.info("CSR data shall be contained in a single file");
		 return null;
	 }

	 return  loadCSRNumericTableAndLabelImpl(this.hdfs_filenames.get(0), context);
   }

   /**
    * @brief Load in an external single csr file to create a daal CSRNumericTable
    *
    * @param inputFile
    * @param context
    *
    * @return 
    */
   public NumericTable loadExternalCSRNumericTable(String inputFiles, DaalContext context) throws IOException
   {//{{{

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

	   if (inputFileList.size() > 1)
	   {
		   LOG.info("Error CSR data shall be within a single file");
	           return null;
	   }

	   String filename = inputFileList.get(0);
	   return loadCSRNumericTableImpl(filename, context);
   }//}}}

   public NumericTable loadExternalCSRNumericTableAndLabel(String inputFiles, DaalContext context) throws IOException
   {//{{{

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

	   if (inputFileList.size() > 1)
	   {
		   LOG.info("Error CSR data shall be within a single file");
	           return null;
	   }

	   String filename = inputFileList.get(0);
	   return loadCSRNumericTableAndLabelImpl(filename, context);
   }//}}}

   private NumericTable loadCSRNumericTableImpl(String filename, DaalContext context) throws IOException
   {//{{{
	   LOG.info("read in file name: " + filename);
	   Path file_path = new Path(filename);

	   FSDataInputStream in = null;
	   try {

		   FileSystem fs =
			   file_path.getFileSystem(conf);
		   in = fs.open(file_path);

	   } catch (Exception e) {
		   LOG.error("Fail to open file "+ e.toString());
		   return null;
	   }

	   //read csr file content
	   //assume a csr file contains three lines
	   //1) row index line
	   //2) colindex line
	   //3) data line

	   // read row indices
	   String rowIndexLine = in.readLine();
	   if (rowIndexLine == null) 
		   return null;

	   int nVectors = getRowLength(rowIndexLine);
	   long[] rowOffsets = new long[nVectors];

	   readRow(rowIndexLine, 0, nVectors, rowOffsets);
	   nVectors = nVectors - 1;

	   // read col indices
	   String columnsLine = in.readLine();
	   if (columnsLine == null) 
		   return null;

	   int nCols = getRowLength(columnsLine);
	   long[] colIndices = new long[nCols];
	   readRow(columnsLine, 0, nCols, colIndices);

	   // read data 
	   String valuesLine = in.readLine();
	   if (valuesLine == null)
		   return null;

	   int nNonZeros = getRowLength(valuesLine);
	   double[] data = new double[nNonZeros];

	   readRow(valuesLine, 0, nNonZeros, data);

	   in.close();

	   // create the daal table
	   long maxCol = 0;
	   for (int i = 0; i < nCols; i++) {
		   if (colIndices[i] > maxCol) {
			   maxCol = colIndices[i];
		   }
	   }
	   int nFeatures = (int) maxCol;

	   if (nCols != nNonZeros || nNonZeros != (rowOffsets[nVectors] - 1) || nFeatures == 0 || nVectors == 0) {
		   throw new IOException("Unable to read input dataset");
	   }

	

	   return new CSRNumericTable(context, data, colIndices, rowOffsets, nFeatures, nVectors);

   }//}}}

   private NumericTable loadCSRNumericTableAndLabelImpl(String filename, DaalContext context) throws IOException
   {//{{{

	   LOG.info("read in file name: " + filename);
	   Path file_path = new Path(filename);

	   FSDataInputStream in = null;
	   try {

		   FileSystem fs =
			   file_path.getFileSystem(conf);
		   in = fs.open(file_path);

	   } catch (Exception e) {
		   LOG.error("Fail to open file "+ e.toString());
		   return null;
	   }

	   //read csr file content
	   //assume a csr file contains three lines
	   //1) row index line
	   //2) colindex line
	   //3) data line
	   //4) Labels one label per line

	   // read row indices
	   String rowIndexLine = in.readLine();
	   if (rowIndexLine == null) 
		   return null;

	   int nVectors = getRowLength(rowIndexLine);
	   long[] rowOffsets = new long[nVectors];

	   readRow(rowIndexLine, 0, nVectors, rowOffsets);
	   nVectors = nVectors - 1;

	   // read col indices
	   String columnsLine = in.readLine();
	   if (columnsLine == null) 
		   return null;

	   int nCols = getRowLength(columnsLine);
	   long[] colIndices = new long[nCols];
	   readRow(columnsLine, 0, nCols, colIndices);

	   // read data 
	   String valuesLine = in.readLine();
	   if (valuesLine == null)
		   return null;

	   int nNonZeros = getRowLength(valuesLine);
	   double[] data = new double[nNonZeros];

	   readRow(valuesLine, 0, nNonZeros, data);


	   // create the daal table
	   long maxCol = 0;
	   for (int i = 0; i < nCols; i++) {
		   if (colIndices[i] > maxCol) {
			   maxCol = colIndices[i];
		   }
	   }
	   int nFeatures = (int) maxCol;

	   if (nCols != nNonZeros || nNonZeros != (rowOffsets[nVectors] - 1) || nFeatures == 0 || nVectors == 0) {
		   throw new IOException("Unable to read input dataset");
	   }

	   // read labels appended to the CSR content
	   this.csrlabels = new double[nVectors];

	   //read file content
	   for (int j=0;j<nVectors; j++) 
	   {
		   String line = in.readLine();
		   if (line == null) break;

		   String[] lineData = line.split(",");
		   this.csrlabels[j] = Double.parseDouble(lineData[0]);
	   }

	   in.close();

	   return new CSRNumericTable(context, data, colIndices, rowOffsets, nFeatures, nVectors);
   }//}}}

   

   /**
    * @brief used in CSR file reading
    *
    * @param line
    *
    * @return 
    */
   private int getRowLength(String line) {
        String[] elements = line.split(",");
        return elements.length;
   }

   /**
    * @brief readi in data vals in CSR file
    *
    * @param line
    * @param offset
    * @param nCols
    * @param data
    *
    * @return 
    */
   private void readRow(String line, int offset, int nCols, double[] data) throws IOException 
   {
	   if (line == null) {
		   throw new IOException("Unable to read input dataset");
	   }

	   String[] elements = line.split(",");
	   for (int j = 0; j < nCols; j++) {
		   data[offset + j] = Double.parseDouble(elements[j]);
	   }

   }

   /**
    * @brief read in index vals in CSR file
    *
    * @param line
    * @param offset
    * @param nCols
    * @param data
    *
    * @return 
    */
   private void readRow(String line, int offset, int nCols, long[] data) throws IOException 
   {
        if (line == null) {
            throw new IOException("Unable to read input dataset");
        }

        String[] elements = line.split(",");
        for (int j = 0; j < nCols; j++) {
            data[offset + j] = Long.parseLong(elements[j]);
        }
   }

   public void loadTestTable(NumericTable testTable)
   {
      testTable.releaseBlockOfRows(0, this.numTestRows, DoubleBuffer.wrap(this.testData));
   }

   public NumericTable loadCSRLabel(DaalContext context)
   {
      NumericTable labelTable = new HomogenNumericTable(context, Double.class, 1, this.csrlabels.length, NumericTable.AllocationFlag.DoAllocate);
      labelTable.releaseBlockOfRows(0, this.csrlabels.length, DoubleBuffer.wrap(this.csrlabels));
      return labelTable;
   }

}

