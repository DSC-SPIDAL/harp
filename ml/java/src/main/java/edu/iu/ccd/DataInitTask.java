/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.ccd;

import edu.iu.harp.schdynamic.Task;
import edu.iu.sgd.VRowCol;
import edu.iu.sgd.VSet;
import edu.iu.sgd.VSetSplit;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataInitTask
  implements Task<VSetSplit, Object> {

  protected static final Log LOG =
    LogFactory.getLog(DataInitTask.class);

  private final Int2ObjectOpenHashMap<VRowCol>[] vSplitMap;

  public DataInitTask(
    Int2ObjectOpenHashMap<VRowCol>[] vSplitMap) {
    this.vSplitMap = vSplitMap;
  }

  @Override
  public Object run(VSetSplit split)
    throws Exception {
    Int2ObjectOpenHashMap<VRowCol> vMap =
      vSplitMap[split.splitID];
    for (VSet vSet : split.list) {
      int id = vSet.getID();
      int numV = vSet.getNumV();
      int[] ids = vSet.getIDs();
      double[] v = vSet.getV();
      VRowCol vRowCol = new VRowCol();
      vMap.put(id, vRowCol);
      vRowCol.id = id;
      vRowCol.numV = numV;
      vRowCol.ids = new int[vRowCol.numV];
      System.arraycopy(ids, 0, vRowCol.ids, 0,
        vRowCol.numV);
      vRowCol.v = new double[vRowCol.numV];
      System.arraycopy(v, 0, vRowCol.v, 0,
        vRowCol.numV);
      // m1 is used to store residuals
      vRowCol.m1 = new double[vRowCol.numV];
    }
    return null;
  }
}
