package edu.iu.harp.keyval;

import org.junit.Assert;
import org.junit.Test;

public class Int2IntKVPartitionTest {
  @Test
  public void testPutKeyVal() {
    Int2IntKVPartition intKVPartition = new Int2IntKVPartition();
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
    Int2IntKVPartition intKVPartition = new Int2IntKVPartition();
    intKVPartition.initialize();
    intKVPartition.putKeyVal(19, 10, new TypeIntCombiner());

    Assert.assertEquals(10, intKVPartition.getVal(19));
  }

  @Test
  public void testSize() {
    Int2IntKVPartition intKVPartition = new Int2IntKVPartition();
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
    Int2IntKVPartition intKVPartition = new Int2IntKVPartition();
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
