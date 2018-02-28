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

package edu.iu.harp.partition;

import edu.iu.harp.resource.Simple;

/*******************************************************
 * A PartitionCombiner defines how to merge two
 * partitions
 ******************************************************/
public abstract class PartitionCombiner<P extends Simple> {

  /**
   * Defines how to merge two partitions.
   * 
   * @param curPar
   *          current partition
   * @param newPar
   *          new partition
   * @return partition status
   */
  public abstract PartitionStatus
    combine(P curPar, P newPar);
}
