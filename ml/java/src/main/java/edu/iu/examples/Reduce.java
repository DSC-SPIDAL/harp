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
package edu.iu.examples;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Reduce operation
 */
public class Reduce extends AbstractExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {


    long startTime = System.currentTimeMillis();
    double expected = getNumWorkers();
    // we are going to call the same operation num iterations times
    for (int i = 0; i < numIterations; i++) {
      Table<DoubleArray> mseTable = createTable();
      // When calling the operation, each invocation should have a unique operation name, otherwise the calls
      // may not complete
      reduce("reduce", "reduce-" + i, mseTable, 0);

      if (verify) {
        if (getSelfID() != 0) {
          verify(mseTable, 1, i);
        } else {
          verify(mseTable, expected, i);
        }
      }
    }
    LOG.info(String.format("Op %s it %d ele %d par %d time %d", cmd, numIterations, elements, numPartitions,
        (System.currentTimeMillis() - startTime)));
  }

  @NotNull
  private Table<DoubleArray> createTable() {
    // lets prepare the data to be reduced
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      // lets start with 1, after every round the o'th worker will have
      double[] values = new double[elements];
      for (int i = 0; i < values.length; i++) {
        values[i] = 1;
      }
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, elements)));
    }
    return mseTable;
  }

  /**
   * Verify the results after broadcast
   * @param mseTable the data table
   * @param expected expected value
   */
  private void verify(Table<DoubleArray> mseTable, double expected, int iteration) {
    ObjectCollection<Partition<DoubleArray>> partitions = mseTable.getPartitions();
    for (Partition<DoubleArray> p : partitions) {
      double[] dArray = p.get().get();
      for (double d : dArray) {
        if (d != expected) {
          LOG.warn("Un-expected value, expected: " + expected + " got: "
              + d + " iteration: " + iteration);
        }
      }
    }
    LOG.info("Verification success");
  }
}
