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

package edu.iu.subgraph;

// import java.util.Random;

import edu.iu.harp.partition.Partitioner;

public class SCPartitioner2
  extends Partitioner {

  // private Random random;
  // private int[] numbers;

  public SCPartitioner2(int numWorkers) {
    super(numWorkers);
  }

  public int getWorkerID(int partitionID) {

      //unpack the partitionID and find recev workerID
      //create partition id, the upper 16 bits stores receiver id
      //the lower 16 bits stores sender id
      return ( partitionID >>> 20 );
  }

}
