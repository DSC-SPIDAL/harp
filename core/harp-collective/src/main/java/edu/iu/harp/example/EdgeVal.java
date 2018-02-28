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

package edu.iu.harp.example;

import edu.iu.harp.keyval.Value;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EdgeVal extends Value {

  private int[] src;
  private int[] val;
  private int[] dest;
  private int size;

  public EdgeVal() {
    this.size = 0;
    this.src = new int[2];
    this.val = new int[2];
    this.dest = new int[2];
  }

  public void addEdge(int s, int v, int d) {
    src[size] = s;
    val[size] = v;
    dest[size++] = d;
    if (size == src.length) {
      int len = 2 * size;
      int[] tmpSrc = new int[len];
      int[] tmpVal = new int[len];
      int[] tmpDest = new int[len];
      System.arraycopy(src, 0, tmpSrc, 0, size);
      System.arraycopy(val, 0, tmpVal, 0, size);
      System.arraycopy(dest, 0, tmpDest, 0, size);
      src = tmpSrc;
      val = tmpVal;
      dest = tmpDest;
    }
  }

  public void addEdgeVal(EdgeVal edgeVal) {
    int otherSize = edgeVal.getNumEdges();
    if (size + otherSize >= src.length) {
      int len = size;
      while (len < size + otherSize) {
        len *= 2;
      }
      int[] tmpSrc = new int[len];
      int[] tmpVal = new int[len];
      int[] tmpDest = new int[len];
      System.arraycopy(src, 0, tmpSrc, 0, size);
      System.arraycopy(val, 0, tmpVal, 0, size);
      System.arraycopy(dest, 0, tmpDest, 0, size);
      src = tmpSrc;
      val = tmpVal;
      dest = tmpDest;
    }
    System.arraycopy(edgeVal.getSrc(), 0, src,
      size, otherSize);
    System.arraycopy(edgeVal.getVal(), 0, val,
      size, otherSize);
    System.arraycopy(edgeVal.getDest(), 0, dest,
      size, otherSize);
    size += otherSize;
  }

  public int[] getSrc() {
    return src;
  }

  public int[] getVal() {
    return val;
  }

  public int[] getDest() {
    return dest;
  }

  public int getNumEdges() {
    return size;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(size);
    for (int i = 0; i < size; i++) {
      out.writeInt(src[i]);
      out.writeInt(val[i]);
      out.writeInt(dest[i]);
    }
  }

  @Override
  public void read(DataInput in)
    throws IOException {
    this.size = in.readInt();
    if (src.length < size) {
      src = new int[size];
      val = new int[size];
      dest = new int[size];
    }
    for (int i = 0; i < size; i++) {
      src[i] = in.readInt();
      val[i] = in.readInt();
      dest[i] = in.readInt();
    }
  }

  @Override
  public int getNumWriteBytes() {
    return 4 + 12 * src.length;
  }

  @Override
  public void clear() {
    size = 0;
  }
}
