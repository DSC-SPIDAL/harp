package edu.iu.daal_als;

import edu.iu.harp.partition.Partitioner;

public class ALSColPartitioner extends
  Partitioner {

  //numbers store the worker id for each row
  private int[] numbers;

  public ALSColPartitioner(int maxColID, int numWorkers) 
  {

    super(numWorkers);
    this.numbers = new int[maxColID + 1];

    int num_pernode = (this.numbers.length + numWorkers)/numWorkers;

    for (int i = 0; i < numbers.length; i++) {
        numbers[i] = i/num_pernode;
    }

  }

  public int getWorkerID(int partitionID) {
    if (partitionID >= 0
      && partitionID < numbers.length) {
      return numbers[partitionID];
    } else {
      return numbers[partitionID - numbers.length];
    }
  }
}
