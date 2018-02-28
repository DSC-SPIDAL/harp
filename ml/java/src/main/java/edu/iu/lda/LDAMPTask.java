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

import edu.iu.dymoro.MPTask;
import edu.iu.harp.partition.Partition;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Random;

public class LDAMPTask extends
  MPTask<Int2ObjectOpenHashMap<DocWord>, TopicCountList> {

  protected static final Log LOG =
    LogFactory.getLog(LDAMPTask.class);
  private final LongArrayList[] docMap;
  private final int numTopics;
  private final double alpha;
  private final double oneOverAlpha;
  private final double beta;
  private final Random random;
  private final double[] commons;
  private final double[] rCoeffDistr;
  private double rCoeffSum;
  private final int[] s;
  private int sSize;
  private final boolean[] sInUse;
  private final int[] sCountDistr;
  private final double[] sCoeffDistr;
  private double sCoeffSum;
  private final double[] tProb;
  private int tSize;
  private double tProbSum;
  private int newTi;

  public LDAMPTask(LongArrayList[] docMap,
    int numTopics, double alpha, double beta,
    double[] globalCommons,
    double[] globalRCoeffDistr) {
    this.docMap = docMap;
    this.numTopics = numTopics;
    this.alpha = alpha;
    this.oneOverAlpha = 1.0 / alpha;
    this.beta = beta;
    random =
      new Random(System.currentTimeMillis());
    commons = globalCommons;
    rCoeffDistr = globalRCoeffDistr;
    rCoeffSum = 0.0;
    s = new int[numTopics];
    sSize = 0;
    sInUse = new boolean[numTopics];
    sCountDistr = new int[numTopics];
    sCoeffDistr = new double[numTopics];
    sCoeffSum = 0.0;
    tProb = new double[numTopics];
    tSize = 0;
    tProbSum = 0.0;
    newTi = -1;
  }

  public void setRCoeffSum(double rCoeffSum) {
    this.rCoeffSum = rCoeffSum;
  }

  @Override
  public long doRun(
    List<Partition<TopicCountList>> partitionList,
    Int2ObjectOpenHashMap<DocWord> docWordMap) {
    long numToken = 0L;
    for (Partition<TopicCountList> partition : partitionList) {
      DocWord docWord =
        docWordMap.get(partition.id());
      if (docWord != null) {
        // Go through all the docs of this word
        LongArrayList wRow =
          partition.get().getTopicCount();
        for (int i = 0; i < docWord.numV; i++) {
          LongArrayList dRow =
            docMap[docWord.id2[i]];
          int[] z = docWord.z[i];
          for (int j = 0; j < z.length; j++) {
            // Init word & doc prob on old zi
            if (j == 0) {
              if (i == 0) {
                initWordProb(wRow);
              }
              updateWordProbOnOldZi(z[j]);
              initDocProb(z[j], dRow);
            } else {
              // Update word & doc prob on old zi
              updateDocProbOnOldZi(z[j],
                updateWordProbOnOldZi(z[j]),
                dRow);
            }
            // Draw a new sample
            int newZi = sample(dRow);
            if (newZi >= 0 && newZi < numTopics) {
              z[j] = newZi;
            } else {
              LOG.info("Fail to sample.");
              newZi = z[j];
            }
            // Update word & doc prob on new Zi
            updateProbOnNewZi(newZi, newTi, wRow,
              dRow, i == (docWord.numV - 1),
              j == (z.length - 1));
          }
          numToken += z.length;
        }
      }
    }
    return numToken;
  }

  private void initWordProb(LongArrayList wRow) {
    sSize = wRow.size();
    for (int i = 0; i < sSize; i++) {
      long t = wRow.getLong(i);
      int wTopicID = (int) t;
      int wTopicCount = (int) (t >>> 32);
      s[i] = wTopicID;
      sInUse[wTopicID] = true;
      sCountDistr[wTopicID] = wTopicCount;
      double sCoeffOnTopic =
        commons[wTopicID] * wTopicCount;
      sCoeffDistr[wTopicID] = sCoeffOnTopic;
      sCoeffSum += sCoeffOnTopic;
    }
  }

  private int updateWordProbOnOldZi(int oldZi) {
    int wTCOnOldZi = --sCountDistr[oldZi];
    if (wTCOnOldZi > 0) {
      sCoeffDistr[oldZi] -= commons[oldZi];
    } else {
      sCoeffDistr[oldZi] = 0.0;
    }
    sCoeffSum -= commons[oldZi];
    return wTCOnOldZi;
  }

  private void initDocProb(int oldZi,
    LongArrayList dRow) {
    tSize = dRow.size();
    tProbSum = 0.0;
    for (int i = 0; i < tSize;) {
      long t = dRow.getLong(i);
      int dTopicID = (int) t;
      int dTopicCount = (int) (t >>> 32);
      if (dTopicID == oldZi) {
        if (--dTopicCount > 0) {
          dRow.set(i, t - Constants.TOPIC_DELTA);
        } else {
          dRow.set(i, dRow.getLong(--tSize));
          dRow.size(tSize);
          continue;
        }
      }
      double tProbOnTopic = (rCoeffDistr[dTopicID]
        + sCoeffDistr[dTopicID]) * dTopicCount;
      tProbSum += tProbOnTopic;
      tProb[i++] = tProbOnTopic;
    }
  }

  private void updateDocProbOnOldZi(int oldZi,
    int wTCOnOldZi, LongArrayList dRow) {
    for (int i = 0; i < tSize; i++) {
      if (oldZi == (int) dRow.getLong(i)) {
        long t = dRow.getLong(i);
        int dTCOnOldZi = (int) (t >>> 32) - 1;
        if (dTCOnOldZi > 0) {
          dRow.set(i, t - Constants.TOPIC_DELTA);
          double delta =
            (beta + 1.0 + wTCOnOldZi + dTCOnOldZi)
              * commons[oldZi];
          tProbSum -= delta;
          tProb[i] -= delta;
        } else {
          tProbSum -= tProb[i];
          tProb[i] = tProb[--tSize];
          dRow.set(i, dRow.getLong(tSize));
          dRow.size(tSize);
        }
        return;
      }
    }
  }

  private int sample(LongArrayList dRow) {
    // int region = -1;
    double nextRandom =
      random.nextDouble() * (tProbSum
        + (rCoeffSum + sCoeffSum) * alpha);
    if (nextRandom <= tProbSum) {
      // t region
      // region = 2;
      for (int ti = 0; ti < tSize; ti++) {
        nextRandom -= tProb[ti];
        if (nextRandom <= 0.0) {
          newTi = ti;
          return (int) dRow.getLong(ti);
        }
      }
    } else {
      // remember s and r only contain
      // coefficient sum
      nextRandom =
        (nextRandom - tProbSum) * oneOverAlpha;
      if (nextRandom <= sCoeffSum) {
        // s region
        // region = 1;
        for (int si = 0; si < sSize; si++) {
          nextRandom -= sCoeffDistr[s[si]];
          if (nextRandom <= 0.0) {
            newTi = -1;
            return s[si];
          }
        }
      } else {
        // r region
        // region = 0;
        nextRandom -= sCoeffSum;
        for (int ri = 0; ri < numTopics; ri++) {
          nextRandom -= rCoeffDistr[ri];
          if (nextRandom <= 0.0) {
            newTi = -1;
            return ri;
          }
        }
      }
    }
    newTi = -1;
    return -1;
  }

  private void updateProbOnNewZi(int newZi,
    int newTi, LongArrayList wRow,
    LongArrayList dRow, boolean lastDoc,
    boolean lastToken) {
    // update word topic count
    int wTCOnNewZi = ++sCountDistr[newZi];
    if (wTCOnNewZi == 1) {
      if (!sInUse[newZi]) {
        sInUse[newZi] = true;
        s[sSize++] = newZi;
      }
    }
    // update doc topic count
    if (newTi == -1) {
      for (int i = 0; i < tSize; i++) {
        if (newZi == (int) dRow.getLong(i)) {
          newTi = i;
          break;
        }
      }
    }
    if (newTi != -1) {
      dRow.set(newTi, dRow.getLong(newTi)
        + Constants.TOPIC_DELTA);
    } else {
      dRow.add(newZi + Constants.TOPIC_DELTA);
      newTi = tSize++;
    }
    // If not the last token,
    // update the probability
    // else write topic count back to word
    if (!(lastDoc && lastToken)) {
      // update s coefficient distribution
      if (wTCOnNewZi == 1) {
        sCoeffDistr[newZi] = commons[newZi];
      } else {
        sCoeffDistr[newZi] += commons[newZi];
      }
      sCoeffSum += commons[newZi];
      // update t probability
      if (!lastToken) {
        int dTCOnNewZi =
          (int) (dRow.getLong(newTi) >>> 32);
        if (dTCOnNewZi == 1) {
          double delta = rCoeffDistr[newZi]
            + sCoeffDistr[newZi];
          tProbSum += delta;
          tProb[newTi] = delta;
        } else {
          double delta =
            (beta - 1.0 + wTCOnNewZi + dTCOnNewZi)
              * commons[newZi];
          tProbSum += delta;
          tProb[newTi] += delta;
        }
      }
    } else {
      // last token in last doc of the word
      // write topic count back
      if (sSize > 0) {
        wRow.clear();
        for (int i = 0; i < sSize; i++) {
          if (sCountDistr[s[i]] > 0) {
            wRow.add(
              (((long) sCountDistr[s[i]]) << 32)
                + (long) s[i]);
            sCountDistr[s[i]] = 0;
            sCoeffDistr[s[i]] = 0.0;
          }
          sInUse[s[i]] = false;
        }
        sSize = 0;
        sCoeffSum = 0.0;
      }
    }
  }
}