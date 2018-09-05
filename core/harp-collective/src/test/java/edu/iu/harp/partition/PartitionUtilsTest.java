package edu.iu.harp.partition;

import edu.iu.harp.combiner.IntArrCombiner;
import edu.iu.harp.combiner.Operation;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.Transferable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class PartitionUtilsTest {
  @Test
  public void testAddPartitionsToTable() {
    Table<IntArray> table = new Table<>(0, new IntArrCombiner(Operation.SUM));
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    Partition<IntArray> p = new Partition<>(0, intArray);

    List<Transferable> transList = new ArrayList<>(1);
    transList.add(p);

    PartitionUtil.addPartitionsToTable(transList, table);
    Assert.assertEquals(1, table.getNumPartitions());
  }
}
