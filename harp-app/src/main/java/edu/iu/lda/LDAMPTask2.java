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
 * This task captures the changes on r zone.
 * Though the computation process is a little
 * different, it seems not affect the results.
 * 
 * @author zhangbj
 *
 */
public class LDAMPTask2 extends
  MPTask<DocWord, TopicCount> {

  protected static final Log LOG = LogFactory
    .getLog(LDAMPTask2.class);
  private final int numTopics;
  private final double alpha;
  private final double oneOverAlpha;
  private final double beta;
  private final double betaV;
  private final Random random;
  private final int[] globalTopicSums;
  private final double[] globalCommons;
  private final double[] globalRCoeffDistr;
  private final int[] topicSums;
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

  public LDAMPTask2(int numTopics, double alpha,
    double beta, int vocabularySize,
    int[] globalTopicSums,
    double[] globalCommons,
    double[] globalRCoeffDistr) {
    this.numTopics = numTopics;
    this.alpha = alpha;
    this.oneOverAlpha = 1.0 / alpha;
    this.beta = beta;
    this.betaV = beta * vocabularySize;
    random =
      new Random(System.currentTimeMillis());
    this.globalTopicSums = globalTopicSums;
    this.globalCommons = globalCommons;
    this.globalRCoeffDistr = globalRCoeffDistr;
    topicSums = new int[numTopics];
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
    System.arraycopy(globalTopicSums, 0,
      topicSums, 0, numTopics);
    System.arraycopy(globalCommons, 0, commons,
      0, numTopics);
    System.arraycopy(globalRCoeffDistr, 0,
      rCoeffDistr, 0, numTopics);
  }

  private void updateRCoeffOnOldZi(int oldZi) {
    int topicSumOnOldZi = --topicSums[oldZi];
    commons[oldZi] =
      1.0 / (topicSumOnOldZi + betaV);
    double rCoeffOnOldZi = commons[oldZi] * beta;
    rCoeffSum +=
      (rCoeffOnOldZi - rCoeffDistr[oldZi]);
    rCoeffDistr[oldZi] = rCoeffOnOldZi;
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
        // Update r
        updateRCoeffOnOldZi(oldZi);
        // Update s
        if (--wTopicCount > 0) {
          entry.setValue(wTopicCount);
          double sCoeffOnOldZi =
            commons[oldZi] * wTopicCount;
          sCoeffDistr[oldZi] = sCoeffOnOldZi;
          sCoeffSum += sCoeffOnOldZi;
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
    updateRCoeffOnOldZi(oldZi);
    int wTCOnOldZi = wRow.addTo(oldZi, -1);
    if (wTCOnOldZi > 1) {
      double sCoeffOnOldZi =
        commons[oldZi] * (wTCOnOldZi - 1);
      sCoeffSum +=
        (sCoeffOnOldZi - sCoeffDistr[oldZi]);
      sCoeffDistr[oldZi] = sCoeffOnOldZi;
    } else {
      wRow.remove(oldZi);
      sCoeffSum -= sCoeffDistr[oldZi];
    }
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
      double tProbOnOldZi = 0.0;
      if (wTCOnOldZi > 1) {
        tProbOnOldZi =
          (rCoeffDistr[oldZi] + sCoeffDistr[oldZi])
            * (dTCOnOldZi - 1);
      } else {
        tProbOnOldZi =
          rCoeffDistr[oldZi] * (dTCOnOldZi - 1);
      }
      tProbSum += (tProbOnOldZi - tProb[tIndex]);
      tProb[tIndex] = tProbOnOldZi;
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
    // Update r
    updateRCoeffOnNewZi(newZi);
    int wTCOnNewZi = wRow.addTo(newZi, 1);
    if (!lastDoc && !lastToken) {
      if (wTCOnNewZi == 0) {
        sCoeffDistr[newZi] = commons[newZi];
        sCoeffSum += commons[newZi];
      } else {
        double sCoeffOnNewZi =
          commons[newZi] * (wTCOnNewZi + 1);
        sCoeffSum +=
          (sCoeffOnNewZi - sCoeffDistr[newZi]);
        sCoeffDistr[newZi] = sCoeffOnNewZi;
      }
    }
    int dTCOnNewZi = dRow.addTo(newZi, 1);
    if (!lastToken) {
      if (dTCOnNewZi == 0) {
        double tProbOnNewZi =
          rCoeffDistr[newZi] + sCoeffDistr[newZi];
        tProbSum += tProbOnNewZi;
        t[tSize] = newZi;
        tProb[tSize++] = tProbOnNewZi;
      } else {
        double tProbOnNewZi =
          (rCoeffDistr[newZi] + sCoeffDistr[newZi])
            * (dTCOnNewZi + 1);
        if (tIndex == -1) {
          do {
            tIndex++;
          } while (t[tIndex] != newZi);
        }
        tProbSum +=
          (tProbOnNewZi - tProb[tIndex]);
        tProb[tIndex] = tProbOnNewZi;
      }
    }
  }

  private void updateRCoeffOnNewZi(int newZi) {
    int topicSumOnNewZi = ++topicSums[newZi];
    commons[newZi] =
      1.0 / (topicSumOnNewZi + betaV);
    double rCoeffOnNewZi = commons[newZi] * beta;
    rCoeffSum +=
      (rCoeffOnNewZi - rCoeffDistr[newZi]);
    rCoeffDistr[newZi] = rCoeffOnNewZi;
  }
}