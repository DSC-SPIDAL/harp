package edu.iu.harp.schstatic;

import edu.iu.harp.schdynamic.SimpleObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class StaticSchedulerTest {
  @Test
  public void testStart() {
    List<TestTask> testTasks = new ArrayList<>();
    testTasks.add(new TestTask());
    testTasks.add(new TestTask());

    StaticScheduler<SimpleObject, SimpleObject, TestTask> scheduler
        = new StaticScheduler<>(testTasks);

    scheduler.start();
    scheduler.stop();
  }

  private static class TestTask extends Task<SimpleObject, SimpleObject>  {
    @Override
    public SimpleObject run(SimpleObject input) throws Exception {
      return new SimpleObject();
    }
  }
}
