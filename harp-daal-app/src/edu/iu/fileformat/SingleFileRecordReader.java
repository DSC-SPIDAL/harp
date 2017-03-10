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

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

// need to be modified
public class SingleFileRecordReader extends
  RecordReader<String, String> {
  private Path path;
  private boolean done = false;

  @Override
  public void initialize(InputSplit split,
    TaskAttemptContext context)
    throws IOException, InterruptedException {
    path = ((FileSplit) split).getPath();
  }

  @Override
  public float getProgress() throws IOException {
    System.out
      .println("in getProgress : " + done);
    if (done) {
      return 1.0f;
    } else {
      return 0.0f;
    }
  }

  @Override
  public String getCurrentKey()
    throws IOException, InterruptedException {
    System.out.println("in current key "
      + path.toString() + " :" + done);
    String pathName = path.getName();
    int index = pathName.lastIndexOf("/");
    return pathName.substring(index + 1,
      pathName.length());
  }

  @Override
  public String getCurrentValue()
    throws IOException, InterruptedException {
    System.out.println(" get Current Value "
      + path.toString() + " :" + done);
    return path.toString();
  }

  @Override
  public boolean nextKeyValue()
    throws IOException, InterruptedException {
    System.out.println("next keyvalue : "
      + path.toString() + " :" + done);
    if (done) {
      return false;
    } else {
      done = true;
      return true;
    }
  }

  @Override
  public void close() throws IOException {
    done = true;
  }
} // end of FileRecordReader