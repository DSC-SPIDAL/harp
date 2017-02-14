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
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

// need to be modified
public class MultiFileRecordReader extends
  RecordReader<String, String> {
  private List<Path> pathList;
  private int progress;

  @Override
  public void initialize(InputSplit split,
    TaskAttemptContext context)
    throws IOException, InterruptedException {
    pathList =
      ((MultiFileSplit) split).getPathList();
    progress = -1;
  }

  @Override
  public float getProgress() throws IOException {
    System.out.println("In getProgress : "
      + progress);
    return (float) progress
      / (float) pathList.size();
  }

  @Override
  public String getCurrentKey()
    throws IOException, InterruptedException {
    Path path = pathList.get(progress);
    System.out.println("in current key "
      + path.toString() + ".");
    return path.toString();
  }

  @Override
  public String getCurrentValue()
    throws IOException, InterruptedException {
    Path path = pathList.get(progress);
    System.out.println(" get Current Value "
      + path.toString() + ".");
    return path.toString();
  }

  @Override
  public boolean nextKeyValue()
    throws IOException, InterruptedException {
    progress++;
    if (progress == pathList.size()) {
      return false;
    }
    return true;
  }

  @Override
  public void close() throws IOException {
    progress = -1;
  }
} // End of FileRecordReader