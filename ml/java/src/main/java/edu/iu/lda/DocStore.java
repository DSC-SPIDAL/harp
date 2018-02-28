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

package edu.iu.lda;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.schdynamic.Task;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class DocWord {
  // A doc with id1 and words with id2
  // Or a word with id1 and words with id2
  int id1 = -1;
  int[] id2 = null;
  int[] v = null;
  int numV = 0;
  int[][] z = null;
}

public class DocStore {
  protected static final Log LOG =
    LogFactory.getLog(DocStore.class);

  private final List<String> inputFiles;
  private final int numThreads;
  private final Configuration conf;
  private final AtomicInteger idGenerator;

  public DocStore(List<String> input,
    int numThreads, Configuration configuration) {
    inputFiles = input;
    this.numThreads = numThreads;
    conf = configuration;
    idGenerator = new AtomicInteger(-1);
  }

  /**
   * Load input based on the number of threads
   * 
   * @return
   */
  public void load(
    Int2ObjectOpenHashMap<DocWord> vDocMap,
    Int2ObjectOpenHashMap<String> docIDMap) {
    long start = System.currentTimeMillis();
    LinkedList<VLoadTask> vLoadTasks =
      new LinkedList<>();
    for (int i = 0; i < numThreads; i++) {
      vLoadTasks
        .add(new VLoadTask(conf, idGenerator));
    }
    DynamicScheduler<String, Object, VLoadTask> vLoadCompute =
      new DynamicScheduler<>(vLoadTasks);
    vLoadCompute.start();
    vLoadCompute.submitAll(inputFiles);
    vLoadCompute.stop();
    int totalNumDocs = 0;
    for (VLoadTask task : vLoadCompute
      .getTasks()) {
      vDocMap.putAll(task.getDocMap());
      docIDMap.putAll(task.getDocIDMap());
      totalNumDocs += task.getNumDocs();
    }
    long end = System.currentTimeMillis();
    // Report the total number of training points
    // loaded
    LOG.info(
      "Load num training docs: " + totalNumDocs
        + ", took: " + (end - start));
  }

  public int getMaxDocID() {
    return idGenerator.get();
  }
}

class VLoadTask implements Task<String, Object> {
  protected static final Log LOG =
    LogFactory.getLog(VLoadTask.class);

  private final Configuration conf;
  private final Int2ObjectOpenHashMap<DocWord> vDocMap;
  private final Int2ObjectOpenHashMap<String> docIDMap;
  private int numDocs;
  private final AtomicInteger idGenerator;

  public VLoadTask(Configuration conf,
    AtomicInteger idGenerator) {
    this.conf = conf;
    vDocMap = new Int2ObjectOpenHashMap<>();
    docIDMap = new Int2ObjectOpenHashMap<>();
    numDocs = 0;
    this.idGenerator = idGenerator;
  }

  @Override
  public Object run(String inputFile)
    throws Exception {
    Path inputFilePath = new Path(inputFile);
    // Open the file
    boolean isFailed = false;
    FSDataInputStream in = null;
    BufferedReader reader = null;
    do {
      isFailed = false;
      try {
        FileSystem fs =
          inputFilePath.getFileSystem(conf);
        in = fs.open(inputFilePath);
        reader = new BufferedReader(
          new InputStreamReader(in), 1048576);
      } catch (Exception e) {
        LOG.error("Fail to open " + inputFile, e);
        isFailed = true;
        if (reader != null) {
          try {
            reader.close();
          } catch (Exception e1) {
          }
        }
        if (in != null) {
          try {
            in.close();
          } catch (Exception e1) {
          }
        }
      }
    } while (isFailed);
    // Read the file
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        String[] tokens =
          line.split("\\p{Blank}");
        int doc = idGenerator.incrementAndGet();
        docIDMap.put(doc, tokens[0]);
        for (int i = 1; i < tokens.length; i++) {
          int word = Integer.parseInt(tokens[i]);
          LDAUtil.addToData(vDocMap, doc, word,
            1);
        }
        numDocs++;
      }
    } catch (Exception e) {
      LOG.error("Fail to read " + inputFile, e);
    } finally {
      reader.close();
    }
    return null;
  }

  public Int2ObjectOpenHashMap<DocWord>
    getDocMap() {
    return vDocMap;
  }

  public Int2ObjectOpenHashMap<String>
    getDocIDMap() {
    return docIDMap;
  }

  public int getNumDocs() {
    return numDocs;
  }
}
