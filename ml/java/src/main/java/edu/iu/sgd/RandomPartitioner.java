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

package edu.iu.sgd;

import edu.iu.harp.partition.Partitioner;

import java.util.Random;

public class RandomPartitioner
  extends Partitioner {

  private Random random;
  private int[] numbers;

  public RandomPartitioner(int maxRowID,
    long seed, int numWorkers) {
    super(numWorkers);
    this.random = new Random(seed);
    this.numbers = new int[maxRowID + 1];
    for (int i = 0; i < numbers.length; i++) {
      numbers[i] = random.nextInt(numWorkers);
    }
  }

  public int getWorkerID(int partitionID) {
    if (partitionID >= 0
      && partitionID < numbers.length) {
      return numbers[partitionID];
    } else {
      return numbers[partitionID
        - numbers.length];
    }
  }
}
