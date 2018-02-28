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

import edu.iu.harp.resource.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class VSet extends Writable {
  private int id;
  private int[] ids;
  private double[] v;
  private int numV;

  public VSet() {
    id = -1;
    ids = new int[Constants.ARR_LEN];
    v = new double[Constants.ARR_LEN];
    numV = 0;
  }

  public VSet(int id, int[] ids, double[] v,
    int numV) {
    this.id = id;
    this.ids = ids;
    this.v = v;
    this.numV = numV;
  }

  private int getArrLen(int numV) {
    return 1 << (32 - Integer
      .numberOfLeadingZeros(numV - 1));
  }

  public int getID() {
    return this.id;
  }

  public int[] getIDs() {
    return this.ids;
  }

  public double[] getV() {
    return this.v;
  }

  public int getNumV() {
    return this.numV;
  }

  public void add(VSet other) {
    if (numV + other.getNumV() > ids.length) {
      int len = getArrLen(numV + other.getNumV());
      int[] newIDs = new int[len];
      double[] newV = new double[len];
      if (numV > 0) {
        System.arraycopy(ids, 0, newIDs, 0, numV);
        System.arraycopy(v, 0, newV, 0, numV);
      }
      ids = newIDs;
      v = newV;
    }
    System.arraycopy(other.getIDs(), 0, ids,
      numV, other.getNumV());
    System.arraycopy(other.getV(), 0, v, numV,
      other.getNumV());
    numV += other.getNumV();
  }

  @Override
  public int getNumWriteBytes() {
    return 8 + 12 * numV;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(id);
    out.writeInt(numV);
    for (int i = 0; i < numV; i++) {
      out.writeInt(ids[i]);
    }
    for (int i = 0; i < numV; i++) {
      out.writeDouble(v[i]);
    }
  }

  @Override
  public void read(DataInput in)
    throws IOException {
    id = in.readInt();
    numV = in.readInt();
    if (ids.length < numV) {
      int len = getArrLen(numV);
      ids = new int[len];
      v = new double[len];
    }
    for (int i = 0; i < numV; i++) {
      ids[i] = in.readInt();
    }
    for (int i = 0; i < numV; i++) {
      v[i] = in.readDouble();
    }
  }

  @Override
  public void clear() {
    id = -1;
    numV = 0;
  }
}
