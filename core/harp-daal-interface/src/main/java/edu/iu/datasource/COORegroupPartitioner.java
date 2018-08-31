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

package edu.iu.datasource;

import java.util.Random;

import edu.iu.harp.partition.Partitioner;

public class COORegroupPartitioner extends
    Partitioner {

  //numbers store the worker id for each row
  private int[] numbers;

  public COORegroupPartitioner(int maxRowID, int numWorkers) {

    //maxRowID here starts from 1
    super(numWorkers);
    this.numbers = new int[maxRowID + 1];

    int num_pernode = (this.numbers.length + numWorkers) / numWorkers;

    for (int i = 1; i < this.numbers.length; i++)
      numbers[i] = i / num_pernode;

  }

  public int getWorkerID(int partitionID) {
    //here partition starts from 1
    if (partitionID >= 0 && partitionID < this.numbers.length)
      return numbers[partitionID];
    else if (partitionID >= this.numbers.length)
      return numbers[partitionID - this.numbers.length];
    else
      return 0;
  }
}
