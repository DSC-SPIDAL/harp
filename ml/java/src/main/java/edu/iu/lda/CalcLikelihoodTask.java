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

import cc.mallet.types.Dirichlet;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.schdynamic.Task;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * code port from
 * mallet::cc.mallet.topics::ParallelTopicModel.java
 * 
 **/
public class CalcLikelihoodTask implements
  Task<Partition<TopicCountList>, Object> {

  protected static final Log LOG =
    LogFactory.getLog(CalcLikelihoodTask.class);

  private int numTopics;
  private double alpha;
  private double beta;
  private double logLikelihood;

  public CalcLikelihoodTask(int numTopics,
    double alpha, double beta) {
    this.alpha = alpha;
    this.beta = beta;
    this.numTopics = numTopics;
    this.logLikelihood = 0.;
  }

  public double getLikelihood() {
    double ret = logLikelihood;
    logLikelihood = 0.0;
    return ret;
  }

  @Override
  public Object
    run(Partition<TopicCountList> partition)
      throws Exception {
    int nonZeroTypeTopics = 0;
    LongArrayList wRow =
      partition.get().getTopicCount();
    for (int i = 0; i < wRow.size(); i++) {
      long t = wRow.getLong(i);
      int topic = (int) t;
      int count = (int) (t >>> 32);
      nonZeroTypeTopics++;
      logLikelihood +=
        Dirichlet.logGammaStirling(beta + count);
    }
    // logGamma(beta) for all type/topic pairs
    // with non-zero count
    logLikelihood -=
      Dirichlet.logGammaStirling(beta)
        * nonZeroTypeTopics;
    return null;
  }
}
