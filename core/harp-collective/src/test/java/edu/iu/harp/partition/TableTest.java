package edu.iu.harp.partition;

import edu.iu.harp.combiner.IntArrCombiner;
import edu.iu.harp.combiner.Operation;
import edu.iu.harp.resource.IntArray;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Assert;
import org.junit.Test;

public class TableTest {
  @Test
  public void testGetTableID() {
    Table<IntArray> table = new Table<>(0, new IntArrCombiner(Operation.SUM));
    Assert.assertEquals(0, table.getTableID());
  }

  @Test
  public void testGetCombiner() {
    IntArrCombiner intArrCombiner = new IntArrCombiner(Operation.SUM);
    Table<IntArray> table = new Table<>(0, intArrCombiner);
    Assert.assertSame(intArrCombiner, table.getCombiner());
  }

  @Test
  public void testGetNumPartitions() {
    Table<IntArray> table = new Table<>(0, new IntArrCombiner(Operation.SUM));
    Assert.assertEquals(0, table.getNumPartitions());

    table.addPartition(new Partition<IntArray>(0, new IntArray(new int[19], 0, 19)));
    Assert.assertEquals(1, table.getNumPartitions());
  }

  @Test
  public void testGetPartitionIDs() {
    Table<IntArray> table = new Table<>(0, new IntArrCombiner(Operation.SUM));
    Assert.assertEquals(0, table.getNumPartitions());

    table.addPartition(new Partition<IntArray>(0, new IntArray(new int[19], 0, 19)));
    table.addPartition(new Partition<IntArray>(1, new IntArray(new int[19], 0, 19)));
    Assert.assertNotNull(table.getPartitionIDs());

    IntSet set = table.getPartitionIDs();
    for (Integer t : set) {
      Assert.assertTrue(t == 0 || t == 1);
    }
  }
}
