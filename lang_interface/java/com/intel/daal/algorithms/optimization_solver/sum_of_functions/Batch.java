/* file: Batch.java */
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
 * @defgroup sum_of_functions Sum of Functions
 * @brief Contains classes for computing the Sum of functions
 * @ingroup objective_function
 * @{
 */
/**
 * @defgroup sum_of_functions_batch Batch
 * @ingroup sum_of_functions
 * @{
 */
/**
 * @brief Contains classes for the objective functions that could be represented as a sum of functions
 */
package com.intel.daal.algorithms.optimization_solver.sum_of_functions;

import com.intel.daal.algorithms.AnalysisBatch;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.services.DaalContext;
import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.optimization_solver.objective_function.Result;
import com.intel.daal.algorithms.optimization_solver.sum_of_functions.Input;
import com.intel.daal.algorithms.optimization_solver.sum_of_functions.Parameter;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__OPTIMIZATION_SOLVER__SUM_OF_FUNCTIONS__BATCH"></a>
 * @brief Computes the Sum of functions algorithm in the batch processing mode
 * <!-- \n<a href="DAAL-REF-SUM_OF_FUNCTIONS-ALGORITHM">Sum of functions algorithm description and usage models</a> -->
 */
public abstract class Batch extends com.intel.daal.algorithms.optimization_solver.objective_function.Batch {
    public long cBatchIface;    /*!< Pointer to the inner implementation of the service callback functionality */
    public Input          input;     /*!< %Input data */
    public Parameter  parameter;     /*!< Parameters of the algorithm */

    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    /**
     * Constructs the Sum of functions algorithm in the batch processing mode
     * @param context  Context to manage the Sum of functions algorithm
     * @param numberOfTerms Number of terms in the objectiove function that can be represent as sum
     */
    public Batch(DaalContext context, long numberOfTerms) {
        super(context);
        this.cBatchIface = cInitBatchIface(numberOfTerms);
    }

    /**
     * Constructs the Sum of functions algorithm by copying input objects and parameters of
     * another Sum of functions algorithm
     * @param context  Context to manage the Sum of functions algorithm
     * @param other    An algorithm to be used as the source to initialize the input objects
     *                 and parameters of this algorithm
     */
    public Batch(DaalContext context, Batch other) {
        super(context);
        this.cBatchIface = cInitBatchIface(other.parameter.getNumberOfTerms());
    }

    /**
     * Registers user-allocated memory to store the results of computing the Sum of functions
     * in the batch processing mode
     * @param result Structure to store results of computing the Sum of functions
     */
    public void setResult(Result result) {
        cSetResult(cBatchIface, result.getCObject());
    }

    /**
     * Return the result of the algorithm
     * @return Result of the algorithm
     */
    public Result getResult() {
        return new Result(getContext(), cGetResult(cBatchIface));
    }

    /**
     * Sets correspond pointers to the native side. Must be called in inherited class constructor after input and parameter initializations.
     */
    public void setPointersToIface() {
        cSetPointersToIface(cBatchIface, input.getCObject(), parameter.cObject);
    }

    /**
    * Releases the memory allocated for the native algorithm object
    */
    @Override
    public void dispose() {
        super.dispose();
        if (cBatchIface != 0) {
            cDispose(cBatchIface);
            cBatchIface = 0;
        }
    }

    /**
     * Returns the newly allocated Sum of functions algorithm
     * with a copy of input objects and parameters of this Sum of functions algorithm
     * @param context Context to manage the Sum of functions algorithm
     *
     * @return The newly allocated algorithm
     */
    @Override
    public abstract Batch clone(DaalContext context);

    private native void cDispose(long cBatchIface);
    private native long cInitBatchIface(long numberOfTerms);
    private native void cSetPointersToIface(long cBatchIface, long cInput, long cParameter);
}
/** @} */
/** @} */
