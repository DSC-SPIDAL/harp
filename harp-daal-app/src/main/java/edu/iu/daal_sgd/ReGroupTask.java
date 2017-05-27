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

package edu.iu.daal_sgd;

import java.lang.System;
import java.util.LinkedList;
import java.util.ArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.harp.schdynamic.Task;

/**
 * @brief regroup the training dataset into hashmap of multiple threads 
 */
public class ReGroupTask implements
  Task<VSet, Object> {

  protected static final Log LOG = LogFactory
    .getLog(ReGroupTask.class);

  private LinkedList<VSet> _setlist;
  private int _num_point;
  private Int2ObjectOpenHashMap<int[]> _colMap;

  public ReGroupTask(Int2ObjectOpenHashMap<int[]> colMap) 
  {
      _setlist = new LinkedList<>();
      _num_point = 0;
      _colMap = colMap;
  }

  @Override
  public Object run(VSet obj)
    throws Exception {

    _setlist.add(obj);
    int col_size = obj.getNumV();
    _num_point += col_size;
    int[] colIds = obj.getIDs();

    for(int j=0;j<col_size;j++)
    {

        int[] p_num = _colMap.get(colIds[j]);

        if (p_num == null)
        {
            p_num = new int[1];
            p_num[0] = 1;
            _colMap.put(colIds[j], p_num);
        }
        else
        {
            p_num[0] += 1;
        }
    }
    
    return null;

  }

  public int getNumPoint()
  {
      return _num_point;
  }

  public LinkedList<VSet> getSetList()
  {
      return _setlist;
  }

}
