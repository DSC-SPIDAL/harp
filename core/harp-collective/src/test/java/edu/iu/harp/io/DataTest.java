package edu.iu.harp.io;

import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.Transferable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DataTest {
  @Test
  public void testData() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList), "reduce");

    Assert.assertTrue(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList), "reduce", 0);

    Assert.assertTrue(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertTrue(data.isPartitionData());
  }

  @Test
  public void testRelease() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data.release();

    Assert.assertFalse(data.isOperationData());
    Assert.assertFalse(data.isData());
    Assert.assertFalse(data.isPartitionData());
  }

  @Test
  public void testEncodeHead() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data.encodeHead();
    data.decodeHeadArray();

    // this should not do anything
    data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));
    DataStatus status = data.decodeHeadArray();
    Assert.assertEquals(DataStatus.DECODED, status);
  }

  @Test
  public void testEncodeBody() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    DataStatus status = data.encodeBody();
    Assert.assertEquals(DataStatus.ENCODED_ARRAY_DECODED, status);
  }

  @Test
  public void testDecodeBody() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data.encodeHead();
    data.encodeBody();

    Data encodedData = new Data(data.getHeadArray(), data.getBodyArray());
    encodedData.decodeHeadArray();
    encodedData.decodeBodyArray();
  }

  @Test
  public void testDecodeHeader() {
    IntArray intArray = new IntArray(new int[1000], 0, 1000);
    List<Transferable> transList = new ArrayList<>(1);
    transList.add(intArray);

    Data data = new Data(DataType.INT_ARRAY, "conn", 0,
        transList, DataUtil.getNumTransListBytes(transList));

    Assert.assertFalse(data.isOperationData());
    Assert.assertTrue(data.isData());
    Assert.assertFalse(data.isPartitionData());

    data.encodeHead();
    data.encodeBody();

    Data encodedData = new Data(data.getHeadArray(), data.getBodyArray());
    encodedData.decodeHeadArray();
  }
}
