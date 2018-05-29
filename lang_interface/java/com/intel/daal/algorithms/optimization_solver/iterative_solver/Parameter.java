/* file: Parameter.java */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

/**
 * @ingroup iterative_solver
 * @{
 */
/**
 * @brief Contains classes for computing iterative solver algorithm
 */
package com.intel.daal.algorithms.optimization_solver.iterative_solver;

import com.intel.daal.services.DaalContext;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.Factory;
import com.intel.daal.algorithms.optimization_solver.sum_of_functions.Batch;
import com.intel.daal.data_management.data.Factory;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__ITERATIVE_SOLVER__PARAMETER"></a>
 * @brief Parameter of the iterative solver algorithm
 */
public class Parameter extends com.intel.daal.algorithms.Parameter {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the parameter for the iterative solver algorithm
     * @param context       Context to manage the parameter for the iterative solver algorithm
     */
    public Parameter(DaalContext context) {
        super(context);
    }

    /**
     * Constructs the parameter for the iterative solver algorithm
     * @param context    Context to manage the iterative solver algorithm
     * @param cObject    Pointer to C++ implementation of the parameter
     */
    public Parameter(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
    * Sets objective function represented as sum of functions
    * @param function Objective function represented as sum of functions
    */
    public void setFunction(Batch function) {
        _function = function;
        cSetFunction(this.cObject, function.cBatchIface);
    }

    /**
     * Gets objective function represented as sum of functions
     * @return Objective function represented as sum of functions
     */
    public Batch getFunction() {
        return _function;
    }

    /**
    * Sets the maximal number of iterations of the algorithm
    * @param nIterations The maximal number of iterations of the algorithm
    */
    public void setNIterations(long nIterations) {
        cSetNIterations(this.cObject, nIterations);
    }

    /**
     * Gets the maximal number of iterations of the algorithm
     * @return The maximal number of iterations of the algorithm
     */
    public long getNIterations() {
        return cGetNIterations(this.cObject);
    }

    /**
    * Sets the accuracy of the algorithm. The algorithm terminates when this accuracy is achieved
    * @param accuracyThreshold The accuracy of the algorithm. The algorithm terminates when this accuracy is achieved
    */
    public void setAccuracyThreshold(double accuracyThreshold) {
        cSetAccuracyThreshold(this.cObject, accuracyThreshold);
    }

    /**
     * Gets the accuracy of the algorithm. The algorithm terminates when this accuracy is achieved
     * @return The accuracy of the algorithm. The algorithm terminates when this accuracy is achieved
     */
    public double getAccuracyThreshold() {
        return cGetAccuracyThreshold(this.cObject);
    }

    /**
     * Sets the optionalResultRequired flag
     * @param flag    The flag. If true, optional result is calculated
     */
    public void setOptionalResultRequired(boolean flag) {
        cSetOptionalResultRequired(this.cObject, flag);
    }

    /**
     * Gets the optionalResultRequired flag
     * @return The flag
     */
    public boolean getOptionalResultRequired() {
        return cGetOptionalResultRequired(this.cObject);
    }

    /**
    * Sets the number of batch indices to compute the stochastic gradient.
    * If batchSize is equal to the number of terms in objective
    * function then no random sampling is performed, and all terms are
    * used to calculate the gradient. This parameter is ignored
    * if batchIndices is provided.
    * @param batchSize The number of batch indices to compute the stochastic gradient.
    * If batchSize is equal to the number of terms in objective
    * function then no random sampling is performed, and all terms are
    * used to calculate the gradient. This parameter is ignored
    * if batchIndices is provided.
    */
    public void setBatchSize(long batchSize) {
        cSetBatchSize(this.cObject, batchSize);
    }

    /**
    * Returns the number of batch indices to compute the stochastic gradient.
    * If batchSize is equal to the number of terms in objective
    * function then no random sampling is performed, and all terms are
    * used to calculate the gradient. This parameter is ignored
    * if batchIndices is provided.
    * @return The number of batch indices to compute the stochastic gradient.
    * If batchSize is equal to the number of terms in objective
    * function then no random sampling is performed, and all terms are
    * used to calculate the gradient. This parameter is ignored
    * if batchIndices is provided.
    */
    public long getBatchSize() {
        return cGetBatchSize(this.cObject);
    }

    private Batch _function;

    private native void cSetFunction(long parAddr, long function);

    private native void cSetNIterations(long parAddr, long nIterations);
    private native long cGetNIterations(long parAddr);

    private native void cSetAccuracyThreshold(long parAddr, double accuracyThreshold);
    private native double cGetAccuracyThreshold(long parAddr);

    private native void cSetOptionalResultRequired(long parAddr, boolean flag);
    private native boolean cGetOptionalResultRequired(long parAddr);

    private native void cSetBatchSize(long parAddr, long batchSize);
    private native long cGetBatchSize(long parAddr);

}
/** @} */
