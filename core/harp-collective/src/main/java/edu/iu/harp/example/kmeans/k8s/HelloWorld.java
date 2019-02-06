package edu.iu.harp.example.kmeans.k8s;

import org.apache.log4j.Logger;

public class HelloWorld {
  protected static final Logger LOG = Logger.getLogger(HelloWorld.class);

  public static void main(String[] args) {
    LOG.info("Hello from a worker. ");

    String podIP = System.getenv("POD_IP");
    LOG.info("My PodIP: " + podIP);

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

}
