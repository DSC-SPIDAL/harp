package edu.iu.harp.example.kmeans.k8s;

import org.apache.log4j.Logger;

import javax.rmi.CORBA.Util;
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

    LOG.info("Hello from the worker: " + Utils.getPodIndex(podName));
    LOG.info("My PodIP: " + podIP);
    LOG.info("My PodName: " + podName);
    LOG.info("numberOfWorkers: " + numberOfWorkers);

    String namespace = "default";
    Map<String, String> podMap =
        PodWatchUtils.getWorkerIPsByWatchingPodsToRunning(namespace, numberOfWorkers, 100);

    Utils.logPodMap(podMap);

//    Utils.echoServer(SERVER_PORT, podName);

    Utils.waitIndefinitely();
  }


}
