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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.mallet.types.Dirichlet;
import cc.mallet.types.LabelSequence;

import edu.iu.harp.schdynamic.Task;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.DoubleArray;

public class CalcLikelihoodTask implements
  Task<List<Partition<TopicCount>>, Object> {

  protected static final Log LOG = LogFactory
    .getLog(CalcLikelihoodTask.class);

  private int numTopics;
  private double alpha;
  private double beta;
  private double logLikelihood; 
  
  //private Int2ObjectOpenHashMap<DocWord>[] vDWMap;
  //List<Partition<TopicCount>>[] wMap;
    
  public CalcLikelihoodTask(int numTopics, double alpha, double beta) {
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
  public Object run(
	List<Partition<TopicCount>> hPartitions)
    throws Exception {
	  
	int nonZeroTypeTopics = 0;
	
    for (Partition<TopicCount> partition : hPartitions) {
      int topic, count;
      
      
      Int2IntOpenHashMap wRow =
    		  partition.get().getTopicCount();
      ObjectIterator<Int2IntMap.Entry> iterator =
    		  wRow.int2IntEntrySet().fastIterator();
      while (iterator.hasNext()) {
    	  	  Int2IntMap.Entry ent = iterator.next();
    		  topic = ent.getIntKey();
    		  count = ent.getIntValue();
    		  
    		  nonZeroTypeTopics++;
    		  logLikelihood += Dirichlet.logGammaStirling(beta + count);
      }
    }
   
	// logGamma(beta) for all type/topic pairs with non-zero count
	logLikelihood -=
		Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;
	
    return null;
  }
  
  }
  
  /*----------------------
  //
  // code port from mallet::cc.mallet.topics::ParallelTopicModel.java
  // @author: pengb, 02172017
  //
  public double modelLogLikelihood() {
		double logLikelihood = 0.0;
		int nonZeroTopics;

		// The likelihood of the model is a combination of a 
		// Dirichlet-multinomial for the words in each topic
		// and a Dirichlet-multinomial for the topics in each
		// document.

		// The likelihood function of a dirichlet multinomial is
		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )

		// So the log likelihood is 
		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

		// Do the documents first

		int[] topicCounts = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];
		int[] docTopics;

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] );
		}
	
		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] + topicCounts[topic]) -
									  topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + docTopics.length);

			Arrays.fill(topicCounts, 0);
		}

		// add the parameter sum term
		logLikelihood += data.size() * Dirichlet.logGammaStirling(alphaSum);

		// And the topics

		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			// reuse this array as a pointer

			topicCounts = typeTopicCounts[type];

			int index = 0;
			while (index < topicCounts.length &&
				   topicCounts[index] > 0) {
				int topic = topicCounts[index] & topicMask;
				int count = topicCounts[index] >> topicBits;
				
				nonZeroTypeTopics++;
				logLikelihood += Dirichlet.logGammaStirling(beta + count);

				if (Double.isNaN(logLikelihood)) {
					logger.warning("NaN in log likelihood calculation");
					return 0;
				}
				else if (Double.isInfinite(logLikelihood)) {
					logger.warning("infinite log likelihood");
					return 0;
				}

				index++;
			}
		}
	
		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= 
				Dirichlet.logGammaStirling( (beta * numTypes) +
											tokensPerTopic[ topic ] );

			if (Double.isNaN(logLikelihood)) {
				logger.info("NaN after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}
			else if (Double.isInfinite(logLikelihood)) {
				logger.info("Infinite value after topic " + topic + " " + tokensPerTopic[ topic ]);
				return 0;
			}

		}
	
		// logGamma(|V|*beta) for every topic
		logLikelihood += 
			Dirichlet.logGammaStirling(beta * numTypes) * numTopics;

		// logGamma(beta) for all type/topic pairs with non-zero count
		logLikelihood -=
			Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;

		if (Double.isNaN(logLikelihood)) {
			logger.info("at the end");
		}
		else if (Double.isInfinite(logLikelihood)) {
			logger.info("Infinite value beta " + beta + " * " + numTypes);
			return 0;
		}

		return logLikelihood;
	}
	
	------------------------------*/
  
  

