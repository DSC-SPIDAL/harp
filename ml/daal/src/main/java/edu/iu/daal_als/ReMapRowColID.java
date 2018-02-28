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

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import it.unimi.dsi.fastutil.ints.IntArrays;

public class ReMapRowColID {

    private int[] prev_ids;
    private int self_id;
    private int num_workers;
    private int[] after_ids;
    private ALSDaalCollectiveMapper collect_mapper;

    public ReMapRowColID(int[] prev_ids, int self_id, int num_workers,ALSDaalCollectiveMapper collect_mapper)
    {
        this.prev_ids = prev_ids;
        this.self_id = self_id;
        this.num_workers = num_workers;
        this.collect_mapper = collect_mapper;
    }

    public int[] getRemapping() {

            Table<IntArray> RowInfo_Table = new Table<>(0, new IntArrPlus());
            IntArray RowInfo_array = new IntArray(prev_ids, 0, prev_ids.length); 
            RowInfo_Table.addPartition(new Partition<>(self_id, RowInfo_array));
            collect_mapper.allgather("als", "get-info", RowInfo_Table);

            int maxRowId = 0;
            int[][] row_ids_all = new int[num_workers][];

            for(int j=0;j<num_workers;j++)
            {
                row_ids_all[j] = RowInfo_Table.getPartition(j).get().get();
                IntArrays.quickSort(row_ids_all[j], 0, row_ids_all[j].length);

                if (row_ids_all[j][row_ids_all[j].length - 1] > maxRowId)
                    maxRowId = row_ids_all[j][row_ids_all[j].length - 1];
            }

            // LOG.info("Max Row ID Prev: " + maxRowId);
            int[] row_mapping = new int[maxRowId+1];
            int row_pos = 1; //CSR pos start from 1
            for(int j=0;j<num_workers;j++)
            {
                for(int k=0;k<row_ids_all[j].length;k++)
                {
                    if (row_mapping[row_ids_all[j][k]] == 0)
                    {
                        row_mapping[row_ids_all[j][k]] = row_pos;
                        row_pos++;
                    }
                }
            }

            return row_mapping;

    }

}
