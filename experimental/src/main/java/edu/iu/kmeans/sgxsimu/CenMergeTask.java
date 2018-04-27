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
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.schdynamic.Task;

import java.util.Arrays;
import java.util.List;

public class CenMergeTask implements
  Task<Partition<DoubleArray>, Object> {
  private final List<CenCalcTask> cenCalcTasks;

  public CenMergeTask(
    List<CenCalcTask> cenCalcTasks) {
    this.cenCalcTasks = cenCalcTasks;
  }

  @Override
  public Object
    run(Partition<DoubleArray> cenPartition)
      throws Exception {
    int partitionID = cenPartition.id();
    double[] centroids = cenPartition.get().get();
    int cenSize = cenPartition.get().size();
    Arrays.fill(centroids, 0.0);

    //simulate overhead of Ecall (from main memory cenPartition to thread enclave)
    int datasize = dataDoubleSizeKB(centroids.length);
    long ecallOverhead = (long)((Constants.Ecall + datasize*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);
    if (Constants.enablesimu)
	 simuOverhead(ecallOverhead);

    // It is safe to iterate concurrently
    // because each task has its own iterator
    for (CenCalcTask task : cenCalcTasks) 
    {

	    double[] localCentroids =
		    task.getLocal()[partitionID];


	    // simulate overhead of Ecall ( from main memory localCentroids to thread enclave )
	    datasize = dataDoubleSizeKB(localCentroids.length);
	    ecallOverhead = (long)((Constants.Ecall + datasize*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);
	    if (Constants.enablesimu)
		    simuOverhead(ecallOverhead);

	    //computing: reduction local centroids to model
	    for (int i = 0; i < cenSize; i++) {
		    centroids[i] += localCentroids[i];
		    localCentroids[i] = 0.0;
	    }

	    
    }

    //simulate overhead of Ocall write cenPartition back to main memory
    datasize = dataDoubleSizeKB(centroids.length);
    long ocallOverhead = (long)((Constants.Ocall + datasize*Constants.cross_enclave_per_kb)*Constants.ms_per_kcycle);
    if (Constants.enablesimu)
	    simuOverhead(ocallOverhead);

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
