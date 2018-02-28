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

package edu.iu.daal_subgraph;

import edu.iu.harp.resource.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SCSet extends Writable {

  private int v_num;
  private int counts_num;

  // array to hold colorset ids for counts
  private int[] v_offset; //size == v_num + 1
  // private int[] counts_idx; //size == counts_num
  private float[] counts_data; //size == counts_num

  private short[] counts_index; //size == counts_num


  public SCSet() {

      v_num = -1;
      counts_num = -1;

      v_offset = new int[SCConstants.ARR_LEN];
      // counts_idx = new int[SCConstants.ARR_LEN] ;
      counts_data = new float[SCConstants.ARR_LEN];
      counts_index = new short[SCConstants.ARR_LEN];
  }

  public SCSet(int v_num, int counts_num, 
          int[] v_offset, 
          float[] counts_data, short[] counts_index) 
  {

      this.v_num = v_num;
      this.counts_num = counts_num;
      this.v_offset = v_offset;
      // this.counts_idx = counts_idx;
      this.counts_data = counts_data;
      this.counts_index = counts_index;
  }

  @Override
  public int getNumWriteBytes() {
    return 8 + 4*(v_num+1) + 6*counts_num;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(v_num);
    out.writeInt(counts_num);

    for (int i = 0; i < v_num+1; i++) {
      out.writeInt(v_offset[i]);
    }
    
    for (int i = 0; i < counts_num; i++) {
      out.writeFloat(counts_data[i]);
    }

    for (int i = 0; i < counts_num; i++) {
      out.writeShort(counts_index[i]);
    }

  }

  @Override
  public void read(DataInput in)
    throws IOException {
    v_num = in.readInt();
    counts_num = in.readInt();

    if (v_offset.length < (v_num + 1))
    {
        v_offset = null;
        v_offset = new int[v_num+1];
    }

    if (counts_data.length < counts_num)
    {
        counts_data = null;
        counts_data = new float[counts_num];

        counts_index = null;
        counts_index = new short[counts_num];
    }

    for (int i = 0; i < v_num+1; i++) {
      v_offset[i] = in.readInt();
    }
    
    for (int i = 0; i < counts_num; i++) {
      counts_data[i] = in.readFloat();
    }

    for (int i = 0; i < counts_num; i++) {
      counts_index[i] = in.readShort();
    }

  }

  @Override
  public void clear() {
      // v_num = -1;
      // counts_num = -1;
  }

  public int get_v_num() {
      return v_num;
  }

  public int get_counts_num(){
      return counts_num;
  }

  public int[] get_v_offset(){
      return v_offset;
  }

  public float[] get_counts_data(){
      return counts_data;
  }

  public short[] get_counts_index(){
      return counts_index;
  }

}
