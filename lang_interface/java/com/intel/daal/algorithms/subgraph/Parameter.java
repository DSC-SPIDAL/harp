/* file: Parameter.java */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
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
*******************************************************************************/

/**
 * @brief Contains classes for computing subgraph algorithm
 */
package com.intel.daal.algorithms.subgraph;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__subgraph__PARAMETER"></a>
 * @brief Parameter of the subgraph_batch algorithm
 */
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the parameter for subgraph_batch algorithm
     * @param context       Context to manage subgraph_batch algorithm
     */
    public Parameter(DaalContext context) {
        super(context);
    }

    /**
     * Constructs the parameter for subgraph_batch algorithm
     * @param context    Context to manage the subgraph_batch algorithm
     * @param cObject    Pointer to C++ implementation of the parameter
     */
    public Parameter(DaalContext context, long cObject) {
        super(context, cObject);
    }

	
	/**
	 * @brief Set up the parameters 
	 *
	 * @param iteration  the iterations of SGD
	 * @param thread_num  specify the threads used by TBB
	 *
	 * @return 
	 */

    public void setParameters(int thread_num,
                              int core_num,
                              int tpc,
                              int affinity,
                              int nbrtasklen,
                              int verbose) {

        cSetParameters(this.cObject, thread_num, core_num, tpc, affinity, nbrtasklen, verbose);
    }

    public void setSubItr(int sub_itr){
        cSetSubItr(this.cObject, sub_itr);
    }

    /**
     * @brief set the id of training iteration, used in distributed mode
     *
     * @param itr
     *
     * @return 
     */
    public void setStage(int stage) {
        cSetStage(this.cObject, stage);
    }

    public void setNbrTaskLen(int len) {
        cSetNbrTaskLen(this.cObject, len);
    }

    public void setPipId(int id) {
        cSetPipId(this.cObject, id);
    }

    public double getTotalCounts() {
        return cGetTotalCounts(this.cObject);
    }

    public double getUpdateCounts() {
        return cGetUpdateCounts(this.cObject);
    }

    public void setOmpSchedule(String opt)
    {
        cSetOmpSchedule(this.cObject, opt);
    }

    private native void cSetParameters(long parAddr, int thread_num, int core_num, int tpc, int affinity, int nbrtasklen, int verbose);
    private native void cSetStage(long parAddr, int stage);
    private native void cSetNbrTaskLen(long parAddr, int len);
    private native void cSetPipId(long parAddr, int id);
    private native void cSetSubItr(long parAddr, int sub_itr);
    private native double cGetTotalCounts(long parAddr);
    private native double cGetUpdateCounts(long parAddr);
    private native void cSetOmpSchedule(long parAddr, String opt);

}
