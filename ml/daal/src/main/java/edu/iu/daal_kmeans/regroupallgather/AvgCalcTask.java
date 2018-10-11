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

package edu.iu.daal_kmeans.regroupallgather;

import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AvgCalcTask implements
  Task<DoubleArray, Object> {

  protected static final Log LOG = LogFactory
    .getLog(AvgCalcTask.class);

  private final int cenVecSize;

  public AvgCalcTask(int cenVecSize) 
  {
        this.cenVecSize = cenVecSize;
  }

  /**
   * @brief compute the average values for 
   * each entrie of centroids vectors
   * sentinel element stores the total counts 
   *
   * @param points
   *
   * @return 
   */
  @Override
  public Object run(DoubleArray points) throws Exception {

      double[] doubles = points.get();
      int size = points.size();
      for (int j = 0; j < size; j += cenVecSize) 
      {
          if (doubles[j] != 0) 
          {
              for (int k = 1; k < cenVecSize; k++) {
                  doubles[j + k] /= doubles[j];
              }
          }

      }

      return null;
  }

}
