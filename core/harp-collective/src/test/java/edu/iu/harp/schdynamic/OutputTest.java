package edu.iu.harp.schdynamic;

import org.junit.Assert;
import org.junit.Test;

public class OutputTest {
  @Test
  public void outputTest() {
    SimpleObject simpleObject = new SimpleObject();
    Output<SimpleObject> output = new Output<>(simpleObject, true);

    SimpleObject so = output.getOutput();

    Assert.assertEquals(simpleObject, so);
    Assert.assertEquals(true, output.isError());
  }
}
