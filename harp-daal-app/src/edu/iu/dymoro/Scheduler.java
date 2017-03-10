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

package edu.iu.dymoro;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.Simple;

public class Scheduler<D, S extends Simple, T extends MPTask<D, S>> {
  protected static final Log LOG = LogFactory
    .getLog(Scheduler.class);

  private final int[] rowCount;
  private final int numRowSplits;
  private final int numRowLimit;
  private final int[] freeRow;
  private int numFreeRows;
  private final int[] colCount;
  private final int numColSplits;
  private final int numColLimit;
  private final int[] freeCol;
  private int numFreeCols;
  private final byte[][] splitMap;
  private long numItemsTrained;
  private final Int2ObjectOpenHashMap<D>[] vWHMap;

  private long time;
  private Timer timer;
  private final Random random;
  private final AtomicBoolean isRunning;
  private final DynamicScheduler<RowColSplit<D, S>, RowColSplit<D, S>, T> compute;

  public Scheduler(int numRowSplits,
    int numColSplits,
    Int2ObjectOpenHashMap<D>[] vWHMap, long time,
    List<T> tasks) {
    rowCount = new int[numRowSplits];
    this.numRowSplits = numRowSplits;
    numRowLimit = numColSplits;
    freeRow = new int[numRowSplits];
    numFreeRows = 0;
    colCount = new int[numColSplits];
    this.numColSplits = numColSplits;
    numColLimit = numRowSplits;
    freeCol = new int[numColSplits];
    numFreeCols = 0;
    splitMap =
      new byte[numRowSplits][numColSplits];
    numItemsTrained = 0L;
    this.vWHMap = vWHMap;

    this.time = time;
    this.timer = new Timer();
    random =
      new Random(System.currentTimeMillis());
    isRunning = new AtomicBoolean(true);
    compute = new DynamicScheduler<>(tasks);
    compute.start();
  }

  public void setTimer(long time) {
    this.time = time;
  }

  public void schedule(List<Partition<S>>[] hMap) {
    for (int i = 0; i < numRowSplits; i++) {
      freeRow[numFreeRows++] = i;
    }
    for (int i = 0; i < numColSplits; i++) {
      freeCol[numFreeCols++] = i;
    }
    while (numFreeRows > 0 && numFreeCols > 0) {
      RowColSplit<D, S> split =
        new RowColSplit<>();
      int rowIndex = random.nextInt(numFreeRows);
      int colIndex = random.nextInt(numFreeCols);
      split.row = freeRow[rowIndex];
      split.col = freeCol[colIndex];
      split.rData = vWHMap[split.row];
      split.cData = hMap[split.col];
      splitMap[split.row][split.col]++;
      rowCount[split.row]++;
      colCount[split.col]++;
      freeRow[rowIndex] = freeRow[--numFreeRows];
      freeCol[colIndex] = freeCol[--numFreeCols];
      compute.submit(split);
    }
    isRunning.set(true);
    TimerTask timerTask = new TimerTask() {
      @Override
      public void run() {
        isRunning.set(false);
      }
    };
    timer.schedule(timerTask, time);
    while (compute.hasOutput()) {
      RowColSplit<D, S> split =
        compute.waitForOutput();
      int freeRowID = -1;
      if (rowCount[split.row] < numRowLimit) {
        freeRowID = split.row;
      }
      int freeColID = -1;
      if (colCount[split.col] < numColLimit) {
        freeColID = split.col;
      }
      numItemsTrained += split.numItems;
      split = null;
      if (isRunning.get()) {
        // Find a matched col for the last row
        if (freeRowID != -1) {
          for (int i = 0; i < numFreeCols; i++) {
            if (splitMap[freeRowID][freeCol[i]] == 0) {
              split = new RowColSplit<>();
              split.row = freeRowID;
              split.col = freeCol[i];
              split.rData = vWHMap[split.row];
              split.cData = hMap[split.col];
              split.numItems = 0L;
              splitMap[split.row][split.col]++;
              rowCount[split.row]++;
              colCount[split.col]++;
              freeCol[i] = freeCol[--numFreeCols];
              freeRowID = -1;
              compute.submit(split);
              break;
            }
          }
        }
        // Find a matched row for the last col
        if (freeColID != -1) {
          for (int i = 0; i < numFreeRows; i++) {
            if (splitMap[freeRow[i]][freeColID] == 0) {
              split = new RowColSplit<>();
              split.row = freeRow[i];
              split.col = freeColID;
              split.rData = vWHMap[split.row];
              split.cData = hMap[split.col];
              split.numItems = 0L;
              splitMap[split.row][split.col]++;
              rowCount[split.row]++;
              colCount[split.col]++;
              freeRow[i] = freeRow[--numFreeRows];
              freeColID = -1;
              compute.submit(split);
              break;
            }
          }
        }
        if (freeRowID != -1) {
          freeRow[numFreeRows++] = freeRowID;
        }
        if (freeColID != -1) {
          freeCol[numFreeCols++] = freeColID;
        }
      } else {
        break;
      }
    }
    timerTask.cancel();
    clean();
    compute.pauseNow();
    while (compute.hasOutput()) {
      numItemsTrained +=
        compute.waitForOutput().numItems;
    }
    compute.cleanInputQueue();
    compute.start();
  }

  private void clean() {
    for (int i = 0; i < numRowSplits; i++) {
      rowCount[i] = 0;
    }
    numFreeRows = 0;
    for (int i = 0; i < numColSplits; i++) {
      colCount[i] = 0;
    }
    numFreeCols = 0;
    byte zero = 0;
    for (int i = 0; i < numRowSplits; i++) {
      for (int j = 0; j < numColSplits; j++) {
        splitMap[i][j] = zero;
      }
    }
  }

  public long getNumVItemsTrained() {
    long num = numItemsTrained;
    numItemsTrained = 0L;
    return num;
  }

  public void stop() {
    timer.cancel();
    compute.stop();
  }
}
