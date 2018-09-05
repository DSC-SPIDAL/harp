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

/**
 * This is an all gather example.
 */
public class AllGather extends BaseExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) {
    long startTime = System.currentTimeMillis();
    // we are going to call the same operation numIterations times
    for (int i = 0; i < numIterations; i++) {
      // lets prepare the data to be reduced
      Table<DoubleArray> mseTable = createTable();
      // When calling the operation, each invocation should have a unique operation name, otherwise the calls
      // may not complete
      allgather("all-gather", "all-gather-" + i, mseTable);

      if (verify) {
        verify(mseTable);
      }
    }

    LOG.info(String.format("Op %s it %d ele %d par %d time %d", cmd, numIterations, elements,
        numPartitions, (System.currentTimeMillis() - startTime)));
  }

  private void verify(Table<DoubleArray> mseTable) {
    ObjectCollection<Partition<DoubleArray>> partitions = mseTable.getPartitions();

    if (partitions.size() != numMappers * numPartitions) {
      throw new RuntimeException("Un-expected number of partitions, expected: "
          + numMappers * numPartitions + " got: " + partitions.size());
    }

    for (Partition<DoubleArray> p : partitions) {
      double[] dArray = p.get().get();
      for (double d : dArray) {
        if (d != 2) {
          throw new RuntimeException("Un-expected value, expected: " + 2 + " got: " + d);
        }
      }
    }
    LOG.info("Verification success");
  }

  @NotNull
  private Table<DoubleArray> createTable() {
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      double[] values = new double[elements];
      for (int k = 0; k < values.length; k++) {
        values[k] = 2;
      }
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, elements)));
    }
    return mseTable;
  }
}