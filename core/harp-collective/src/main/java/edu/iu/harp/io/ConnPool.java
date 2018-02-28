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

package edu.iu.harp.io;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;

/*******************************************************
 * This class manages connection objects
 ******************************************************/
public class ConnPool {

  private static final Logger LOG =
    Logger.getLogger(ConnPool.class);

  private static ConnPool instance = null;

  private Object2ObjectOpenHashMap<HostPort, Pool> connMap =
    new Object2ObjectOpenHashMap<>();

  /*******************************************************
   * The class for host and port information
   ******************************************************/
  private class HostPort {
    private String host;
    private int port;

    private HostPort(String host, int port) {
      this.host = host;
      this.port = port;
    }

    /**
     * The hashCode function
     */
    public int hashCode() {
      return port;
    }

    /**
     * The equals function
     */
    public boolean equals(Object object) {
      HostPort hp = (HostPort) object;
      if (this.host.equals(hp.host)
        && this.port == hp.port) {
        return true;
      } else {
        return false;
      }
    }
  }

  /*******************************************************
   * This pool of connection objects
   ******************************************************/
  private class Pool {
    /** In-use connection objects */
    private HashSet<Connection> inUseSet;
    /** Not-in-use connection objects */
    private LinkedList<Connection> freeQueue;

    private Pool() {
      inUseSet = new HashSet<>();
      freeQueue = new LinkedList<>();
    }
  }

  private ConnPool() {
    connMap = new Object2ObjectOpenHashMap<>();
  }

  /**
   * Singleton pattern. Get the ConnPool instance
   * 
   * @return the CoonnPool instance
   */
  public static ConnPool get() {
    if (instance != null) {
      return instance;
    } else {
      return create();
    }
  }

  /**
   * Create a ConnPool object
   * 
   * @return the ConnPool object
   */
  private synchronized static ConnPool create() {
    if (instance == null) {
      instance = new ConnPool();
    }
    return instance;
  }

  /**
   * Get a connection object by host and port
   * information.
   * 
   * @param host
   *          the host
   * @param port
   *          the port
   * @param useCache
   *          use cache or not
   * @return the connection object
   */
  synchronized Connection getConn(String host,
    int port, boolean useCache) {
    if (useCache) {
      HostPort hostPort =
        new HostPort(host, port);
      Pool pool = connMap.get(hostPort);
      if (pool == null) {
        pool = new Pool();
        connMap.put(hostPort, pool);
      }
      if (!pool.freeQueue.isEmpty()) {
        Connection conn =
          pool.freeQueue.removeFirst();
        pool.inUseSet.add(conn);
        return conn;
      } else {
        Connection conn =
          newConn(host, port, useCache);
        if (conn != null) {
          pool.inUseSet.add(conn);
        }
        return conn;
      }
    } else {
      return newConn(host, port, useCache);
    }
  }

  /**
   * Create a new connection for the host and the
   * port
   * 
   * @param host
   *          the host
   * @param port
   *          the port
   * @param useCache
   *          use cache or not
   * @return the connection object
   */
  private Connection newConn(String host,
    int port, boolean useCache) {
    Connection conn = null;
    boolean isFailed = false;
    int count = 0;
    do {
      isFailed = false;
      try {
        conn =
          new Connection(host, port, 0, useCache);
      } catch (Exception e) {
        isFailed = true;
        count++;
        LOG.error("Error when connecting " + host
          + ":" + port + ", " + e.getMessage());
        try {
          Thread.sleep(Constant.LONG_SLEEP);
        } catch (Exception e1) {
        }
      }
    } while (isFailed
      && count < Constant.SMALL_RETRY_COUNT);
    if (isFailed) {
      LOG.error(
        "Fail to connect " + host + ":" + port);
    }
    return conn;
  }

  synchronized boolean
    releaseConn(Connection conn) {
    if (conn == null) {
      return false;
    }
    HostPort hostPort = new HostPort(
      conn.getNode(), conn.getPort());
    Pool pool = connMap.get(hostPort);
    if (pool != null) {
      if (pool.inUseSet.remove(conn)) {
        pool.freeQueue.add(conn);
        return true;
      } else {
        closeConn(conn);
        return false;
      }
    } else {
      closeConn(conn);
      return false;
    }
  }

  synchronized void freeConn(Connection conn) {
    if (conn == null) {
      return;
    }
    HostPort hostPort = new HostPort(
      conn.getNode(), conn.getPort());
    Pool pool = connMap.get(hostPort);
    if (pool != null) {
      pool.inUseSet.remove(conn);
    }
    closeConn(conn);
  }

  private void closeConn(Connection conn) {
    // LOG.info("Close the connection to "
    // + conn.getNode() + ":" + conn.getPort());
    // Client side notification
    try {
      conn.getOutputStream()
        .write(Constant.CONNECTION_END);
      conn.getOutputStream().flush();
    } catch (Exception e) {
    }
    conn.close();
  }

  /**
   * Clean all released connections
   */
  public synchronized void clean() {
    for (Pool pool : connMap.values()) {
      for (Connection conn : pool.freeQueue) {
        closeConn(conn);
      }
      pool.freeQueue.clear();
    }
  }

  public synchronized void log() {
    ObjectIterator<Object2ObjectMap.Entry<HostPort, Pool>> iterator =
      connMap.object2ObjectEntrySet()
        .fastIterator();
    while (iterator.hasNext()) {
      Object2ObjectMap.Entry<HostPort, Pool> entry =
        iterator.next();
      HostPort hostPort = entry.getKey();
      LOG.info("connection=" + hostPort.host + ":"
        + hostPort.port + ", use="
        + entry.getValue().inUseSet.size()
        + ", released="
        + entry.getValue().freeQueue.size());
    }
  }
}
