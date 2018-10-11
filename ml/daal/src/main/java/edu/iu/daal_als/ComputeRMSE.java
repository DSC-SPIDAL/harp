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

import edu.iu.datasource.COOGroup;
import edu.iu.harp.schdynamic.Task;

import java.util.HashMap;

public class ComputeRMSE implements Task<COOGroup, Object> 
{

    private int numWorker;
    private int selfID;
    private HashMap<Long, Integer> row_mapping;
    private HashMap<Long, Integer> col_mapping;
    private long startRowIndex;
    private long endRowIndex;
    private long[] itemsPartition;
    private double[] userModelTestData;
    private double[][] itemModelTestData;

    private double alpha;
    private int dim;
    private double[] rmse_vals; 

    public ComputeRMSE(int numWorker, int selfID, HashMap<Long, Integer> row_mapping, HashMap<Long, Integer> col_mapping, long startRowIndex, long endRowIndex, 
            long[] itemsPartition, double[] userModelTestData, double[][] itemModelTestData, double alpha, int dim, double[] rmse_vals) {


        this.numWorker = numWorker;
        this.selfID = selfID;
        this.row_mapping = row_mapping;
        this.col_mapping = col_mapping;
        this.startRowIndex = startRowIndex;
        this.endRowIndex = endRowIndex;
        this.itemsPartition = itemsPartition;
        this.userModelTestData = userModelTestData;
        this.itemModelTestData = itemModelTestData;
        this.alpha = alpha;
        this.dim = dim;
        this.rmse_vals = rmse_vals;
    }

  @Override
  public Object run(COOGroup obj) throws Exception {

      int row_idx_map = this.row_mapping.get(obj.getGID()).intValue() - 1; //starts from 0 

      if (row_idx_map >= this.startRowIndex && row_idx_map < this.endRowIndex)
      {
          int index_row = row_idx_map - (int)this.startRowIndex;
          int index_col = 0;

          for(int j=0;j<obj.getNumEntry();j++)
          {

              int item_buck = -1;
              if (this.col_mapping.get(obj.getIds()[j]) == null || this.col_mapping.get(obj.getIds()[j]).intValue() == 0)
		      continue;

              index_col = this.col_mapping.get(obj.getIds()[j]).intValue() - 1;

              double rating = obj.getVals()[j];

              //get the relative itemblock that contains index_col
              for(int k=0;k<this.numWorker;k++)
              {
                  if (index_col < (int)this.itemsPartition[k+1])
                  {
                      item_buck = k;
                      break;
                  }
              }

              if (item_buck >= 0)
              {
                  index_col = index_col - (int)this.itemsPartition[item_buck];
                  //get the item buck data
                  double[] item_calc = this.itemModelTestData[item_buck]; 
                  double[] user_calc = this.userModelTestData;

                  double predict = 0;
                  for (int k=0;k<this.dim;k++)
                  {
                      predict += (user_calc[index_row*this.dim + k]*item_calc[index_col*this.dim + k]); 
                  }

                  this.rmse_vals[0] += (((1.0 - predict)*(1.0 - predict))*(1+alpha*rating));
                  this.rmse_vals[1]++;

              }

          }

      }

      return null;

  } 


}
















