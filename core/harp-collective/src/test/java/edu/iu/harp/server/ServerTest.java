package edu.iu.harp.server;

import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.worker.Workers;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;

public class ServerTest {
  private String fileName;

  @Before
  public void setUp() {
    ClassLoader classLoader = getClass().getClassLoader();
    fileName = new File(Objects.requireNonNull(
        classLoader.getResource("test_nodes")).getFile()).getAbsolutePath();
  }

  @Test
  public void testStart() throws Exception {
    Workers workers = new Workers(new BufferedReader(new FileReader(fileName)), 0);

    Server s = new Server("localhost", 10092, new EventQueue(), new DataMap(), workers);
    s.start();
    s.stop();
  }
}
