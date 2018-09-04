package edu.iu.kmeans.allreduce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.kmeans.common.KMeansConstants;

/**
 * The idea behind this implementation is to use
 */
public class KmeansMapper extends CollectiveMapper<String, String, Object, Object> {
  // number of features in a point
  private int dimension;
  // number of iterations
  private int iteration;
  // number of points
  private int numPoints;
  private double MSE;

  /**
   * This is the initialization function of the K-Means Mapper. Here we can read the parameters
   * as in a normal Hadoop job.
   * @param context the context
   */
  @Override
  protected void setup(Context context) {
    LOG.info("start setup" + new SimpleDateFormat("yyyyMMdd_HHmmss")
        .format(Calendar.getInstance().getTime()));

    long startTime = System.currentTimeMillis();
    Configuration configuration = context.getConfiguration();
    dimension = configuration.getInt(KMeansConstants.VECTOR_SIZE, 20);
    iteration = configuration.getInt(KMeansConstants.NUM_ITERATONS, 1);
    long endTime = System.currentTimeMillis();
    LOG.info("config done (ms) :" + (endTime - startTime));
  }

  /**
   * This is the entry point of each map task. Here we will do the parallel computation
   * @param reader reader
   * @param context context
   * @throws IOException if an error occurs
   * @throws InterruptedException if an error occurs
   */
  protected void mapCollective(KeyValReader reader, Context context)
      throws IOException, InterruptedException {
    LOG.info("Start collective mapper.");
    long startTime = System.currentTimeMillis();
    List<String> pointFiles = new ArrayList<String>();

    // first lets read the name of the files containing the points
    while (reader.nextKeyValue()) {
      String key = reader.getCurrentKey();
      String value = reader.getCurrentValue();
      LOG.info("Key: " + key + ", Value: " + value);
      pointFiles.add(value);
    }
    Configuration conf = context.getConfiguration();

    // lets run the K-Means calculation
    runKmeans(pointFiles, conf, context);
    LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
  }

  /**
   * Load centroids, for every partition in the centroid table, we will use the last element to
   * store the number of points which are clustered to the particular partitionID.
   */
  private void runKmeans(List<String> fileNames, Configuration conf, Context context)
      throws IOException {

    Table<DoubleArray> cenTable =
        new Table<>(0, new DoubleArrPlus());
    if (this.isMaster()) {
      loadCentroids(cenTable, dimension, conf.get(KMeansConstants.CFILE), conf);
    }

    LOG.info("After loading centroids");
    printTable(cenTable);

    // broadcast centroids
    broadcastCentroids(cenTable);

    // after broadcasting
    LOG.info("After brodcasting centroids");
    printTable(cenTable);

    // load data
    ArrayList<DoubleArray> dataPoints = loadData(fileNames, dimension, conf);
    numPoints = dataPoints.size();

    Table<DoubleArray> previousCenTable;
    // iterations
    for (int iter = 0; iter < iteration; iter++) {
      previousCenTable = cenTable;
      cenTable = new Table<>(0, new DoubleArrPlus());

      LOG.info("Iteraton No." + iter);

      // compute new partial centroid table using
      // previousCenTable and data points
      MSE = computation(cenTable, previousCenTable,
          dataPoints);

      // AllReduce;
      allreduce("main", "allreduce_" + iter,
          cenTable);
      // we can calculate new centroids
      calculateCentroids(cenTable);

      printTable(cenTable);
    }

    // output results
    calcMSE(conf);
    if (this.isMaster()) {
      outputCentroids(cenTable, context);
    }

  }

  /**
   * Broadcast the centroids from master to others
   * @param cenTable centroids
   * @throws IOException if fail to broadcast
   */
  private void broadcastCentroids(Table<DoubleArray> cenTable) throws IOException {
    // broadcast centroids
    boolean isSuccess = false;
    try {
      isSuccess = broadcast("main", "broadcast-centroids",
              cenTable, this.getMasterID(), false);
    } catch (Exception e) {
      LOG.error("Fail to bcast.", e);
    }
    if (!isSuccess) {
      throw new IOException("Fail to bcast");
    }
  }

  /**
   * Main computation of K-Means, go through every point and assigned it to the nearest centroid
   * We put the sum of the points into the new centroid and the number of points assigned
   * @param cenTable centroids
   * @param previousCenTable previous centroids
   * @param dataPoints data points
   * @return the error, this can be used to terminate the calculation
   */
  private double computation(Table<DoubleArray> cenTable, Table<DoubleArray> previousCenTable,
                             ArrayList<DoubleArray> dataPoints) {
    double err = 0;
    for (DoubleArray aPoint : dataPoints) {
      // for each data point, find the nearest
      // centroid
      double minDist = -1;
      double tempDist;
      int nearestPartitionID = -1;
      for (Partition ap : previousCenTable.getPartitions()) {
        DoubleArray aCentroid = (DoubleArray) ap.get();
        tempDist = calcEucDistSquare(aPoint, aCentroid, dimension);
        if (minDist == -1 || tempDist < minDist) {
          minDist = tempDist;
          nearestPartitionID = ap.id();
        }
      }
      err += minDist;

      // for the certain data point, found the
      // nearest centroid.
      // add the data to a new cenTable.
      double[] partial = new double[dimension + 1];
      for (int j = 0; j < dimension; j++) {
        partial[j] = aPoint.get()[j];
      }
      partial[dimension] = 1;

      if (cenTable.getPartition(nearestPartitionID) == null) {
        Partition<DoubleArray> tmpAp = new Partition<DoubleArray>(nearestPartitionID, new DoubleArray(
                partial, 0, dimension + 1));
        cenTable.addPartition(tmpAp);
      } else {
        Partition<DoubleArray> apInCenTable = cenTable.getPartition(nearestPartitionID);
        for (int i = 0; i < dimension + 1; i++) {
          apInCenTable.get().get()[i] +=
              partial[i];
        }
      }
    }
    LOG.info("Errors: " + err);
    return err;
  }

  /**
   * Lets calculate the error
   * @param conf configuration
   * @throws IOException error
   */
  private void calcMSE(Configuration conf) throws IOException {
    double[] arrMSE = new double[2];
    arrMSE[0] = numPoints;
    arrMSE[1] = MSE;
    Table<DoubleArray> mseTable = new Table<>(0, new DoubleArrPlus());
    Partition<DoubleArray> tmpAp = new Partition<>(0, new DoubleArray(
        arrMSE, 0, 2));
    mseTable.addPartition(tmpAp);

    // allreduce
    allreduce("main", "allreduce-mse", mseTable);

    //get result
    double[] finalArrMSE = mseTable.getPartition(0).get().get();
    double finalMSE = finalArrMSE[1] / finalArrMSE[0];

    mseTable.release();

    //save
    if (this.isMaster()) {
      FileSystem fs = FileSystem.get(conf);
      Path path = new Path(conf.get(KMeansConstants.WORK_DIR) + "/evaluation");
      FSDataOutputStream output = fs.create(path, true);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
      writer.write("MSE : " + finalMSE + "\n");
      writer.close();
    }
  }

  // output centroids
  private void outputCentroids(Table<DoubleArray> cenTable, Context context) {
    StringBuilder output = new StringBuilder();
    for (Partition<DoubleArray> ap : cenTable.getPartitions()) {
      double res[] = ap.get().get();
      for (int i = 0; i < dimension; i++) {
        output.append(res[i]).append("\t");
      }
      output.append("\n");
    }
    try {
      context.write(null, new Text(output.toString()));
    } catch (IOException | InterruptedException e) {
      LOG.error("Failed to write the output", e);
    }
  }

  /**
   * Calculate the new centroids  by deviding each value by the number of points assigned to
   * that centroid
   * @param cenTable centroid table
   */
  private void calculateCentroids(Table<DoubleArray> cenTable) {
    for (Partition<DoubleArray> partialCenTable : cenTable.getPartitions()) {
      double[] doubles = partialCenTable.get().get();
      for (int h = 0; h < dimension; h++) {
        doubles[h] /= doubles[dimension];
      }
      doubles[dimension] = 0;
    }
    LOG.info("after calculate new centroids");
    printTable(cenTable);
  }

  // calculate Euclidean distance.
  private double calcEucDistSquare(DoubleArray aPoint, DoubleArray otherPoint, int vectorSize) {
    double dist = 0;
    for (int i = 0; i < vectorSize; i++) {
      dist += Math.pow(aPoint.get()[i] - otherPoint.get()[i], 2);
    }
    return Math.sqrt(dist);
  }

  /**
   * load centroids from HDFS, we read each centroind as a separate partition of the
   * partition table. We also allocate one element more that the number of features to accomadate
   * the number of points assigned to a centroid
   */
  private void loadCentroids(Table<DoubleArray> cenTable, int vectorSize,
                             String cFileName, Configuration configuration) throws IOException {
    Path cPath = new Path(cFileName);
    FileSystem fs = FileSystem.get(configuration);
    FSDataInputStream in = fs.open(cPath);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String line;
    String[] vector;
    int partitionId = 0;
    while ((line = br.readLine()) != null) {
      vector = line.split("\\s+");
      if (vector.length != vectorSize) {
        throw new RuntimeException("Errors while loading centroids .");
      } else {
        double[] aCen = new double[vectorSize + 1];
        for (int i = 0; i < vectorSize; i++) {
          aCen[i] = Double.parseDouble(vector[i]);
        }
        aCen[vectorSize] = 0;
        Partition<DoubleArray> ap = new Partition<>(partitionId, new DoubleArray(aCen, 0,
            vectorSize + 1));
        cenTable.addPartition(ap);
        partitionId++;
      }
    }
  }

  // load data form HDFS
  private ArrayList<DoubleArray> loadData(
      List<String> fileNames, int vectorSize,
      Configuration conf) throws IOException {
    ArrayList<DoubleArray> data = new ArrayList<>();
    for (String filename : fileNames) {
      FileSystem fs = FileSystem.get(conf);
      Path dPath = new Path(filename);
      FSDataInputStream in = fs.open(dPath);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String line;
      String[] vector;
      while ((line = br.readLine()) != null) {
        vector = line.split("\\s+");
        if (vector.length != vectorSize) {
          throw new RuntimeException("Errors while loading data.");
        } else {
          double[] aDataPoint = new double[vectorSize];
          for (int i = 0; i < vectorSize; i++) {
            aDataPoint[i] = Double.parseDouble(vector[i]);
          }
          DoubleArray da = new DoubleArray(aDataPoint, 0, vectorSize);
          data.add(da);
        }
      }
    }
    return data;
  }

  // for testing
  private void printTable(Table<DoubleArray> dataTable) {
    for (Partition<DoubleArray> ap : dataTable.getPartitions()) {
      double res[] = ap.get().get();
      System.out.print("ID: " + ap.id() + ":");
      for (double re : res) {
        System.out.print(re + "\t");
      }
      System.out.println();
    }
  }
}
