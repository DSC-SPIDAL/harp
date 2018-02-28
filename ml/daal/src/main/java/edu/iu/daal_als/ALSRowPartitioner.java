package edu.iu.daal_als;

import edu.iu.harp.partition.Partitioner;

public class ALSRowPartitioner extends
  Partitioner {

  //numbers store the worker id for each row
  private int[] numbers;

  public ALSRowPartitioner(int maxRowID, int numWorkers) 
  {

    //maxRowID here starts from 1
    super(numWorkers);
    this.numbers = new int[maxRowID+1];

    int num_pernode = (this.numbers.length + numWorkers)/numWorkers;

    for (int i = 1; i < numbers.length; i++) {
        numbers[i] = i/num_pernode;
    }

  }

  public int getWorkerID(int partitionID) {
    //here partition starts from 1
    if (partitionID >= 0
      && partitionID < numbers.length) {
      return numbers[partitionID];
    } else {
      return numbers[partitionID - numbers.length];
    }
  }
}
