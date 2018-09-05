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

package edu.iu.harp.partition;

import edu.iu.harp.resource.Simple;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

/*******************************************************
 * The abstraction of distributed dataset. A table
 * is assigned with an ID for reference.
 ******************************************************/
public class Table<P extends Simple> {

  private final int tableID;
  private final Int2ObjectOpenHashMap<Partition<P>> partitions;
  /*
   * Combiner defines how to merge two partitions
   */
  private final PartitionCombiner<P> combiner;

  /**
   * Constructor.
   *
   * @param tableID  a table is assigned with an ID which
   *                 is convenient for reference. Any ID
   *                 is allowed.
   * @param combiner the combiner used for partitions
   */
  public Table(int tableID,
               PartitionCombiner<P> combiner) {
    this.tableID = tableID;
    this.partitions =
        new Int2ObjectOpenHashMap<>();
    this.combiner = combiner;
  }

  public Table(int tableID,
               PartitionCombiner<P> combiner, int size) {
    this.tableID = tableID;
    this.partitions =
        new Int2ObjectOpenHashMap<>(size);
    this.combiner = combiner;
  }

  /**
   * Get the table ID
   *
   * @return table ID
   */
  public int getTableID() {
    return tableID;
  }

  /**
   * Get the combiner
   *
   * @return combiner
   */
  public PartitionCombiner<P> getCombiner() {
    return this.combiner;
  }

  /**
   * Get the number of partitions in this table
   *
   * @return number of partitions
   */
  public final int getNumPartitions() {
    return partitions.size();
  }

  /**
   * Get the IDs of the partitions in this table
   *
   * @return ID set
   */
  public final IntSet getPartitionIDs() {
    return partitions.keySet();
  }

  /**
   * Get the partitions in this table
   *
   * @return partition collection
   */
  public final ObjectCollection<Partition<P>> getPartitions() {
    return partitions.values();
  }

  /**
   * Add a partition into a table. If the ID of
   * the partition already exists in this table,
   * combine this new partition with the partition
   * of the same ID in this table. Otherwise,
   * insert this new partition to the table.
   *
   * @param partition
   * @return partition status
   */
  public final PartitionStatus addPartition(Partition<P> partition) {
    if (partition == null) {
      return PartitionStatus.ADD_FAILED;
    }
    Partition<P> curPartition =
        this.partitions.get(partition.id());
    if (curPartition == null) {
      return insertPartition(partition);
    } else {
      return combiner.combine(curPartition.get(),
          partition.get());
    }
  }

  /**
   * Insert a partition to this table
   *
   * @param partition
   * @return PartitionStatus.ADDED
   */
  protected final PartitionStatus insertPartition(Partition<P> partition) {
    partitions.put(partition.id(), partition);
    return PartitionStatus.ADDED;
  }

  /**
   * Get the partition by partitionID
   *
   * @param partitionID
   * @return the partition associated with the
   * partitionID
   */
  public final Partition<P> getPartition(int partitionID) {
    return partitions.get(partitionID);
  }

  /**
   * Remove the partition from this table
   *
   * @param partitionID
   * @return the removed partition
   */
  public final Partition<P> removePartition(int partitionID) {
    return partitions.remove(partitionID);
  }

  /**
   * If this table is empty, return true; else,
   * return false.
   *
   * @return true or false
   */
  public final boolean isEmpty() {
    return partitions.isEmpty();
  }

  /**
   * Release the partitions
   */
  public final void release() {
    for (Partition<P> partition : partitions
        .values()) {
      partition.release();
    }
    partitions.clear();
  }

  /**
   * Free the partitions
   */
  public final void free() {
    for (Partition<P> partition : partitions
        .values()) {
      partition.free();
    }
    partitions.clear();
  }
}
