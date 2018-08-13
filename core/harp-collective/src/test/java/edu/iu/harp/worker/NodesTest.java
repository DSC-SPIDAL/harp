package edu.iu.harp.worker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.soap.Node;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class NodesTest {
  private Nodes nodes;

  @Before
  public void setUp() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("test_nodes").getFile());
    nodes = new Nodes(new BufferedReader(new FileReader(file.getAbsolutePath())));
  }

  @Test
  public void nodesListTest() throws Exception {
    List<String> list = nodes.getNodeList();
    Assert.assertTrue(list.contains("127.0.0.1"));
  }

  @Test
  public void rackListTest() throws Exception {
    List<Integer> list = nodes.getRackList();
    Assert.assertTrue(list.contains(0));
    Assert.assertTrue(list.contains(1));
  }

  @Test
  public void sortRacksTest() throws Exception {
    nodes.sortRacks();
  }
}
