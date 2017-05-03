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

package edu.iu.harp.keyval;

import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.Table;

/*******************************************************
 * KVTable is a table storing key-value pairs
 ******************************************************/
public abstract class KVTable<P extends KVPartition>
  extends Table<P> {

  public KVTable(int tableID,
    PartitionCombiner<P> combiner) {
    super(tableID, combiner);
  }
}
