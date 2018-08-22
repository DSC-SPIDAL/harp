package edu.iu.examples;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;

public class BCast extends AbstractExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    // lets prepare the data to be reduced
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      double[] values = new double[elements];
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, elements)));
    }

    long startTime = System.currentTimeMillis();
    // we are going to call the same operation num iterations times
    for (int i = 0; i < numIterations; i++) {
      // When calling the operation, each invocation should have a unique operation name, otherwise the calls
      // may not complete
      broadcast("bcast", "bcast-" + i, mseTable, 0, true);
    }
    LOG.info(String.format("Op %s it %d ele %d par %d time %d", cmd, numIterations, elements, numPartitions,
        (System.currentTimeMillis() - startTime)));
  }
}