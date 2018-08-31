package edu.iu.harp.keyval;

import org.junit.Assert;
import org.junit.Test;

public class Int2LongKVPartitionTest {
  @Test
  public void testPutKeyVal() {
    Int2LongKVPartition intKVPartition = new Int2LongKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeLongCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testGetVal() {
    Int2LongKVPartition intKVPartition = new Int2LongKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeLongCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testSize() {
    Int2LongKVPartition intKVPartition = new Int2LongKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeLongCombiner());

    Assert.assertEquals(5, intKVPartition.size());
  }

  @Test
  public void clearSize() {
    Int2LongKVPartition intKVPartition = new Int2LongKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(11, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(13, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(14, 10, new TypeLongCombiner());
    intKVPartition.putKeyVal(15, 10, new TypeLongCombiner());

    intKVPartition.clear();
    Assert.assertEquals(Long.MIN_VALUE, intKVPartition.getVal(19));
  }
}
