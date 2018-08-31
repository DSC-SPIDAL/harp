package edu.iu.harp.keyval;

import org.junit.Assert;
import org.junit.Test;

public class Long2IntKVPartitionTest {
  @Test
  public void testPutKeyVal() {
    Long2IntKVPartition intKVPartition = new Long2IntKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeIntCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testGetVal() {
    Long2IntKVPartition intKVPartition = new Long2IntKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeIntCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testSize() {
    Long2IntKVPartition intKVPartition = new Long2IntKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeIntCombiner());

    Assert.assertEquals(5, intKVPartition.size());
  }

  @Test
  public void clearSize() {
    Long2IntKVPartition intKVPartition = new Long2IntKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeIntCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeIntCombiner());

    intKVPartition.clear();
    Assert.assertEquals(Integer.MIN_VALUE, intKVPartition.getVal(19));
  }
}
