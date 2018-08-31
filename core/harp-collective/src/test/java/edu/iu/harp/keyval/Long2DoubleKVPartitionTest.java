package edu.iu.harp.keyval;

import org.junit.Assert;
import org.junit.Test;

public class Long2DoubleKVPartitionTest {
  @Test
  public void testPutKeyVal() {
    Long2DoubleKVPartition intKVPartition = new Long2DoubleKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeDoubleCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testGetVal() {
    Long2DoubleKVPartition intKVPartition = new Long2DoubleKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeDoubleCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testSize() {
    Long2DoubleKVPartition intKVPartition = new Long2DoubleKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeDoubleCombiner());

    Assert.assertEquals(5, intKVPartition.size());
  }

  @Test
  public void clearSize() {
    Long2DoubleKVPartition intKVPartition = new Long2DoubleKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeDoubleCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeDoubleCombiner());

    intKVPartition.clear();
    Assert.assertEquals(Integer.MIN_VALUE, intKVPartition.getVal(19));
  }
}
