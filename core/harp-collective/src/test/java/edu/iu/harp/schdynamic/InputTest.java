package edu.iu.harp.schdynamic;

import org.junit.Assert;
import org.junit.Test;

public class InputTest {
  @Test
  public void intputTest() {
    SimpleObject simpleObject = new SimpleObject();
    Input<SimpleObject> input = new Input<>(simpleObject, false, false);

    SimpleObject si = input.getInput();
    Assert.assertEquals(simpleObject, si);
    Assert.assertEquals(false, input.isPause());
    Assert.assertEquals(false, input.isStop());
  }
}
