package edu.iu.harp.example.kmeans.k8s;

import org.apache.log4j.Logger;

public class HelloWorld {
  protected static final Logger LOG = Logger.getLogger(HelloWorld.class);

  public static void main(String[] args) {
    String podIP = System.getenv("POD_IP");
    String podName = System.getenv("POD_NAME");
    String numberOfWorkers = System.getenv("NUMBER_OF_WORKERS");

    LOG.info("Hello from the worker: " + getPodIndex(podName));
    LOG.info("My PodIP: " + podIP);
    LOG.info("My PodName: " + podName);
    LOG.info("numberOfWorkers: " + numberOfWorkers);

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

}
