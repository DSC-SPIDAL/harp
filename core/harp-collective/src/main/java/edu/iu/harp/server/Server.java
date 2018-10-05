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

package edu.iu.harp.server;

import edu.iu.harp.io.Connection;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.io.IOUtil;
import edu.iu.harp.schdynamic.ComputeUtil;
import edu.iu.harp.worker.Workers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/*******************************************************
 * The server for communication
 ******************************************************/
public class Server implements Runnable {

  private static final Logger LOG =
          Logger.getLogger(Server.class);

  /** Data queue shared with the event machine */
  private final EventQueue eventQueue;
  /**
   * Data map for collective communication
   * operations
   */
  private final DataMap dataMap;
  private Thread server;
  private List<Thread> acceptorThreads;
  private List<Acceptor> acceptors;
  /** Make sure the access is synchronized */
  private final Workers workers;

  private boolean forceStopped;

  /**
   * Cache necessary information since "workers"
   * is global
   */
  private final String node;
  private final int port;
  /** Server socket */
  private final ServerSocket serverSocket;

  /**
   * Initialization
   *
   * @param node
   *          the host
   * @param port
   *          the port
   * @param queue
   *          the EventQueue
   * @param map
   *          the DataMap
   * @param workers
   *          the Workers
   * @throws Exception
   */
  public Server(String node, int port,
                EventQueue queue, DataMap map,
                Workers workers) throws Exception {
    this.eventQueue = queue;
    this.dataMap = map;
    server = new Thread(this);
    server.setName("Harp-Server");
    acceptorThreads = new ObjectArrayList<>();
    acceptors = new ObjectArrayList<>();
    this.workers = workers;
    // Cache local information
    this.node = node;
    this.port = port;
    // Server socket
    try {
      serverSocket = new ServerSocket();
      IOUtil.setServerSocketOptions(serverSocket);
      serverSocket
              .bind(new InetSocketAddress(node, port));
    } catch (Exception e) {
      LOG.error("Error in starting receiver.", e);
      throw new Exception(e);
    }
    LOG.info("Server on " + this.node + " "
            + this.port + " starts.");
  }

  /**
   * Start the server
   */
  public void start() {
    server.start();
  }

  /**
   * Stop the server. Close acceptorThreads and the
   * server
   */
  public void stop() {
    this.stop(false);
  }

  public void stop(boolean force){
    if(!force) {
      closeServer(this.node, this.port);
      for (Thread thread : acceptorThreads) {
        ComputeUtil.joinThread(thread);
      }
      ComputeUtil.joinThread(server);
      this.closeSocket();
    }else{
      this.closeSocket();
      this.forceStopped = true;
      for (Acceptor acceptor : acceptors) {
        acceptor.forceStop();
      }
      ComputeUtil.joinThread(server);
    }

  }

  private void closeSocket(){
    try {
      serverSocket.close();
    } catch (IOException e) {
      LOG.error("Fail to stop the server.", e);
    }
    LOG.info("Server on " + this.node + " "
            + this.port + " is stopped.");
  }

  /**
   * Close the server
   *
   * @param ip
   * @param port
   */
  private void closeServer(String ip, int port) {
    Connection conn =
            Connection.create(ip, port, false);
    if (conn == null) {
      LOG.error("Fail to close the server");
      return;
    }
    try {
      OutputStream out = conn.getOutputStream();
      out.write(Constant.SERVER_QUIT);
      out.flush();
    } catch (Exception e) {
      LOG.error("Fail to close the server", e);
    } finally {
      conn.free();
    }
  }

  /**
   * The overridden run function for receiving
   * data from clients
   */
  @Override
  public void run() {
    // All commands should use positive byte
    // integer 0 ~ 127
    byte commandType = -1;
    int threadNum=0;
    while (!this.forceStopped) {
      ServerConn conn = null;
      try {
        Socket socket = serverSocket.accept();
        IOUtil.setSocketOptions(socket);
        InputStream in = socket.getInputStream();
        // Receiver connection
        conn = new ServerConn(in, socket);
        commandType = (byte) in.read();
      } catch (Exception e) {
        LOG.error("Exception on Server", e);
        if (conn != null) {
          conn.close();
          conn = null;
        }
        continue;
      }
      if (commandType == Constant.SERVER_QUIT) {
        conn.close();
        break;
      } else {
        Acceptor acceptor =
                new Acceptor(conn, eventQueue, dataMap,
                        workers, commandType);
        acceptors.add(acceptor);
        Thread thread = new Thread(acceptor);
        thread.setName("harp-thread-"+(threadNum++));
        thread.start();
        acceptorThreads.add(thread);
      }
    }
  }
}