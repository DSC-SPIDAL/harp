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

import java.io.IOException;

/**
 * This example demonstrate the use of AllReduce collective operation
 */
public class AllReduce extends AbstractExampleMapper {
  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    for (int j = 0; j < numPartitions; j++) {
      double[] values = new double[elements];
      mseTable.addPartition(new Partition<>(0, new DoubleArray(values, 0, 10000)));
    }

    for (int i = 0; i < numIterations; i++) {
      allreduce("all-reduce", "all-reduce", mseTable);
      mseTable.release();
    }
  }
}
