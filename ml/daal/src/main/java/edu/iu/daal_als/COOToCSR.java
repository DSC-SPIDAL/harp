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

package edu.iu.daal_als;

import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.services.DaalContext;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @brief convert Sparse format COO to CSR 
 */
public class COOToCSR {

  private Table<VSet> harpTable;
  private CSRNumericTable daalTable;

  private int nVector = 0;
  private int nCols = 0;
  private int nData = 0;

  private static DaalContext daal_Context = new DaalContext();
  private int[] mapping;
  protected static final Log LOG = LogFactory.getLog(COOToCSR.class);

  public COOToCSR(Table<VSet> harpTable, int[] mapping) 
  {
      this.harpTable = harpTable;
      this.daalTable = null;
      this.mapping = mapping;
      nVector = harpTable.getNumPartitions();

  }

  public CSRNumericTable convert()
  {

      long[] rowOffset = new long[nVector+1];
      rowOffset[0] = 1;
          
      //here partition id starts from 1
      IntArray idArray = IntArray.create(nVector, false);
      harpTable.getPartitionIDs().toArray(idArray.get());
      IntArrays.quickSort(idArray.get(), 0, idArray.size());
      int[] ids = idArray.get();

      // write the rowoffset
      for (int i = 0; i < idArray.size(); i++) 
      {
          VSet vset = harpTable.getPartition(ids[i]).get();
          rowOffset[i+1] = rowOffset[i] + vset.getNumV(); 
          nCols += vset.getNumV();
      }

      nData = nCols;

      long[] colIndex = new long[nCols];
      double[] data = new double[nData];

      int itr_pos = 0;
      int itr_ids = 0;
      long maxCol = 0;

      //write colIndex and CSR data
      for (int i = 0; i < idArray.size(); i++) 
      {
          VSet vset = harpTable.getPartition(ids[i]).get();
          int[] vids = vset.getIDs();
          for(int j=0;j<vset.getNumV();j++)
          {
              // colIndex[itr_ids] = vids[j]+1; //CSR format colIndex start from 1
              colIndex[itr_ids] = mapping[vids[j]]; //CSR format colIndex start from 1
              if (colIndex[itr_ids] > maxCol)
                  maxCol = colIndex[itr_ids];

              itr_ids++;
          }

          System.arraycopy(vset.getV(), 0, data, itr_pos, vset.getNumV());
          itr_pos += vset.getNumV();
      }

      int nFeatures = (int) maxCol;

      if (nData != (rowOffset[nVector] - 1) || nFeatures == 0 || nVector == 0) {
          LOG.info("Wrong CSR format: ");
          return null;
      }
      else
        return new CSRNumericTable(daal_Context, data, colIndex, rowOffset, nFeatures, nVector);

  }

}
