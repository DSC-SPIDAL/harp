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
 *
 * */

package edu.iu.daal_sgd;

import java.util.Arrays;
import java.lang.System;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.ListIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.intel.daal.data_management.data.*;
import com.intel.daal.data_management.data_source.*;
import com.intel.daal.services.*;
import com.intel.daal.algorithms.mf_sgd.*;

/**
 * @brief  This class implements a data type conversion 
 * between Harp and Intel's DAAL
 */
public class TaskIndex implements Runnable {

    private int _th_id;
    private int _task_id;
    private int _th_num;
    private int _task_num;
    private int _col_id;
    private Int2ObjectOpenHashMap<VRowCol>[] _vWHMap;
    private Int2ObjectOpenHashMap<ArrayList<VPointD>>[] _taskMap_daal;
    private ArrayList<VPointD>[] _DiaSelectMap;
    private Int2ObjectOpenHashMap<Integer> _wMap_index;
    private Int2ObjectOpenHashMap<Integer> _hMap_index;
    private int[] _colSet;
    private int _colNum;
    private int _is_absent;
    private int _absent_count;
    private int[] _absent_num;

    //constructor
    TaskIndex(
            int th_id, 
            int th_num,
            int task_num, 
            Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
            Int2ObjectOpenHashMap<ArrayList<VPointD>>[] taskMap_daal, 
            ArrayList<VPointD>[] DiaSelectMap,
            Int2ObjectOpenHashMap<Integer> wMap_index,
            Int2ObjectOpenHashMap<Integer> hMap_index,
            int[] colSet,
            int colNum,
            int[] absentNum
    )
    {
        _th_id = th_id;
        _task_id = th_id;
        _th_num = th_num;
        _task_num = task_num;
        _vWHMap = vWHMap;
        _taskMap_daal = taskMap_daal;
        _DiaSelectMap = DiaSelectMap;
        _wMap_index = wMap_index;
        _hMap_index = hMap_index;
        _colSet = colSet;
        _colNum = colNum;
        _is_absent = 0;
        _absent_count = 0;
        _absent_num = absentNum;
    }

    @Override
    public void run() {

        while(_task_id < _task_num)
        {
            Int2ObjectOpenHashMap<VRowCol> vHMap = _vWHMap[_task_id];
            Int2ObjectOpenHashMap<ArrayList<VPointD>> task_daal = _taskMap_daal[_task_id];
            ArrayList<VPointD> DiaSelect = _DiaSelectMap[_task_id];

            for(int p = 0;p<_colNum;p++)
            {
                _col_id = _colSet[p];

                if (task_daal.get(_col_id) == null)
                {
                    VRowCol elemV = vHMap.get(_col_id);               

                    ArrayList<VPointD> inputTask = new ArrayList<>();

                    if (elemV != null)
                    {
                        //to do deal with elemV null for test data points
                        for (int q = 0; q<elemV.numV;q++)
                        {

                            int row_pos = -1;
                            int col_pos = -1;
                            double vVal = 0;
                            if (_wMap_index.get(elemV.ids[q]) != null)
                            {
                                row_pos = (_wMap_index.get(elemV.ids[q])).intValue();
                                col_pos = (_hMap_index.get(_col_id)).intValue();
                                vVal = elemV.v[q];
                                inputTask.add(new VPointD(row_pos, col_pos, vVal));
                            }

                        }
                    }

                    task_daal.put(_col_id, inputTask);

                }

                DiaSelect.addAll(task_daal.get(_col_id));

            }

            _task_id += _th_num;

        }

        _absent_num[_th_id] = _absent_count;

    }


}
