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

package edu.iu.sgd;

import edu.iu.harp.schdynamic.Task;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataInitTask
  implements Task<VSetSplit, Object> {

  protected static final Log LOG =
    LogFactory.getLog(DataInitTask.class);

  private final Int2ObjectOpenHashMap<VRowCol>[] vWHMap;

  public DataInitTask(
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap) {
    this.vWHMap = vWHMap;
  }

  @Override
  public Object run(VSetSplit vSetList)
    throws Exception {
    Int2ObjectOpenHashMap<VRowCol> vHMap =
      vWHMap[vSetList.splitID];
    for (VSet vSet : vSetList.list) {
      int id = vSet.getID();
      int[] ids = vSet.getIDs();
      double[] v = vSet.getV();
      int numV = vSet.getNumV();
      for (int i = 0; i < numV; i++) {
        VStore.add(vHMap, ids[i], id, v[i]);
      }
    }
    ObjectIterator<Int2ObjectMap.Entry<VRowCol>> iterator =
      vHMap.int2ObjectEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2ObjectMap.Entry<VRowCol> entry =
        iterator.next();
      VRowCol vRowCol = entry.getValue();
      int[] ids = new int[vRowCol.numV];
      System.arraycopy(vRowCol.ids, 0, ids, 0,
        vRowCol.numV);
      double[] v = new double[vRowCol.numV];
      System.arraycopy(vRowCol.v, 0, v, 0,
        vRowCol.numV);
      vRowCol.ids = ids;
      vRowCol.v = v;
      vRowCol.m1 = null;
      vRowCol.m2 = null;
    }
    vHMap.trim();
    return null;
  }
}
