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
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.NotNull;

public class Rotate extends AbstractExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) {
    Table<DoubleArray> mseTable = createTable(getSelfID());

    long startTime = System.currentTimeMillis();
    Int2IntMap rotate = new Int2IntArrayMap();
    double expecting = (getSelfID() + 1) % getNumWorkers();
    // we are going to call the same operation num iterations times
    for (int i = 0; i < numIterations; i++) {
      // When calling the operation, each invocation should have a unique
      // operation name, otherwise the calls may not complete
      rotate("main", "rotate-" + i, mseTable, null);
      if (verify) {
        verify(mseTable, expecting, i);
        expecting = (expecting + 1) / getNumWorkers();
      }
    }
    LOG.info(String.format("Op %s it %d ele %d par %d time %d", cmd, numIterations,
        elements, numPartitions, (System.currentTimeMillis() - startTime)));
  }

  @NotNull
  private Table<DoubleArray> createTable(int value) {
    // lets prepare the data to be reduced
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      double[] values = new double[elements];
      mseTable.addPartition(new Partition<>(0,
          new DoubleArray(values, 0, elements)));
    }
    return mseTable;
  }

  /**
   * Verify the results after rotate
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
