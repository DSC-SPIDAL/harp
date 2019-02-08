package edu.iu.harp.example.kmeans.k8s;

import edu.iu.harp.client.SyncClient;
import edu.iu.harp.collective.Communication;
import edu.iu.harp.io.Constant;
import edu.iu.harp.io.DataMap;
import edu.iu.harp.io.EventQueue;
import edu.iu.harp.worker.WorkerInfo;
import edu.iu.harp.worker.Workers;
import edu.iu.harp.server.Server;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HarpWorker {
  protected static final Logger LOG = Logger.getLogger(HarpWorker.class);

  public static void main(String[] args) {
    String podIP = System.getenv("POD_IP");
    String podName = System.getenv("POD_NAME");
    int numberOfWorkers = Integer.parseInt(System.getenv("NUMBER_OF_WORKERS"));
    int workerID = Utils.getPodIndex(podName);

    LOG.info("Hello from the worker: " + workerID);
    LOG.info("My PodIP: " + podIP);
    LOG.info("My PodName: " + podName);
    LOG.info("numberOfWorkers: " + numberOfWorkers);

    String namespace = "default";
    Map<String, String> podMap =
        PodWatchUtils.getWorkerIPsByWatchingPodsToRunning(namespace, numberOfWorkers, 100);

    Utils.logPodMap(podMap);

    Map<Integer, List<String>> nodes = new HashMap<>();
    nodes.put(0, podMap.values().stream().collect(Collectors.toList()));

    LinkedList<Integer> nodeRackIDs = new LinkedList<>();
    nodeRackIDs.add(0);

    int noOfPhysicalNodes = nodeRackIDs.size();
    Workers workers = new Workers(nodes, nodeRackIDs, noOfPhysicalNodes, workerID);
    DataMap dataMap = new DataMap();

//    Map<Integer, WorkerInfo> workerInfos = constructWorkerInfos(podMap);
//    int masterID = 0;
//    Map<Integer, List<Integer>> rackWorkers = new HashMap<>();
//    rackWorkers.put(0, workerInfos.keySet().stream().collect(Collectors.toList()));
//    int maxID = numberOfWorkers - 1;
//    int minID = 0;
//    int middleID = maxID / 2;
//    int nextID = (workerID + 1) % numberOfWorkers;
//    Workers workers = new Workers(workerID, masterID, workerInfos, rackWorkers, maxID, minID, middleID, nextID);

    int harpPort = Constant.DEFAULT_WORKER_POART_BASE + workerID;

    Server server;
    try {
      server = new Server(
          podIP,
          harpPort,
          new EventQueue(),
          dataMap,
          workers
      );
    } catch (Exception e) {
      LOG.warn(String.format("Failed to start harp server for the worker[%d] on %s:%d "
              + "on  %s:%d", workerID, podIP, harpPort), e);
      throw new RuntimeException("Failed to start Harp Server");
    }

    SyncClient syncClient = new SyncClient(workers);
    LOG.info("Starting Harp Sync client");
    syncClient.start();

    LOG.info(String.format("Starting harp server on port : %d", harpPort));
    server.start();
    LOG.info(String.format("Harp server started. %s:%d ", podIP, harpPort));

    try {
      LOG.info("Trying master barrier");
      doMasterBarrier("start-worker", "handshake", dataMap, workers);
      LOG.info("Master barrier done");
    } catch (IOException e) {
      LOG.warn("Failed to do master barrier", e);
      server.stop();
      syncClient.stop();
      throw new RuntimeException("Failed to do master barrier");
    }

    // do computations here

    //stopping servers, releasing resources
    LOG.info("Execution completed. Shutting harp Sync Client down....");
    syncClient.stop();
    LOG.info("Harp Sync Client stopped.");
    LOG.info("Shutting harp server down....");
    server.stop(true);
    LOG.info("Harp server stopped.");


    Utils.waitIndefinitely();
  }

  private static void doMasterBarrier(String contextName, String operationName,
                                      DataMap dataMap, Workers workers) throws IOException {
    boolean successful = Communication.barrier(contextName, operationName, dataMap, workers);
    dataMap.cleanOperationData(contextName, operationName);
    if (!successful) {
      throw new IOException("Failed to do master barrier");
    }
  }



  public static Map<Integer, WorkerInfo> constructWorkerInfos(Map<String, String> podMap) {

    int workerPortBase = Constant.DEFAULT_WORKER_POART_BASE;
    int rack = 0;

    Map<Integer, WorkerInfo> workerInfos = new HashMap<>();
    for (Map.Entry<String, String> entry: podMap.entrySet()) {
      int workerID = Utils.getPodIndex(entry.getKey());
      String workerIP = entry.getValue();
      int workerPort = workerPortBase + workerID;
      WorkerInfo workerInfo = new WorkerInfo(workerID, workerIP, workerPort, rack);
      workerInfos.put(workerID, workerInfo);
    }

    return workerInfos;
  }


}
