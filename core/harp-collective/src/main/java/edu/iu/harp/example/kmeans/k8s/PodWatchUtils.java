package edu.iu.harp.example.kmeans.k8s;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.util.Watch;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PodWatchUtils {
  private static final Logger LOG = Logger.getLogger(PodWatchUtils.class.getName());

  public static CoreV1Api coreApi;
  public static ApiClient apiClient;

  private PodWatchUtils() {
  }

  public static void createApiInstances() {

    try {
      apiClient = io.kubernetes.client.util.Config.defaultClient();
      apiClient.getHttpClient().setReadTimeout(0, TimeUnit.MILLISECONDS);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception when creating ApiClient: ", e);
      throw new RuntimeException(e);
    }
    Configuration.setDefaultApiClient(apiClient);

    coreApi = new CoreV1Api(apiClient);
  }

  /**
   * watch the worker pods until they are Running and get their IP addresses
   * we assume that workers have the unique twister2-role label and value pair
   * we get the ip addresses of all workers including the worker pod calling this method
   *
   * getting IP addresses by list method does not work,
   * since uninitialized pod IPs are not returned by list method
   *
   * return null, if it can not get the full list
   */
  public static Map<String, String> getWorkerIPsByWatchingPodsToRunning(String namespace,
                                                                      int numberOfPods,
                                                                      int timeout) {

    if (apiClient == null || coreApi == null) {
      createApiInstances();
    }

    String podLabel = "role=harp-worker";
    String podPhase = "Running";

    LOG.info("Starting the watcher for the worker pods: " + namespace + ", " + ", " + podLabel);
    Integer timeoutSeconds = timeout;
    Watch<V1Pod> watch = null;

    try {
      watch = Watch.createWatch(
          apiClient,
          coreApi.listNamespacedPodCall(namespace, null, null, null, null, podLabel,
              null, null, timeoutSeconds, Boolean.TRUE, null, null),
          new TypeToken<Watch.Response<V1Pod>>() {
          }.getType());

    } catch (ApiException e) {
      String logMessage = "Exception when watching the pods to get the IPs: \n"
          + "exCode: " + e.getCode() + "\n"
          + "responseBody: " + e.getResponseBody();
      LOG.log(Level.SEVERE, logMessage, e);
      throw new RuntimeException(e);
    }

    int eventCounter = 0;
    LOG.finest("Getting watcher events.");
    Map<String, String> podMap = new TreeMap<>();

    for (Watch.Response<V1Pod> item : watch) {
      if (item.object != null
          && podPhase.equalsIgnoreCase(item.object.getStatus().getPhase())) {

        String podName = item.object.getMetadata().getName();
        String podIP = item.object.getStatus().getPodIP();

        LOG.info(eventCounter++ + "-Received pod Running event: "
            + item.object.getMetadata().getName() + ", "
            + item.object.getStatus().getPodIP() + ", "
            + item.object.getStatus().getPhase());

        if (!podMap.containsKey(podName)) {
          podMap.put(podName, podIP);
        }
        if (podMap.size() == numberOfPods) {
          break;
        }
      }
    }

    try {
      watch.close();
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Exception closing watcher.", e);
    }

    if (podMap.size() == numberOfPods) {
      return podMap;
    } else {
      StringBuffer toLog = new StringBuffer();
      for (Map.Entry<String, String> entry: podMap.entrySet()) {
        toLog.append(entry.getKey() + ": " + entry.getValue() + "\n");
      }

      LOG.severe("Could not get IPs of all worker pods. Retrieved IPs:\n" + toLog.toString());
      return null;
    }

  }

}
