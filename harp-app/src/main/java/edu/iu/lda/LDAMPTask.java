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

package edu.iu.lda;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.iu.dymoro.MPTask;
import edu.iu.harp.partition.Partition;

/**
 * This task treats r the same in one iteration.
 * 
 * @author zhangbj
 *
 */
public class LDAMPTask extends
  MPTask<DocWord, TopicCount> {

  protected static final Log LOG = LogFactory
    .getLog(LDAMPTask.class);
  private final int numTopics;
  private final double alpha;
  private final double oneOverAlpha;
  private final double betaPlusOne;
  private final double betaMinusOne;
  private final Random random;
  private final double[] globalCommons;
  private final double[] globalRCoeffDistr;
  private final double[] commons;
  private final double[] rCoeffDistr;
  private double rCoeffSum;
  private boolean rInit;
  private final double[] sCoeffDistr;
  private double sCoeffSum;
  private final int[] t;
  private final double[] tProb;
  private int tSize;
  private double tProbSum;

  public LDAMPTask(int numTopics, double alpha,
    double beta, double[] globalCommons,
    double[] globalRCoeffDistr) {
    this.numTopics = numTopics;
    this.alpha = alpha;
    this.oneOverAlpha = 1.0 / alpha;
    this.betaPlusOne = beta + 1.0;
    this.betaMinusOne = beta - 1.0;
    random =
      new Random(System.currentTimeMillis());
    this.globalCommons = globalCommons;
    this.globalRCoeffDistr = globalRCoeffDistr;
    commons = new double[numTopics];
    rCoeffDistr = new double[numTopics];
    rCoeffSum = 0.0;
    rInit = true;
    sCoeffDistr = new double[numTopics];
    sCoeffSum = 0.0;
    t = new int[numTopics];
    tProb = new double[numTopics];
    tSize = 0;
    tProbSum = 0.0;
  }

  public void setRCoeffSum(double rCoeffSum) {
    this.rCoeffSum = rCoeffSum;
    this.rInit = true;
  }

  @Override
  public long doRun(
    List<Partition<TopicCount>> partitionList,
    Int2ObjectOpenHashMap<DocWord> docWordMap) {
    if (rInit) {
      initRCoeffSum();
      rInit = false;
    }
    long numToken = 0L;
    for (Partition<TopicCount> partition : partitionList) {
      DocWord docWord =
        docWordMap.get(partition.id());
      if (docWord != null) {
        numToken +=
          updateModel(docWord, partition.get()
            .getTopicCount());
      }
    }
    return numToken;
  }

  private long updateModel(DocWord docWord,
    Int2IntOpenHashMap wRow) {
    // Go through all the docs of this word
    long totalNumToken = 0L;
    int numDocs = docWord.numV;
    for (int i = 0; i < numDocs; i++) {
      Int2IntOpenHashMap dRow = docWord.m2[i];
      int numTokens = docWord.v[i];
      int[] z = docWord.z[i];
      // Go through each token
      for (int j = 0; j < numTokens; j++) {
        // Init sCoeff (word)
        int wTCOnOldZi = 0;
        if (i == 0 && j == 0) {
          wTCOnOldZi = initWordProb(z[j], wRow);
        } else {
          wTCOnOldZi =
            updateWordProbOnOldZi(z[j], wRow);
        }
        // Minus 1 on oldZi
        // Update word prob on old zi
        // Update doc prob on old zi
        if (j == 0) {
          initDocProb(z[j], wTCOnOldZi, wRow,
            dRow);
        } else {
          updateDocProbOnOldZi(z[j], wTCOnOldZi,
            wRow, dRow);
        }
        // Draw a new sample
        int newZi = -1;
        int tIndex = -1;
        // int region = -1;
        double nextRandom =
          random.nextDouble()
            * (tProbSum + (rCoeffSum + sCoeffSum)
              * alpha);
        if (nextRandom <= tProbSum) {
          // t region
          // region = 2;
          for (int ti = 0; ti < tSize; ti++) {
            nextRandom -= tProb[ti];
            if (nextRandom <= 0.0) {
              newZi = t[ti];
              tIndex = ti;
              break;
            }
          }
        } else {
          // remember s and r only contain
          // coefficient sum
          nextRandom =
            (nextRandom - tProbSum)
              * oneOverAlpha;
          if (nextRandom <= sCoeffSum) {
            // s region
            // region = 1;
            IntIterator iterator =
              wRow.keySet().iterator();
            while (iterator.hasNext()) {
              int topicID = iterator.nextInt();
              nextRandom -= sCoeffDistr[topicID];
              if (nextRandom <= 0.0) {
                newZi = topicID;
                break;
              }
            }
          } else {
            // r region
            // region = 0;
            nextRandom -= sCoeffSum;
            for (int ri = 0; ri < numTopics; ri++) {
              nextRandom -= rCoeffDistr[ri];
              if (nextRandom <= 0.0) {
                newZi = ri;
                break;
              }
            }
          }
        }
        // if (newZi < 0 || newZi >= numTopics) {
        // LOG.info("Fail to sample " + " " + z[j]
        // + " " + newZi // + " " + region
        // );
        // newZi = z[j];
        // }
        z[j] = newZi;
        // Add 1 on newZi
        // Update word prob on new Zi
        // Update doc prob on new Zi
        updateProbOnNewZi(newZi, wRow, dRow,
          tIndex, i == (numDocs - 1),
          j == (numTokens - 1));
      }
      totalNumToken += numTokens;
    }
    return totalNumToken;
  }

  private void initRCoeffSum() {
    System.arraycopy(globalCommons, 0, commons,
      0, numTopics);
    System.arraycopy(globalRCoeffDistr, 0,
      rCoeffDistr, 0, numTopics);
  }

  private int initWordProb(int oldZi,
    Int2IntOpenHashMap wRow) {
    sCoeffSum = 0.0;
    boolean removeOldZi = false;
    int wTCOnOldZi = 0;
    ObjectIterator<Int2IntMap.Entry> iterator =
      wRow.int2IntEntrySet().fastIterator();
    while (iterator.hasNext()) {
      Int2IntMap.Entry entry = iterator.next();
      int wTopicID = entry.getIntKey();
      int wTopicCount = entry.getIntValue();
      if (wTopicID == oldZi) {
        wTCOnOldZi = wTopicCount;
        if (--wTopicCount > 0) {
          entry.setValue(wTopicCount);
          double sCoeffOnTopic =
            commons[wTopicID] * wTopicCount;
          sCoeffDistr[wTopicID] = sCoeffOnTopic;
          sCoeffSum += sCoeffOnTopic;
        } else {
          removeOldZi = true;
        }
      } else {
        double sCoeffOnTopic =
          commons[wTopicID] * wTopicCount;
        sCoeffDistr[wTopicID] = sCoeffOnTopic;
        sCoeffSum += sCoeffOnTopic;
      }
    }
    if (removeOldZi) {
      wRow.remove(oldZi);
    }
    return wTCOnOldZi;
  }

  private int updateWordProbOnOldZi(int oldZi,
    Int2IntOpenHashMap wRow) {
    int wTCOnOldZi = wRow.addTo(oldZi, -1);
    if (wTCOnOldZi > 1) {
      sCoeffDistr[oldZi] -= commons[oldZi];
    } else {
      wRow.remove(oldZi);
    }
    sCoeffSum -= commons[oldZi];
    return wTCOnOldZi;
  }

  private void initDocProb(int oldZi,
    int wTCOnOldZi, Int2IntOpenHashMap wRow,
    Int2IntOpenHashMap dRow) {
    tSize = 0;
    tProbSum = 0.0;
    ObjectIterator<Int2IntMap.Entry> iterator =
      dRow.int2IntEntrySet().fastIterator();
    boolean removeOldZi = false;
    while (iterator.hasNext()) {
      Int2IntMap.Entry entry = iterator.next();
      int dTopicID = entry.getIntKey();
      int dTopicCount = entry.getIntValue();
      if (dTopicID == oldZi) {
        if (--dTopicCount > 0) {
          entry.setValue(dTopicCount);
          double tProbOnTopic = 0.0;
          if (wTCOnOldZi > 1) {
            tProbOnTopic =
              (rCoeffDistr[oldZi] + sCoeffDistr[oldZi])
                * dTopicCount;
          } else {
            tProbOnTopic =
              rCoeffDistr[oldZi] * dTopicCount;
          }
          t[tSize] = oldZi;
          tProb[tSize++] = tProbOnTopic;
          tProbSum += tProbOnTopic;
        } else {
          removeOldZi = true;
        }
      } else {
        int wTCOnD = wRow.get(dTopicID);
        double tProbOnTopic = 0.0;
        if (wTCOnD > 0) {
          tProbOnTopic =
            (rCoeffDistr[dTopicID] + sCoeffDistr[dTopicID])
              * dTopicCount;
        } else {
          tProbOnTopic =
            rCoeffDistr[dTopicID] * dTopicCount;
        }
        t[tSize] = dTopicID;
        tProb[tSize++] = tProbOnTopic;
        tProbSum += tProbOnTopic;
      }
    }
    if (removeOldZi) {
      dRow.remove(oldZi);
    }
  }

  private void updateDocProbOnOldZi(int oldZi,
    int wTCOnOldZi, Int2IntOpenHashMap wRow,
    Int2IntOpenHashMap dRow) {
    int dTCOnOldZi = dRow.addTo(oldZi, -1);
    int tIndex = 0;
    while (t[tIndex] != oldZi) {
      tIndex++;
    }
    if (dTCOnOldZi > 1) {
      double delta =
        (betaMinusOne + wTCOnOldZi + dTCOnOldZi)
          * commons[oldZi];
      tProbSum -= delta;
      tProb[tIndex] -= delta;
    } else {
      dRow.remove(oldZi);
      tProbSum -= tProb[tIndex];
      t[tIndex] = t[--tSize];
      tProb[tIndex] = tProb[tSize];
    }
  }

  private void updateProbOnNewZi(int newZi,
    Int2IntOpenHashMap wRow,
    Int2IntOpenHashMap dRow, int tIndex,
    boolean lastDoc, boolean lastToken) {
    int wTCOnNewZi = wRow.addTo(newZi, 1);
    if (!lastDoc && !lastToken) {
      if (wTCOnNewZi == 0) {
        sCoeffDistr[newZi] = commons[newZi];
      } else {
        sCoeffDistr[newZi] += commons[newZi];
      }
      sCoeffSum += commons[newZi];
    }
    int dTCOnNewZi = dRow.addTo(newZi, 1);
    if (!lastToken) {
      if (dTCOnNewZi == 0) {
        double delta =
        // rCoeffDistr[newZi] +
        // sCoeffDistr[newZi];
        // Or
          (betaPlusOne + wTCOnNewZi)
            * commons[newZi];
        tProbSum += delta;
        t[tSize] = newZi;
        tProb[tSize++] = delta;
      } else {
        double delta =
          (betaPlusOne + wTCOnNewZi + dTCOnNewZi)
            * commons[newZi];
        if (tIndex == -1) {
          do {
            tIndex++;
          } while (t[tIndex] != newZi);
        }
        tProbSum += delta;
        tProb[tIndex] += delta;
      }
    }
  }
}