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

package edu.iu.sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.harp.schdynamic.Task;

public class DataInitTask implements
  Task<VSetSplit, Object> {

  protected static final Log LOG = LogFactory
    .getLog(DataInitTask.class);

  private final Int2ObjectOpenHashMap<VRowCol>[] vWHMap;
  private final Int2ObjectOpenHashMap<double[]> wMap;

  public DataInitTask(
    Int2ObjectOpenHashMap<VRowCol>[] vWHMap,
    Int2ObjectOpenHashMap<double[]> wMap) {
    this.vWHMap = vWHMap;
    this.wMap = wMap;
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
      double[] v = new double[vRowCol.numV];
      System.arraycopy(vRowCol.v, 0, v, 0,
        vRowCol.numV);
      vRowCol.v = v;
      vRowCol.m2 = new double[vRowCol.numV][];
      for (int j = 0; j < vRowCol.numV; j++) {
        vRowCol.m2[j] = wMap.get(vRowCol.ids[j]);
        if (vRowCol.m2[j] == null) {
          LOG.info("null W");
        }
      }
      vRowCol.ids = null;
      vRowCol.m1 = null;
    }
    vHMap.trim();
    return null;
  }
}
