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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

public class MultiFileInputFormat extends
  FileInputFormat<String, String> {

  private static final Log LOG = LogFactory
    .getLog(MultiFileInputFormat.class);

  static final String NUM_INPUT_FILES =
    "mapreduce.input.num.files";

  @Override
  public List<InputSplit>
    getSplits(JobContext job) throws IOException {
    // Generate splits
    List<InputSplit> splits =
      new ArrayList<InputSplit>();
    List<FileStatus> files = listStatus(job);
    org.apache.hadoop.mapred.JobConf jobConf =
      (JobConf) job.getConfiguration();
    int numMaps = jobConf.getNumMapTasks();
    LOG.info("NUMBER OF FILES: " + files.size());
    LOG.info("NUMBER OF MAPS: " + numMaps);
    // randomizeFileListOrder(files);
    int avg = files.size() / numMaps;
    int rest = files.size() % numMaps;
    int tmp = 0;
    long length = 0;
    List<Path> pathList = null;
    Set<String> hostSet = null;
    for (FileStatus file : files) {
      if (tmp == 0) {
        pathList = new ArrayList<Path>();
        hostSet = new HashSet<String>();
      }
      if (tmp < avg) {
        pathList.add(file.getPath());
        length = length + file.getLen();
        FileSystem fs =
          file.getPath().getFileSystem(
            job.getConfiguration());
        BlockLocation[] blkLocations =
          fs.getFileBlockLocations(file, 0,
            file.getLen());
        for (BlockLocation blockLocation : blkLocations) {
          for (String host : blockLocation
            .getHosts()) {
            hostSet.add(host);
          }
        }
        tmp++;
        if (tmp == avg && rest == 0) {
          LOG.info("Split on host: "
            + getHostsString(hostSet));
          splits.add(new MultiFileSplit(pathList,
            length, hostSet
              .toArray(new String[0])));
          tmp = 0;
          length = 0;
        }
      } else if (tmp == avg && rest > 0) {
        pathList.add(file.getPath());
        length = length + file.getLen();
        FileSystem fs =
          file.getPath().getFileSystem(
            job.getConfiguration());
        BlockLocation[] blkLocations =
          fs.getFileBlockLocations(file, 0,
            file.getLen());
        for (BlockLocation blockLocation : blkLocations) {
          for (String host : blockLocation
            .getHosts()) {
            hostSet.add(host);
          }
        }
        rest--;
        LOG.info("Split on host: "
          + getHostsString(hostSet));
        splits
          .add(new MultiFileSplit(pathList,
            length, hostSet
              .toArray(new String[0])));
        tmp = 0;
        length = 0;
      }
    }
    // Save the number of input files in the
    // job-conf
    job.getConfiguration().setLong(
      NUM_INPUT_FILES, numMaps);
    LOG.info("Total # of splits: "
      + splits.size());
    return splits;
  }

  @SuppressWarnings("unused")
  private void randomizeFileListOrder(
    List<FileStatus> files) {
    Random random =
      new Random(System.currentTimeMillis());
    int numFiles = files.size();
    for (int i = numFiles - 1; i > 0; i--) {
      int nextRandom = random.nextInt(i + 1);
      if (nextRandom != i) {
        FileStatus tmpFile = files.get(i);
        files.set(i, files.get(nextRandom));
        files.set(nextRandom, tmpFile);
      }
    }
  }

  private String
    getHostsString(Set<String> hosts) {
    StringBuffer buffer = new StringBuffer();
    for (String host : hosts) {
      buffer.append(host + " ");
    }
    return buffer.toString();
  }

  @Override
  public RecordReader<String, String>
    createRecordReader(InputSplit split,
      TaskAttemptContext context)
      throws IOException, InterruptedException {
    return new MultiFileRecordReader();
  }
}
