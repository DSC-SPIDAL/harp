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
import java.lang.*;

public class CenCalcTask
  implements Task<double[][], Object> {

  protected static final Log LOG =
    LogFactory.getLog(CenCalcTask.class);

  private double[][] centroids;
  private double[][] local;
  private final int cenVecSize;
  private int enclave_eff_per_thd;
  private long sgxoverheadEcall;
  private long sgxoverheadMem;
  private long sgxoverheadOcall;
  private boolean enablesimu;

  public CenCalcTask(Table<DoubleArray> cenTable,
    int cenVecSize, int enclave_eff_per_thd, boolean enablesimu) 
  {
    //record sgx centroid data size
    int sgxdatasize = 0;
    this.sgxoverheadEcall = 0;
    this.sgxoverheadMem = 0;
    this.sgxoverheadOcall = 0;
    this.cenVecSize = cenVecSize;
    this.enclave_eff_per_thd = enclave_eff_per_thd;
    this.enablesimu = enablesimu;

    centroids =
      new double[cenTable.getNumPartitions()][];
    local = new double[centroids.length][];

    //debug output centroid length
    LOG.info("Centroid length: " + cenTable.getNumPartitions());

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
    
    if (enablesimu)
    {
	    //each thread fetches its centroids data from main memory into thread enclave
	    //each thread writes back centroids data from thread enclave to main memory after all tasks
	    long ecallOverhead = (long)((Constants.Ecall + dataDoubleSizeKB(sgxdatasize)*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);
	    long ocallOverhead = (long)((Constants.Ocall + dataDoubleSizeKB(sgxdatasize)*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);

	    simuOverhead(ecallOverhead + ocallOverhead);
	    this.sgxoverheadEcall += ecallOverhead; 
	    this.sgxoverheadOcall += ocallOverhead;
    }


  }

  public long getSGXEcall() { return this.sgxoverheadEcall; }
  public long getSGXOcall() { return this.sgxoverheadOcall; }
  public long getSGXMem() { return this.sgxoverheadMem; }

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
     this.sgxoverheadMem = 0;
  }

  @Override
  public Object run(double[][] points)
    throws Exception {

    double minDistance = Double.MAX_VALUE;
    double distance = 0.0;
    int minCenParID = 0;
    int minOffset = 0;
    
    int ptearraysize = 0;
    if (points[0] != null)
	  ptearraysize = points[0].length;

    long ecallOverhead = 0;

    if (enablesimu)
    {
         //each thread fetch the data from enclave of main thread 
         int datasize = dataDoubleSizeKB(points.length*ptearraysize);
         //simulate overhead of Ecall
         ecallOverhead = (long)((Constants.Ecall + datasize*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);

         //check overhead made by page swapping (default 4K page size)
         // if (datasize > enclave_eff_per_thd*1024)
     		// ecallOverhead += (long)(Constants.swap_page_penalty*(datasize/4)*Constants.ms_per_kcycle);

         simuOverhead(ecallOverhead);
         this.sgxoverheadEcall += ecallOverhead;
    }

    long start_thd = System.currentTimeMillis();

    // -------------------- main body of computation for k-means --------------------
    for (int i=0; i < points.length; i++)
    {
	double[] pte = points[i];
	minDistance = Double.MAX_VALUE;
	minCenParID = 0;

	for (int j=0; j< centroids.length; j++)
	{
	   for(int k=0;k<centroids[j].length; k+=cenVecSize)
	   {

	   	   distance = 0.0;
		   //compute one distance for each centroids
		   for (int p=0;p<pte.length;p++)
		   {
			   // first element of centroid is jumpped
			   double diff = (pte[p] - centroids[j][k+p+1]);
			   distance += diff*diff; 
		   }

		   if (distance < minDistance)
		   {
			   minDistance = distance;
			   minCenParID = j;
			   minOffset = k; 
		   }
	   }
	   
	}

	//add count of minCenParID
	local[minCenParID][minOffset]++;
	for(int j=1; j<cenVecSize;j++)
		local[minCenParID][minOffset+j] += pte[j-1]; 
    }
    
    // ---------------------- end of computation ----------------------

    // computation time in ms
    if (enablesimu)
    {
	    long effec_time_overhead = (System.currentTimeMillis() - start_thd);
	    int datasize = dataDoubleSizeKB(points.length*ptearraysize);
	    long additional_sgx_overhead = (long)(effec_time_overhead*sgx_overhead_func(datasize));
	    additional_sgx_overhead -= ecallOverhead;
	    if (additional_sgx_overhead < 0)
		additional_sgx_overhead = 0;

	    simuOverhead(additional_sgx_overhead);
	    this.sgxoverheadMem += additional_sgx_overhead;
    }

    //no simulate overhead of Ocall
    //training data is not changed 
    
    return null;
  }

  private double sgx_overhead_func(int datasize)
  {
      //in MB
      double simu_size = (double)datasize/10.0/1024.0;
      // double ratio = (-0.000592887941*Math.pow(simu_size,3.0) + 0.03776145898*Math.pow(simu_size, 2.0) - 0.172624736*simu_size
	      // + 0.08813241271);
      double ratio = (-0.000592887941*simu_size*simu_size*simu_size + 0.03776145898*simu_size*simu_size - 0.172624736*simu_size
	      + 0.08813241271);

      return ratio;
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
