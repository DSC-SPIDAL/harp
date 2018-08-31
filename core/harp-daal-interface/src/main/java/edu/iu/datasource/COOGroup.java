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

package edu.iu.datasource;

import edu.iu.harp.resource.Writable;
import edu.iu.data_aux.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * @brief
 */
public class COOGroup extends Writable {
  private long gId;
  private long[] Ids;
  private double[] vals;
  private int num_entry;

  public COOGroup() {
    this.gId = -1;
    this.Ids = new long[HarpDAALConstants.ARR_LEN];
    this.vals = new double[HarpDAALConstants.ARR_LEN];
    num_entry = 0;
  }

  public COOGroup(long gId) {
    this.gId = gId;
    this.Ids = new long[HarpDAALConstants.ARR_LEN];
    this.vals = new double[HarpDAALConstants.ARR_LEN];
    num_entry = 0;
  }

  public COOGroup(long gId, long[] Ids, double[] vals, int num_entry) {
    this.gId = gId;
    this.Ids = Ids;
    this.vals = vals;
    this.num_entry = num_entry;
  }

  public long getGID() {
    return this.gId;
  }

  public long[] getIds() {
    return this.Ids;
  }

  public double[] getVals() {
    return this.vals;
  }

  public int getNumEntry() {
    return this.num_entry;
  }

  /**
   * @param elem
   * @param isRow
   * @return
   * @brief add a COO element to COOGroup
   */
  public void add(COO elem, boolean isRow) {//{{{

    //resize the arrays for Ids and vals
    if (this.num_entry + 1 > this.Ids.length) {
      int newlen = this.Ids.length + HarpDAALConstants.ARR_LEN;
      long[] newIds = new long[newlen];
      double[] newVals = new double[newlen];
      System.arraycopy(this.Ids, 0, newIds, 0, this.num_entry);
      System.arraycopy(this.vals, 0, newVals, 0, this.num_entry);
      this.Ids = newIds;
      this.vals = newVals;
    }

    //check the gId
    if (isRow) {
      if (elem.getRowId() != this.gId)
        return;
      else {
        this.Ids[this.num_entry] = elem.getColId();
        this.vals[this.num_entry++] = elem.getVal();
      }
    } else {
      if (elem.getColId() != this.gId)
        return;
      else {
        this.Ids[this.num_entry] = elem.getRowId();
        this.vals[this.num_entry++] = elem.getVal();
      }
    }

  }//}}}

  /**
   * @param other
   * @return
   * @brief add another COOGroup object
   */
  public void add(COOGroup other) {//{{{

    if (this.num_entry + other.getNumEntry() > this.Ids.length) {
      int len = getArrLen(this.num_entry + other.getNumEntry());
      long[] newIDs = new long[len];
      double[] newVals = new double[len];

      if (this.num_entry > 0) {
        System.arraycopy(this.Ids, 0, newIDs, 0, this.num_entry);
        System.arraycopy(this.vals, 0, newVals, 0, this.num_entry);
      }

      this.Ids = newIDs;
      this.vals = newVals;
    }

    System.arraycopy(other.getIds(), 0, this.Ids,
        this.num_entry, other.getNumEntry());
    System.arraycopy(other.getVals(), 0, this.vals, this.num_entry,
        other.getNumEntry());

    this.num_entry += other.getNumEntry();

  }//}}}

  @Override
  public int getNumWriteBytes() {
    return 12 + 16 * this.num_entry;
  }

  @Override
  public void write(DataOutput out)
      throws IOException {
    out.writeLong(this.gId);
    out.writeInt(this.num_entry);
    for (int i = 0; i < this.num_entry; i++)
      out.writeLong(this.Ids[i]);

    for (int i = 0; i < this.num_entry; i++)
      out.writeDouble(this.vals[i]);
  }

  private int getArrLen(int numV) {
    return 1 << (32 - Integer.numberOfLeadingZeros(numV - 1));
  }

  @Override
  public void read(DataInput in)
      throws IOException {
    this.gId = in.readLong();
    this.num_entry = in.readInt();
    if (this.Ids.length < this.num_entry) {
      int len = getArrLen(this.num_entry);
      this.Ids = new long[len];
      this.vals = new double[len];
    }

    for (int i = 0; i < this.num_entry; i++)
      this.Ids[i] = in.readLong();

    for (int i = 0; i < this.num_entry; i++)
      this.vals[i] = in.readDouble();
  }

  @Override
  public void clear() {
    this.gId = -1;
    this.num_entry = 0;
  }
}

