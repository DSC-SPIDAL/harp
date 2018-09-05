/*
 * Copyright 2013-2017 Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.harp.worker;

import edu.iu.harp.depl.Depl;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/*******************************************************
 * The information of the nodes
 ******************************************************/
public class Nodes {

  private static final Logger LOG =
      Logger.getLogger(Nodes.class);

  /**
   * Rack ID and its related nodes
   */
  private Map<Integer, List<String>> nodes;
  /**
   * Maintain the order of racks
   */
  private LinkedList<Integer> nodeRackIDs;
  /**
   * The number of physical nodes
   */
  private int numPhysicalNodes;

  public Nodes() throws Exception {
    BufferedReader reader = new BufferedReader(
        new FileReader(Depl.nodes_file));
    initializeNodes(reader);
    reader.close();
  }

  public Nodes(Map<Integer, List<String>> nodes, LinkedList<Integer> nodeRackIDs,
               int numPhysicalNodes) {
    this.nodes = nodes;
    this.nodeRackIDs = nodeRackIDs;
    this.numPhysicalNodes = numPhysicalNodes;
  }

  public Nodes(BufferedReader reader)
      throws Exception {
    if (reader == null) {
      LOG.info("Read from default nodes file. "
          + Depl.nodes_file);
      reader = new BufferedReader(
          new FileReader(Depl.nodes_file));
      initializeNodes(reader);
      reader.close();
    } else {
      initializeNodes(reader);
    }
  }

  /**
   * Initialization
   *
   * @param reader the BufferedReader
   * @throws Exception
   */
  private void initializeNodes(
      BufferedReader reader) throws Exception {
    nodes = new HashMap<Integer, List<String>>();
    nodeRackIDs = new LinkedList<Integer>();
    int currentRackID = 0;
    Set<String> nodeSet = new HashSet<String>();
    try {
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (Depl.isRack(line)) {
          currentRackID = Depl.getRackID(line);
          // Check if this rack id exists
          if (!nodes.containsKey(currentRackID)) {
            nodeRackIDs.add(currentRackID);
          }
        } else if (!line.equals("")) {
          addNode(currentRackID, line);
          // Check if this is a new physical node
          if (!nodeSet.contains(line)) {
            nodeSet.add(line);
          }
        }
      }
      numPhysicalNodes = nodeSet.size();
    } catch (Exception e) {
      LOG.error("Errors when reading nodes information.", e);
      throw e;
    }
  }

  /**
   * Add the node to the rack
   *
   * @param rackID the ID of the rack
   * @param line   the string of the node
   */
  private void addNode(int rackID, String line) {
    List<String> nodeList = nodes.get(rackID);
    // Add list
    if (nodeList == null) {
      nodeList = new LinkedList<String>();
      nodes.put(rackID, nodeList);
    }
    // If the node exists, put them close.
    int pos = nodeList.indexOf(line);
    if (pos > 0) {
      nodeList.add(pos, line);
    } else {
      nodeList.add(line);
    }
  }

  /**
   * Get the number of physical nodes
   *
   * @return
   */
  public int getNumPhysicalNodes() {
    return numPhysicalNodes;
  }

  /**
   * Get the map from rackID to its associated
   * nodes
   *
   * @return
   */
  protected Map<Integer, List<String>> getNodes() {
    return nodes;
  }

  /**
   * Get the list of the racks
   *
   * @return the ID list of the racks
   */
  protected List<Integer> getRackList() {
    return this.nodeRackIDs;
  }

  /**
   * Get the list of all the nodes
   *
   * @return the list of all the nodes
   */
  public List<String> getNodeList() {
    List<String> nodeList =
        new LinkedList<String>();
    for (Entry<Integer, List<String>> entry : nodes
        .entrySet()) {
      nodeList.addAll(entry.getValue());
    }
    return nodeList;
  }

  /**
   * Sorting the racks based on the number of the
   * nodes associated with the racks in descending
   * order
   */
  public void sortRacks() {
    SortedMap<Integer, Integer> sortedRacks =
        new TreeMap<>();
    // Sort racks based on sizes in Natural
    // ordering
    for (Entry<Integer, List<String>> entry : nodes
        .entrySet()) {
      sortedRacks.put(entry.getValue().size(),
          entry.getKey());
    }
    // Put to list
    List<Integer> rackIDs =
        new LinkedList<>(sortedRacks.values());
    nodeRackIDs.clear();
    // From the highest to the lowest
    for (int rackID : rackIDs) {
      nodeRackIDs.addFirst(rackID);
    }
  }

  /**
   * print the rack and the nodes information
   *
   * @return the rack and the nodes information
   */
  public List<String> printToNodesFile() {
    List<String> rackNodeList =
        new LinkedList<String>();
    for (int rackID : nodeRackIDs) {
      rackNodeList.add("#" + rackID);
      rackNodeList.addAll(nodes.get(rackID));
    }
    return rackNodeList;
  }
}
