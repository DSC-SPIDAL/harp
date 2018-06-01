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

package edu.iu.kmeans.sgxsimu;

import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.iu.data_gen.*;
import edu.iu.datasource.*;
import java.io.IOException;

public class PointLoadTask
  implements Task<String, double[]> {

  protected static final Log LOG =
    LogFactory.getLog(PointLoadTask.class);

  private int pointsPerFile;
  private int cenVecSize;
  private Configuration conf;

  public PointLoadTask(int cenVecSize, Configuration conf) {
    this.cenVecSize = cenVecSize;
    this.conf = conf;
  }

  @Override
  public double[] run(String fileName)
    throws Exception {
    int count = 0;
    boolean isSuccess = false;
    do {
      try {
        double[] array = DataLoader.loadPointsMMDense(fileName, cenVecSize, conf);
        return array;
      } catch (Exception e) {
        LOG.error("load " + fileName
          + " fails. Count=" + count, e);
        Thread.sleep(100);
        isSuccess = false;
        count++;
      }
    } while (!isSuccess && count < 100);
    LOG.error("Fail to load files.");
    return null;
  }
  
}
