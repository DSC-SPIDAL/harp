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

package edu.iu.sgd;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SGDUtil {
  protected static final Log LOG =
    LogFactory.getLog(SGDUtil.class);

  static Int2ObjectOpenHashMap<VRowCol> loadVWMap(
    LinkedList<String> vFilePaths, int numThreads,
    Configuration configuration) {
    VStore vStore = new VStore(vFilePaths,
      numThreads, configuration);
    vStore.load(false, true);
    return vStore.getVWMap();
  }

  public static Int2ObjectOpenHashMap<VRowCol>
    loadTestVHMap(String testFilePath,
      Configuration configuration,
      int numThreads) {
    List<String> testFilePaths =
      new LinkedList<>();
    Path path = new Path(testFilePath);
    try {
      FileSystem fs =
        path.getFileSystem(configuration);
      RemoteIterator<LocatedFileStatus> iterator =
        fs.listFiles(path, true);
      while (iterator.hasNext()) {
        String name = iterator.next().getPath()
          .toUri().toString();
        testFilePaths.add(name);
      }
    } catch (IOException e) {
      LOG.error("Fail to get test files", e);
    }
    VStore testVStore = new VStore(testFilePaths,
      numThreads, configuration);
    testVStore.load(true, false);
    return testVStore.getVHMap();
  }

  public static void randomize(Random random,
    double[] row, int size, double oneOverSqrtR) {
    // Non-zero initialization
    for (int i = 0; i < size; i++) {
      double rowi = 0.0;
      do {
        rowi = random.nextDouble();
      } while (rowi == 0.0);
      row[i] = rowi * oneOverSqrtR;
    }
  }
}
