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

package edu.iu.kmeans.sgxsimu;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CenCalcTask2
  implements Task<double[][], Object> {

  protected static final Log LOG =
    LogFactory.getLog(CenCalcTask.class);

  private double[][] centroids;
  private double[][] local;
  private final int cenVecSize;
  private long sgxoverheadEcall;
  private long sgxoverheadOcall;

  public CenCalcTask2(Table<DoubleArray> cenTable,
    int cenVecSize) 
  {
    //record sgx centroid data size
    int sgxdatasize = 0;
    this.sgxoverheadEcall = 0;
    this.sgxoverheadOcall = 0;

    centroids =
      new double[cenTable.getNumPartitions()][];
    local = new double[centroids.length][];
    for (Partition<DoubleArray> partition : cenTable
      .getPartitions()) {
      int partitionID = partition.id();
      DoubleArray array = partition.get();
      centroids[partitionID] = array.get();
      local[partitionID] =
        new double[array.size()];
	
      //record sgx centroid data size
      sgxdatasize += array.size();
    }
    
    if (Constants.enablesimu)
    {
	    //each thread fetches its centroids data from main memory into thread enclave
	    //each thread writes back centroids data from thread enclave to main memory after all tasks
	    long ecallOverhead = (long)((Constants.Ecall + dataDoubleSizeKB(sgxdatasize)*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);
	    long ocallOverhead = (long)((Constants.Ocall + dataDoubleSizeKB(sgxdatasize)*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);

	    simuOverhead(ecallOverhead + ocallOverhead);
	    this.sgxoverheadEcall += ecallOverhead; 
	    this.sgxoverheadOcall += ocallOverhead;
    }

    this.cenVecSize = cenVecSize;

  }

  public long getSGXEcall() { return this.sgxoverheadEcall; }
  public long getSGXOcall() { return this.sgxoverheadOcall; }

  public void
    update(Table<DoubleArray> cenTable) {
    for (Partition<DoubleArray> partition : cenTable
      .getPartitions()) {
      int partitionID = partition.id();
      DoubleArray array = partition.get();
      centroids[partitionID] = array.get();
    }
  }

  public double[][] getLocal() {
    return local;
  }

  public void resetSGX() {
     this.sgxoverheadEcall = 0;
     this.sgxoverheadOcall = 0;
  }

  @Override
  public Object run(double[][] points)
    throws Exception {

    double minDistance = Double.MAX_VALUE;
    double distance = 0.0;
    int minCenParID = 0;
    
    int ptearraysize = 0;
    if (points[0] != null)
	  ptearraysize = points[0].length;
    
    if (Constants.enablesimu)
    {
	    //each thread fetch the data from enclave of main thread 
	    int datasize = dataDoubleSizeKB(points.length*ptearraysize);
	    //simulate overhead of Ecall
	    long ecallOverhead = (long)((Constants.Ecall + datasize*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);

	    simuOverhead(ecallOverhead);
	    this.sgxoverheadEcall += ecallOverhead;
    }

    // -------------------- main body of computation for k-means --------------------
    for (int i=0; i < points.length; i++)
    {
	double[] pte = points[i];
	minDistance = Double.MAX_VALUE;
	minCenParID = 0;

	for (int j=0; j< centroids.length; j++)
	{
	   distance = 0.0;
	   //compute one distance for each centroids
	   for (int k=0;k<pte.length;k++)
	   {
		   double diff = (pte[k] - centroids[j][k]);
		   distance += diff*diff; 
	   }

	   if (distance < minDistance)
	   {
		   minDistance = distance;
		   minCenParID = j;
	   }
	}

	//add count of minCenParID
	local[minCenParID][0]++;
	for(int j=1; j<cenVecSize;j++)
		local[minCenParID][j] += pte[j-1]; 
    }

    // for (int i = 0; i < points.length;) {
    //
    //   double minDistance = Double.MAX_VALUE;
    //   int minCenParID = 0;
    //   int minOffset = 0;
    //
    //   for (int j = 0; j < centroids.length; j++) {
    //     for (int k = 0; k < local[j].length;) {
    //       int pStart = i;
    //       k++;
    //
    //       double distance = 0.0;
    //       for (int l = 1; l < cenVecSize; l++) {
    //         double diff = (points[pStart++]
    //           - centroids[j][l]);
    //         distance += diff * diff;
    //       }
    //
	//   k+= (cenVecSize - 1);
    //       if (distance < minDistance) {
    //         minDistance = distance;
    //         minCenParID = j;
    //         minOffset = k - cenVecSize;
    //       }
    //     }
    //   }
    //   // Count + 1
    //   local[minCenParID][minOffset++]++;
    //   // Add the point
    //   for (int j = 1; j < cenVecSize; j++) {
    //     local[minCenParID][minOffset++] +=
    //       points[i++];
    //   }
    //
    // }

    // ---------------------- end of computation ----------------------

    //no simulate overhead of Ocall
    //training data is not changed 
    
    return null;
  }

  /**
   * @brief calculate the data size in and out enclave (KB)
   * double precision assumed
   *
   * @param size
   *
   * @return 
   */
  private int dataDoubleSizeKB(int size)
  {
     return size*Double.SIZE/Byte.SIZE/1024;
  }

  /**
   * @brief simulate the overhead (ms)
   * of a SGX-related operation
   *
   * @param time
   *
   * @return 
   */
  private void simuOverhead(long time)
  {
	  try{
		  Thread.sleep(time);
	  }catch (Exception e)
	  {
		  System.out.println(e);
	  }
  }
}
