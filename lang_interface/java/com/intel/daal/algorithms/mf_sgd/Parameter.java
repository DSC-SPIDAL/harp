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
 * @brief Contains classes for computing mf_sgd algorithm
 */
package com.intel.daal.algorithms.mf_sgd;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__PARAMETER"></a>
 * @brief Parameter of the mf_sgd algorithm
 */
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the parameter for mf_sgd algorithm
     * @param context       Context to manage mf_sgd algorithm
     */
    public Parameter(DaalContext context) {
        super(context);
    }

    /**
     * Constructs the parameter for mf_sgd algorithm
     * @param context    Context to manage the mf_sgd algorithm
     * @param cObject    Pointer to C++ implementation of the parameter
     */
    public Parameter(DaalContext context, long cObject) {
        super(context, cObject);
    }

	
	/**
	 * @brief Set up the parameters 
	 *
	 * @param learningRate  the rate of learning by SGD  
	 * @param lambda  the lambda parameter in standard SGD
	 * @param Dim_r  the feature dimension of model W and H
	 * @param Dim_w  the row num of model W
	 * @param Dim_h  the column num of model H
	 * @param iteration  the iterations of SGD
	 * @param thread_num  specify the threads used by TBB
	 * @param tbb_grainsize  specify the grainsize for TBB parallel_for
	 * @param Avx_explicit  specify whether use explicit Avx instructions
	 *
	 * @return 
	 */
    public void set(double learningRate, double lambda, long Dim_r, long Dim_w, long Dim_h, int iteration, int thread_num, int tbb_grainsize, int Avx_explicit) {
        cSetParameters(this.cObject,learningRate, lambda, Dim_r, Dim_w,  Dim_h, iteration, thread_num, tbb_grainsize, Avx_explicit );
    }

    /**
     * @brief control the percentage of tasks to execute
     *
     * @param ratio
     *
     * @return 
     */
    public void setRatio(double ratio) {
        cSetRatio(this.cObject, ratio);
    }

    /**
     * @brief set the id of training iteration, used in distributed mode
     *
     * @param itr
     *
     * @return 
     */
    public void setIteration(int itr) {
        cSetIteration(this.cObject, itr);
    }

    /**
     * @brief set the id of inner training iteration, used in distributed mode, e.g., model rotation
     *
     * @param innerItr
     *
     * @return 
     */
    public void setInnerItr(int innerItr) {
        cSetInnerItr(this.cObject, innerItr);
    }

    /**
     * @brief total num of inner training iteration, used in distributed mode, e.g., model rotation
     *
     * @param innerNum
     *
     * @return 
     */
    public void setInnerNum(int innerNum) {
        cSetInnerNum(this.cObject, innerNum);
    }

    /**
     * @brief set up the flag to train or test on dataset
     * 1 for train and 0 for test
     *
     * @param isTrain
     *
     * @return 
     */
    public void setIsTrain(int isTrain) {
        cSetIsTrain(this.cObject, isTrain);
    }

    /**
     * @brief set up the timer (seconds) 
     * for distributed mode
     *
     * @param timeout
     *
     * @return 
     */
    public void setTimer(double timeout) {
        cSetTimer(this.cObject, timeout);
    }

    /**
     * @brief set up the choice of re-order
     * 1: TBB with re-order
     * 2: OpenMP with re-order
     * 3: OpenMP -no-re-order
     * default: TBB -no-re-order
     *
     * @param isReorder
     *
     * @return 
     */
    public void setIsReorder(int isReorder) {
        cSetIsReorder(this.cObject, isReorder);
    }

    /**
     * @brief set up the number of absent points in test dataset
     *
     * @param absentNum
     *
     * @return 
     */
    public void setAbsentTestNum(int absentNum) {
        cSetAbsentTestNum(this.cObject, absentNum);
    }

    public void setTestV(int testV) {

        cSetTestV(this.cObject, testV);
    }

    public int GetAbsentTestNum() {
        return cGetAbsentTestNum(this.cObject);
    }

    public int GetTestV() {

        return cGetTestV(this.cObject);
    }

    public void setIsSGD2(int isSGD2) {
        cSetIsSGD2(this.cObject, isSGD2);
    }

    
    /**
     * @brief get the actual trained num of points in
     * each iteration if the timer is used
     *
     * @return 
     */
    public long GetTrainedNumV() {

        return cGetTrainedNumV(this.cObject);

    }

    public void freeData() {
        cFreeData(this.cObject);
    }

    public long GetComputeTaskTime() {
        return cGetComputeTaskTime(this.cObject);
    }

    public void ResetComputeTaskTime() {
        cResetComputeTaskTime(this.cObject);
    }

    public long GetDataConvertTime() {
        return cGetDataConvertTime(this.cObject);
    }

    public void ResetDataConvertTime() {
        cResetDataConvertTime(this.cObject);
    }

    public long GetItrTimeStamp() {
        return cGetItrTimeStamp(this.cObject);
    }

    public void ResetItrTimeStamp() {
        cResetItrTimeStamp(this.cObject);
    }

    public double GetPeakMem() {
        return cGetPeakMem(this.cObject);
    }

    private native void cSetParameters(long parAddr, double learningRate, double lambda, long Dim_r, long Dim_w, long Dim_h, int iteration, int thread_num, int tbb_grainsize, 
			int Avx_explicit );

    private native void cSetRatio(long parAddr, double ratio);

    private native void cSetIteration(long parAddr, int itr);

    private native void cSetInnerItr(long parAddr, int innerItr);

    private native void cSetInnerNum(long parAddr, int innerNum);

    private native void cSetIsTrain(long parAddr, int isTrain);

    private native void cSetTimer(long parAddr, double timeout);

    private native void cSetIsReorder(long parAddr, int isReorder);

    private native void cSetAbsentTestNum(long parAddr, int absentNum);

    private native int cGetAbsentTestNum(long parAddr);

    private native void cSetTestV(long parAddr, int testV);

    private native int cGetTestV(long parAddr);

    private native long cGetTrainedNumV(long parAddr);

    private native void cSetIsSGD2(long parAddr, int isSGD2);

    private native void cFreeData(long parAddr);

    private native long cGetComputeTaskTime(long parAddr);

    private native void cResetComputeTaskTime(long parAddr);

    private native long cGetDataConvertTime(long parAddr);

    private native void cResetDataConvertTime(long parAddr);

    private native long cGetItrTimeStamp(long parAddr);

    private native void cResetItrTimeStamp(long parAddr);

    private native double cGetPeakMem(long parAddr);

}
