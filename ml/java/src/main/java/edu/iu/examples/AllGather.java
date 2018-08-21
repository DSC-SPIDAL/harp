package edu.iu.examples;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;

import java.io.IOException;

/**
 * This is an all gather example.
 */
public class AllGather extends AbstractExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {

    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      double[] values = new double[elements];
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, 10000)));
    }

    for (int i = 0; i < numIterations; i++) {
      allgather("all-gather", "all-gather", mseTable);
      mseTable.release();
    }
  }
}