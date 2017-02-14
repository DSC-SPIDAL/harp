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

package edu.iu.fileformat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

public class MultiFileSplit extends InputSplit
  implements Writable {

  private List<Path> files;
  private long length;
  private String[] hosts;

  MultiFileSplit() {
  }

  /**
   * Constructs a split with host information
   * 
   * @param file
   *          The file name
   * @param length
   *          The number of bytes in the file to
   *          process
   * @param hosts
   *          The list of hosts containing the
   *          block, possibly null
   */
  public MultiFileSplit(List<Path> files,
    long length, String[] hosts) {
    this.files = files;
    this.length = length;
    this.hosts = hosts;
  }

  public List<Path> getPathList() {
    return files;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
    out.writeInt(files.size());
    for (Path file : files) {
      Text.writeString(out, file.toString());
    }
    out.writeLong(length);
  }

  @Override
  public void readFields(DataInput in)
    throws IOException {
    int size = in.readInt();
    files = new ArrayList<Path>(size);
    for (int i = 0; i < size; i++) {
      Path file = new Path(Text.readString(in));
      files.add(file);
    }
    length = in.readLong();
    hosts = null;
  }

  @Override
  public long getLength() throws IOException,
    InterruptedException {
    return length;
  }

  @Override
  public String[] getLocations()
    throws IOException, InterruptedException {
    if (this.hosts == null) {
      return new String[] {};
    } else {
      return this.hosts;
    }
  }
}
