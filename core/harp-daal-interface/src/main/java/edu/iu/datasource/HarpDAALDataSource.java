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

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.DaalContext;
import com.intel.daal.services.Environment;

public class HarpDAALDataSource {
  private int harpthreads; // threads used in parallel data reading/transfer
  private Configuration conf;
  protected static final Log LOG = LogFactory.getLog(HarpDAALDataSource.class);

  public HarpDAALDataSource(int harpthreads, Configuration conf) {
    this.harpthreads = harpthreads;
    this.conf = conf;
  }

  // ------------------------------  Dense CSV files I/O ------------------------------

  public NumericTable createDenseNumericTable(List<String> inputFiles, int nFeatures, String sep, DaalContext context) {//{{{

    try {

      //load in data block
      List<double[]> inputData = this.loadDenseCSVFiles(inputFiles, nFeatures, sep);

      //debug
      LOG.info("Finish loading csv files");

      // create daal table
      NumericTable inputTable = new HomogenNumericTable(context, Double.class, nFeatures, inputData.size(), NumericTable.AllocationFlag.DoAllocate);

      //debug
      LOG.info("Finish creating daal table");

      // load data blk to daal table
      this.loadDataBlock(inputTable, inputData, nFeatures);

      //debug
      LOG.info("Finish loading daal table");

      return inputTable;

    } catch (Exception e) {
      LOG.error("Fail to create dense numeric table", e);
      return null;
    }
  }//}}}

  public NumericTable createDenseNumericTable(String inputFile, int nFeatures, String sep, DaalContext context) {//{{{

    try {

      //load in data block
      List<double[]> inputData = this.loadDenseCSVFiles(inputFile, nFeatures, sep);

      // create daal table
      NumericTable inputTable = new HomogenNumericTable(context, Double.class, nFeatures, inputData.size(), NumericTable.AllocationFlag.DoAllocate);

      // load data blk to daal table
      this.loadDataBlock(inputTable, inputData, nFeatures);
      return inputTable;

    } catch (Exception e) {
      LOG.error("Fail to create dense numeric table", e);
      return null;
    }
  }//}}}

  public NumericTable[] createDenseNumericTableSplit(List<String> inputFiles, int nFeature1, int nFeature2, String sep, DaalContext context) {//{{{

    try {

      //load in data block
      List<double[]> inputData = this.loadDenseCSVFiles(inputFiles, (nFeature1 + nFeature2), sep);

      // create daal table
      NumericTable[] inputTable = new NumericTable[2];
      inputTable[0] = new HomogenNumericTable(context, Double.class, nFeature1, inputData.size(), NumericTable.AllocationFlag.DoAllocate);
      inputTable[1] = new HomogenNumericTable(context, Double.class, nFeature2, inputData.size(), NumericTable.AllocationFlag.DoAllocate);

      MergedNumericTable mergedTable = new MergedNumericTable(context);
      mergedTable.addNumericTable(inputTable[0]);
      mergedTable.addNumericTable(inputTable[1]);

      // load data blk to daal table
      this.loadDataBlock(mergedTable, inputData, (nFeature1 + nFeature2));
      return inputTable;

    } catch (Exception e) {
      LOG.error("Fail to create dense numeric table", e);
      return null;
    }

  }//}}}

  public NumericTable[] createDenseNumericTableSplit(String inputFile, int nFeature1, int nFeature2, String sep, DaalContext context) {//{{{

    try {

      //load in data block
      List<double[]> inputData = this.loadDenseCSVFiles(inputFile, (nFeature1 + nFeature2), sep);

      // create daal table
      NumericTable[] inputTable = new NumericTable[2];
      inputTable[0] = new HomogenNumericTable(context, Double.class, nFeature1, inputData.size(), NumericTable.AllocationFlag.DoAllocate);
      inputTable[1] = new HomogenNumericTable(context, Double.class, nFeature2, inputData.size(), NumericTable.AllocationFlag.DoAllocate);

      MergedNumericTable mergedTable = new MergedNumericTable(context);
      mergedTable.addNumericTable(inputTable[0]);
      mergedTable.addNumericTable(inputTable[1]);

      // load data blk to daal table
      this.loadDataBlock(mergedTable, inputData, (nFeature1 + nFeature2));

      return inputTable;

    } catch (Exception e) {
      LOG.error("Fail to create dense numeric table", e);
      return null;
    }
  }//}}}

  public Tensor createDenseTensor(String inputFile, int nFeatures, String sep, DaalContext context) {//{{{
    try {

      List<double[]> inputData = loadDenseCSVFiles(inputFile, nFeatures, sep);
      long[] dims = {inputData.size(), nFeatures};
      double[] data_array = new double[nFeatures * inputData.size()];
      for (int j = 0; j < inputData.size(); j++)
        System.arraycopy(inputData.get(j), 0, data_array, j * nFeatures, nFeatures);

      Tensor tsr = new HomogenTensor(context, dims, data_array);
      return tsr;

    } catch (Exception e) {
      LOG.error("Fail to create dense numeric tensor", e);
      return null;
    }
  }//}}}

  public List<double[]> loadDenseCSVFiles(List<String> inputFiles, int nFeatures, String sep) {//{{{

    MTReader reader = new MTReader();
    List<double[]> output = reader.readDenseCSV(inputFiles, nFeatures, sep, this.conf, this.harpthreads);
    return output;

  }//}}}

  public List<double[][]> loadDenseCSVFilesSharding(List<String> inputFiles, int nFeatures, int shardsize, String sep) {//{{{

    MTReader reader = new MTReader();
    List<double[][]> output = reader.readDenseCSVSharding(inputFiles, nFeatures, shardsize, sep, this.conf, this.harpthreads);
    return output;

  }//}}}

  public List<double[]> loadDenseCSVFiles(String inputFile, int nFeatures, String sep) {//{{{

    Path inputFilePaths = new Path(inputFile);
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
    while (file_itr.hasNext()) {
      String file_name = file_itr.next();
      LOG.info("read in file name: " + file_name);

      Path file_path = new Path(file_name);
      try {

        FileSystem fs =
            file_path.getFileSystem(conf);
        in = fs.open(file_path);

      } catch (Exception e) {
        LOG.error("Fail to open file " + e.toString());
        return null;
      }

      //read file content
      try {
        while (true) {
          String line = in.readLine();
          if (line == null) break;

          String[] lineData = line.split(sep);
          double[] cell = new double[nFeatures];

          for (int t = 0; t < nFeatures; t++)
            cell[t] = Double.parseDouble(lineData[t]);

          points.add(cell);
        }

        in.close();

      } catch (Exception e) {
        LOG.error("Fail to read data " + e.toString());
        return null;
      }

    }

    return points;

  }//}}}

  private void loadDataBlock(NumericTable dst_table, List<double[]> inputData, int nFeatures) {//{{{

    // check the datalist obj
    if (inputData == null) {
      LOG.info("Error no hdfs data to load");
      return;
    }

    int totalRows = inputData.size();
    int rowBlkStart = 0;
    int rowBlkEnd = 0;
    int rowBlkSize = (totalRows / this.harpthreads) > 0 ? (totalRows / this.harpthreads) : totalRows;
    while (rowBlkEnd < totalRows) {
      // get a sublist
      rowBlkEnd = (rowBlkStart + rowBlkSize) > totalRows ? totalRows : (rowBlkStart + rowBlkSize);
      List<double[]> blk_elem = inputData.subList(rowBlkStart, rowBlkEnd);

      double[] rowblocks = new double[blk_elem.size() * nFeatures];
      for (int j = 0; j < blk_elem.size(); j++)
        System.arraycopy(blk_elem.get(j), 0, rowblocks, j * nFeatures, nFeatures);

      //copy rowblock to NumericTable
      dst_table.releaseBlockOfRows(rowBlkStart, blk_elem.size(), DoubleBuffer.wrap(rowblocks));

      rowBlkStart = rowBlkEnd;
    }

  }//}}}

  // ------------------------------  Sparse COO files I/O ------------------------------

  public List<COO> loadCOOFiles(List<String> FilePaths, String regex) {//{{{

    try {

      MTReader reader = new MTReader();
      List<COO> output = reader.readCOO(FilePaths, regex, this.conf, this.harpthreads);
      // this.totallines = reader.getTotalLines();
      // this.totalPoints = reader.getTotalPoints();
      return output;

    } catch (Exception e) {
      LOG.error("Fail to read COO files", e);
      return null;
    }
  }//}}}

  public List<COO> loadCOOFiles(String FilePath, String regex) {//{{{
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
    // this.totallines = reader.getTotalLines();
    // this.totalPoints = reader.getTotalPoints();
    return output;
  }//}}}

  public HashMap<Long, COOGroup> groupCOOByIDs(List<COO> inputData, boolean isRow) {//{{{
    //regroup by multi-threading
    MTReader reader = new MTReader();
    HashMap<Long, COOGroup> output_map = reader.regroupCOO(inputData, isRow, this.conf, this.harpthreads);
    return output_map;
  }//}}}

  public HashMap<Long, Integer> remapCOOIDs(HashMap<Long, COOGroup> input_map, int mapper_id, int num_mappers, CollectiveMapper mapper) {//{{{

    long[] inputIDs = new long[input_map.keySet().size()];
    int id_itr = 0;
    for (Long key : input_map.keySet())
      inputIDs[id_itr++] = key;

    Table<LongArray> RowInfo_Table = new Table<>(0, new LongArrPlus());
    LongArray RowInfo_array = new LongArray(inputIDs, 0, inputIDs.length);

    RowInfo_Table.addPartition(new Partition<>(mapper_id, RowInfo_array));
    mapper.allgather("coo", "remap_ids", RowInfo_Table);

    long[][] row_ids_all = new long[num_mappers][];

    for (int j = 0; j < num_mappers; j++) {
      row_ids_all[j] = RowInfo_Table.getPartition(j).get().get();
    }

    HashMap<Long, Integer> row_mapping = new HashMap<>();
    int row_pos = 1; //CSR pos start from 1
    for (int j = 0; j < num_mappers; j++) {
      for (int k = 0; k < row_ids_all[j].length; k++) {
        if (row_mapping.get(row_ids_all[j][k]) == null) {
          row_mapping.put(row_ids_all[j][k], row_pos);
          row_pos++;
        }

      }
    }

    return row_mapping;
  }//}}}

  public Table<COOGroup> regroupCOOList(HashMap<Long, COOGroup> input_map, HashMap<Long, Integer> remap_idx, CollectiveMapper mapper, int[] maxCompactRowID) {//{{{

    int maxRowID = -1;
    Table<COOGroup> regroup_table = new Table<>(0, new COOGroupCombiner(), input_map.size());

    for (Map.Entry<Long, COOGroup> entry : input_map.entrySet()) {
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

  public CSRNumericTable COOToCSR(Table<COOGroup> inputTable, HashMap<Long, Integer> remapIDs, DaalContext daal_Context) {//{{{
    int num_pars = inputTable.getNumPartitions();
    int num_cols = 0;
    int num_vals = 0;
    long[] rowOffset = new long[num_pars + 1];
    rowOffset[0] = 1;

    //here partition id starts from 1
    IntArray idArray = IntArray.create(num_pars, false);
    inputTable.getPartitionIDs().toArray(idArray.get());
    Arrays.sort(idArray.get(), 0, idArray.size());
    int[] ids = idArray.get();

    // write the rowoffset
    for (int i = 0; i < idArray.size(); i++) {
      COOGroup elem = inputTable.getPartition(ids[i]).get();
      rowOffset[i + 1] = rowOffset[i] + elem.getNumEntry();
      num_cols += elem.getNumEntry();
    }

    num_vals = num_cols;

    long[] colIndex = new long[num_cols];
    double[] data = new double[num_vals];

    int itr_pos = 0;
    int itr_ids = 0;
    long maxCol = 0;

    //write colIndex and CSR data
    for (int i = 0; i < idArray.size(); i++) {
      COOGroup elem_g = inputTable.getPartition(ids[i]).get();
      long[] Ids = elem_g.getIds();
      int num_entry = elem_g.getNumEntry();

      for (int j = 0; j < num_entry; j++) {
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
    } else
      return new CSRNumericTable(daal_Context, data, colIndex, rowOffset, nFeatures, num_pars);

  }//}}}

  // ------------------------------  Sparse CSR files I/O ------------------------------

  public NumericTable loadCSRNumericTable(List<String> inputFiles, String sep, DaalContext context) throws IOException {//{{{
    if (inputFiles.size() > 1) {
      LOG.info("CSR data shall be contained in a single file");
      return null;
    }

    return loadCSRNumericTableImpl(inputFiles.get(0), sep, context);
  }//}}}

  public NumericTable loadCSRNumericTable(String inputFiles, String sep, DaalContext context) throws IOException {//{{{

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

    if (inputFileList.size() > 1) {
      LOG.info("Error CSR data shall be within a single file");
      return null;
    }

    String filename = inputFileList.get(0);
    return loadCSRNumericTableImpl(filename, sep, context);
  }//}}}

  public NumericTable[] loadCSRNumericTableAndLabel(List<String> inputFiles, String sep, DaalContext context) throws IOException {//{{{
    if (inputFiles.size() > 1) {
      LOG.info("CSR data shall be contained in a single file");
      return null;
    }

    return loadCSRNumericTableAndLabelImpl(inputFiles.get(0), sep, context);
  }//}}}

  public NumericTable[] loadCSRNumericTableAndLabel(String inputFiles, String sep, DaalContext context) throws IOException {//{{{

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

    if (inputFileList.size() > 1) {
      LOG.info("Error CSR data shall be within a single file");
      return null;
    }

    String filename = inputFileList.get(0);

    return loadCSRNumericTableAndLabelImpl(filename, sep, context);

  }//}}}

  private NumericTable loadCSRNumericTableImpl(String filename, String sep, DaalContext context) throws IOException {//{{{
    LOG.info("read in file name: " + filename);
    Path file_path = new Path(filename);

    FSDataInputStream in = null;
    try {

      FileSystem fs =
          file_path.getFileSystem(conf);
      in = fs.open(file_path);

    } catch (Exception e) {
      LOG.error("Fail to open file " + e.toString());
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

    int nVectors = getRowLength(rowIndexLine, sep);
    long[] rowOffsets = new long[nVectors];

    readRow(rowIndexLine, sep, 0, nVectors, rowOffsets);
    nVectors = nVectors - 1;

    // read col indices
    String columnsLine = in.readLine();
    if (columnsLine == null)
      return null;

    int nCols = getRowLength(columnsLine, sep);
    long[] colIndices = new long[nCols];
    readRow(columnsLine, sep, 0, nCols, colIndices);

    // read data
    String valuesLine = in.readLine();
    if (valuesLine == null)
      return null;

    int nNonZeros = getRowLength(valuesLine, sep);
    double[] data = new double[nNonZeros];

    readRow(valuesLine, sep, 0, nNonZeros, data);

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

  private NumericTable[] loadCSRNumericTableAndLabelImpl(String filename, String sep, DaalContext context) throws IOException {//{{{

    LOG.info("read in file name: " + filename);
    Path file_path = new Path(filename);

    FSDataInputStream in = null;
    try {

      FileSystem fs =
          file_path.getFileSystem(conf);
      in = fs.open(file_path);

    } catch (Exception e) {
      LOG.error("Fail to open file " + e.toString());
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

    int nVectors = getRowLength(rowIndexLine, sep);
    long[] rowOffsets = new long[nVectors];

    readRow(rowIndexLine, sep, 0, nVectors, rowOffsets);
    nVectors = nVectors - 1;

    // read col indices
    String columnsLine = in.readLine();
    if (columnsLine == null)
      return null;

    int nCols = getRowLength(columnsLine, sep);
    long[] colIndices = new long[nCols];
    readRow(columnsLine, sep, 0, nCols, colIndices);

    // read data
    String valuesLine = in.readLine();
    if (valuesLine == null)
      return null;

    int nNonZeros = getRowLength(valuesLine, sep);
    double[] data = new double[nNonZeros];

    readRow(valuesLine, sep, 0, nNonZeros, data);

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

    CSRNumericTable dataTable = new CSRNumericTable(context, data, colIndices, rowOffsets, nFeatures, nVectors);

    // read labels appended to the CSR content
    double[] labelData = new double[nVectors];

    //read file content
    for (int j = 0; j < nVectors; j++) {
      String line = in.readLine();
      if (line == null) break;

      String[] lineData = line.split(sep);
      labelData[j] = Double.parseDouble(lineData[0]);
    }

    NumericTable labelTable = new HomogenNumericTable(context, Double.class, 1, labelData.length, NumericTable.AllocationFlag.DoAllocate);
    labelTable.releaseBlockOfRows(0, labelData.length, DoubleBuffer.wrap(labelData));

    NumericTable[] output = new NumericTable[2];
    output[0] = dataTable;
    output[1] = labelTable;

    in.close();

    return output;

  }//}}}

  private int getRowLength(String line, String sep) {//{{{
    String[] elements = line.split(sep);
    return elements.length;
  }//}}}

  private void readRow(String line, String sep, int offset, int nCols, double[] data) throws IOException {//{{{
    if (line == null) {
      throw new IOException("Unable to read input dataset");
    }

    String[] elements = line.split(sep);
    for (int j = 0; j < nCols; j++) {
      data[offset + j] = Double.parseDouble(elements[j]);
    }

  }//}}}

  private void readRow(String line, String sep, int offset, int nCols, long[] data) throws IOException {//{{{
    if (line == null) {
      throw new IOException("Unable to read input dataset");
    }

    String[] elements = line.split(sep);
    for (int j = 0; j < nCols; j++) {
      data[offset + j] = Long.parseLong(elements[j]);
    }
  }//}}}


}

