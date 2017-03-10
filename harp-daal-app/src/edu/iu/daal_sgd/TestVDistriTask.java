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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.harp.schdynamic.Task;

/**
 * @brief redistribute test dataset points into LinkedList of multiple threads
 */
public class TestVDistriTask implements
  Task<VRowCol, Object> {

  protected static final Log LOG = LogFactory
    .getLog(ReGroupTask.class);

  private LinkedList<VRowCol> _setlist;
  private int _num_point;

  public TestVDistriTask() 
  {
      _setlist = new LinkedList<>();
      _num_point = 0;
  }

  @Override
  public Object run(VRowCol obj)
    throws Exception {

    _setlist.add(obj);
    _num_point += obj.numV;
    
    return null;
  }

  public int getNumPoint()
  {
      return _num_point;
  }

  public LinkedList<VRowCol> getSetList()
  {
      return _setlist;
  }

}
