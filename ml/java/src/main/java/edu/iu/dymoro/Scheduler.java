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

package edu.iu.dymoro;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.Simple;
import edu.iu.harp.schdynamic.DynamicScheduler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scheduler<D, S extends Simple, T extends MPTask<D, S>> {
  protected static final Log LOG =
    LogFactory.getLog(Scheduler.class);

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
  private final boolean[][] submissionMap;
  private final RowColSplit<D, S>[][] splitMap;

  private long time;
  private Timer timer;
  private final Random random;
  private final AtomicBoolean isRunning;
  private final DynamicScheduler<RowColSplit<D, S>, RowColSplit<D, S>, T> compute;

  public Scheduler(int numRowSplits,
    int numColSplits, D[] vWHMap, long time,
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
    submissionMap =
      new boolean[numRowSplits][numColSplits];
    splitMap =
      new RowColSplit[numRowSplits][numColSplits];
    for (int i = 0; i < numRowSplits; i++) {
      for (int j = 0; j < numColSplits; j++) {
        splitMap[i][j] = new RowColSplit<>();
        splitMap[i][j].row = i;
        splitMap[i][j].col = j;
        splitMap[i][j].rData = vWHMap[i];
        splitMap[i][j].cData = null;
      }
    }

    this.time = time;
    this.timer = new Timer();
    random =
      new Random(System.currentTimeMillis());
    isRunning = new AtomicBoolean(true);
    compute = new DynamicScheduler<>(tasks);
    compute.start();
    compute.pauseNow();
  }

  public void setTimer(long time) {
    this.time = time;
  }

  public void schedule(List<Partition<S>>[] hMap,
    boolean record) {
    init(hMap);
    for (int i = 0; i < numRowSplits; i++) {
      freeRow[numFreeRows++] = i;
    }
    for (int i = 0; i < numColSplits; i++) {
      freeCol[numFreeCols++] = i;
    }
    while (numFreeRows > 0 && numFreeCols > 0) {
      int rowIndex = random.nextInt(numFreeRows);
      int colIndex = random.nextInt(numFreeCols);
      RowColSplit<D, S> split =
        splitMap[freeRow[rowIndex]][freeCol[colIndex]];
      // split.cData = hMap[split.col];
      submissionMap[split.row][split.col] = true;
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
    // --------------------------
    // Record starts
    if (record) {
      for (T task : compute.getTasks()) {
        task.startRecord(record);
      }
    } else {
      for (T task : compute.getTasks()) {
        task.startRecord(record);
      }
    }
    // -------------------------
    timer.schedule(timerTask, time);
    compute.start();
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
      if (isRunning.get()) {
        // Find a matched col for the last row
        if (freeRowID != -1 && numFreeCols > 0) {
          // Reservoir sampling
          int selectedColIndex = -1;
          int count = 0;
          for (int i = 0; i < numFreeCols; i++) {
            if (!submissionMap[freeRowID][freeCol[i]]) {
              count++;
              if (count == 1) {
                selectedColIndex = i;
              } else if (random
                .nextInt(count) == 0) {
                selectedColIndex = i;
              }
            }
          }
          if (count > 0) {
            RowColSplit<D, S> s =
              splitMap[freeRowID][freeCol[selectedColIndex]];
            // s.cData = hMap[s.col];
            submissionMap[s.row][s.col] = true;
            rowCount[s.row]++;
            colCount[s.col]++;
            freeCol[selectedColIndex] =
              freeCol[--numFreeCols];
            freeRowID = -1;
            compute.submit(s);
          }
        }
        // Find a matched row for the last col
        if (freeColID != -1 && numFreeRows > 0) {
          // Reservoir sampling
          int selectedRowIndex = -1;
          int count = 0;
          for (int i = 0; i < numFreeRows; i++) {
            if (!submissionMap[freeRow[i]][freeColID]) {
              count++;
              if (count == 1) {
                selectedRowIndex = i;
              } else if (random
                .nextInt(count) == 0) {
                selectedRowIndex = i;
              }
            }
          }
          if (count > 0) {
            RowColSplit<D, S> s =
              splitMap[freeRow[selectedRowIndex]][freeColID];
            // s.cData = hMap[s.col];
            submissionMap[s.row][s.col] = true;
            rowCount[s.row]++;
            colCount[s.col]++;
            freeRow[selectedRowIndex] =
              freeRow[--numFreeRows];
            freeColID = -1;
            compute.submit(s);
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
    compute.pauseNow();
    timerTask.cancel();
    while (compute.hasOutput()) {
      compute.waitForOutput();
    }
    compute.cleanInputQueue();
    // ------------------------
    // Record ends
    if (record) {
      int i = 0;
      for (T task : compute.getTasks()) {
        LOG.info("Task " + i + " took "
          + task.getRecordDuration()
          + ", trained "
          + task.getItemsRecorded());
        i++;
      }
    }
    // -----------------------
  }

  private void init(List<Partition<S>>[] hMap) {
    for (int i = 0; i < numRowSplits; i++) {
      rowCount[i] = 0;
    }
    numFreeRows = 0;
    for (int i = 0; i < numColSplits; i++) {
      colCount[i] = 0;
    }
    numFreeCols = 0;
    for (int i = 0; i < numRowSplits; i++) {
      for (int j = 0; j < numColSplits; j++) {
        splitMap[i][j].cData = hMap[j];
        if (submissionMap[i][j]) {
          submissionMap[i][j] = false;
        }
      }
    }
  }

  public long getNumVItemsTrained() {
    long numItemsTrained = 0L;
    for (T task : compute.getTasks()) {
      numItemsTrained +=
        task.getNumItemsProcessed();
    }
    return numItemsTrained;
  }

  public void stop() {
    compute.stop();
    while (compute.hasOutput()) {
      compute.waitForOutput();
    }
    compute.cleanInputQueue();
    timer.cancel();
    // Remove the reference
    for (int i = 0; i < numRowSplits; i++) {
      for (int j = 0; j < numColSplits; j++) {
        splitMap[i][j].cData = null;
      }
    }
  }
}
