package edu.iu.harp.worker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class WorkerTest {
  private String fileName;

  @Before
  public void setUp() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    fileName = new File(classLoader.getResource("test_nodes").getFile()).getAbsolutePath();
  }

  @Test
  public void testWorker() throws Exception {
    Workers workers = new Workers(new BufferedReader(new FileReader(fileName)), 0);

    Assert.assertEquals(workers.getMasterID(), 0);
    Assert.assertEquals(workers.getMaxID(), 1);
    Assert.assertEquals(workers.getMiddleID(), 0);
  }
}
