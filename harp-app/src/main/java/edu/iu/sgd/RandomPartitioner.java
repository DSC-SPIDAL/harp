package edu.iu.sgd;

import java.util.Random;

import edu.iu.harp.partition.Partitioner;

public class RandomPartitioner extends
  Partitioner {

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
      return numbers[partitionID - numbers.length];
    }
  }
}
