package edu.iu.harp.example.kmeans.k8s;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class HelloWorld {
  protected static final Logger LOG = Logger.getLogger(HelloWorld.class);

  public static int SERVER_PORT = 9009;

  public static void main(String[] args) {
    String podIP = System.getenv("POD_IP");
    String podName = System.getenv("POD_NAME");
    int numberOfWorkers = Integer.parseInt(System.getenv("NUMBER_OF_WORKERS"));

    LOG.info("Hello from the worker: " + getPodIndex(podName));
    LOG.info("My PodIP: " + podIP);
    LOG.info("My PodName: " + podName);
    LOG.info("numberOfWorkers: " + numberOfWorkers);

    String namespace = "default";
    Map<String, String> podMap =
        PodWatchUtils.getWorkerIPsByWatchingPodsToRunning(namespace, numberOfWorkers, 100);

    // List pods:
    StringBuffer toLog = new StringBuffer();
    for (Map.Entry<String, String> entry: podMap.entrySet()) {
      toLog.append(entry.getKey() + ": " + entry.getValue() + "\n");
    }
    LOG.info("Pod List: \n" + toLog.toString());

//    echoServer(SERVER_PORT, podName);

    waitIndefinitely();
  }

  /**
   * a test method to make the worker wait indefinitely
   */
  public static void waitIndefinitely() {

    while (true) {
      try {
        LOG.info("Waiting idly to be deleted. Sleeping 100sec. "
            + "Time: " + new java.util.Date());
        Thread.sleep(100000);
      } catch (InterruptedException e) {
        LOG.warn("Thread sleep interrupted.", e);
      }
    }
  }

  /**
   * get pod Index from pod name
   * @param podName
   * @return
   */
  public static int getPodIndex(String podName) {
    return Integer.parseInt(podName.substring(podName.lastIndexOf("-") + 1));
  }

  /**
   * an echo server.
   */
  public static void echoServer(int serverPort, String podName) {

    // create socket
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(serverPort);
    } catch (IOException e) {
      LOG.warn("Could not start ServerSocket.", e);
    }

    LOG.info("Echo Server started on port " + serverPort);

    // repeatedly wait for connections, and process
    while (true) {

      try {
        // a "blocking" call which waits until a connection is requested
        Socket clientSocket = serverSocket.accept();
        LOG.info("Accepted a connection from the client:" + clientSocket.getInetAddress());

        InputStream is = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        out.println("hello from the server: " + podName);
        out.println("Will echo your messages:");

        String s;
        while ((s = reader.readLine()) != null) {
          out.println(s);
        }

        // close IO streams, then socket
        LOG.info("Closing the connection with client");
        out.close();
        reader.close();
        clientSocket.close();

      } catch (IOException ioe) {
        throw new IllegalArgumentException(ioe);
      }
    }
  }


}
